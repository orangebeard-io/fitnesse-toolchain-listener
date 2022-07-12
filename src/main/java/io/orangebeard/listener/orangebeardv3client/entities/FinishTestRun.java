package io.orangebeard.listener.orangebeardv3client.entities;

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

    private ZonedDateTime endTime;
}
