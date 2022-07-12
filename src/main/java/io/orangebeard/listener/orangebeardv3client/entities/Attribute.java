package io.orangebeard.listener.orangebeardv3client.entities;


import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Attribute {
    private final String key;
    @NotNull
    private final String value;

    public Attribute(String value) {
        this.key = null;
        this.value = value;
    }
}
