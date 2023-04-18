package io.orangebeard.listener.helper;

import io.orangebeard.client.OrangebeardV3Client;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.LogLevel;

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
        Log testLog = Log.builder().logLevel(LogLevel.error).build();

        logStasher.stashLogItem(testId, testLog);
        logStasher.sendLogs(testId);

        ArgumentCaptor<Log> argumentCaptor = ArgumentCaptor.forClass(Log.class);
        verify(orangebeardClient).log(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(testLog);
    }
}
