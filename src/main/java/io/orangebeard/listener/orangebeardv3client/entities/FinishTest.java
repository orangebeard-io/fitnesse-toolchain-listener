package io.orangebeard.listener.orangebeardv3client.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class FinishTest {
    @NotNull
    private UUID testRunUUID;
    @NotNull
    private TestStatus status;
    @NotNull
    private ZonedDateTime endTime;
}
