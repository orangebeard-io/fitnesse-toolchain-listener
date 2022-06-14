package io.orangebeard.listener.v3client.datatype;

import lombok.AllArgsConstructor;
import lombok.Getter;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Getter
public class Attribute {
    private final String key;

    @NotNull
    private final String value;

    @Override
    public String toString() {
        if (key == null)
            return value;
        else
            return key + ":" + value;
    }
}
