package io.orangebeard.listener;


import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.LogLevel;
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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

import io.orangebeard.listener.orangebeardv3client.entities.Attribute;
import io.orangebeard.listener.orangebeardv3client.entities.FinishTest;
import io.orangebeard.listener.orangebeardv3client.entities.FinishTestRun;
import io.orangebeard.listener.orangebeardv3client.entities.StartSuiteRQ;
import io.orangebeard.listener.orangebeardv3client.entities.StartTest;
import io.orangebeard.listener.orangebeardv3client.entities.StartTestRun;
import io.orangebeard.listener.orangebeardv3client.entities.TestStatus;

import io.orangebeard.listener.orangebeardv3client.OrangebeardV3Client;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fitnesse.testsystems.ExecutionResult.getExecutionResult;
import static io.orangebeard.listener.helper.TestPageHelper.getRelativeName;
import static io.orangebeard.listener.helper.TestPageHelper.getTestName;
import static io.orangebeard.listener.helper.TypeConverter.v3ClientDeterminePageType;
import static io.orangebeard.listener.helper.TypeConverter.convertAttributes;
import static io.orangebeard.listener.helper.TypeConverter.convertTestResultStatus;

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
    private OrangebeardV3Client orangebeardV3Client;
    private ToolchainRunningContext runContext;
    private List<String> suites1;

    /**
     * constructor for testing purposes
     */
    OrangebeardTestSystemListener(
            OrangebeardProperties orangebeardProperties,
            ToolchainRunningContext runContext,
            OrangebeardV3Client orangebeardV3Client,
            AttachmentHandler attachmentHandler,
            ScenarioLibraries scenarioLibraries,
            LogStasher logStasher) {
        this.orangebeardProperties = orangebeardProperties;
        this.runContext = runContext;
        this.orangebeardV3Client = orangebeardV3Client;
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
        this.orangebeardV3Client = createOrangebeardClient(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(orangebeardV3Client, rootPath);
        this.logStasher = new LogStasher(orangebeardV3Client);
    }

    public OrangebeardTestSystemListener(@Nullable String propertyFileName, String rootPath) {
        if (propertyFileName != null) {
            this.propertyFileName = propertyFileName;
        }
        this.rootPath = rootPath;
        this.orangebeardProperties = new OrangebeardProperties();
        logger.info(format("Log level is set to: %s", orangebeardProperties.getLogLevel()));
        this.scenarioLibraries = new ScenarioLibraries();
        this.orangebeardV3Client = createOrangebeardClient(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(orangebeardV3Client, rootPath);
        this.logStasher = new LogStasher(orangebeardV3Client);
    }

    public OrangebeardTestSystemListener(OrangebeardProperties orangebeardProperties) {
        this.orangebeardProperties = orangebeardProperties;
        logger.info(format("Log level is set to: %s", orangebeardProperties.getLogLevel()));
        this.scenarioLibraries = new ScenarioLibraries();
        this.rootPath = getFitnesseRootPath(propertyFileName);
        this.orangebeardV3Client = createOrangebeardClient(orangebeardProperties);
        this.attachmentHandler = new AttachmentHandler(orangebeardV3Client, rootPath);
        this.logStasher = new LogStasher(orangebeardV3Client);
    }

    @Override
    public void testSystemStarted(TestSystem testSystem) {
        orangebeardProperties.checkPropertiesArePresent();

        StartTestRun testRun = StartTestRun.builder().testSetName( orangebeardProperties.getTestSetName())
                .description(orangebeardProperties.getDescription())
                .attributes(getTestRunAttributes(testSystem.getName()))
                .startTime(ZonedDateTime.now())
                .changedComponents(new ArrayList<>(ChangedComponentsHelper.getChangedComponents())).build();

        this.runContext = new ToolchainRunningContext(orangebeardV3Client.startTestRun(testRun));
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
                orangebeardV3Client.log(logItem);
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
        startSuite((WikiTestPage) testPage);
        StartTest startTest = getStartTest(testPage);
        UUID testId = orangebeardV3Client.startTestItem( startTest);

        runContext.addTest(getTestName(testPage), testId);

        logScenarioLibraries(testId, ((WikiTestPage) testPage).getScenarioLibraries());
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        String testName = getTestName(testPage);
        UUID testId = runContext.getTestId(testName);
        ExecutionResult result = getExecutionResult(getRelativeName(testPage), testSummary);
        if (runContext.hasTest(testName)) {

            FinishTest finishTest = new FinishTest(runContext.getTestRunUUID(),
                    TestStatus.valueOf(convertTestResultStatus(result).toString()),
                    ZonedDateTime.now());

            if (orangebeardProperties.isLogsAtEndOfTest()) {
                logStasher.sendLogs(testId);
                numberOfLogRequests++;
            }
            orangebeardV3Client.FinishTest(testId, finishTest);
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

                orangebeardV3Client.log(logItem);
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
        orangebeardV3Client.finishTestRun(runContext.getTestRunUUID(), new FinishTestRun(ZonedDateTime.now()));
        reset();
    }

    /**
     * Resets the context and client. Used when a FitNesse Run switches testsystems i.e. from FIT to SLiM.
     * This results in starting e new Orangebeard run with a different testSytem attribute.
     */
    private void reset() {
        this.runContext = null;
        this.orangebeardV3Client = createOrangebeardClient(orangebeardProperties);
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


    private void startSuite(WikiTestPage testPage) {
        String fullSuiteName = TestPageHelper.getFullSuiteName(testPage);
        String[] suites = fullSuiteName.split("\\.");
        List<String> suitesToCreate = new ArrayList<>();
        List<String> suitePath =new ArrayList<>();
        UUID parentSuiteId = null;
        UUID suiteId;

        for(String suite : suites) {
            suitePath.add(suite);
            suiteId = runContext.getSuiteId(String.join(".",suitePath));
            if(suiteId != null) {
                parentSuiteId=suiteId;
            }else {
                suitesToCreate.add(suite);
            }
        }

        //StartSuiteRQ suiteItem= runContext.getStartSuite(Arrays.asList(suites));

        if (!suitesToCreate.isEmpty()) {
            StartSuiteRQ suiteItem = new StartSuiteRQ(runContext.getTestRunUUID(), parentSuiteId, null, new HashSet<>(), suitesToCreate);

            runContext.addSuite(orangebeardV3Client.startSuite(suiteItem));
        }
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


    @SneakyThrows
    private Set<Attribute> getTestRunAttributes(String testSystemName) {
        Set<io.orangebeard.client.entity.Attribute> tags = new HashSet<>(orangebeardProperties.getAttributes());
        tags.add(new io.orangebeard.client.entity.Attribute("Test System", testSystemName));

        return new HashSet<>();
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

    private StartTest getStartTest(TestPage testPage) {
         String fullSuiteName = TestPageHelper.getFullSuiteName(testPage);

        StartTest.StartTestBuilder startTest = StartTest.builder()
                .testRunUUID(runContext.getTestRunUUID())
                .startTime(ZonedDateTime.now())
                .testName(getTestName(testPage))
                .testType(v3ClientDeterminePageType(testPage.getName()))
                .suiteUUID(runContext.getSuiteId(fullSuiteName));

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

    private static OrangebeardV3Client createOrangebeardClient(OrangebeardProperties orangebeardProperties) {
        return new OrangebeardV3Client(
                orangebeardProperties.getEndpoint(),
                orangebeardProperties.getAccessToken(),
               orangebeardProperties.getProjectName(),
                orangebeardProperties.requiredValuesArePresent());
    }
}
