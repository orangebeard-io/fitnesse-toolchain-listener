package io.orangebeard.listener.helper;

import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.entity.Attachment;
import io.orangebeard.client.entity.LogLevel;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.lingala.zip4j.ZipFile;
import org.slf4j.LoggerFactory;

public class OrangebeardLogger {
    private static final Pattern attachmentPattern = Pattern.compile("href=\"([^\"]*)\"");
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardLogger.class);
    private final OrangebeardClient orangebeardClient;
    private final String rootPath;

    public OrangebeardLogger(OrangebeardClient orangebeardClient, String rootPath) {
        this.orangebeardClient = orangebeardClient;
        this.rootPath = rootPath;
    }

    public void attachFilesIfPresent(UUID testId, UUID testRunId, String message) {
        Matcher attachments = attachmentPattern.matcher(message);
        while (attachments.find()) {
            if (!attachments.group(1).startsWith("http://") && !attachments.group(1).startsWith("https://")) {
                File attachmentFile = new File(rootPath, attachments.group(1));
                String fileName = attachmentFile.getName();

                try {
                    Attachment attachment = Attachment.builder()
                            .file(new Attachment.File(attachmentFile))
                            .message(fileName)
                            .logLevel(LogLevel.debug)
                            .itemUuid(testId)
                            .testRunUUID(testRunId)
                            .time(LocalDateTime.now())
                            .build();

                    orangebeardClient.sendAttachment(attachment);
                } catch (IOException e) {
                    logger.warn("Unable to read attachment file: " + fileName);
                }
            }
        }
    }

    public void attachFitNesseResultsToRun(UUID testRunId) {
        try {
            ZipFile testReportBundle = new ZipFile("FitNesseResults.zip");
            testReportBundle.addFolder(new File(rootPath));
            File reportZip = testReportBundle.getFile();
            logger.info("Attaching result zip");

            Attachment attachment = Attachment.builder()
                    .file(new Attachment.File(reportZip))
                    .message("FitNesse-results.zip")
                    .logLevel(LogLevel.info)
                    .itemUuid(null)
                    .testRunUUID(testRunId)
                    .time(LocalDateTime.now())
                    .build();

            orangebeardClient.sendAttachment(attachment);
        } catch (IOException e) {
            logger.warn("An exception occurred when attempting to attach the html report to the launch", e);
        }
    }
}
