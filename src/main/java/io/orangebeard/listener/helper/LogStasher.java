package io.orangebeard.listener.helper;

import io.orangebeard.client.OrangebeardV3Client;
import io.orangebeard.client.entity.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LogStasher {
    private final OrangebeardV3Client orangebeardClient;
    private final Map<UUID, Set<Log>> stashedLogs;

    public LogStasher(OrangebeardV3Client orangebeardClient) {
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
        if(stashedLogs.containsKey(testId)) {
            List<Log> logs = new ArrayList<>(stashedLogs.get(testId));
            orangebeardClient.sendLogBatch(logs);
            stashedLogs.remove(testId);
        }
    }
}
