package io.orangebeard.listener.v3client.entities;


import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Attribute {
    private final String key;
    @NotNull
    private final String value;

    public Attribute(String value) {
        this.key = null;
        this.value = value;
    }
}
