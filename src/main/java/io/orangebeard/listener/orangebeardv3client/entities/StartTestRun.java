package io.orangebeard.listener.orangebeardv3client.entities;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
public class StartTestRun {

    @NotNull
    @Size(min = 1, max = 128)
    private final String testSetName;

    @Size(min = 1)
    private final String description;

    @NotNull
    private final ZonedDateTime startTime;

    @Valid
    private final Set<Attribute> attributes;

    @Valid
    private final List<ChangedComponent> changedComponents;

}
