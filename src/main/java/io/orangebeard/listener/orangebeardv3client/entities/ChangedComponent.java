package io.orangebeard.listener.orangebeardv3client.entities;

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

}
