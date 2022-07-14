package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.StartSuiteRQ;
import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ToolchainRunningContext {

    private final UUID testRun;
    private final HashMap<String, UUID> tests = new HashMap<>();
    private final HashMap<String, UUID> suiteIndex = new HashMap<>();
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
        if (suiteIndex.containsKey(fullSuiteName)) {
            return suiteIndex.get(fullSuiteName);
        }
        return null;
    }

    public StartSuiteRQ getStartSuite(String fullSuiteName) {
        String[] suiteNames =  fullSuiteName.split("\\.");

        UUID parentSuiteId = null;
        List<String> suitesToCreate = new ArrayList<>();
        List<String> suitePath =new ArrayList<>();
        UUID suiteId;
        for(String suite : suiteNames) {
            suitePath.add(suite);
            suiteId = getSuiteId(String.join(".",suitePath));
            if(suiteId != null) {
                parentSuiteId=suiteId;
            }else {
                suitesToCreate.add(suite);
            }
        }
        if(suitesToCreate.isEmpty())
            return null;
        return new StartSuiteRQ(getTestRunUUID(), parentSuiteId, null, new HashSet<>(), suitesToCreate);
    }

    public void addSuites(List<Suite> suites) {
        if(!suites.isEmpty()) {
            suites.forEach(suite -> {
                if (suite.getParentUUID() == null) {
                    suitePath.put(suite.getSuiteUUID(), String.join(".", suite.getFullSuitePath()));
                    this.suiteIndex.put(String.join(".", suite.getFullSuitePath()), suite.getSuiteUUID());
                } else {
                    String parentSuitePath = suitePath.get(suite.getParentUUID());
                    this.suiteIndex.put(format("%s.%s", parentSuitePath, suite.getLocalSuiteName()), suite.getSuiteUUID());
                    suitePath.put(suite.getSuiteUUID(), format("%s.%s", parentSuitePath, String.join(".", suite.getLocalSuiteName())));
                }
            });
        }
    }

    public String getLatestTestName() {
        return latestTest;
    }
}

