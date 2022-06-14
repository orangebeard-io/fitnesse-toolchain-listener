package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.Log;
import io.orangebeard.listener.v3client.v3Client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LogStasher {

    private final v3Client v3client;
    private final Map<UUID, Set<Log>> stashedLogs;

    public LogStasher(v3Client v3client) {
        this.v3client = v3client;
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
        v3client.log(stashedLogs.get(testId));
        stashedLogs.remove(testId);
    }
}
