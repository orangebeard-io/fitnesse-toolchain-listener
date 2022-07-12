package io.orangebeard.listener.orangebeardv3client.entities;

import java.util.List;
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
public class StartSuiteRQ {

    private UUID testRunUUID;
    private UUID parentSuiteUUID;
    private String description;
    private Set<Attribute> attributes;
    private List<String> suiteNames;
}
