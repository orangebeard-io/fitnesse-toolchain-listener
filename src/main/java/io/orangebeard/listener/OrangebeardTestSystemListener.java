package io.orangebeard.listener;

import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.OrangebeardV3Client;
import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.LogLevel;
import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.suite.StartSuite;
import io.orangebeard.client.entity.suite.Suite;
import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;
import io.orangebeard.listener.entity.ScenarioLibraries;
import io.orangebeard.listener.helper.AttachmentHandler;
import io.orangebeard.listener.helper.LogStasher;
import io.orangebeard.listener.helper.OrangebeardTableLogParser;
import io.orangebeard.listener.helper.TestPageHelper;
import io.orangebeard.listener.helper.ToolchainRunningContext;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
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

    private int numberOfLogRequests = 0;

    private final OrangebeardProperties orangebeardProperties;
    private final ScenarioLibraries scenarioLibraries;
    private final AttachmentHandler attachmentHandler;
    private final LogStasher logStasher;
    private OrangebeardV3Client orangebeardClient;

    public ToolchainRunningContext getRunContext() {
        return runContext;
    }

    private ToolchainRunningContext runContext;

    /**
     * constructor for testing purposes
     */
    OrangebeardTestSystemListener(OrangebeardProperties orangebeardProperties, ToolchainRunningContext runContext, OrangebeardV3Client orangebeardClient,
                                  AttachmentHandler attachmentHandler, ScenarioLibraries scenarioLibraries, LogStasher logStasher) {
        this.orangebeardProperties = orangebeardProperties;
        this.runContext = runContext;
        this.orangebeardClient = orangebeardClient;
        this.attachmentHandler = attachmentHandler;
        this.scenarioLibraries = scenarioLibraries;
        this.logStasher = logStasher;
        this.rootPath = getFitnesseRootPath(propertyFileName);
    }

    public OrangebeardTestSystemListener() {
        this.orangebeardProperties = new OrangebeardProperties();
        logger.info("Log level is set to:  {}", orangebeardProperties.getLogLevel());
        this.scenarioLibraries = new ScenarioLibraries();
        this.rootPath = getFitnesseRootPath(propertyFileName);
        this.orangebeardClient = createOrangebeardV3Client(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(orangebeardClient, rootPath);
        this.logStasher = new LogStasher(orangebeardClient);
    }

    public OrangebeardTestSystemListener(@Nullable String propertyFileName, String rootPath) {
        if (propertyFileName != null) {
            this.propertyFileName = propertyFileName;
        }
        this.rootPath = rootPath;
        this.orangebeardProperties = new OrangebeardProperties();

        logger.info("Log level is set to: {}", orangebeardProperties.getLogLevel());

        this.scenarioLibraries = new ScenarioLibraries();
        this.orangebeardClient = createOrangebeardV3Client(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(orangebeardClient, rootPath);
        this.logStasher = new LogStasher(orangebeardClient);
    }

    public OrangebeardTestSystemListener(OrangebeardProperties orangebeardProperties) {
        this.orangebeardProperties = orangebeardProperties;

        logger.info("Log level is set to: {}", orangebeardProperties.getLogLevel());

        this.scenarioLibraries = new ScenarioLibraries();
        this.rootPath = getFitnesseRootPath(propertyFileName);
        this.orangebeardClient = createOrangebeardV3Client(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(orangebeardClient, rootPath);
        this.logStasher = new LogStasher(orangebeardClient);
    }

    @Override
    public void testSystemStarted(TestSystem testSystem) {
        orangebeardProperties.checkPropertiesArePresent();

        // If a test-run UUID is present in orangebeard.properties it belongs to the uuid that was retrieved from
        // announcing a test run and that scenario is triggered from the atp-integration test pipeline.
        // If no UUID is present then start the test run normally by providing a proper StartTestRun object.

        if (orangebeardProperties.isAnnouncedUUIDPresent()) {
            UUID testRunUUID = orangebeardProperties.getTestRunUUID();
            orangebeardClient.startAnnouncedTestRun(testRunUUID);
            this.runContext = new ToolchainRunningContext(testRunUUID);
        } else {
            StartTestRun testRun = new StartTestRun(orangebeardProperties.getTestSetName(), orangebeardProperties.getDescription(), getTestRunAttributes(testSystem.getName()), ChangedComponentsHelper.getChangedComponents());
            this.runContext = new ToolchainRunningContext(orangebeardClient.startTestRun(testRun));
        }
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
                    .testRunUUID(runContext.getTestRunUUID())
                    .testUUID(testId)
                    .logFormat(LogFormat.HTML)
                    .logLevel(logLevel)
                    .logTime(ZonedDateTime.now())
                    .build();

            if (orangebeardProperties.isLogsAtEndOfTest() && !attachmentHandler.hasFilesToAttach(log)) {
                logStasher.stashLogItem(testId, logItem);
            } else {
                UUID logUUID = orangebeardClient.log(logItem);
                numberOfLogRequests++;
                attachmentHandler.attachFilesIfPresent(testId, runContext.getTestRunUUID(), log, logUUID);
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
        // Get the full suite name here, so we can split the full suite string in separate suites and iterate
        // to check if the suite is already in the run context or not.
        String fullSuiteName = TestPageHelper.getFullSuiteName(testPage);
        String[] suites = fullSuiteName.split("\\.");
        String suitePath = "";

        UUID parentSuiteId = runContext.getSuiteId(suitePath);
        UUID suiteId = null;
        UUID testRunUUID = runContext.getTestRunUUID();

        for (String suite : suites) {
            suitePath = format("%s.%s", suitePath, suite);

            // If the suite doesn't exist in the context map it means that the test is included in a new suite
            // and the latter needs to be started as well
            if (!runContext.suiteExists(suitePath)) {
                String description = "";
                Set<Attribute> attributes = Collections.emptySet();

                WikiTestPage wikiTestPage = (WikiTestPage) testPage;
                PageData suitePageData = getPageDataForSuite(suitePath, wikiTestPage.getSourcePage());

                if (suitePageData != null && suitePageData.getAttribute(WikiPageProperty.SUITES) != null) {
                    attributes = convertAttributes(suitePageData.getAttribute(WikiPageProperty.SUITES));
                }
                if (suitePageData != null && suitePageData.getAttribute(WikiPageProperty.HELP) != null) {
                    description = suitePageData.getAttribute(WikiPageProperty.HELP);
                }
                StartSuite startSuite = new StartSuite(testRunUUID, parentSuiteId, description, attributes, List.of(suites));
                orangebeardClient.startSuite(startSuite);
                suiteId = UUID.randomUUID();
                runContext.addSuite(suitePath, suiteId, parentSuiteId, suites);
            } else {
                suiteId = runContext.getSuiteId(suitePath);
            }
        }

        // Start the test here
        StartTest startTest = getStartTest(suiteId, testPage);
        UUID testId = orangebeardClient.startTest(startTest);
        runContext.addTest(getTestName(testPage), testId);

        logScenarioLibraries(testId, ((WikiTestPage) testPage).getScenarioLibraries());
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        String testName = getTestName(testPage);
        UUID testId = runContext.getTestId(testName);
        ExecutionResult result = getExecutionResult(getRelativeName(testPage), testSummary);

        if (runContext.hasTest(testName)) {
            FinishTest finishedTestItem = new FinishTest(runContext.getTestRunUUID(), convertTestResultStatus(result), ZonedDateTime.now());
            if (orangebeardProperties.isLogsAtEndOfTest()) {
                logStasher.sendLogs(testId);
                numberOfLogRequests++;
            }
            orangebeardClient.finishTest(testId, finishedTestItem);
            runContext.remove(testName);
        }
    }

    private void logScenarioLibraries(UUID testUUID, List<WikiPage> scenarioLibraries) {
        if (orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.debug)) {

            for (WikiPage scenarioLibrary : scenarioLibraries) {
                String log = scenarioLibrary.getHtml();

                log = OrangebeardTableLogParser.applyOrangebeardTableStyling(log);
                String enrichedLog = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(log, rootPath);

                // Workaround for corner case where table contains binary representation with 0x00 unicode chars
                enrichedLog = enrichedLog.replace("\u0000", "");

                Log logItem = Log.builder().message(enrichedLog)
                        .testUUID(testUUID)
                        .testRunUUID(runContext.getTestRunUUID())
                        .logLevel(LogLevel.debug)
                        .logTime(ZonedDateTime.now())
                        .logFormat(LogFormat.HTML)
                        .build();

                orangebeardClient.log(logItem);
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
        orangebeardClient.finishTestRun(runContext.getTestRunUUID(), new FinishTestRun());
        reset();
    }

    /**
     * Resets the context and client. Used when a FitNesse Run switches testsystems i.e. from FIT to SLiM.
     * This results in starting e new Orangebeard run with a different testSytem attribute.
     */
    private void reset() {
        this.runContext = null;
        this.orangebeardClient = createOrangebeardV3Client(orangebeardProperties);
    }

    @Override
    public void testAssertionVerified(Assertion assertion, TestResult testResult) {
        // TODO document why this method is empty
    }

    @Override
    public void testExceptionOccurred(Assertion assertion, ExceptionResult exceptionResult) {
        // TODO document why this method is empty
    }

    @Override
    public void close() {
        // TODO document why this method is empty
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
        // TODO check if sorting should be performed
//        suites.sort(Comparator.comparing(Suite::getStartTime).reversed());

        for (Suite suite : suites) {
            stopSuite(suite.getSuiteUUID());
        }
    }

    private void stopSuite(UUID suiteId) {
        FinishTest test = new FinishTest(runContext.getTestRunUUID(), null, null);
        orangebeardClient.finishTest(suiteId, test);
    }

    @SneakyThrows
    private Set<Attribute> getTestRunAttributes(String testSystemName) {
        Set<Attribute> tags = new HashSet<>(orangebeardProperties.getAttributes());
        tags.add(new Attribute("Test System", testSystemName));

        return tags;
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

    private StartTest getStartTest(UUID suiteId, TestPage testPage) {
        StartTest.StartTestBuilder startTest = StartTest.builder()
                .testRunUUID(runContext.getTestRunUUID())
                .suiteUUID(suiteId)
                .startTime(ZonedDateTime.now())
                .testName(getTestName(testPage))
                .testType(determinePageType(testPage.getName()));

        if (testPage instanceof WikiTestPage) {
            PageData pageData = ((WikiTestPage) testPage).getData();

            if (pageData.getAttribute(WikiPageProperty.HELP) != null) {
                String helpText = pageData.getAttribute(WikiPageProperty.HELP);
                startTest.description(helpText);
            }

            if (pageData.getAttribute(WikiPageProperty.SUITES) != null) {
                Set<Attribute> tags = convertAttributes(pageData.getAttribute(WikiPageProperty.SUITES));
                startTest.attributes(tags);
            }
        }
        return startTest.build();
    }

    private static OrangebeardV3Client createOrangebeardV3Client(OrangebeardProperties orangebeardProperties) {
        return new OrangebeardV3Client(orangebeardProperties.getEndpoint(), orangebeardProperties.getAccessToken(), orangebeardProperties.getProjectName(), true);
    }
}
