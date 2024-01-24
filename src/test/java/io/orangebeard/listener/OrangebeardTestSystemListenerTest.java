package io.orangebeard.listener;

import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.log.LogLevel;
import io.orangebeard.client.v3.OrangebeardAsyncV3Client;
import io.orangebeard.listener.entity.ScenarioLibraries;
import io.orangebeard.listener.helper.AttachmentHandler;
import io.orangebeard.listener.helper.LogStasher;
import io.orangebeard.listener.helper.TestPageHelper;
import io.orangebeard.listener.helper.ToolchainRunningContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystem;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Before;
import org.junit.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrangebeardTestSystemListenerTest {
    @Mock
    private OrangebeardProperties orangebeardProperties;
    @Mock
    private ToolchainRunningContext runningContext;
    @Mock
    private OrangebeardAsyncV3Client orangebeardClient;
    @Mock
    private AttachmentHandler orangebeardLogger;
    @Mock
    private ScenarioLibraries scenarioLibraries;
    @Mock
    private LogStasher logStasher;
    @Mock
    private WikiTestPage testPage;
    @Mock
    private WikiPage wikiPage;
    @Mock
    private PageData testPageData;

    @InjectMocks
    private OrangebeardTestSystemListener orangebeardTestSystemListener;

    private ArgumentCaptor<UUID> uuidArgumentCaptor;
    private ArgumentCaptor<StartV3TestRun> startTestRunArgumentCaptor;
    private String testSetName;
    private String description;
    private TestSystem testSystem;
    private Set<Attribute> attributes;
    private String fullSuiteName;
    private String testPageName;
    private String sourcePageName;

    @Before
    public void setup() {
        testSystem = mock(TestSystem.class);
        uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        startTestRunArgumentCaptor = ArgumentCaptor.forClass(StartV3TestRun.class);
        testSetName = "testSetName";
        description = "desc";
        attributes = Set.of(new Attribute("key", "value"));
        fullSuiteName = "IntegrationTests.ApiTests.Projects";
        testPageName = "SuiteSetUp";
        sourcePageName = "Projects";
    }

    @Test
    public void test_system_is_started_properly_when_uuid_is_present() {
        UUID testRunUUID = UUID.randomUUID();

        // We want to verify here that the flow is processed properly and that announce test run is started
        doNothing().when(orangebeardProperties).checkPropertiesArePresent();
        when(orangebeardProperties.isAnnouncedUUIDPresent()).thenReturn(true);
        when(orangebeardProperties.getTestRunUUID()).thenReturn(testRunUUID);
        doNothing().when(orangebeardClient).startAnnouncedTestRun(any(UUID.class));

        orangebeardTestSystemListener.testSystemStarted(testSystem);

        verify(orangebeardClient, times(1)).startAnnouncedTestRun(uuidArgumentCaptor.capture());
        assertThat(uuidArgumentCaptor.getValue()).isEqualTo(testRunUUID);

        // We want to verify here if the proper value of test run uuid is passed to the running context
        ToolchainRunningContext runContext = orangebeardTestSystemListener.getRunContext();
        assertThat(runContext.getTestRunUUID()).isEqualTo(testRunUUID);

        verify(orangebeardClient, times(0)).startTestRun(any());
    }

    @Test
    public void test_system_is_started_properly_when_uuid_is_not_present() {
        UUID testRunUUID = UUID.randomUUID();

        // We want to verify here that the flow is processed properly and that test run is started
        doNothing().when(orangebeardProperties).checkPropertiesArePresent();
        when(orangebeardProperties.isAnnouncedUUIDPresent()).thenReturn(false);
        when(orangebeardProperties.getTestSetName()).thenReturn(testSetName);
        when(orangebeardProperties.getDescription()).thenReturn(description);
        when(orangebeardProperties.getAttributes()).thenReturn(attributes);
        when(orangebeardClient.startTestRun(any())).thenReturn(testRunUUID);
        when(testSystem.getName()).thenReturn("Test System");

        orangebeardTestSystemListener.testSystemStarted(testSystem);

        verify(orangebeardClient, times(1)).startTestRun(startTestRunArgumentCaptor.capture());
        assertThat(startTestRunArgumentCaptor.getValue().getTestSetName()).isEqualTo(testSetName);
        assertThat(startTestRunArgumentCaptor.getValue().getDescription()).isEqualTo(description);

        // We want to verify here if the proper object of StartTestRun is passed to the running context
        // If the proper StartTestRun is passed then run context will be initialized properly and an uuid will be returned
        ToolchainRunningContext runContext = orangebeardTestSystemListener.getRunContext();
        assertThat(runContext.getTestRunUUID()).isNotNull();

        verify(orangebeardClient, times(0)).startAnnouncedTestRun(any());
    }

    @Test
    public void when_a_test_system_is_started_the_orangebeard_client_is_called() {
        String testSetName = "testSetName";
        String description = "desc";

        TestSystem testSystem = mock(TestSystem.class);
        when(testSystem.getName()).thenReturn("test system name");

        when(orangebeardProperties.getTestSetName()).thenReturn(testSetName);
        when(orangebeardProperties.getDescription()).thenReturn(description);

        orangebeardTestSystemListener.testSystemStarted(testSystem);

        ArgumentCaptor<StartV3TestRun> argumentCaptor = ArgumentCaptor.forClass(StartV3TestRun.class);
        verify(orangebeardClient).startTestRun(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getTestSetName()).isEqualTo(testSetName);
        assertThat(argumentCaptor.getValue().getDescription()).isEqualTo(description);
        assertThat(argumentCaptor.getValue().getStartTime()).isNotNull();
    }

    @Test
    public void when_a_test_system_is_started_changed_components_are_passed_along_if_available() throws Exception {
        TestSystem testSystem = mock(TestSystem.class);
        when(testSystem.getName()).thenReturn("test system name");

        withEnvironmentVariable("orangebeard.changedComponents", "componentA, componentB").execute(() -> {
            orangebeardTestSystemListener.testSystemStarted(testSystem);

            ArgumentCaptor<StartV3TestRun> argumentCaptor = ArgumentCaptor.forClass(StartV3TestRun.class);
            verify(orangebeardClient).startTestRun(argumentCaptor.capture());

            Assertions.assertThat(argumentCaptor.getValue().getChangedComponents()).extracting("componentName").containsOnly("componentA", "componentB");
        });
    }

    @Test
    public void when_a_log_is_not_in_the_scenario_library_it_is_logged_separately() {
        UUID testId = UUID.randomUUID();
        UUID testRunUUID = UUID.randomUUID();
        UUID logUUID = UUID.randomUUID();
        String latestTestName = "test name";

        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.DEBUG)).thenReturn(true);
        when(runningContext.getTestRunUUID()).thenReturn(testRunUUID);
        when(runningContext.getLatestTestName()).thenReturn(latestTestName);
        when(runningContext.getTestId(anyString())).thenReturn(testId);
        when(scenarioLibraries.contains(any())).thenReturn(false);
        when(orangebeardClient.log(any())).thenReturn(logUUID);

        orangebeardTestSystemListener.testOutputChunk(testPage, "");

        verify(orangebeardClient, times(1)).log(any(Log.class));
        verify(orangebeardLogger, times(1)).attachFilesIfPresent(eq(testId), eq(testRunUUID), anyString(), eq(logUUID));
    }

    @Test
    public void when_a_log_is_in_the_scenario_library_it_is_not_logged_separately() {
        when(scenarioLibraries.contains(any())).thenReturn(true);

        orangebeardTestSystemListener.testOutputChunk(testPage, "");

        verify(orangebeardClient, times(0)).log(any(Log.class));
    }

    @Test
    public void scenario_libraries_from_a_test_page_are_added() {
        orangebeardTestSystemListener.testOutputChunk(testPage, "");

        verify(scenarioLibraries).add(any());
    }

    @Test
    public void when_a_table_is_logged_and_it_is_an_error_loglevel_becomes_error_and_log_format_is_html() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.ERROR)).thenReturn(true);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);
        verify(orangebeardClient, times(1)).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getLogLevel()).isEqualTo(LogLevel.ERROR);
        assertThat(argumentCaptor.getValue().getLogFormat()).isEqualTo(LogFormat.HTML);
    }

    @Test
    public void when_a_table_is_logged_and_it_is_an_warn_loglevel_it_is_not_sent_to_orangebeard_when_this_should_not_be_the_case() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.ERROR)).thenReturn(false);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

        verify(orangebeardClient, times(0)).log(any(Log.class));
    }

    @Test
    public void weird_characters_are_removed_from_the_log_and_orangebeard_styling_is_applied_on_tables() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.ERROR)).thenReturn(true);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\">\u0000</table>");

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);
        verify(orangebeardClient, times(1)).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("<table style=\"background-color:#ffe67b; padding: 3px; border-radius: 3px;\"></table>");
        assertThat(argumentCaptor.getValue().getLogFormat()).isEqualTo(LogFormat.HTML);
    }

    @Test
    public void test_can_be_started_properly_in_existing_suite() {
        UUID suiteUUID = UUID.fromString("f07908f8-70c4-4c10-ae27-771a8372a0ef");
        UUID testRunUUID = UUID.randomUUID();

        when(runningContext.getTestRunUUID()).thenReturn(testRunUUID);
        when(runningContext.getSuiteId(any())).thenReturn(suiteUUID);

        when(testPage.getFullPath()).thenReturn("test");
        when(testPage.getName()).thenReturn(testPageName);
        when(testPage.getData()).thenReturn(testPageData);
        when(testPageData.getAttribute(anyString())).thenReturn("Suites");

        orangebeardTestSystemListener.testStarted(testPage);

        verify(orangebeardClient, times(1)).startTest(any());
        verify(orangebeardClient, times(0)).startSuite(any());
        verify(runningContext, times(0)).addSuite(anyString(), any(UUID.class), any(UUID.class), any());
    }

    @Test
    public void test_can_be_started_properly_in_new_suite() {

        UUID testRunUUID = UUID.randomUUID();
        UUID dummySuite = UUID.randomUUID();

        when(runningContext.getTestRunUUID()).thenReturn(testRunUUID);
        when(runningContext.getSuiteId(any())).thenReturn(null);

        when(testPage.getFullPath()).thenReturn(fullSuiteName + ".Test");
        when(testPage.getName()).thenReturn(testPageName);
        when(testPage.getData()).thenReturn(testPageData);
        when(testPage.getSourcePage()).thenReturn(wikiPage);
        when(wikiPage.getParent()).thenReturn(wikiPage);
        when(wikiPage.getName()).thenReturn(sourcePageName);
        when(testPageData.getAttribute(anyString())).thenReturn("Suites");
        when(orangebeardClient.startSuite(any())).thenReturn(List.of(dummySuite));

        orangebeardTestSystemListener.testStarted(testPage);

        verify(orangebeardClient, times(1)).startTest(any());

        String[] suites = fullSuiteName.split("\\.");
        verify(orangebeardClient, times(suites.length)).startSuite(any());
    }

    @Test
    public void when_a_test_is_started_related_scenario_libraries_are_logged_on_debug_level() {
        UUID suiteUUID = UUID.randomUUID();
        UUID testRunUUID = UUID.randomUUID();

        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.DEBUG)).thenReturn(true);
        when(wikiPage.getHtml()).thenReturn("<table class=\"error\">\u0000</table>");
        when(testPage.getScenarioLibraries()).thenReturn(List.of(wikiPage));

        when(runningContext.getTestRunUUID()).thenReturn(testRunUUID);
        when(runningContext.getSuiteId(any())).thenReturn(suiteUUID);

        when(testPage.getFullPath()).thenReturn(fullSuiteName + ".Test");
        when(testPage.getName()).thenReturn(testPageName);
        when(testPage.getData()).thenReturn(testPageData);
        when(testPageData.getAttribute(anyString())).thenReturn("Suites");

        orangebeardTestSystemListener.testStarted(testPage);

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);

        verify(orangebeardClient).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getLogLevel()).isEqualTo(LogLevel.DEBUG);
        assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("<table style=\"background-color:#ffe67b; padding: 3px; border-radius: 3px;\"></table>");
    }

    @Test
    public void when_the_log_level_is_above_debug_scenario_libraries_are_not_logged() {
        UUID suiteUUID = UUID.randomUUID();
        UUID testRunUUID = UUID.randomUUID();

        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.DEBUG)).thenReturn(false);
        when(testPage.getScenarioLibraries()).thenReturn(List.of(wikiPage));

        when(runningContext.getTestRunUUID()).thenReturn(testRunUUID);
        when(runningContext.getSuiteId(any())).thenReturn(suiteUUID);

        when(testPage.getFullPath()).thenReturn(fullSuiteName + ".Test");
        when(testPage.getName()).thenReturn(testPageName);
        when(testPage.getData()).thenReturn(testPageData);
        when(testPageData.getAttribute(anyString())).thenReturn("Suites");

        orangebeardTestSystemListener.testStarted(testPage);

        verify(orangebeardClient, times(0)).log(any(Log.class));
    }

    @Test
    public void when_log_stashing_is_disabled_logs_are_sent_directly_and_not_stashed() {
        var testId = UUID.randomUUID();

        try (MockedStatic<TestPageHelper> tphMock = mockStatic(TestPageHelper.class)) {
            tphMock.when(() -> TestPageHelper.getRelativeName(any(TestPage.class))).thenReturn("blabla");

            when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.ERROR)).thenReturn(true);
            when(orangebeardProperties.isLogsAtEndOfTest()).thenReturn(false);

            when(runningContext.hasTest(any())).thenReturn(true);
            when(runningContext.getTestId(any())).thenReturn(testId);

            orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");
            orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

            orangebeardTestSystemListener.testComplete(testPage, new TestSummary(2, 0, 0, 0));

            verify(orangebeardClient, times(2)).log(any(Log.class));
            verify(logStasher, times(0)).sendLogs(any());
        }
    }

    @Test
    public void when_log_stashing_is_enabled_logs_are_stashed_and_sent_when_the_test_is_completed() {
        var testId = UUID.randomUUID();

        try (MockedStatic<TestPageHelper> tphMock = mockStatic(TestPageHelper.class)) {
            tphMock.when(() -> TestPageHelper.getRelativeName(any(TestPage.class))).thenReturn("blabla");

            when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.ERROR)).thenReturn(true);
            when(orangebeardProperties.isLogsAtEndOfTest()).thenReturn(true);

            when(runningContext.hasTest(any())).thenReturn(true);
            when(runningContext.getTestId(any())).thenReturn(testId);

            orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");
            orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

            orangebeardTestSystemListener.testComplete(testPage, new TestSummary(2, 0, 0, 0));

            verify(orangebeardClient, times(0)).log(any(Log.class));
            verify(logStasher, times(1)).sendLogs(testId);
        }
    }
}
