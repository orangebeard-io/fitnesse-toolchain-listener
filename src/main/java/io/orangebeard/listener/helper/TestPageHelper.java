package io.orangebeard.listener.helper;

import fitnesse.testrunner.WikiTestPage;
import fitnesse.testrunner.WikiTestPageUtil;
import fitnesse.testsystems.TestPage;
import fitnesse.wiki.PageCrawler;

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

    public static String getRelativeName(TestPage testPage) {
        PageCrawler pageCrawler = ((WikiTestPage) testPage).getSourcePage().getPageCrawler();
        String relativeName = pageCrawler.getRelativeName(WikiTestPageUtil.getSourcePage(testPage));
        if ("".equals(relativeName)) {
            relativeName = String.format("(%s)", testPage.getName());
        }
        return relativeName;
    }
}
