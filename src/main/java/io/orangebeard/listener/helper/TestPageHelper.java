package io.orangebeard.listener.helper;

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

    public static String getFullSuiteName(TestPage testPage) {
        String fullTestName = getFullTestName(testPage);
        if (fullTestName.contains(".")) {
            return fullTestName.substring(0, fullTestName.lastIndexOf("."));
        } else {
            return "default";
        }
    }
}
