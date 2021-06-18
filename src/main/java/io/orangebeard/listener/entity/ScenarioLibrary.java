package io.orangebeard.listener.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

import static io.orangebeard.listener.helper.OrangebeardTableLogParser.normalizeXML;

@Getter
public class ScenarioLibrary {
    private final String name;
    private final String normalizedHtml;
    private final List<String> scenarioTables = new ArrayList<>();
    private final List<String> scenarioTitles = new ArrayList<>();

    public ScenarioLibrary(String name, String htmlPage) {
        this.name = name;
        scenarioTables.addAll(Arrays.stream(normalizeXML(htmlPage).split("<br/>")).filter(it -> it.startsWith("<table")).collect(Collectors.toList()));
        scenarioTitles.addAll(scenarioTables.stream().map(ScenarioLibrary::getScenarioTitle).collect(Collectors.toList()));
        this.normalizedHtml = normalizeXML(htmlPage);
    }

    public boolean containsTitleOf(String table) {
        return scenarioTitles.contains(getScenarioTitle(normalizeXML(table)));
    }

    public static String getScenarioTitle(String xml) {
        return xml.substring(0, xml.indexOf("</tr>"));
    }
}
