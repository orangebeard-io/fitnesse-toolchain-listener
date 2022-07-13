package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.UUID;


/**
 * Parallel execution context and set of operations to interact with it
 */
public class ToolchainRunningContext {

    private final UUID testRun;
    private final HashMap<String, UUID> tests = new HashMap<>();
    private final HashMap<String, Suite> suites = new HashMap<>();
    private final HashMap<UUID, String> suitePath = new HashMap<>();
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

    public UUID getSuiteId(String fullSuiteName) {
        if (suites.containsKey(fullSuiteName)) {
            return suites.get(fullSuiteName).getSuiteUUID();
        }
        return null;
    }

    public String getSuitePath(UUID suiteUUID) {
       return suitePath.get(suiteUUID);
    }

    public void addSuite(String fullSuiteName, Suite suite) {
        suites.put(fullSuiteName, suite);
    }
    public void addSuitePath( UUID suiteId,String fullSuiteName) {
        suitePath.put(suiteId,fullSuiteName);
    }


    public String getLatestTestName() {
        return latestTest;
    }


}

