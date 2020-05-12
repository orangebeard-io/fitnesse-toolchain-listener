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
        Pattern imgPattern = Pattern.compile("<img(\\s+.*?)?\\s+src=\"(.*?)\".*?/>", Pattern.CASE_INSENSITIVE);
        html = html.replaceAll("<a.+?>(.+?)</a>", "$1");
        Matcher imgMatcher = imgPattern.matcher(html);
        while (imgMatcher.find()) {
            String src = imgMatcher.group(2);
            String root = Environment.getInstance().getFitNesseRootDir();
            String img = root + "/" + src;
            File imageFile = new File(img);
            html = imgMatcher.replaceAll("<img src=\"data:image/png;base64," + encodeFile(imageFile) + "\" width=\"200\" onClick=\"openImage(this)\">");
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

    private String encodeFile(File file) {
        String base64Image = "";
        try (FileInputStream imageInFile = new FileInputStream(file)) {
            // Reading a Image file from file system
            byte[] imageData = new byte[(int) file.length()];
            int i = 0;
            while (i != -1) {
                i = imageInFile.read(imageData);
            }
            base64Image = Base64.getEncoder().encodeToString(imageData);
        } catch (FileNotFoundException e) {
            logger.error("Image not found", e);
        } catch (IOException ioe) {
            logger.error("Exception while reading the Image", ioe);
        }
        return base64Image;
    }


}
