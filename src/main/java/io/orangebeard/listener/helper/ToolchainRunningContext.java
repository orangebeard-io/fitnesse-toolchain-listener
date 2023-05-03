package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.suite.Suite;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ToolchainRunningContext {
    private final UUID testRun;
    private final HashMap<String, UUID> tests = new HashMap<>();
    private final HashMap<String, Suite> suites = new HashMap<>();
    private String latestTest;

    public ToolchainRunningContext(UUID testRunId) {
        testRun = testRunId;
    }

    public UUID getTestRunUUID() {
        return testRun;
    }

    public void addTest(String testName, UUID id) {
        tests.put(testName, id);
        latestTest = testName;
    }

    public UUID getTestId(String testName) {
        return tests.get(testName);
    }

    public void remove(String testName) {
        tests.remove(testName);
    }

    public boolean hasTest(String testName) {
        return tests.containsKey(testName);
    }

    public String getLatestTestName() {
        return latestTest;
    }

    public UUID getSuiteId(String fullSuiteName) {
        if (suites.containsKey(fullSuiteName)) {
            return suites.get(fullSuiteName).getSuiteUUID();
        }
        return null;
    }

    public boolean suiteExists(String suiteName) {
        return suites.containsKey(suiteName);
    }

    public void addSuite(String suiteName, UUID suiteId, UUID parentSuiteId, String[] fullSuiteName) {
        suites.put(suiteName, new Suite(suiteId, parentSuiteId, suiteName, List.of(fullSuiteName)));
    }

    public void addSuite(String suitePath, Suite suite) {
        suites.put(suitePath, suite);
    }
}

