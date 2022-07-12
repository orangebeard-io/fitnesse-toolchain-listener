package io.orangebeard.listener.orangebeardv3client.entities;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Attribute {
    private final String key;
    private final String value;

    public Attribute(String value) {
        this.key = null;
        this.value = value;
    }
}