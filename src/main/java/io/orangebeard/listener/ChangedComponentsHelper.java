package io.orangebeard.listener;

import io.orangebeard.client.entity.ChangedComponent;

import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class ChangedComponentsHelper {
    private ChangedComponentsHelper() {
        // static class
    }

    public static Set<ChangedComponent> getChangedComponents() {
        String changedComponents = System.getenv("orangebeard.changedComponents");
        if (changedComponents == null) {
            return null;
        }
        return Arrays.stream(changedComponents
                .split(","))
                .map(String::trim)
                .map(it -> new ChangedComponent(it, null))
                .collect(toSet());
    }
}
