package io.orangebeard.testlisteners.fitnesse.helper;

import fitnesse.testsystems.TestPage;

public class TestPageHelper {

    //hide!
    private TestPageHelper() {
    }

    public static String getFullTestName(TestPage testPage) {
        return testPage.getFullPath();
    }

    public static String getTestName(TestPage testPage) {
        return testPage.getName();
    }

    public static String getSuiteName(TestPage testPage) {
        String[] testNameParts = getFullTestName(testPage).split("\\.");
        if (testNameParts.length > 1) {
            return testNameParts[testNameParts.length - 2];
        } else {
            return "default";
        }
    }
}
