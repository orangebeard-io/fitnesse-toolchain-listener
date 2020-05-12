package io.orangebeard.testlisteners.fitnesse;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import fitnesse.testsystems.*;
import io.orangebeard.testlisteners.fitnesse.helper.OrangebeardLogger;
import io.orangebeard.testlisteners.fitnesse.helper.OrangebeardTableLogParser;
import io.orangebeard.testlisteners.fitnesse.helper.ToolchainRunningContext;
import io.reactivex.Maybe;
import lombok.NoArgsConstructor;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static io.orangebeard.testlisteners.fitnesse.helper.TestPageHelper.getSuiteName;
import static io.orangebeard.testlisteners.fitnesse.helper.TestPageHelper.getTestName;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@NoArgsConstructor
public class OrangebeardTestSystemListener implements TestSystemListener, Closeable {

    private static final String PROP_LAUNCHNAME = "rp.launch";
    private static final String PROP_AUGMENTED_ENDPOINT = "at.augmented-testing.endpoint";
    private static final String PROP_AUGMENTED_TOKEN = "at.augmented-testing.token";
    private static final String AUGMENTED_TESTING_TOKEN = "AUGMENTED_TESTING_TOKEN";

    private static final String PROP_TAGS = "rp.tags";
    private static final String PROP_ATTRIBUTES = "rp.attributes";
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardTestSystemListener.class);
    private String propertyFileName = "reportportal.properties";
    private OrangebeardLogger orangebeardLogger;
    private ReportPortal reportPortal;
    private Launch launch;
    private ToolchainRunningContext context = new ToolchainRunningContext();
    private OrangebeardTableLogParser htmlChunkParser = new OrangebeardTableLogParser();
    private Maybe<String> launchId;


    public OrangebeardTestSystemListener(OrangebeardLogger orangebeardLogger, ReportPortal reportPortal, ToolchainRunningContext context) {
        this.orangebeardLogger = orangebeardLogger;
        this.reportPortal = reportPortal;
        this.context = context;
    }

    @Override
    public void testSystemStarted(TestSystem testSystem) {

    }



    @Override
    public void testOutputChunk(String s) {
        Maybe<String> testId = context.getTestId(context.getLatestTestName());
        String level = "DEBUG";
        String tableBlock = s.substring(s.indexOf("<table"), s.lastIndexOf("</table>") + 8);
        if(s.contains("class=\"error\"") || s.contains("class=\"fail\"")) {
            level = "ERROR";
        } else if (reportTable(tableBlock)) {
            level = "INFO";
        }

        if(s.toLowerCase().contains("<table")) {
            s = tableBlock;
        }

        s = s.replaceAll("class=\"fail\"", "style=\"background-color:#ffaeaf; padding: 3px; border-radius: 3px;\"");
        s = s.replaceAll("class=\"pass\"", "style=\"background-color:#44ffa5; padding: 3px; border-radius: 3px;\"");
        s = s.replaceAll("class=\"diff\"", "style=\"background-color:#f1e38f; padding: 3px; border-radius: 3px;\"");
        s = s.replaceAll("class=\"ignore\"", "style=\"background-color:#a8e2ff; padding: 3px; border-radius: 3px;\"");
        s = s.replaceAll("class=\"error\"", "style=\"background-color:#ffe67b; padding: 3px; border-radius: 3px;\"");
        s = s.replaceAll("class=\"slimRowTitle\"", "style=\"font-weight:bold; background-color: #ececec;\"");
        s = s.replaceAll("class=\"title\"", "style=\"font-size:1.2em; font-weight:bold; border-bottom:1px solid silver;\"");
        s = s.replaceAll("class=\"toolchainTable [^\"]*\"", "style=\"box-shadow: 0px 3px 10px 0px rgba(0, 0, 0, 0.19);\"");

        String enrichedLog = htmlChunkParser.embedImagesAndStripHyperlinks(s);
        orangebeardLogger.sendLogData(testId.blockingGet(), enrichedLog, level);
        orangebeardLogger.sendAttachmentsIfPresent(testId.blockingGet(), s);
    }

    private boolean reportTable(String tableBlock) {
       return !tableBlock.startsWith("<table class=\"toolchainTable scenarioTable\"") &&
               !tableBlock.startsWith("<table class=\"toolchainTable importTable\"") &&
               !tableBlock.startsWith("<table class=\"toolchainTable libraryTable\"");
    }


    @Override
    public void testStarted(TestPage testPage) {

        startLaunchIfRequired(testPage);

        Maybe<String> suiteId = getAndOrStartSuite(testPage);
        String testName = getTestName(testPage);

        StartTestItemRQ startTestItemRQ = getTest(testPage);
        Maybe<String> id = launch.startTestItem(suiteId, startTestItemRQ);
        context.addTest(testName, id);
        logger.info("[Orangebeard] test {} started with id {} and suite id {}", testName, id.blockingGet(), suiteId.blockingGet());
    }

    private void startLaunchIfRequired(TestPage testPage) {
        if (this.reportPortal == null) {
            this.reportPortal = ReportPortal.builder().build();
            this.orangebeardLogger = new OrangebeardLogger(reportPortal);
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
        if (context.hasTest(testName)) {
            logger.info("[Orangebeard] test {} finished", getTestName(testPage));
            FinishTestItemRQ rq = getFinishTestItemRQ(testResult(testSummary));
            Maybe<String> testId = context.getTestId(getTestName(testPage));
            launch.finishTestItem(testId, rq);
            context.remove(testName);
        }
    }

    @Override
    public void testSystemStopped(TestSystem testSystem, Throwable throwable) {
        logger.info("[Orangebeard] test run finished");
        stopAllSuites();
        FinishExecutionRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Date.from(Instant.now()));
        orangebeardLogger.attachFitNesseResultsToRun(launchId.blockingGet());
        launch.finish(rq);
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
        if(testSummary.getExceptions() > 0 || testSummary.getWrong() > 0) {
            return "failed";
        } else {
            return "passed";
        }
    }

    private Maybe<String> getAndOrStartSuite(TestPage testPage) {
        String suiteName = getSuiteName(testPage);
        Maybe<String> suiteId = context.getSuiteId(suiteName);
        if (suiteId == null) {
            StartTestItemRQ rq = getSuite(suiteName);
            suiteId = launch.startTestItem(rq);
            context.addSuite(suiteName, suiteId);
            logger.info("[Orangebeard] suite {} started with id {}", suiteName, suiteId.blockingGet());
        }
        return suiteId;
    }

    private void stopAllSuites() {
        for (Maybe<String> suiteId : context.getAllSuiteIds()) {
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
        startLaunchRQ.setAttributes(getAttributes());
        return startLaunchRQ;
    }

    private void reportStartLaunchToAugmented(String launchUUID) {
        String endpoint = getEndpointFromProperties();
        String projectId = System.getenv("projectId");
        String jobId = "jenkins-" + System.getenv("JOB_NAME") + "-" + System.getenv("BUILD_ID");
        String token = getToken();

        if (endpoint == null) {
            logger.warn("[Orangebeard] Cannot find " + PROP_AUGMENTED_ENDPOINT + " in reportportal.properties. Nothing will be reported to the augmented endpoint!");
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

    private Set<ItemAttributesRQ> getAttributes() {
        Set<ItemAttributesRQ> tags = new HashSet<>();
        tags.addAll(extractTags(System.getProperty(PROP_TAGS)));
        tags.addAll(extractAttributes(System.getProperty(PROP_ATTRIBUTES)));

        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.propertyFileName)));
            tags.addAll(extractTags(propertyFile.getProperty(PROP_TAGS)));
            tags.addAll(extractAttributes(propertyFile.getProperty(PROP_ATTRIBUTES)));
        } catch (NullPointerException | IOException e) {
            logger.warn("[Orangebeard] Cannot find " + PROP_TAGS + " or " + PROP_ATTRIBUTES + " in reportportal.properties!");
        }
        return tags;
    }

    private Set<ItemAttributesRQ> extractAttributes(String propAttributes) {
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
            return Arrays.stream(propTags.split(";")).map(ItemAttributesRQ::new).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private String getLaunchNameFromProperties(TestPage testPage) {
        if (System.getProperty(PROP_LAUNCHNAME) != null) {
            return System.getProperty(PROP_LAUNCHNAME);
        }

        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.propertyFileName)));
            String launchName = propertyFile.getProperty(PROP_LAUNCHNAME);
            return launchName != null ? launchName : testPage.getFullPath();
        } catch (NullPointerException | IOException e) {
            logger.warn("[Orangebeard] Cannot find " + PROP_LAUNCHNAME + " in reportportal.properties. Classname is set as launch name!");
            return testPage.getFullPath();
        }
    }

    private String getEndpointFromProperties() {
        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.propertyFileName)));
            return propertyFile.getProperty(PROP_AUGMENTED_ENDPOINT);
        } catch (NullPointerException | IOException e) {
            return null;
        }
    }

    private String getToken() {
        Properties propertyFile = new Properties();
        try {
            propertyFile.load(requireNonNull(OrangebeardTestSystemListener.class.getClassLoader().getResourceAsStream(this.propertyFileName)));
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
        startTestItemRQ.setType("STEP");
        startTestItemRQ.setUniqueId(UUID.randomUUID().toString());
        return startTestItemRQ;
    }

    private StartTestItemRQ getSuite(String suiteName) {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(suiteName);
        rq.setType("SUITE");
        rq.setStartTime(Calendar.getInstance().getTime());
        return rq;
    }

    private FinishTestItemRQ getFinishTestItemRQ(String status) {
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Date.from(Instant.now()));
        rq.setStatus(status);
        return rq;
    }
}
