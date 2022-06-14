package io.orangebeard.listener.v3client.entities;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
    @NotNull
    private UUID testRunUUID;
    private UUID parentSuiteUUID;
    @Size(min = 1)
    private String description;
    private Set<Attribute> attributes;
    @NotNull
    @NotEmpty
    private List<String> suiteNames;
}
