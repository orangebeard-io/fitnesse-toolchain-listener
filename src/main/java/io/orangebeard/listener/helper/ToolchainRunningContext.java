package io.orangebeard.listener.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Parallel execution context and set of operations to interact with it
 */

public class ToolchainRunningContext {

    private final UUID testRun;
    private final HashMap<String, UUID> tests = new HashMap<>();
    private final HashMap<String, UUID> suites = new HashMap<>();
    private String latestTest;

    public ToolchainRunningContext(UUID testRunId) {
        testRun = testRunId;
    }

    public UUID getTestRun() {
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

    public UUID getSuiteId(String fullSuiteName) {
        return suites.get(fullSuiteName);
    }

    public void addSuite(String fullSuiteName, UUID suiteId) {
        suites.put(fullSuiteName, suiteId);
    }

    public List<UUID> getAllSuiteIds() {
        return new ArrayList<>(suites.values());
    }

    public String getLatestTestName() {
        return latestTest;
    }
}
