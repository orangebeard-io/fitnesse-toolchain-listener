package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ToolchainRunningContextTest {

//    @Test
//    public void when_the_suite_is_present_a_suite_id_is_returned() {
//        String suiteName = "suite";
//        UUID suiteId = UUID.fromString("77d58d71-babf-4038-bded-f2a618383b51");
//        List<Suite> suite =new ArrayList<>(new Suite(suiteId,null,suiteName , Collections.singletonList(suiteName)));
//        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());
//
//        toolchainRunningContext.addSuite(suiteName, suiteId);
//
//        assertThat(toolchainRunningContext.getSuiteId(suiteName)).isEqualTo(suiteId);
//    }

    @Test
    public void when_there_are_no_suites_null_is_returned_as_a_suite_id() {
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        assertThat(toolchainRunningContext.getSuiteId("suite")).isNull();
    }
}
