package io.orangebeard.listener;

import io.orangebeard.client.entity.ChangedComponent;

import java.util.ArrayList;
import java.util.Set;
import org.junit.Test;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangedComponentsHelperTest {

    @Test
    public void without_the_environment_variable_the_list_of_changed_components_is_empty() {
        Set<ChangedComponent> result = ChangedComponentsHelper.getChangedComponents();

        assertThat(result).isNull();
    }

    @Test
    public void get_changed_component_from_an_environment_variable() throws Exception {
        withEnvironmentVariable("orangebeard.changedComponents", "componentA")
                .execute(() -> {
                    Set<ChangedComponent> result = ChangedComponentsHelper.getChangedComponents();

                    assertThat(new ArrayList<>(result).get(0).getComponentName()).isEqualTo("componentA");
                });
    }

    @Test
    public void get_changed_components_from_an_environment_variable() throws Exception {
        withEnvironmentVariable("orangebeard.changedComponents", "componentA, componentB")
                .execute(() -> {
                    Set<ChangedComponent> result = ChangedComponentsHelper.getChangedComponents();

                    assertThat(result).extracting("componentName").containsOnly("componentA", "componentB");
                });
    }
}
