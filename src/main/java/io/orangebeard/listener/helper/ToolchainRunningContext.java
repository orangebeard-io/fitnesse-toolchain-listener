package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.StartSuiteRQ;
import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ToolchainRunningContext {

    private final UUID testRun;
    private final HashMap<String, UUID> tests = new HashMap<>();
    private final Map<String, UUID> suiteUUIDIndex = new HashMap<>();
    private final Map<UUID, String> suitePathIndex = new HashMap<>();

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
        if (suiteUUIDIndex.containsKey(fullSuiteName)) {
            return suiteUUIDIndex.get(fullSuiteName);
        }
        return null;
    }

    public StartSuiteRQ getStartSuite(String fullSuiteName) {
        String[] suiteNames = fullSuiteName.split("\\.");

        UUID parentSuiteId = null;
        List<String> suitesToCreate = new ArrayList<>();
        List<String> suitePath = new ArrayList<>();
        UUID suiteId;
        for (String suite : suiteNames) {
            suitePath.add(suite);
            suiteId = getSuiteId(String.join(".", suitePath));
            if (suiteId != null) {
                parentSuiteId = suiteId;
            } else {
                suitesToCreate.add(suite);
            }
        }
        if (suitesToCreate.isEmpty()) {
            return null;
        }
        return new StartSuiteRQ(getTestRunUUID(), parentSuiteId, null, new HashSet<>(), suitesToCreate);
    }

    public void addSuites(List<Suite> suites) {
        if (suites != null) {
            suites.forEach(suite -> {
                if (suite.getParentUUID() == null) {
                    this.suitePathIndex.put(suite.getSuiteUUID(), String.join(".", suite.getFullSuitePath()));
                    this.suiteUUIDIndex.put(String.join(".", suite.getFullSuitePath()), suite.getSuiteUUID());
                } else {
                    String parentSuitePath = suitePathIndex.get(suite.getParentUUID());
                    this.suitePathIndex.put(suite.getSuiteUUID(), format("%s.%s", parentSuitePath, String.join(".", suite.getLocalSuiteName())));
                    this.suiteUUIDIndex.put(format("%s.%s", parentSuitePath, suite.getLocalSuiteName()), suite.getSuiteUUID());
                }
            });
        }
    }

    public String getLatestTestName() {
        return latestTest;
    }
}

