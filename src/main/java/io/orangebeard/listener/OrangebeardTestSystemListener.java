package io.orangebeard.listener;

import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.Assertion;
import fitnesse.testsystems.ExceptionResult;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestResult;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystem;
import fitnesse.testsystems.TestSystemListener;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPageProperty;
import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.FinishTestItem;
import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.StartTestItem;
import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.client.entity.Status;
import io.orangebeard.client.entity.TestItemType;
import io.orangebeard.listener.helper.OrangebeardLogger;
import io.orangebeard.listener.helper.OrangebeardTableLogParser;
import io.orangebeard.listener.helper.TestPageHelper;
import io.orangebeard.listener.helper.ToolchainRunningContext;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.orangebeard.listener.helper.TestPageHelper.getTestName;
import static java.util.Objects.requireNonNull;

@NoArgsConstructor
public class OrangebeardTestSystemListener implements TestSystemListener, Closeable {
    private String PROPERTY_FILE_NAME = "orangebeard.properties";
    private static final String PROP_ROOT_PATH = "fitnesseroot.path";

    private OrangebeardClient orangebeardClient;
    private final OrangebeardProperties orangebeardProperties = new OrangebeardProperties();
    private ToolchainRunningContext runContext;

    private String rootPath = getFitnesseRootPath();
    private final OrangebeardTableLogParser htmlChunkParser = new OrangebeardTableLogParser();
    private OrangebeardLogger orangebeardLogger;

    private boolean local = false;


    public OrangebeardTestSystemListener(@Nullable String propertyFile) {
        if (propertyFile != null) {
            PROPERTY_FILE_NAME = propertyFile;
        }
        rootPath = getFitnesseRootPath();
    }

    public OrangebeardTestSystemListener(@Nullable String propertyFile, String rootPath) {
        if (propertyFile != null) {
            PROPERTY_FILE_NAME = propertyFile;
        }
        this.rootPath = rootPath;
    }

    public OrangebeardTestSystemListener(String propertyFile, boolean local) {
        this(propertyFile);
        this.local = local;
    }

    @Override
    public void testSystemStarted(TestSystem testSystem) {
        startTestRun(testSystem.getName());
    }

    @Override
    public void testOutputChunk(String s) {
        UUID testId = runContext.getTestId(runContext.getLatestTestName());

        if (s.toLowerCase().contains("<table")) {
            s = htmlChunkParser.removeNonTableProlog(s);
            s = htmlChunkParser.applyOrangebeardTableStyling(s);
        }

        String enrichedLog = htmlChunkParser.embedImagesAndStripHyperlinks(s, rootPath);

        //Workaround for corner case where table contains binary representation with 0x00 unicode chars
        enrichedLog = enrichedLog.replaceAll("\u0000", "");

        Log logItem = Log.builder()
                .message(enrichedLog)
                .itemUuid(testId)
                .testRunUUID(runContext.getTestRun())
                .logLevel(htmlChunkParser.determineLogLevel(enrichedLog))
                .time(LocalDateTime.now())
                .build();

        orangebeardClient.log(logItem);
        orangebeardLogger.attachFilesIfPresent(testId, runContext.getTestRun(), s);
    }

    @Override
    public void testStarted(TestPage testPage) {
        UUID suiteId = getAndOrStartSuite(testPage);
        StartTestItem testItem = getStartTestItem(testPage);
        UUID testId = orangebeardClient.startTestItem(suiteId, testItem);

        runContext.addTest(getTestName(testPage), testId);
    }

    private void startTestRun(String testSystemName) {
        orangebeardProperties.checkPropertiesArePresent();
        this.orangebeardClient = new OrangebeardClient(
                orangebeardProperties.getEndpoint(),
                orangebeardProperties.getAccessToken(),
                orangebeardProperties.getProjectName(),
                orangebeardProperties.requiredValuesArePresent()
        );

        this.orangebeardLogger = new OrangebeardLogger(orangebeardClient, getFitnesseRootPath());

        StartTestRun testrun = new StartTestRun(
                orangebeardProperties.getTestSetName(),
                orangebeardProperties.getDescription(),
                getTestRunAttributes(testSystemName)
        );

        runContext = new ToolchainRunningContext(orangebeardClient.startTestRun(testrun));
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        String testName = getTestName(testPage);
        if (runContext.hasTest(testName)) {
            FinishTestItem item = new FinishTestItem(runContext.getTestRun(), testResult(testSummary), null, null);
            orangebeardClient.finishTestItem(runContext.getTestId(testName), item);
            runContext.remove(testName);
        }
    }

    @Override
    public void testSystemStopped(TestSystem testSystem, Throwable throwable) {
        stopAllSuites();
        orangebeardClient.finishTestRun(runContext.getTestRun(), new FinishTestRun(null));

        if (!local) {
            orangebeardLogger.attachFitNesseResultsToRun(runContext.getTestRun());
        }
        reset();
    }

    private void reset() {
        orangebeardClient = null;
        runContext = null;
    }

    @Override
    public void testAssertionVerified(Assertion assertion, TestResult testResult) {

    }

    @Override
    public void testExceptionOccurred(Assertion assertion, ExceptionResult exceptionResult) {
    }

    @Override
    public void close() {

    }

    private Status testResult(TestSummary testSummary) {
        if (testSummary.getExceptions() > 0 || testSummary.getWrong() > 0) {
            return Status.FAILED;
        } else {
            return Status.PASSED;
        }
    }

    private UUID getAndOrStartSuite(TestPage testPage) {
        String fullSuiteName = TestPageHelper.getFullSuiteName(testPage);
        String[] suites = fullSuiteName.split("\\.");
        String suitePath = "";
        UUID suiteId = null;

        for (String suite : suites) {
            UUID parentSuiteId = runContext.getSuiteId(suitePath);
            suitePath = suitePath + "." + suite;
            suiteId = runContext.getSuiteId(suitePath);
            if (suiteId == null) {
                Set<Attribute> suiteAttrs = retrieveSuiteMetaData(testPage);
                suiteId = startSuite(parentSuiteId, suite, suiteAttrs);
                runContext.addSuite(suitePath, suiteId);
            }
        }

        return suiteId;
    }

    private Set<Attribute> retrieveSuiteMetaData(TestPage testPage) {
        Set<Attribute> metaData = new HashSet<>();
        if (testPage instanceof WikiTestPage) {

            PageData suitePageData = ((WikiTestPage) testPage).getSourcePage().getParent().getData();
            if (suitePageData.getAttribute(WikiPageProperty.SUITES) != null) {
                metaData.add(new Attribute("tags", suitePageData.getAttribute(WikiPageProperty.SUITES)));
            }
        }
        return metaData;
    }

    private UUID startSuite(UUID parentId, String shortName, Set<Attribute> attributes) {
        StartTestItem suite = new StartTestItem(runContext.getTestRun(), shortName, TestItemType.SUITE, null, attributes);
        return orangebeardClient.startTestItem(parentId, suite);
    }

    private void stopAllSuites() {
        for (UUID suiteId : runContext.getAllSuiteIds()) {
            stopSuite(suiteId);
        }
    }

    private void stopSuite(UUID suiteId) {
        FinishTestItem item = new FinishTestItem(runContext.getTestRun(), null, null, null);
        orangebeardClient.finishTestItem(suiteId, item);
    }


    @SneakyThrows
    private Set<Attribute> getTestRunAttributes(String testSystemName) {
        Set<Attribute> tags = new HashSet<>(orangebeardProperties.getAttributes());
        tags.add(new Attribute("Test System", testSystemName));
        if (local) {
            tags.add(new Attribute("wiki", InetAddress.getLocalHost().getHostName()));
        }

        return tags;
    }


    private Set<Attribute> extractTags(String propTags) {
        if (propTags != null) {
            return Arrays.stream(propTags.split("\\s*[;,]\\s*")).map(Attribute::new).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private String getFitnesseRootPath() {
        Properties propertyFile = new Properties();
        String defaultRoot;
        if (System.getProperty("user.dir").endsWith("wiki")) {
            defaultRoot = System.getProperty("user.dir") + File.separator + "FitNesseRoot" + File.separator;
        } else {
            defaultRoot = System.getProperty("user.dir") + File.separator + "wiki" + File.separator + "FitNesseRoot" + File.separator;
        }
        String rootPath = System.getProperty(PROP_ROOT_PATH);
        if (rootPath == null) {
            try {
                propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.PROPERTY_FILE_NAME)));
                rootPath = propertyFile.getProperty(PROP_ROOT_PATH) != null ? propertyFile.getProperty(PROP_ROOT_PATH) : defaultRoot;
            } catch (NullPointerException | IOException e) {
                rootPath = defaultRoot;
            }
        }
        return rootPath;
    }


    private StartTestItem getStartTestItem(TestPage testPage) {
        StartTestItem.StartTestItemBuilder testItem = StartTestItem.builder()
                .testRunUUID(runContext.getTestRun())
                .startTime(LocalDateTime.now())
                .name(getTestName(testPage))
                .type(determinePageType(testPage.getName()));


        if (testPage instanceof WikiTestPage) {
            PageData pageData = ((WikiTestPage) testPage).getData();

            if (pageData.getAttribute(WikiPageProperty.HELP) != null) {
                String helpText = pageData.getAttribute(WikiPageProperty.HELP);
                testItem.description(helpText);
            }

            if (pageData.getAttribute(WikiPageProperty.SUITES) != null) {
                Set<Attribute> tags = extractTags(pageData.getAttribute(WikiPageProperty.SUITES));
                testItem.attributes(tags);
            }
        }

        return testItem.build();
    }

    private TestItemType determinePageType(String pageName) {
        switch (pageName) {
            case "SuiteSetUp":
                return TestItemType.BEFORE_METHOD;
            case "SuiteTearDown":
                return TestItemType.AFTER_METHOD;
            default:
                return TestItemType.STEP;
        }
    }
}
