package io.orangebeard.listener;

import java.util.Arrays;
import java.util.Set;

import io.orangebeard.listener.v3client.entities.ChangedComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public class ChangedComponentsHelper {
    private static final Logger logger = LoggerFactory.getLogger(OrangebeardTestSystemListener.class);

    private ChangedComponentsHelper() {
        // static class
    }

    public static Set<ChangedComponent> getChangedComponents() {
        String changedComponents = System.getenv("orangebeard.changedComponents");
        if (changedComponents == null || changedComponents.isEmpty()) {
            changedComponents = System.getProperty("orangebeard.changedComponents");
        }
        logger.info("Changed components: [" + changedComponents + "]");
        if (changedComponents == null) {
            return emptySet();
        }
        return Arrays.stream(changedComponents
                .split(","))
                .map(String::trim)
                .map(it -> new ChangedComponent(it, null))
                .collect(toSet());
    }
}
