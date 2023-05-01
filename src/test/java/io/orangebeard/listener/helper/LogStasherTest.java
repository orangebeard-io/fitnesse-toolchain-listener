package io.orangebeard.listener.helper;

import io.orangebeard.client.OrangebeardV3Client;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.log.LogLevel;

import java.util.List;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LogStasherTest {
    @Mock
    private OrangebeardV3Client orangebeardClient;
    @InjectMocks
    private LogStasher logStasher;

    @Test
    public void logs_can_be_stashed_and_then_sent() {
        UUID testId = UUID.randomUUID();
        Log testLog = Log.builder().logLevel(LogLevel.ERROR).build();

        logStasher.stashLogItem(testId, testLog);
        logStasher.sendLogs(testId);

        ArgumentCaptor<List<Log>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(orangebeardClient).sendLogBatch(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(List.of(testLog));
    }
}
