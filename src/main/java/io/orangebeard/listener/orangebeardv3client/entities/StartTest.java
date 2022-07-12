package io.orangebeard.listener.orangebeardv3client.entities;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class StartTest {
    @NotNull
    private UUID testRunUUID;
    @NotNull
    private UUID suiteUUID;

    @NotNull
    @Size(min = 1)
    private String testName;
    @NotNull
    private TestType testType;
    @Size(min = 1)
    private String description;
    private Set<Attribute> attributes;
    @NotNull
    private ZonedDateTime startTime;
}
