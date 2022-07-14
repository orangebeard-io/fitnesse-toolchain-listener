package io.orangebeard.listener.helper;

import io.orangebeard.listener.orangebeardv3client.entities.StartSuiteRQ;
import io.orangebeard.listener.orangebeardv3client.entities.Suite;

import static java.lang.String.format;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ToolchainRunningContext {

    private final UUID testRun;
    private final HashMap<String, UUID> tests = new HashMap<>();
    private final HashMap<String, UUID> suites = new HashMap<>();
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
            return suites.get(fullSuiteName);
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


    public void addSuite(List<Suite> suite) {
        suite.forEach(ss ->
        {
            if(ss.getParentUUID() == null) {
                suitePath.put(ss.getSuiteUUID(),String.join(".",ss.getFullSuitePath()));
                suites.put(String.join(".",ss.getFullSuitePath()),ss.getSuiteUUID());
            } else {
                String parentSuitePath = suitePath.get(ss.getParentUUID());
                suites.put(format("%s.%s",ss.getParentPath() ,ss.getFullSuitePath()),ss.getSuiteUUID());
                suitePath.put(ss.getSuiteUUID(),format("%s.%s",parentSuitePath,String.join(".", ss.getLocalSuiteName())));

            }
        });
    }


    public String getLatestTestName() {
        return latestTest;
    }


}

