package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.StartSuiteRQ;
import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ToolchainRunningContextTest {

    @Test
    public void when_the_suite_is_present_a_suite_id_is_returned() {
        String suiteName = "suite";
        UUID suiteId = UUID.fromString("77d58d71-babf-4038-bded-f2a618383b51");
        List<Suite> suite = Arrays.asList(new Suite(suiteId,null,suiteName , Collections.singletonList(suiteName)));
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        toolchainRunningContext.addSuites(suite);

        assertThat(toolchainRunningContext.getSuiteId(suiteName)).isEqualTo(suiteId);
    }

    @Test
    public void when_the_suitepath_is_given_then_returns_suitestobecreated_along_with_parentsuiteid() {
        String suiteName = "parentSuite.childsuite";
        String parentSuiteName = "parentSuite";
        UUID suiteId = UUID.fromString("77d58d71-babf-4038-bded-f2a618383b51");
        UUID parentSuiteId = UUID.fromString("98d58d71-babf-4012-bded-f2a618383b12");

        List<Suite> parentSuite = Arrays.asList(new Suite(parentSuiteId,null,parentSuiteName , Collections.singletonList(parentSuiteName)));
        List<Suite> suite = Arrays.asList(new Suite(suiteId,parentSuiteId,suiteName , Collections.singletonList(suiteName)));
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        toolchainRunningContext.addSuites(parentSuite);
        StartSuiteRQ startSuite= toolchainRunningContext.getStartSuite(Arrays.asList(suiteName.split("\\.")));

        assertThat(startSuite.getParentSuiteUUID()).isEqualTo(parentSuiteId);
        assertThat(startSuite.getSuiteNames()).containsOnly("childsuite");
    }


    @Test
    public void when_there_are_no_suites_null_is_returned_as_a_suite_id() {
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        assertThat(toolchainRunningContext.getSuiteId("suite")).isNull();
    }
}
