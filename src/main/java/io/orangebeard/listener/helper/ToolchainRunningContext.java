package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

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

//    public StartSuiteRQ getStartSuite(List<String> suiteNames) {
//        UUID parentSuiteId = null;
//        List<String> suitesToCreate = new ArrayList<>();
//        List<String> suitePath =new ArrayList<>();
//        UUID suiteId;
//        for(String suite : suiteNames) {
//            suitePath.add(suite);
//            suiteId = getSuiteId(String.join(".",suitePath));
//            if(suiteId != null) {
//                parentSuiteId=suiteId;
//            }else {
//                suitesToCreate.add(suite);
//            }
//        }
//        return new StartSuiteRQ(getTestRunUUID(), parentSuiteId, null, new HashSet<>(), suitesToCreate);
//    }

    public void addSuites(List<Suite> suites) {
        suites.forEach(suite -> {
            if (suite.getParentUUID() == null) {
                this.suiteIndex.put(String.join(".", suite.getFullSuitePath()), suite.getSuiteUUID());
            } else {
                this.suiteIndex.put(format("%s.%s", suite.getParentPath(), suite.getFullSuitePath()), suite.getSuiteUUID());
            }
        });
    }

    public String getLatestTestName() {
        return latestTest;
    }
}

