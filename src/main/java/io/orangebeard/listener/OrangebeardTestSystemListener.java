package io.orangebeard.listener;

import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.OrangebeardV1Client;
import io.orangebeard.client.OrangebeardV2Client;
import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.FinishTestItem;
import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.LogLevel;
import io.orangebeard.client.entity.StartTestItem;
import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.client.entity.TestItemType;
import io.orangebeard.listener.entity.ApiVersion;
import io.orangebeard.listener.entity.ScenarioLibraries;
import io.orangebeard.listener.entity.Suite;
import io.orangebeard.listener.helper.AttachmentHandler;
import io.orangebeard.listener.helper.OrangebeardTableLogParser;
import io.orangebeard.listener.helper.TestPageHelper;
import io.orangebeard.listener.helper.ToolchainRunningContext;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.Assertion;
import fitnesse.testsystems.ExceptionResult;
import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestResult;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystem;
import fitnesse.testsystems.TestSystemListener;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageProperty;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fitnesse.testsystems.ExecutionResult.getExecutionResult;
import static io.orangebeard.listener.entity.ApiVersion.V1;
import static io.orangebeard.listener.entity.ApiVersion.V2;
import static io.orangebeard.listener.helper.TestPageHelper.getRelativeName;
import static io.orangebeard.listener.helper.TestPageHelper.getTestName;
import static io.orangebeard.listener.helper.TypeConverter.convertAttributes;
import static io.orangebeard.listener.helper.TypeConverter.convertTestResultStatus;
import static io.orangebeard.listener.helper.TypeConverter.determinePageType;
import static java.util.Objects.requireNonNull;

public class OrangebeardTestSystemListener implements TestSystemListener, Closeable {
    private final Logger logger = LoggerFactory.getLogger(OrangebeardTestSystemListener.class);
    private static final String PROP_ROOT_PATH = "fitnesseroot.path";
    private static final String PROP_ATTACH_ZIP = "attach.zipfile";
    private static final String USER_DIR_PROPERTY = "user.dir";
    private final boolean attachZip;
    private final String rootPath;
    private String propertyFileName = "orangebeard.properties";
    /**
     * Property that determines whether or not the test run is run from the browser in the wiki or not.
     */
    private boolean wikiTestRun = false;
    private static int numberOfLogs = 0;

    private final OrangebeardProperties orangebeardProperties;
    private final ScenarioLibraries scenarioLibraries;
    private OrangebeardClient orangebeardClient;
    private ToolchainRunningContext runContext;
    private AttachmentHandler attachmentHandler;

    /**
     * constructor for testing purposes
     */
    OrangebeardTestSystemListener(
            OrangebeardProperties orangebeardProperties,
            ToolchainRunningContext runContext,
            OrangebeardClient orangebeardClient,
            AttachmentHandler attachmentHandler,
            ScenarioLibraries scenarioLibraries) {
        this.orangebeardProperties = orangebeardProperties;
        this.runContext = runContext;
        this.orangebeardClient = orangebeardClient;
        this.attachmentHandler = attachmentHandler;
        this.scenarioLibraries = scenarioLibraries;
        this.attachZip = false;
        this.rootPath = getFitnesseRootPath();
    }

    public OrangebeardTestSystemListener() {
        this.orangebeardProperties = new OrangebeardProperties();
        this.scenarioLibraries = new ScenarioLibraries();
        this.attachZip = false;
        this.rootPath = getFitnesseRootPath();
    }

    public OrangebeardTestSystemListener(@Nullable String propertyFileName, String rootPath) {
        if (propertyFileName != null) {
            this.propertyFileName = propertyFileName;
        }
        this.attachZip = attachZip();
        this.rootPath = rootPath;
        this.orangebeardProperties = new OrangebeardProperties();
        this.scenarioLibraries = new ScenarioLibraries();
    }

    public OrangebeardTestSystemListener(String propertyFileName, boolean wikiTestRun) {
        this.propertyFileName = propertyFileName;
        this.attachZip = attachZip();
        this.wikiTestRun = wikiTestRun;
        this.orangebeardProperties = new OrangebeardProperties();
        this.scenarioLibraries = new ScenarioLibraries();
        this.rootPath = getFitnesseRootPath();
    }

    @Override
    public void testSystemStarted(TestSystem testSystem) {
        orangebeardProperties.checkPropertiesArePresent();
        ApiVersion apiVersion = determineApiVersion(propertyFileName);
        if (apiVersion == V2) {
            this.orangebeardClient = new OrangebeardV2Client(
                    orangebeardProperties.getEndpoint(),
                    orangebeardProperties.getAccessToken(),
                    orangebeardProperties.getProjectName(),
                    orangebeardProperties.requiredValuesArePresent()
            );
        } else {
            this.orangebeardClient = new OrangebeardV1Client(
                    orangebeardProperties.getEndpoint(),
                    orangebeardProperties.getAccessToken(),
                    orangebeardProperties.getProjectName(),
                    orangebeardProperties.requiredValuesArePresent()
            );
        }
        logger.info("Listener api version {} is used", apiVersion);

        this.attachmentHandler = new AttachmentHandler(orangebeardClient, rootPath);

        StartTestRun testrun = new StartTestRun(
                orangebeardProperties.getTestSetName(),
                orangebeardProperties.getDescription(),
                getTestRunAttributes(testSystem.getName())
        );

        this.runContext = new ToolchainRunningContext(orangebeardClient.startTestRun(testrun));
    }

    @Override
    public void testOutputChunk(TestPage testPage, String chunk) {
        String log = OrangebeardTableLogParser.removeNonTableProlog(chunk);
        UUID testId = runContext.getTestId(runContext.getLatestTestName());

        updateScenarioLibraries(testPage);
        if (scenarioLibraries.contains(log)) {
            return;
        }
        LogLevel logLevel = LogLevel.debug;

        if (log.toLowerCase().contains("<table")) {
            logLevel = OrangebeardTableLogParser.getLogLevel(log);
            log = OrangebeardTableLogParser.applyOrangebeardTableStyling(log);
        }

        String enrichedLog = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(log, rootPath);
        //Workaround for corner case where table contains binary representation with 0x00 unicode chars
        enrichedLog = enrichedLog.replace("\u0000", "");

        Log logItem = Log.builder()
                .message(enrichedLog)
                .itemUuid(testId)
                .testRunUUID(runContext.getTestRunUUID())
                .logLevel(logLevel)
                .time(LocalDateTime.now())
                .build();

        orangebeardClient.log(logItem);
        numberOfLogs++;
        attachmentHandler.attachFilesIfPresent(testId, runContext.getTestRunUUID(), log);
    }

    @Override
    public void testStarted(TestPage testPage) {
        UUID suiteId = getAndOrStartSuite((WikiTestPage) testPage);
        StartTestItem testItem = getStartTestItem(testPage);
        UUID testId = orangebeardClient.startTestItem(suiteId, testItem);

        runContext.addTest(getTestName(testPage), testId);

        logScenarioLibraries(testId, ((WikiTestPage) testPage).getScenarioLibraries());
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        String testName = getTestName(testPage);
        UUID testId = runContext.getTestId(testName);
        ExecutionResult result = getExecutionResult(getRelativeName(testPage), testSummary);
        if (runContext.hasTest(testName)) {
            FinishTestItem item = new FinishTestItem(
                    runContext.getTestRunUUID(),
                    convertTestResultStatus(result),
                    null,
                    null
            );

            orangebeardClient.finishTestItem(testId, item);
            runContext.remove(testName);
        }
    }

    private void logScenarioLibraries(UUID testUUID, List<WikiPage> scenarioLibraries) {
        for (WikiPage scenarioLibrary : scenarioLibraries) {
            String log = scenarioLibrary.getHtml();

            log = OrangebeardTableLogParser.applyOrangebeardTableStyling(log);
            String enrichedLog = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(log, rootPath);

            //Workaround for corner case where table contains binary representation with 0x00 unicode chars
            enrichedLog = enrichedLog.replace("\u0000", "");

            Log logItem = Log.builder()
                    .message(enrichedLog)
                    .itemUuid(testUUID)
                    .testRunUUID(runContext.getTestRunUUID())
                    .logLevel(LogLevel.debug)
                    .time(LocalDateTime.now())
                    .build();

            orangebeardClient.log(logItem);
            numberOfLogs++;
        }
    }

    private void updateScenarioLibraries(TestPage testPage) {
        if (testPage instanceof WikiTestPage) {
            WikiTestPage wiki = (WikiTestPage) testPage;
            scenarioLibraries.add(wiki.getScenarioLibraries());
        }
    }

    @Override
    public void testSystemStopped(TestSystem testSystem, Throwable throwable) {
        logger.info("Number of logs: {}", numberOfLogs);
        stopAllSuites();
        orangebeardClient.finishTestRun(runContext.getTestRunUUID(), new FinishTestRun());

        if (attachZip) {
            attachmentHandler.attachFitNesseResultsToRun(runContext.getTestRunUUID());
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

    private UUID getAndOrStartSuite(WikiTestPage testPage) {
        String fullSuiteName = TestPageHelper.getFullSuiteName(testPage);
        String[] suites = fullSuiteName.split("\\.");
        String suitePath = "";
        UUID suiteId = null;

        for (String suite : suites) {
            UUID parentSuiteId = runContext.getSuiteId(suitePath);
            suitePath = suitePath + "." + suite;
            suiteId = runContext.getSuiteId(suitePath);
            if (suiteId == null) {
                String description = null;
                Set<Attribute> suiteAttrs = null;
                PageData suitePageData = getPageDataForSuite(suitePath, testPage.getSourcePage());
                if (suitePageData != null && suitePageData.getAttribute(WikiPageProperty.SUITES) != null) {
                    suiteAttrs = convertAttributes(suitePageData.getAttribute(WikiPageProperty.SUITES));
                }
                if (suitePageData != null && suitePageData.getAttribute(WikiPageProperty.HELP) != null) {
                    description = suitePageData.getAttribute(WikiPageProperty.HELP);
                }

                StartTestItem suiteItem = new StartTestItem(runContext.getTestRunUUID(), suite, TestItemType.SUITE, description, suiteAttrs);
                suiteId = orangebeardClient.startTestItem(parentSuiteId, suiteItem);
                runContext.addSuite(suitePath, suiteId, suiteItem.getStartTime());
            }
        }

        return suiteId;
    }

    /**
     * Return the PageData object for the WikiPage of a given suitePath
     *
     * @param suitePath  The full (FitNesse) path of the suite we are looking for
     * @param sourcePage The SourcePage to iterate through
     *                   (as it is an object that contains its ancestor SourcePages as well)
     * @return The PageData object of the given Suite's sourcePage, or null if the sourcePage does not contain
     * a page with the given SuitePath (should not happen)
     */
    private PageData getPageDataForSuite(String suitePath, WikiPage sourcePage) {
        if (suitePath.endsWith(sourcePage.getParent().getName())) {
            return sourcePage.getParent().getData();
        } else if (sourcePage != sourcePage.getParent()) {
            return getPageDataForSuite(suitePath, sourcePage.getParent());
        } else {
            logger.warn("No PageData was found for suite {}", suitePath);
            return null;
        }
    }

    private void stopAllSuites() {
        List<Suite> suites = runContext.getAllSuites();
        // reverse suite ids so suites are stopped in the reverse order of which these are started.
        suites.sort(Comparator.comparing(Suite::getStartTime).reversed());

        for (Suite suite : suites) {
            stopSuite(suite.getUuid());
        }
    }

    private void stopSuite(UUID suiteId) {
        FinishTestItem item = new FinishTestItem(runContext.getTestRunUUID(), null, null, null);
        orangebeardClient.finishTestItem(suiteId, item);
    }

    @SneakyThrows
    private Set<Attribute> getTestRunAttributes(String testSystemName) {
        Set<Attribute> tags = new HashSet<>(orangebeardProperties.getAttributes());
        tags.add(new Attribute("Test System", testSystemName));
        if (wikiTestRun) {
            tags.add(new Attribute("wiki", InetAddress.getLocalHost().getHostName()));
        }

        return tags;
    }

    private String getFitnesseRootPath() {
        Properties propertyFile = new Properties();
        String defaultRoot;
        if (System.getProperty(USER_DIR_PROPERTY).endsWith("wiki")) {
            defaultRoot = System.getProperty(USER_DIR_PROPERTY) + File.separator + "FitNesseRoot" + File.separator;
        } else {
            defaultRoot = System.getProperty(USER_DIR_PROPERTY) + File.separator + "wiki" + File.separator + "FitNesseRoot" + File.separator;
        }
        String fitnesseRootPath = System.getProperty(PROP_ROOT_PATH);
        if (fitnesseRootPath == null) {
            try {
                propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.propertyFileName)));
                fitnesseRootPath = propertyFile.getProperty(PROP_ROOT_PATH) != null ? propertyFile.getProperty(PROP_ROOT_PATH) : defaultRoot;
            } catch (NullPointerException | IOException e) {
                fitnesseRootPath = defaultRoot;
            }
        }
        return fitnesseRootPath;
    }

    private StartTestItem getStartTestItem(TestPage testPage) {
        StartTestItem.StartTestItemBuilder testItem = StartTestItem.builder()
                .testRunUUID(runContext.getTestRunUUID())
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
                Set<Attribute> tags = convertAttributes(pageData.getAttribute(WikiPageProperty.SUITES));
                testItem.attributes(tags);
            }
        }

        return testItem.build();
    }

    private boolean attachZip() {
        try {
            Properties propertyFile = new Properties();
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.propertyFileName)));
            return propertyFile.getProperty(PROP_ATTACH_ZIP) != null ? Boolean.parseBoolean(propertyFile.getProperty(PROP_ROOT_PATH)) : attachZip;
        } catch (NullPointerException | IOException e) {
            //keep value
        }
        return false;
    }

    private ApiVersion determineApiVersion(String propertyFileName) {
        try {
            Properties properties = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertyFileName);

            if (inputStream != null) {
                properties.load(inputStream);
            }
            String api = properties.getProperty("orangebeard.api");
            if (api != null && api.toUpperCase().equals(V2.toString())) {
                return V2;
            }
            return V1;
        } catch (IOException e) {
            return V1;
        }
    }
}
