package io.orangebeard.listener.orangebeardv3client.entities;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;



@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ChangedComponent {

    private String componentName;
    private String componentVersion;

}
