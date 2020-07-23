package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.LogLevel;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrangebeardTableLogParser {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardTableLogParser.class);

    public String embedImagesAndStripHyperlinks(String html, String rootPath) {
        Pattern imgPattern = Pattern.compile("(<img(\\s+.*?)?\\s+src=\"(.*?)\".*?/>)", Pattern.CASE_INSENSITIVE);
        html = html.replaceAll("<a.+?>(.+?)</a>", "$1");
        Matcher imgMatcher = imgPattern.matcher(html);

        while (imgMatcher.find()) {
            String src = imgMatcher.group(3);
            String img = rootPath + "/" + src;
            File imageFile = new File(img);
            try {
                html = html.replace(imgMatcher.group(), "<img src=\"data:image/png;base64," + ImageEncoder.encode(imageFile) + "\" width=\"200\" onClick=\"openImage(this)\">");
            } catch (IOException ioe) {
                logger.error("Exception while reading the Image", ioe);
            }
        }
        return html;
    }

    public String removeNonTableProlog(String html) {
        if (html.toLowerCase().contains("<table")) {
            html = html.substring(html.indexOf("<table"), html.lastIndexOf("</table>") + 8);
        }
        return html;
    }

    public LogLevel determineLogLevel(String logChunk) {
        LogLevel level = LogLevel.debug;
        if (logChunk.contains("class=\"error\"") || logChunk.contains("class=\"fail\"")) {
            level = LogLevel.error;
        } else if (reportTable(removeNonTableProlog(logChunk))) {
            level = LogLevel.info;
        }
        return level;
    }

    public String applyOrangebeardTableStyling(String table) {
        table = table.replaceAll("class=\"fail\"", "style=\"background-color:#ffaeaf; padding: 3px; border-radius: 3px;\"");
        table = table.replaceAll("class=\"pass\"", "style=\"background-color:#44ffa5; padding: 3px; border-radius: 3px;\"");
        table = table.replaceAll("class=\"diff\"", "style=\"background-color:#f1e38f; padding: 3px; border-radius: 3px;\"");
        table = table.replaceAll("class=\"ignore\"", "style=\"background-color:#a8e2ff; padding: 3px; border-radius: 3px;\"");
        table = table.replaceAll("class=\"error\"", "style=\"background-color:#ffe67b; padding: 3px; border-radius: 3px;\"");
        table = table.replaceAll("class=\"slimRowTitle\"", "style=\"font-weight:bold; background-color: #ececec;\"");
        table = table.replaceAll("class=\"title\"", "style=\"font-size:1.2em; font-weight:bold; border-bottom:1px solid silver;\"");
        table = table.replaceAll("class=\"toolchainTable [^\"]*\"", "style=\"box-shadow: 0px 3px 10px 0px rgba(0, 0, 0, 0.19);\"");
        return table;
    }

    //Scenario's, templates, libraries and imports are debug info
    private boolean reportTable(String tableBlock) {
        return !tableBlock.startsWith("<table class=\"toolchainTable scenarioTable\"") &&
                !tableBlock.startsWith("<table class=\"toolchainTable tableTemplate\"") &&
                !tableBlock.startsWith("<table class=\"toolchainTable importTable\"") &&
                !tableBlock.startsWith("<table class=\"toolchainTable libraryTable\"");
    }

}
