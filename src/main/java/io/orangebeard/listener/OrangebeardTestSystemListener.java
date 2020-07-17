package io.orangebeard.listener;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
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
import io.orangebeard.listener.helper.OrangebeardLogger;
import io.orangebeard.listener.helper.OrangebeardTableLogParser;
import io.orangebeard.listener.helper.TestPageHelper;
import io.orangebeard.listener.helper.ToolchainRunningContext;
import io.reactivex.Maybe;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.orangebeard.listener.helper.TestPageHelper.getTestName;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@NoArgsConstructor
public class OrangebeardTestSystemListener implements TestSystemListener, Closeable {

    private static final String PROP_LAUNCHNAME = "rp.launch";
    private static final String PROP_AUGMENTED_ENDPOINT = "at.augmented-testing.endpoint";
    private static final String PROP_AUGMENTED_TOKEN = "at.augmented-testing.token";
    private static final String PROP_ROOT_PATH = "fitnesseroot.path";
    private static final String AUGMENTED_TESTING_TOKEN = "AUGMENTED_TESTING_TOKEN";

    private static final String PROP_TAGS = "rp.tags";
    private static final String PROP_ATTRIBUTES = "rp.attributes";
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardTestSystemListener.class);
    private String PROPERTY_FILE_NAME = "orangebeard.properties";
    private OrangebeardLogger orangebeardLogger;
    private ReportPortal reportPortal;
    private Launch launch;
    private ToolchainRunningContext runContext = new ToolchainRunningContext();
    private String rootPath = getFitnesseRootPath();
    private final OrangebeardTableLogParser htmlChunkParser = new OrangebeardTableLogParser();
    public Maybe<String> launchId;
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
        runContext.setTestSystemName(testSystem.getName());
    }

    @Override
    public void testOutputChunk(String s) {
        Maybe<String> testId = runContext.getTestId(runContext.getLatestTestName());
        String level = htmlChunkParser.determineLogLevel(s);

        if (s.toLowerCase().contains("<table")) {
            s = htmlChunkParser.removeNonTableProlog(s);
            s = htmlChunkParser.applyOrangebeardTableStyling(s);
        }

        String enrichedLog = htmlChunkParser.embedImagesAndStripHyperlinks(s, rootPath);
        orangebeardLogger.sendLogData(testId.blockingGet(), launchId.blockingGet(), enrichedLog, level);
        orangebeardLogger.sendAttachmentsIfPresent(testId.blockingGet(), launchId.blockingGet(), s);
    }

    @Override
    public void testStarted(TestPage testPage) {
        startLaunchIfRequired(testPage);

        Maybe<String> suiteId = getAndOrStartSuite(testPage);
        String testName = getTestName(testPage);

        StartTestItemRQ startTestItemRQ = getTest(testPage);
        Maybe<String> id = launch.startTestItem(suiteId, startTestItemRQ);
        runContext.addTest(testName, id);
        logger.info("[Orangebeard] test {} started with id {} and suite id {}", testName, id.blockingGet(), suiteId.blockingGet());
    }

    private void startLaunchIfRequired(TestPage testPage) {
        if (this.reportPortal == null) {
            this.reportPortal = ReportPortal.builder().withParameters(
                    new ListenerParameters(PropertiesLoader.load(PROPERTY_FILE_NAME))
            ).build();
            this.orangebeardLogger = new OrangebeardLogger(reportPortal, rootPath);
        }
        if (launch == null) {
            StartLaunchRQ startLaunchRQ = getStartLaunchRQ(testPage);
            this.launch = reportPortal.newLaunch(startLaunchRQ);
            if (launch != null) {
                Maybe<String> id = launch.start();
                launchId = id;
                reportStartLaunchToAugmented(id.blockingGet());
            }
        }
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        String testName = getTestName(testPage);
        if (runContext.hasTest(testName)) {
            logger.info("[Orangebeard] test {} finished", getTestName(testPage));
            FinishTestItemRQ rq = getFinishTestItemRQ(testResult(testSummary));
            Maybe<String> testId = runContext.getTestId(getTestName(testPage));
            launch.finishTestItem(testId, rq);
            runContext.remove(testName);
        }
    }

    @Override
    public void testSystemStopped(TestSystem testSystem, Throwable throwable) {
        logger.info("[Orangebeard] test run finished");
        stopAllSuites();
        FinishExecutionRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Date.from(Instant.now()));
        if (!local) {
            orangebeardLogger.attachFitNesseResultsToRun(launchId.blockingGet());
        }
        launch.finish(rq);
        flushLaunchData();
    }

    private void flushLaunchData() {
        launch = null;
        reportPortal = null;
        runContext = new ToolchainRunningContext();
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

    private String testResult(TestSummary testSummary) {
        if (testSummary.getExceptions() > 0 || testSummary.getWrong() > 0) {
            return "failed";
        } else {
            return "passed";
        }
    }

    private Maybe<String> getAndOrStartSuite(TestPage testPage) {
        String fullSuiteName = TestPageHelper.getFullSuiteName(testPage);
        String[] suites = fullSuiteName.split("\\.");
        String suitePath = "";
        Maybe<String> suiteId = null;

        for (String suite : suites) {
            Maybe<String> parentSuiteId = runContext.getSuiteId(suitePath);
            suitePath = suitePath + "." + suite;
            suiteId = runContext.getSuiteId(suitePath);
            if (suiteId == null) {
                Map<String, String> suiteMetaData = retrieveSuiteMetaData(testPage);

                suiteId = startSuite(parentSuiteId, suite, suiteMetaData);
                runContext.addSuite(suitePath, suiteId);
            }
        }

        return suiteId;
    }

    private Map<String, String> retrieveSuiteMetaData(TestPage testPage) {
        Map<String, String> metaData = new HashMap<>();
        if (testPage instanceof WikiTestPage) {

            PageData suitePageData = ((WikiTestPage) testPage).getSourcePage().getParent().getData();
            metaData.put("description", suitePageData.getAttribute(WikiPageProperty.HELP));
            if (suitePageData.getAttribute(WikiPageProperty.SUITES) != null) {
                metaData.put("tags", suitePageData.getAttribute(WikiPageProperty.SUITES));
            }
        }
        return metaData;
    }

    private Maybe<String> startSuite(Maybe<String> parentId, String shortName, Map<String, String> metaData) {
        StartTestItemRQ rq = getSuite(shortName, metaData);
        Maybe<String> suiteId = launch.startTestItem(parentId, rq);
        logger.info("[Orangebeard] suite {} started in {} with id {}", shortName, parentId, suiteId.blockingGet());
        return suiteId;
    }

    private void stopAllSuites() {
        for (Maybe<String> suiteId : runContext.getAllSuiteIds()) {
            stopSuite(suiteId);
        }
    }

    private void stopSuite(Maybe<String> suiteId) {
        final FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        launch.finishTestItem(suiteId, rq);
    }

    private StartLaunchRQ getStartLaunchRQ(TestPage testPage) {
        StartLaunchRQ startLaunchRQ = new StartLaunchRQ();
        startLaunchRQ.setMode(Mode.DEFAULT);
        startLaunchRQ.setStartTime(Date.from(Instant.now()));
        startLaunchRQ.setName(getLaunchNameFromProperties(testPage));
        startLaunchRQ.setAttributes(getLauchAttributes());
        return startLaunchRQ;
    }

    private void reportStartLaunchToAugmented(String launchUUID) {
        String endpoint = getEndpointFromProperties();
        String projectId = System.getenv("projectId");
        String jobId = "jenkins-" + System.getenv("JOB_NAME") + "-" + System.getenv("BUILD_ID");
        String token = getToken();

        if (endpoint == null) {
            logger.warn("[Orangebeard] Cannot find " + PROP_AUGMENTED_ENDPOINT + " in " + PROPERTY_FILE_NAME + ". Nothing will be reported to the augmented endpoint!");
        }
        if (token == null) {
            logger.warn("[Orangebeard] Cannot find " + AUGMENTED_TESTING_TOKEN + " in environment variables or in properties. Nothing will be reported to the augmented endpoint!");
        }
        if (projectId == null || launchUUID == null) {
            logger.warn("[Orangebeard] Project id {}, Launch id {} and/or job id {} is null", projectId, launchUUID, jobId);
            return;
        }

        String endpointWithVariables = format("%s/launch?project-id=%s&job-id=%s&launch-uuid=%s", endpoint, projectId, jobId, launchUUID);
        logger.info("[Orangebeard] Put to: {}", endpointWithVariables);

        try {
            HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
            HttpHeaders headers = new HttpHeaders();
            headers.put("reportportal-token", token);
            HttpRequest request = requestFactory.buildPutRequest(new GenericUrl(endpointWithVariables), new EmptyContent()).setHeaders(headers);
            request.execute();
        } catch (IOException e) {
            logger.error("[Orangebeard] Request is not sent!");
        }
    }

    @SneakyThrows
    private Set<ItemAttributesRQ> getLauchAttributes() {
        Set<ItemAttributesRQ> tags = new HashSet<>();
        tags.addAll(extractTags(System.getProperty(PROP_TAGS)));
        tags.addAll(getLaunchAttributesFromProperties(System.getProperty(PROP_ATTRIBUTES)));
        tags.add(new ItemAttributesRQ("Test System", runContext.getTestSystemName()));
        if (local) {
            tags.add(new ItemAttributesRQ("wiki", InetAddress.getLocalHost().getHostName()));
        }
        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.PROPERTY_FILE_NAME)));
            tags.addAll(extractTags(propertyFile.getProperty(PROP_TAGS)));
            tags.addAll(getLaunchAttributesFromProperties(propertyFile.getProperty(PROP_ATTRIBUTES)));
        } catch (NullPointerException | IOException e) {
            logger.info("[Orangebeard] Cannot find " + PROP_TAGS + " or " + PROP_ATTRIBUTES + " in " + PROPERTY_FILE_NAME);
        }
        return tags;
    }

    private Set<ItemAttributesRQ> getLaunchAttributesFromProperties(String propAttributes) {
        if (propAttributes == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(propAttributes.split(";")).map(attribute -> {
            String[] split = attribute.split(":");
            if (split.length == 1) {
                return new ItemAttributesRQ(split[0]);
            } else if (split.length == 2) {
                return new ItemAttributesRQ(split[0], split[1]);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<ItemAttributesRQ> extractTags(String propTags) {
        if (propTags != null) {
            return Arrays.stream(propTags.split("\\s*[;,]\\s*")).map(ItemAttributesRQ::new).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private String getLaunchNameFromProperties(TestPage testPage) {
        if (System.getProperty(PROP_LAUNCHNAME) != null) {
            return System.getProperty(PROP_LAUNCHNAME);
        }

        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.PROPERTY_FILE_NAME)));
            String launchName = propertyFile.getProperty(PROP_LAUNCHNAME);
            return launchName != null ? launchName : testPage.getFullPath();
        } catch (NullPointerException | IOException e) {
            logger.warn("[Orangebeard] Cannot find " + PROP_LAUNCHNAME + " in " + PROPERTY_FILE_NAME + ". Classname is set as launch name!");
            return testPage.getFullPath();
        }
    }

    private String getEndpointFromProperties() {
        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.PROPERTY_FILE_NAME)));
            return propertyFile.getProperty(PROP_AUGMENTED_ENDPOINT);
        } catch (NullPointerException | IOException e) {
            return null;
        }
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

    private String getToken() {
        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.PROPERTY_FILE_NAME)));
            String token = propertyFile.getProperty(PROP_AUGMENTED_TOKEN);
            if (token != null) {
                return token;
            } else {
                return System.getenv(AUGMENTED_TESTING_TOKEN);
            }
        } catch (NullPointerException | IOException e) {
            return null;
        }
    }

    private StartTestItemRQ getTest(TestPage testPage) {
        StartTestItemRQ startTestItemRQ = new StartTestItemRQ();
        startTestItemRQ.setStartTime(Date.from(Instant.now()));
        startTestItemRQ.setName(getTestName(testPage));
        startTestItemRQ.setType(determinePageType(testPage.getName()));
        startTestItemRQ.setUniqueId(UUID.randomUUID().toString());

        if (testPage instanceof WikiTestPage) {
            PageData pageData = ((WikiTestPage) testPage).getData();

            String helpText = pageData.getAttribute(WikiPageProperty.HELP);
            startTestItemRQ.setDescription(helpText);

            if (pageData.getAttribute(WikiPageProperty.SUITES) != null) {
                Set<ItemAttributesRQ> tags = extractTags(pageData.getAttribute(WikiPageProperty.SUITES));
                startTestItemRQ.setAttributes(tags);
            }
        }

        return startTestItemRQ;
    }

    private String determinePageType(String pageName) {
        switch (pageName) {
            case "SuiteSetUp":
                return "BEFORE_METHOD";
            case "SuiteTearDown":
                return "AFTER_METHOD";
            default:
                return "STEP";
        }
    }

    private StartTestItemRQ getSuite(String suiteName, Map<String, String> metaData) {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(suiteName);
        rq.setType("SUITE");
        rq.setStartTime(Calendar.getInstance().getTime());
        if (metaData.containsKey("description") && metaData.get("description") != null) {
            rq.setDescription(metaData.get("description"));
        }
        if (metaData.containsKey("tags")) {
            rq.setAttributes(extractTags(metaData.get("tags")));
        }


        return rq;
    }

    private FinishTestItemRQ getFinishTestItemRQ(String status) {
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Date.from(Instant.now()));
        rq.setStatus(status);
        return rq;
    }
}
