package io.orangebeard.listener.v3client.entities;


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

    @JsonIgnore
    public Set<io.orangebeard.listener.v3client.datatype.ChangedComponent> getChangedComponentDatatypes() {
        if (changedComponents == null || changedComponents.isEmpty()) {
            return null;
        }
        return changedComponents.stream()
                .filter(Objects::nonNull)
                .filter(it -> it.getComponentName() != null && !it.getComponentName().equals(""))
                .map(ChangedComponent::toDataType)
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    public Set<io.orangebeard.listener.v3client.datatype.Attribute> getAttributesDataTypes() {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        return attributes
                .stream()
                .filter(Objects::nonNull)
                .map(it -> new io.orangebeard.listener.v3client.datatype.Attribute(it.getKey(), it.getValue()))
                .collect(Collectors.toSet());
    }
}
