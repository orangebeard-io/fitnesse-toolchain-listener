package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.Status;
import io.orangebeard.client.entity.TestItemType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import fitnesse.testsystems.ExecutionResult;

import static io.orangebeard.client.entity.Status.FAILED;
import static io.orangebeard.client.entity.Status.PASSED;
import static io.orangebeard.client.entity.Status.SKIPPED;

public class TypeConverter {

    private TypeConverter() {
        //static class
    }

    public static Set<Attribute> convertAttributes(String attributeString) {
        if (attributeString != null && attributeString.length() > 0) {
            return Arrays.stream(attributeString.split("\\s*[;,]\\s*"))
                    .map(Attribute::new)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public static TestItemType determinePageType(String pageName) {
        switch (pageName) {
            case "SuiteSetUp":
                return TestItemType.BEFORE_METHOD;
            case "SuiteTearDown":
                return TestItemType.AFTER_METHOD;
            default:
                return TestItemType.STEP;
        }
    }

    public static Status convertTestResultStatus(ExecutionResult result) {
        switch (result) {
            case ERROR:
            case FAIL:
                return FAILED;
            case IGNORE:
                return SKIPPED;
            default:
                return PASSED;
        }
    }
}
