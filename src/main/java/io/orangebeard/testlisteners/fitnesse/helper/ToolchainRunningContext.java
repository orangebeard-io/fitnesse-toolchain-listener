package io.orangebeard.testlisteners.fitnesse.helper;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ToolchainRunningContext {

    private HashMap<String, Maybe<String>> tests = new HashMap<>();
    private HashMap<String, Maybe<String>> suites = new HashMap<>();
    private String latestTest;
    private String testSystemName;

    public void addTest(String testName, Maybe<String> id) {
        tests.put(testName, id);
        latestTest = testName;
    }

    public Maybe<String> getTestId(String testName) {
        return tests.get(testName);
    }

    public void remove(String testName) {
        tests.remove(testName);
    }

    public boolean hasTest(String testName) {
        return tests.containsKey(testName);
    }

    public Maybe<String> getSuiteId(String fullSuiteName) {
        return suites.get(fullSuiteName);
    }

    public void addSuite(String fullSuiteName, Maybe<String> suiteId) {
        suites.put(fullSuiteName, suiteId);
    }

    public List<Maybe<String>> getAllSuiteIds() {
        return new ArrayList<>(suites.values());
    }

    public String getLatestTestName() {
        return latestTest;
    }

    public String getTestSystemName() {
        return testSystemName;
    }

    public void setTestSystemName(String testSystemName) {
        this.testSystemName = testSystemName;
    }
}
