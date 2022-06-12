package io.orangebeard.listener.v3client.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ChangedComponent {

    @Size(max = 128)
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]*$")
    private String componentName;
    private String componentVersion;

    @JsonIgnore
    public io.orangebeard.datatypes.ChangedComponent toDataType() {
        return new io.orangebeard.datatypes.ChangedComponent(componentName, componentVersion);
    }
}
