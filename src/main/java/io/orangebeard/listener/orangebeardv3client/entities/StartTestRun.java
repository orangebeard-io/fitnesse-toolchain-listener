package io.orangebeard.listener.orangebeardv3client.entities;



import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
public class StartTestRun {


    private final String testSetName;
    private final String description;
    private final ZonedDateTime startTime;
    private final Set<Attribute> attributes;
    private final List<ChangedComponent> changedComponents;

}
