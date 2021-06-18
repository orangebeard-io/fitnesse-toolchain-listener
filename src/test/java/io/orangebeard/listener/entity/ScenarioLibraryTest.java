package io.orangebeard.listener.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScenarioLibraryTest{


    @Test
    public void test(){
        ScenarioLibrary scenarioLibrary = new ScenarioLibrary("name", scenarioLibraryHtml);

        assertThat(scenarioLibrary.getScenarioTables()).hasSize(42);

        assertThat(scenarioLibrary.containsTitleOf("")).isTrue();
    }

    //language=xml
    private String scenarioLibraryHtml = "";

}
