package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.StartSuiteRQ;
import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ToolchainRunningContextTest {

    @Test
    public void when_the_suite_is_present_a_suite_id_is_returned() {
        String suiteName = "suite";
        UUID suiteId = UUID.fromString("77d58d71-babf-4038-bded-f2a618383b51");
        List<Suite> suite = Arrays.asList(new Suite(suiteId, null, suiteName, Collections.singletonList(suiteName)));
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        toolchainRunningContext.addSuites(suite);

        assertThat(toolchainRunningContext.getSuiteId(suiteName)).isEqualTo(suiteId);
    }

    @Test
    public void when_the_root_suite_is_known_by_the_context_only_the_subsuites_will_need_to_be_started() {
        String fullSuiteName = "parentSuite.childsuite.grandchildsuite";
        String rootSuiteName = "parentSuite";
        UUID parentSuiteId = UUID.fromString("98d58d71-babf-4012-bded-f2a618383b12");

        Suite rootSuite = new Suite(parentSuiteId, null, rootSuiteName, List.of(rootSuiteName));

        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        toolchainRunningContext.addSuites(List.of(rootSuite));
        StartSuiteRQ startSuite = toolchainRunningContext.getStartSuite(fullSuiteName);

        assertThat(startSuite.getParentSuiteUUID()).isEqualTo(parentSuiteId);
        assertThat(startSuite.getSuiteNames()).containsExactly("childsuite", "grandchildsuite");
    }

    @Test
    public void when_the_child_suite_is_known_by_the_context_only_the_grandchild_suite_will_need_to_be_started() {
        String fullSuiteName = "parentSuite.childsuite.grandchildsuite";
        String rootSuiteName = "parentSuite";
        String childSuiteName = "childsuite";
        UUID parentSuiteId = UUID.fromString("98d58d71-babf-4012-bded-f2a618383b12");
        UUID childSuiteId = UUID.fromString("f05124e3-48ee-4511-a487-a57365e09083");

        Suite rootSuite = new Suite(parentSuiteId, null, rootSuiteName, List.of(rootSuiteName));
        Suite childSuite = new Suite(childSuiteId, parentSuiteId, childSuiteName, List.of(rootSuiteName, childSuiteName));

        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        toolchainRunningContext.addSuites(List.of(rootSuite, childSuite));
        StartSuiteRQ startSuite = toolchainRunningContext.getStartSuite(fullSuiteName);

        assertThat(startSuite.getParentSuiteUUID()).isEqualTo(childSuiteId);
        assertThat(startSuite.getSuiteNames()).containsExactly("grandchildsuite");
    }

    @Test
    public void when_there_are_no_suites_null_is_returned_as_a_suite_id() {
        ToolchainRunningContext toolchainRunningContext = new ToolchainRunningContext(UUID.randomUUID());

        assertThat(toolchainRunningContext.getSuiteId("suite")).isNull();
    }
}
