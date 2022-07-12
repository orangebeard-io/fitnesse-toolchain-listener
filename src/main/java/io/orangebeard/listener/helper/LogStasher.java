package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.Log;
import io.orangebeard.listener.orangebeardv3client.OrangebeardV3Client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LogStasher {

    private final OrangebeardV3Client orangebeardV3Client;
    private final Map<UUID, Set<Log>> stashedLogs;

    public LogStasher(OrangebeardV3Client orangebeardV3Client) {
        this.orangebeardV3Client = orangebeardV3Client;
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
        orangebeardV3Client.log(stashedLogs.get(testId));
        stashedLogs.remove(testId);
    }
}
