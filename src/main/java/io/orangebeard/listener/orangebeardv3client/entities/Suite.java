package io.orangebeard.listener.orangebeardv3client.entities;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Suite {
    private UUID suiteUUID;
    private UUID parentUUID;
    private String localSuiteName;
    private List<String> fullSuitePath;
    private List<String> parentPath;
}
