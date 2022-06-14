package io.orangebeard.listener;

import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.LogLevel;
import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.listener.entity.ScenarioLibraries;
import io.orangebeard.listener.helper.AttachmentHandler;
import io.orangebeard.listener.helper.LogStasher;
import io.orangebeard.listener.helper.TestPageHelper;
import io.orangebeard.listener.helper.ToolchainRunningContext;

import java.util.List;
import java.util.UUID;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystem;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TestPageHelper.class})
public class OrangebeardTestSystemListenerTest {

    @Mock
    private OrangebeardProperties orangebeardProperties;

    @Mock
    private ToolchainRunningContext runningContext;

    @Mock
    private OrangebeardClient orangebeardClient;

    @Mock
    private AttachmentHandler orangebeardLogger;

    @Mock
    private ScenarioLibraries scenarioLibraries;

    @Mock
    private LogStasher logStasher;

    @Mock
    private WikiTestPage testPage;

    @InjectMocks
    private OrangebeardTestSystemListener orangebeardTestSystemListener;

    @Test
    public void when_a_test_system_is_started_the_orangebeard_client_is_called() {
        String testSetName = "testSetName";
        String description = "desc";

        TestSystem testSystem = mock(TestSystem.class);
        when(testSystem.getName()).thenReturn("test system name");

        when(orangebeardProperties.getTestSetName()).thenReturn(testSetName);
        when(orangebeardProperties.getDescription()).thenReturn(description);

        orangebeardTestSystemListener.testSystemStarted(testSystem);

        ArgumentCaptor<StartTestRun> argumentCaptor = ArgumentCaptor.forClass(StartTestRun.class);
        verify(orangebeardClient).startTestRun(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getName()).isEqualTo(testSetName);
        assertThat(argumentCaptor.getValue().getDescription()).isEqualTo(description);
        assertThat(argumentCaptor.getValue().getStartTime()).isNotNull();
    }

    @Test
    public void when_a_test_system_is_started_changed_components_are_passed_along_if_available() throws Exception {
        TestSystem testSystem = mock(TestSystem.class);
        when(testSystem.getName()).thenReturn("test system name");

        withEnvironmentVariable("orangebeard.changedComponents", "componentA, componentB")
                .execute(() -> {
                    orangebeardTestSystemListener.testSystemStarted(testSystem);

                    ArgumentCaptor<StartTestRun> argumentCaptor = ArgumentCaptor.forClass(StartTestRun.class);
                    verify(orangebeardClient).startTestRun(argumentCaptor.capture());

                    Assertions.assertThat(argumentCaptor.getValue().getChangedComponents()).extracting("componentName").containsOnly("componentA", "componentB");
                });
    }

    @Test
    public void when_a_log_is_not_in_the_scenario_library_it_is_logged_separately() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.debug)).thenReturn(true);
        when(scenarioLibraries.contains(any())).thenReturn(false);

        orangebeardTestSystemListener.testOutputChunk(testPage, "");

        verify(orangebeardClient, times(1)).log(any(Log.class));
        verify(orangebeardLogger).attachFilesIfPresent(any(), any(), any());
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
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.error)).thenReturn(true);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);
        verify(orangebeardClient, times(1)).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getLogLevel()).isEqualTo(LogLevel.error);
        assertThat(argumentCaptor.getValue().getLogFormat()).isEqualTo(LogFormat.HTML);
    }

    @Test
    public void when_a_table_is_logged_and_it_is_an_warn_loglevel_it_is_not_sent_to_orangebeard_when_this_should_not_be_the_case() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.error)).thenReturn(false);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

        verify(orangebeardClient, times(0)).log(any(Log.class));
    }

    @Test
    public void weird_characters_are_removed_from_the_log_and_orangebeard_styling_is_applied_on_tables() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.error)).thenReturn(true);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\">\u0000</table>");

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);
        verify(orangebeardClient, times(1)).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("<table style=\"background-color:#ffe67b; padding: 3px; border-radius: 3px;\"></table>");
        assertThat(argumentCaptor.getValue().getLogFormat()).isEqualTo(LogFormat.HTML);
    }

    @Test
    public void a_test_can_be_started() {
        UUID suiteUUID = UUID.fromString("f07908f8-70c4-4c10-ae27-771a8372a0ef");

        when(testPage.getFullPath()).thenReturn("test");
        when(testPage.getName()).thenReturn("test");
        when(runningContext.getSuiteId(any())).thenReturn(suiteUUID);
        when(testPage.getData()).thenReturn(mock(PageData.class));

        orangebeardTestSystemListener.testStarted(testPage);

        verify(orangebeardClient).startTestItem(eq(suiteUUID), any());
    }

    @Test
    public void when_a_test_is_started_related_scenario_libraries_are_logged_on_debug_level() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.debug)).thenReturn(true);
        WikiPage scenarioLibrary = mock(WikiPage.class);
        when(scenarioLibrary.getHtml()).thenReturn("<table class=\"error\">\u0000</table>");

        when(testPage.getFullPath()).thenReturn("test");
        when(testPage.getName()).thenReturn("test");
        when(runningContext.getSuiteId(any())).thenReturn(UUID.fromString("f07908f8-70c4-4c10-ae27-771a8372a0ef"));
        when(testPage.getData()).thenReturn(mock(PageData.class));

        when(testPage.getScenarioLibraries()).thenReturn(List.of(scenarioLibrary));

        orangebeardTestSystemListener.testStarted(testPage);

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);

        verify(orangebeardClient).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getLogLevel()).isEqualTo(LogLevel.debug);
        assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("<table style=\"background-color:#ffe67b; padding: 3px; border-radius: 3px;\"></table>");
    }

    @Test
    public void when_the_log_level_is_above_debug_scenario_libraries_are_not_logged() {
        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.debug)).thenReturn(false);
        WikiPage scenarioLibrary = mock(WikiPage.class);

        when(testPage.getFullPath()).thenReturn("test");
        when(testPage.getName()).thenReturn("test");
        when(runningContext.getSuiteId(any())).thenReturn(UUID.fromString("f07908f8-70c4-4c10-ae27-771a8372a0ef"));
        when(testPage.getData()).thenReturn(mock(PageData.class));

        when(testPage.getScenarioLibraries()).thenReturn(List.of(scenarioLibrary));

        orangebeardTestSystemListener.testStarted(testPage);

        verify(orangebeardClient, times(0)).log(any(Log.class));
    }

    @Test
    public void when_log_stashing_is_disabled_logs_are_sent_directly_and_not_stashed() {
        var testId = UUID.randomUUID();

        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.error)).thenReturn(true);
        when(orangebeardProperties.isLogsAtEndOfTest()).thenReturn(false);

        mockStatic(TestPageHelper.class);
        when(TestPageHelper.getTestName(any())).thenAnswer((Answer<String>) invocation -> "blabla");
        when(runningContext.hasTest(any())).thenReturn(true);
        when(runningContext.getTestId(any())).thenReturn(testId);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");
        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

        orangebeardTestSystemListener.testComplete(testPage, new TestSummary(2, 0, 0, 0));

        verify(orangebeardClient, times(2)).log(any(Log.class));
        verify(logStasher, times(0)).sendLogs(any());
    }

    @Test
    public void when_log_stashing_is_enabled_logs_are_stashed_and_sent_when_the_test_is_completed() {
        var testId = UUID.randomUUID();

        when(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.error)).thenReturn(true);
        when(orangebeardProperties.isLogsAtEndOfTest()).thenReturn(true);

        mockStatic(TestPageHelper.class);
        when(TestPageHelper.getTestName(any())).thenAnswer((Answer<String>) invocation -> "blabla");
        when(runningContext.hasTest(any())).thenReturn(true);
        when(runningContext.getTestId(any())).thenReturn(testId);

        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");
        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

        orangebeardTestSystemListener.testComplete(testPage, new TestSummary(2, 0, 0, 0));

        verify(orangebeardClient, times(0)).log(any(Log.class));
        verify(logStasher, times(1)).sendLogs(eq(testId));
    }
}
