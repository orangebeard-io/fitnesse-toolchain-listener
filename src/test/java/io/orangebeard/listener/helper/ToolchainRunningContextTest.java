package io.orangebeard.listener.helper;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ToolchainRunningContextTest {

    @Test
    public void when_the_suite_is_present_a_suite_id_is_returned() {
        String suiteName = "suite";
        UUID suiteId = UUID.fromString("77d58d71-babf-4038-bded-f2a618383b51");
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        toolchainRunningContext.addSuite(suiteName, suiteId, LocalDateTime.now());

        assertThat(toolchainRunningContext.getSuiteId(suiteName)).isEqualTo(suiteId);
    }

    @Test
    public void when_there_are_no_suites_null_is_returned_as_a_suite_id() {
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        assertThat(toolchainRunningContext.getSuiteId("suite")).isNull();
    }
}
