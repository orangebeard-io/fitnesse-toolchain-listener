package io.orangebeard.listener.helper;

import io.orangebeard.client.OrangebeardClient;
import io.orangebeard.client.entity.Attachment;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.LogLevel;
import net.lingala.zip4j.ZipFile;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrangebeardLogger {
    private static final Pattern attachmentPattern = Pattern.compile("href=\"([^\"]*)\"");
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardLogger.class);
    private final OrangebeardClient orangebeard;
    private final String rootPath;

    public OrangebeardLogger(OrangebeardClient orangebeard, String rootPath) {
        this.orangebeard = orangebeard;
        this.rootPath = rootPath;
    }

    public void sendLogData(UUID testId, UUID testRunId, String chunk, LogLevel level) {
        Log.LogBuilder log = Log.builder()
                .message(chunk)
                .itemUuid(testId)
                .testRunUUID(testRunId)
                .logLevel(level)
                .time(LocalDateTime.now());

        orangebeard.log(log.build());
    }

    private void sendAttachment(String message, LogLevel level, UUID testId, UUID testRunId, File file) {
        try {
            Attachment.File attachmentFile = new Attachment.File(
                    file.getName(),
                    Files.readAllBytes(file.toPath()),
                    Files.probeContentType(file.toPath())
            );

            Attachment.AttachmentBuilder attachment = Attachment.builder()
                    .file(attachmentFile)
                    .message(message)
                    .logLevel(level)
                    .itemUuid(testId)
                    .testRunUUID(testRunId)
                    .time(LocalDateTime.now());

            orangebeard.sendAttachment(attachment.build());
        } catch (IOException e) {
            logger.error("Could not read file: " + file.getAbsolutePath());
        }

    }

    public void attachFilesIfPresent(UUID testId, UUID launchId, String message) {
        Matcher attachments = attachmentPattern.matcher(message);
        while (attachments.find()) {
            if (!attachments.group(1).startsWith("http://") && !attachments.group(1).startsWith("https://")) {
                File attachmentFile = new File(rootPath, attachments.group(1));
                String fileName = attachmentFile.getName();
                sendAttachment(fileName, LogLevel.debug, testId, launchId, attachmentFile);
            }
        }
    }

    public void attachFitNesseResultsToRun(UUID testRunId) {
        try {
            ZipFile testReportBundle = new ZipFile("FitNesseResults.zip");
            testReportBundle.addFolder(new File(rootPath));
            File reportZip = testReportBundle.getFile();
            logger.info("Attaching result zip");
            sendAttachment("FitNesse-results.zip", LogLevel.info, null, testRunId, reportZip);
        } catch (Exception e) {
            logger.error("An exception occured when attempting to attach the html report to the launch", e);
        }
    }
}
