package io.orangebeard.listener.orangebeardv3client.entities;

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class FinishTestRun {

    @NotNull
    private ZonedDateTime endTime;
}
