package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.Attribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import fitnesse.testsystems.ExecutionResult;

import io.orangebeard.client.entity.test.TestStatus;
import io.orangebeard.client.entity.test.TestType;

import static io.orangebeard.client.entity.test.TestStatus.FAILED;
import static io.orangebeard.client.entity.test.TestStatus.PASSED;
import static io.orangebeard.client.entity.test.TestStatus.SKIPPED;

public class TypeConverter {

    private TypeConverter() {
        //static class
    }

    public static Set<Attribute> convertAttributes(String attributeString) {
        if (attributeString != null && !attributeString.isEmpty()) {
            return Arrays.stream(attributeString.split("\\s*[;,]\\s*"))
                    .map(Attribute::new)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public static TestType determinePageType(String pageName) {
        switch (pageName) {
            case "SuiteSetUp":
                return TestType.BEFORE;
            case "SuiteTearDown":
                return TestType.AFTER;
            default:
                return TestType.TEST;
        }
    }


    public static TestStatus convertTestResultStatus(ExecutionResult result) {
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
