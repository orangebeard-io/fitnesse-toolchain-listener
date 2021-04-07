package io.orangebeard.listener.helper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class Suite {
    private UUID uuid;
    private LocalDateTime startTime;
}
