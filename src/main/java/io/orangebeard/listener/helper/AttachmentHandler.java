package io.orangebeard.listener.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.orangebeard.client.entity.attachment.Attachment;

import io.orangebeard.client.v3.OrangebeardAsyncV3Client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttachmentHandler {
    private static final Pattern attachmentPattern = Pattern.compile("href=\"([^$<>\"]*)\"");
    private final OrangebeardAsyncV3Client orangebeardClient;
    private final String rootPath;

    public AttachmentHandler(OrangebeardAsyncV3Client orangebeardClient, String rootPath) {
        this.orangebeardClient = orangebeardClient;
        this.rootPath = rootPath;
    }

    public boolean hasFilesToAttach(String message) {
        Matcher attachments = attachmentPattern.matcher(message);
        return attachments.find();
    }

    public void attachFilesIfPresent(UUID testId, UUID testRunId, String message, UUID logUUID) {
        Matcher attachments = attachmentPattern.matcher(message);
        while (attachments.find()) {
            if (!attachments.group(1).isEmpty() && !attachments.group(1).startsWith("http://") &&
                    !attachments.group(1).startsWith("https://") &&
                    !attachments.group(1).startsWith("mailto:")) {
                try {
                    File attachmentFile = new File(rootPath, attachments.group(1));

                    Attachment.AttachmentFile file = new Attachment.AttachmentFile(
                            attachmentFile.getName(),
                            Files.readAllBytes(attachmentFile.toPath()),
                            Files.probeContentType(attachmentFile.toPath())
                    );
                    Attachment.AttachmentMetaData metaData = new Attachment.AttachmentMetaData(
                            testRunId, testId, null, logUUID, ZonedDateTime.now()
                    );
                    Attachment attachment = new Attachment(file, metaData);

                    orangebeardClient.sendAttachment(attachment);
                } catch (IOException | InvalidPathException e) {
                    log.info("Unable to read attachment file for: {}", attachments.group(1));
                }
            }
        }
    }
}
