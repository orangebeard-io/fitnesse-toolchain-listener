package io.orangebeard.listener.helper;

//import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.LogLevel;

import io.orangebeard.listener.orangebeardv3client.OrangebeardV3Client;

import java.util.Set;
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
    private OrangebeardV3Client orangebeardV3Client;

    @InjectMocks
    private LogStasher logStasher;

    @Test
    public void logs_can_be_stashed_and_then_sent() {
        UUID testId = UUID.randomUUID();

        logStasher.stashLogItem(testId, Log.builder().logLevel(LogLevel.error).build());
        logStasher.stashLogItem(testId, Log.builder().build());

        logStasher.sendLogs(testId);

        ArgumentCaptor<Set<Log>> argumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(orangebeardV3Client).log(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).hasSize(2);
    }
}
