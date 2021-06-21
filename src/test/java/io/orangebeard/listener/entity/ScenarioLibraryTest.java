package io.orangebeard.listener.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScenarioLibraryTest{

    @Test
    public void test(){
        ScenarioLibrary scenarioLibrary = new ScenarioLibrary("name", scenarioLibraryHtml);

        assertThat(scenarioLibrary.getScenarioTables()).hasSize(3);

        assertThat(scenarioLibrary.containsTitleOf("" +
                "<table class=\"toolchainTable scenarioTable\">\n" +
                "    <tr class=\"slimRowTitle\">\n" +
                "        <td>scenario</td>\n" +
                "        <td colspan=\"3\">dit is een scenario</td>\n" +
                "    </tr>\n" +
                "    <tr class=\"slimRowColor4\">\n" +
                "        <td>set value for</td>\n" +
                "        <td>application/x-www-form-urlencoded</td>\n" +
                "        <td>header</td>\n" +
                "        <td>Content-Type</td>\n" +
                "    </tr>\n")).isTrue();
    }

    //language=xml
    private String scenarioLibraryHtml = "<table class=\"toolchainTable scenarioTable\">\n" +
            "    <tr class=\"slimRowTitle\">\n" +
            "        <td>scenario</td>\n" +
            "        <td colspan=\"3\">dit is een scenario</td>\n" +
            "    </tr>\n" +
            "    <tr class=\"slimRowColor4\">\n" +
            "        <td>set value for</td>\n" +
            "        <td>application/x-www-form-urlencoded</td>\n" +
            "        <td>header</td>\n" +
            "        <td>Content-Type</td>\n" +
            "    </tr>\n" +
            "    <tr class=\"slimRowColor4\">\n" +
            "        <td>post</td>\n" +
            "        <td>client_id=orangebeard&amp;username=superadmin&amp;password=1234&amp;grant_type=password</td>\n" +
            "        <td>to</td>\n" +
            "        <td>\n" +
            "            <a href=\"https://test-keycloak.orangebeard.app/auth/realms/orangebeard/protocol/openid-connect/token\">\n" +
            "                https://test-keycloak.orangebeard.app/auth/realms/orangebeard/protocol/openid-connect/token\n" +
            "            </a>\n" +
            "        </td>\n" +
            "    </tr>\n" +
            "    <tr class=\"slimRowColor6\">\n" +
            "        <td>$accessToken=</td>\n" +
            "        <td>json path</td>\n" +
            "        <td colspan=\"2\">$.access_token</td>\n" +
            "    </tr>\n" +
            "</table>\n" +
            "<br/><table class=\"toolchainTable scenarioTable\">\n" +
            "<tr class=\"slimRowTitle\">\n" +
            "    <td>scenario</td>\n" +
            "    <td>controleer of het personal project bestaat</td>\n" +
            "    <td>projectName</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor4\">\n" +
            "    <td>navigeer naar submenu pagina</td>\n" +
            "    <td colspan=\"2\">projects_page</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor0\">\n" +
            "    <td>is visible on page</td>\n" +
            "    <td colspan=\"2\">@{projectName}_personal</td>\n" +
            "</tr>\n" +
            "</table>\n" +
            "<br/><br/><table class=\"toolchainTable scenarioTable\">\n" +
            "<tr class=\"slimRowTitle\">\n" +
            "    <td>scenario</td>\n" +
            "    <td colspan=\"4\">wacht tot alle testrapport events er zijn</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor6\">\n" +
            "    <td colspan=\"5\">setup haal herhaaldelijk event counts op</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor9\">\n" +
            "    <td>show</td>\n" +
            "    <td>repeat until json path</td>\n" +
            "    <td>$.numberOfTestRunFinishedEvents</td>\n" +
            "    <td>is</td>\n" +
            "    <td>5</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor6\">\n" +
            "    <td colspan=\"5\">setup haal herhaaldelijk event counts op</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor9\">\n" +
            "    <td>show</td>\n" +
            "    <td>repeat until json path</td>\n" +
            "    <td>$.numberOfTestRunFinishedEvents</td>\n" +
            "    <td>is</td>\n" +
            "    <td>5</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor6\">\n" +
            "    <td colspan=\"5\">setup haal herhaaldelijk event counts op</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor9\">\n" +
            "    <td>show</td>\n" +
            "    <td>repeat until json path</td>\n" +
            "    <td>$.numberOfTestRunFinishedEvents</td>\n" +
            "    <td>is</td>\n" +
            "    <td>5</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor6\">\n" +
            "    <td colspan=\"5\">setup haal herhaaldelijk event counts op</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor4\">\n" +
            "    <td>repeat until json path</td>\n" +
            "    <td>$.numberOfTestRunFinishedEvents</td>\n" +
            "    <td>is</td>\n" +
            "    <td colspan=\"2\">5</td>\n" +
            "</tr>\n" +
            "<tr class=\"slimRowColor9\">\n" +
            "    <td>show</td>\n" +
            "    <td colspan=\"4\">repeat count</td>\n" +
            "</tr>\n" +
            "</table>";

}
