package io.orangebeard.listener.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import fitnesse.wiki.WikiPage;

public class ScenarioLibraries {
    private final Map<String, ScenarioLibrary> scenarioLibraries = new HashMap<>();

    public void add(List<WikiPage> libraries) {
        libraries.forEach(s -> scenarioLibraries.put(
                s.getFullPath().toString(),
                new ScenarioLibrary(s.getFullPath().toString(), s.getHtml())));
    }

    public boolean contains(String table) {
        return !scenarioLibraries.values().stream()
                .map(it -> it.containsTitleOf(table))
                .collect(Collectors.toList())
                .contains(false);
    }
}
