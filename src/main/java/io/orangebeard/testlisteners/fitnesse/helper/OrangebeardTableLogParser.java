package io.orangebeard.testlisteners.fitnesse.helper;

import nl.hsac.fitnesse.fixture.Environment;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrangebeardTableLogParser {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardTableLogParser.class);

    public String embedImagesAndStripHyperlinks(String html) {
        Pattern imgPattern = Pattern.compile("(<img(\\s+.*?)?\\s+src=\"(.*?)\".*?/>)", Pattern.CASE_INSENSITIVE);
        html = html.replaceAll("<a.+?>(.+?)</a>", "$1");
        Matcher imgMatcher = imgPattern.matcher(html);

        while (imgMatcher.find()) {
            String src = imgMatcher.group(3);
            String root = Environment.getInstance().getFitNesseRootDir();
            String img = root + "/" + src;
            File imageFile = new File(img);
            try {
            html = html.replace(imgMatcher.group(), "<img src=\"data:image/png;base64," + ImageEncoder.encode(imageFile) + "\" width=\"200\" onClick=\"openImage(this)\">");
            } catch (IOException ioe) {
                logger.error("Exception while reading the Image", ioe);
            }
        }
        return html;
    }

    private String removeNonTableProlog(String html) {
        if(html.toLowerCase().contains("<table")) {
            html = html.substring(html.indexOf("<table"), html.lastIndexOf("</table>") + 8);
        }
        return html;
    }

    private String determineLogLevel(String logChunk) {
        String level;
        if(logChunk.contains("class=\"error\"") || logChunk.contains("class=\"fail\"")) {
            level = "ERROR";
        } else {
            level = "INFO";
        }
        return level;
    }

}
