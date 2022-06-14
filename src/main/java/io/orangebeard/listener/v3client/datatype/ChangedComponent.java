package io.orangebeard.listener.v3client.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangedComponent {
    public final String componentName;
    public final String componentVersion;

}
