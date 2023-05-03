package io.orangebeard.listener.helper;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.orangebeard.client.entity.log.LogLevel;

import org.slf4j.LoggerFactory;

public class OrangebeardTableLogParser {

    private OrangebeardTableLogParser() {
        // only static functions
    }

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardTableLogParser.class);

    public static String parseLogMessage(String chunk, String rootPath) {
        String log = removeNonTableProlog(chunk);

        if (log.toLowerCase().contains("<table")) {
            log = applyOrangebeardTableStyling(log);
        }

        String enrichedLog = OrangebeardTableLogParser.embedImagesAndStripHyperlinks(log, rootPath);
        //Workaround for corner case where table contains binary representation with 0x00 unicode chars
        return enrichedLog.replace("\u0000", "");
    }

    public static String embedImagesAndStripHyperlinks(String html, String rootPath) {
        Pattern imgPattern = Pattern.compile("(<img(\\s+.*?)?\\s+src=\"(.*?)\".*?/>)", Pattern.CASE_INSENSITIVE);
        html = html.replaceAll("<a.+?>(.+?)</a>", "$1");
        Matcher imgMatcher = imgPattern.matcher(html);

        while (imgMatcher.find()) {
            String src = imgMatcher.group(3);
            String img = rootPath + "/" + src;
            File imageFile = new File(img);
            try {
                html = html.replace(imgMatcher.group(), "<img src=\"data:image/png;base64," + ImageEncoder.encodeForEmbedding(imageFile) + "\" width=\"200\" onClick=\"openImage(this)\">");
            } catch (IOException ioe) {
                logger.error("Exception while reading image: " + img + " (" + ioe.getMessage() + ")");
            }
        }
        return html;
    }

    public static String removeNonTableProlog(String html) {
        if (html.toLowerCase().contains("<table")) {
            html = html.substring(html.indexOf("<table"), html.lastIndexOf("</table>") + 8);
        }
        return html;
    }

    public static LogLevel getLogLevel(String logChunk) {
        io.orangebeard.client.entity.log.LogLevel level = io.orangebeard.client.entity.log.LogLevel.DEBUG;
        if (logChunk.contains("class=\"error\"") || logChunk.contains("class=\"fail\"")) {
            level = io.orangebeard.client.entity.log.LogLevel.ERROR;
        } else if (reportTable(logChunk)) {
            level = io.orangebeard.client.entity.log.LogLevel.INFO;
        }
        return level;
    }

    public static String applyOrangebeardTableStyling(String table) {
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
    private static boolean reportTable(String tableBlock) {
        return !tableBlock.startsWith("<table class=\"toolchainTable scenarioTable\"") &&
                !tableBlock.startsWith("<table class=\"toolchainTable tableTemplate\"") &&
                !tableBlock.startsWith("<table class=\"toolchainTable importTable\"") &&
                !tableBlock.startsWith("<table class=\"toolchainTable libraryTable\"");
    }
}
