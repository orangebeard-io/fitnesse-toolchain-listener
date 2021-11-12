package io.orangebeard.listener;

import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.LogLevel;
import io.orangebeard.listener.entity.ScenarioLibraries;
import io.orangebeard.listener.helper.AttachmentHandler;
import io.orangebeard.listener.helper.ToolchainRunningContext;

import java.util.List;
import java.util.UUID;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private OrangebeardClient orangebeardClient;

    @Mock
    private AttachmentHandler orangebeardLogger;

    @Mock
    private ScenarioLibraries scenarioLibraries;

    @InjectMocks
    private OrangebeardTestSystemListener orangebeardTestSystemListener;

    @Mock
    private WikiTestPage testPage;

    @Test
    public void when_a_log_is_not_in_the_scenario_library_it_is_logged_separately() {
        when(scenarioLibraries.contains(any())).thenReturn(false);

        orangebeardTestSystemListener.testOutputChunk(testPage, "");

        verify(orangebeardClient, times(1)).log(any());
        verify(orangebeardLogger).attachFilesIfPresent(any(), any(), any());
    }

    @Test
    public void when_a_log_is_in_the_scenario_library_it_is_not_logged_separately() {
        when(scenarioLibraries.contains(any())).thenReturn(true);

        orangebeardTestSystemListener.testOutputChunk(testPage, "");

        verify(orangebeardClient, times(0)).log(any());
    }

    @Test
    public void scenario_libraries_from_a_test_page_are_added() {
        orangebeardTestSystemListener.testOutputChunk(testPage, "");

        verify(scenarioLibraries).add(any());
    }

    @Test
    public void when_a_table_is_logged_and_it_is_an_error_loglevel_becomes_error() {
        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\"></table>");

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);
        verify(orangebeardClient, times(1)).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getLogLevel()).isEqualTo(LogLevel.error);
    }

    @Test
    public void weird_characters_are_removed_from_the_log_and_orangebeard_styling_is_applied_on_tables() {
        orangebeardTestSystemListener.testOutputChunk(testPage, "<table class=\"error\">\u0000</table>");

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);
        verify(orangebeardClient, times(1)).log(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("<table style=\"background-color:#ffe67b; padding: 3px; border-radius: 3px;\"></table>");
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
}
