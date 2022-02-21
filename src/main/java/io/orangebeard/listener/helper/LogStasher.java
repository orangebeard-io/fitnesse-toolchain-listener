package io.orangebeard.listener.helper;

import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.entity.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LogStasher {

    private final OrangebeardClient orangebeardClient;
    private final Map<UUID, Set<Log>> stashedLogs;

    public LogStasher(OrangebeardClient orangebeardClient) {
        this.orangebeardClient = orangebeardClient;
        this.stashedLogs = new HashMap<>();
    }

    public void stashLogItem(UUID testId, Log logItem) {
        if (stashedLogs.containsKey(testId)) {
            stashedLogs.get(testId).add(logItem);
        } else {
            HashSet<Log> set = new HashSet<>();
            set.add(logItem);
            stashedLogs.put(testId, set);
        }
    }

    public void sendLogs(UUID testId) {
        orangebeardClient.log(stashedLogs.get(testId));
        stashedLogs.remove(testId);
    }
}
