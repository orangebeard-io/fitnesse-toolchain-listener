package io.orangebeard.listener;


import io.orangebeard.client.OrangebeardProperties;
//import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.LogLevel;
//import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.listener.entity.ScenarioLibraries;
import io.orangebeard.listener.entity.Suite;
import io.orangebeard.listener.helper.AttachmentHandler;
import io.orangebeard.listener.helper.LogStasher;
import io.orangebeard.listener.helper.OrangebeardTableLogParser;
import io.orangebeard.listener.helper.TestPageHelper;
import io.orangebeard.listener.helper.ToolchainRunningContext;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

import io.orangebeard.listener.v3client.datatype.TestType;
import io.orangebeard.listener.v3client.entities.Attribute;
import io.orangebeard.listener.v3client.entities.FinishTest;
import io.orangebeard.listener.v3client.entities.StartSuiteRQ;
import io.orangebeard.listener.v3client.entities.StartTest;
import io.orangebeard.listener.v3client.entities.StartTestRun;
import io.orangebeard.listener.v3client.entities.TestStatus;

import io.orangebeard.listener.v3client.v3Client;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fitnesse.testsystems.ExecutionResult.getExecutionResult;
import static io.orangebeard.listener.helper.TestPageHelper.getRelativeName;
import static io.orangebeard.listener.helper.TestPageHelper.getTestName;
import static io.orangebeard.listener.helper.TypeConverter.convertAttributes;
import static io.orangebeard.listener.helper.TypeConverter.convertTestResultStatus;
import static io.orangebeard.listener.helper.TypeConverter.determinePageType;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class OrangebeardTestSystemListener implements TestSystemListener, Closeable {
    private final Logger logger = LoggerFactory.getLogger(OrangebeardTestSystemListener.class);
    private static final String PROP_ROOT_PATH = "fitnesseroot.path";
    private static final String USER_DIR_PROPERTY = "user.dir";
    private final String rootPath;
    private String propertyFileName = "orangebeard.properties";

    private static int numberOfLogRequests = 0;

    private final OrangebeardProperties orangebeardProperties;
    private final ScenarioLibraries scenarioLibraries;
    private final AttachmentHandler attachmentHandler;
    private final LogStasher logStasher;
    private v3Client v3client;
    private ToolchainRunningContext runContext;

    /**
     * constructor for testing purposes
     */
    OrangebeardTestSystemListener(
            OrangebeardProperties orangebeardProperties,
            ToolchainRunningContext runContext,
            v3Client v3client,
            AttachmentHandler attachmentHandler,
            ScenarioLibraries scenarioLibraries,
            LogStasher logStasher) {
        this.orangebeardProperties = orangebeardProperties;
        this.runContext = runContext;
        this.v3client = v3client;
        this.attachmentHandler = attachmentHandler;
        this.scenarioLibraries = scenarioLibraries;
        this.logStasher = logStasher;
        this.rootPath = getFitnesseRootPath(propertyFileName);
    }

    public OrangebeardTestSystemListener() {
        this.orangebeardProperties = new OrangebeardProperties();
        logger.info(format("Log level is set to: %s", orangebeardProperties.getLogLevel()));
        this.scenarioLibraries = new ScenarioLibraries();
        this.rootPath = getFitnesseRootPath(propertyFileName);
        this.v3client = createOrangebeardClient(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(v3client, rootPath);
        this.logStasher = new LogStasher(v3client);
    }

    public OrangebeardTestSystemListener(@Nullable String propertyFileName, String rootPath) {
        if (propertyFileName != null) {
            this.propertyFileName = propertyFileName;
        }
        this.rootPath = rootPath;
        this.orangebeardProperties = new OrangebeardProperties();
        logger.info(format("Log level is set to: %s", orangebeardProperties.getLogLevel()));
        this.scenarioLibraries = new ScenarioLibraries();
        this.v3client = createOrangebeardClient(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(v3client, rootPath);
        this.logStasher = new LogStasher(v3client);
    }

    public OrangebeardTestSystemListener(OrangebeardProperties orangebeardProperties) {
        this.orangebeardProperties = orangebeardProperties;
        logger.info(format("Log level is set to: %s", orangebeardProperties.getLogLevel()));
        this.scenarioLibraries = new ScenarioLibraries();
        this.rootPath = getFitnesseRootPath(propertyFileName);
        this.v3client = createOrangebeardClient(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(v3client, rootPath);
        this.logStasher = new LogStasher(v3client);
    }

    @Override
    public void testSystemStarted(TestSystem testSystem) {
        orangebeardProperties.checkPropertiesArePresent();

//        StartTestRun testRun = new StartTestRun(
//                orangebeardProperties.getTestSetName(),
//                orangebeardProperties.getDescription(),
//                getTestRunAttributes(testSystem.getName()),
//                ChangedComponentsHelper.getChangedComponents());

        StartTestRun testRun = StartTestRun.builder().testSetName( orangebeardProperties.getTestSetName())
                .description(orangebeardProperties.getDescription())
                .attributes(getTestRunAttributes(testSystem.getName()))
                .startTime(ZonedDateTime.now())
                .changedComponents(new ArrayList<>(ChangedComponentsHelper.getChangedComponents())).build();

        this.runContext = new ToolchainRunningContext(v3client.startTestRun(testRun));
    }

    @Override
    public void testOutputChunk(TestPage testPage, String chunk) {
        String log = OrangebeardTableLogParser.removeNonTableProlog(chunk);
        UUID testId = runContext.getTestId(runContext.getLatestTestName());
        updateScenarioLibraries(testPage);

        if (scenarioLibraries.contains(log)) {
            return;
        }
        String logMessage = OrangebeardTableLogParser.parseLogMessage(log, rootPath);
        LogLevel logLevel = getLogLevel(log);
        if (orangebeardProperties.logShouldBeDispatchedToOrangebeard(logLevel)) {
            Log logItem = Log.builder()
                    .message(logMessage)
                    .itemUuid(testId)
                    .testRunUUID(runContext.getTestRunUUID())
                    .logLevel(logLevel)
                    .time(LocalDateTime.now())
                    .logFormat(LogFormat.HTML)
                    .build();
            if (orangebeardProperties.isLogsAtEndOfTest() && !attachmentHandler.hasFilesToAttach(log)) {
                logStasher.stashLogItem(testId, logItem);
            } else {
                v3client.log(logItem);
                numberOfLogRequests++;
                attachmentHandler.attachFilesIfPresent(testId, runContext.getTestRunUUID(), log);
            }
        }
    }

    private LogLevel getLogLevel(String logMessage) {
        if (logMessage.toLowerCase().contains("<table")) {
            return OrangebeardTableLogParser.getLogLevel(logMessage);
        }
        return LogLevel.debug;
    }

    @Override
    public void testStarted(TestPage testPage) {
         getAndOrStartSuite((WikiTestPage) testPage);
        StartTest testItem = getStartTestItem(testPage);
        UUID testId = v3client.startTestItem( testItem);

        runContext.addTest(getTestName(testPage), testId);

        logScenarioLibraries(testId, ((WikiTestPage) testPage).getScenarioLibraries());
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        String testName = getTestName(testPage);
        UUID testId = runContext.getTestId(testName);
        ExecutionResult result = getExecutionResult(getRelativeName(testPage), testSummary);
        if (runContext.hasTest(testName)) {
            FinishTest finishedTestItem = new FinishTest(runContext.getTestRunUUID(), TestStatus.valueOf(convertTestResultStatus(result).toString()),ZonedDateTime.now());
            if (orangebeardProperties.isLogsAtEndOfTest()) {
                logStasher.sendLogs(testId);
                numberOfLogRequests++;
            }
            v3client.finishTestItem(testId, finishedTestItem);
            runContext.remove(testName);
        }
    }

    private void logScenarioLibraries(UUID testUUID, List<WikiPage> scenarioLibraries) {
        if (orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.debug)) {

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
                        .logFormat(LogFormat.HTML)
                        .build();

                v3client.log(logItem);
                numberOfLogRequests++;
            }
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
        logger.info("Number of log requests: {}", numberOfLogRequests);
        numberOfLogRequests = 0;
        stopAllSuites();
        v3client.finishTestRun(runContext.getTestRunUUID(), new FinishTestRun());
        reset();
    }

    /**
     * Resets the context and client. Used when a FitNesse Run switches testsystems i.e. from FIT to SLiM.
     * This results in starting e new Orangebeard run with a different testSytem attribute.
     */
    private void reset() {
        this.runContext = null;
        this.v3client = createOrangebeardClient(orangebeardProperties);
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

    private void getAndOrStartSuite(WikiTestPage testPage) {
        String fullSuiteName = TestPageHelper.getFullSuiteName(testPage);
        String[] suites = fullSuiteName.split("\\.");
        String suitePath = "";
        UUID suiteId = null;

        UUID parentSuiteId = runContext.getSuiteId(suitePath);

//        for (String suite : suites) {
//             parentSuiteId = runContext.getSuiteId(suitePath);
//            suitePath = format("%s.%s", suitePath, suite);
//            suiteId = runContext.getSuiteId(suitePath);
//            if (suiteId == null) {
//                String description = null;
//                Set<Attribute> suiteAttrs = null;
//                PageData suitePageData = getPageDataForSuite(suitePath, testPage.getSourcePage());
//                if (suitePageData != null && suitePageData.getAttribute(WikiPageProperty.SUITES) != null) {
//                    suiteAttrs = convertAttributes(suitePageData.getAttribute(WikiPageProperty.SUITES));
//                }
//                if (suitePageData != null && suitePageData.getAttribute(WikiPageProperty.HELP) != null) {
//                    description = suitePageData.getAttribute(WikiPageProperty.HELP);
//                }
//
//                StartTestItem suiteItem = new StartTestItem(runContext.getTestRunUUID(), suite, TestItemType.SUITE, description, suiteAttrs);
//               // suiteId = orangebeardClient.startTestItem(parentSuiteId, suiteItem);
//                runContext.addSuite(suitePath, suiteId, suiteItem.getStartTime());
//            }
//        }
        StartSuiteRQ suiteItem = new StartSuiteRQ(runContext.getTestRunUUID(), parentSuiteId, new String(), new HashSet<Attribute>(), Arrays.asList(suites));
        v3client.startSuite(suiteItem);

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
        FinishTest item = new FinishTest(runContext.getTestRunUUID(), null, null);
        v3client.finishTestItem(suiteId, item);
    }

    @SneakyThrows
    private Set<Attribute> getTestRunAttributes(String testSystemName) {
        Set<io.orangebeard.client.entity.Attribute> tags = new HashSet<>(orangebeardProperties.getAttributes());
        tags.add(new io.orangebeard.client.entity.Attribute("Test System", testSystemName));

        return new HashSet<Attribute>();
    }

    private static String getFitnesseRootPath(String propertyFileName) {
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
                propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(propertyFileName)));
                fitnesseRootPath = propertyFile.getProperty(PROP_ROOT_PATH) != null ? propertyFile.getProperty(PROP_ROOT_PATH) : defaultRoot;
            } catch (NullPointerException | IOException e) {
                fitnesseRootPath = defaultRoot;
            }
        }
        return fitnesseRootPath;
    }

    private StartTest getStartTestItem(TestPage testPage) {
       //StartTestItem.StartTestItemBuilder testItem = StartTestItem.builder()
//                .testRunUUID(runContext.getTestRunUUID())
//                .startTime(LocalDateTime.now())
//                .name(getTestName(testPage))
//                .type(determinePageType(testPage.getName()));


        StartTest.StartTestBuilder testItem = StartTest.builder()
                .testRunUUID(runContext.getTestRunUUID())
                .startTime(ZonedDateTime.now())
                .testName(getTestName(testPage))
                .testType(TestType.valueOf(determinePageType(testPage.getName()).toString()));

        if (testPage instanceof WikiTestPage) {
            PageData pageData = ((WikiTestPage) testPage).getData();

            if (pageData.getAttribute(WikiPageProperty.HELP) != null) {
                String helpText = pageData.getAttribute(WikiPageProperty.HELP);
                testItem.description(helpText);
            }

            if (pageData.getAttribute(WikiPageProperty.SUITES) != null) {
                Set<Attribute> tags = convertAttributes(pageData.getAttribute(WikiPageProperty.SUITES));
                testItem.attributes(new ArrayList<>(tags));
            }
        }

        return testItem.build();
    }

    private static v3Client createOrangebeardClient(OrangebeardProperties orangebeardProperties) {
        return new v3Client(
                orangebeardProperties.getEndpoint(),
                orangebeardProperties.getAccessToken(),
               orangebeardProperties.getProjectName(),
                orangebeardProperties.requiredValuesArePresent());
    }
}
