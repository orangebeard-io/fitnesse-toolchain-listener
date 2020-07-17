package io.orangebeard.testlisteners.fitnesse.helper;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.BatchElementCreatedRS;
import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.io.Files;
import io.reactivex.Maybe;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;

public class OrangebeardLogger {
    private static final Pattern attachmentPattern = Pattern.compile("href=\"([^\"]*)\"");

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(OrangebeardLogger.class);

    private final ReportPortal reportPortal;
    private final String rootPath;


    public OrangebeardLogger(ReportPortal reportPortal, String rootPath) {
        this.reportPortal = reportPortal;
        this.rootPath = rootPath;
    }

    public void sendLogData(String testId, String launchId, String chunk, String level) {
        SaveLogRQ saveLogRQ = new SaveLogRQ();
        saveLogRQ.setMessage(chunk);
        saveLogRQ.setLogTime(Calendar.getInstance().getTime());
        saveLogRQ.setItemUuid(testId);
        saveLogRQ.setLaunchUuid(launchId);
        saveLogRQ.setLevel(level);

        saveLog(saveLogRQ);
    }


    private void sendAttachment(final String message, final String level, final String testId, String launchId,
                                final SaveLogRQ.File file) {
        SaveLogRQ rq = new SaveLogRQ();
        rq.setMessage(message);
        rq.setLevel(level);
        rq.setItemUuid(testId);
        rq.setLaunchUuid(launchId);
        rq.setLogTime(Calendar.getInstance().getTime());
        if (file != null) {
            sendFile(file, rq);
        } else {
            reportPortal.getClient().log(rq);
        }
    }

    private void sendFile(SaveLogRQ.File file, SaveLogRQ rq) {
        rq.setFile(file);
        MultiPartRequest request = HttpRequestUtils.buildLogMultiPartRequest(singletonList(rq));
        List<BatchElementCreatedRS> response = reportPortal.getClient().log(request).blockingGet().getResponses();
        if (response == null || response.isEmpty()) {
            throw new InternalReportPortalClientException("Report portal hasn't sent any response.");
        }
    }

    private void saveLog(SaveLogRQ saveLogRQ) {
        Maybe<EntryCreatedAsyncRS> logItem = reportPortal.getClient().log(saveLogRQ);
        try {
            logItem.blockingGet(); // not entirely sure, but if this is not here, (probably because of a timing issue), the log is not reported.
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendAttachmentsIfPresent(String testId, String launchId, String message) {
        Matcher attachments = attachmentPattern.matcher(message);
        while (attachments.find()) {
            if (!attachments.group(1).startsWith("http://") && !attachments.group(1).startsWith("https://")) {

                String filePath = new File(rootPath, attachments.group(1)).getAbsolutePath();
                try {
                    String fileName = new File(filePath).getName();
                    SaveLogRQ.File attachmentFile = new SaveLogRQ.File();
                    attachmentFile.setContent(Files.asByteSource(new File(filePath)).read());
                    attachmentFile.setName(fileName);
                    sendAttachment(fileName, "DEBUG", testId, launchId, attachmentFile);
                } catch (IOException e) {
                    sendLogData(testId, launchId, "Unable to read attachment file for: " + message, "DEBUG");
                }
            }
        }
    }

    public void attachFitNesseResultsToRun(String launchId) {
        try {
            ZipFile testReportBundle = new ZipFile("FitNesseResults.zip");
            testReportBundle.addFolder(new File(rootPath));
            FileInputStream zipStr = new FileInputStream(testReportBundle.getFile());
            byte[] zippedReport = IOUtils.toByteArray(zipStr);
            SaveLogRQ.File zipFile = new SaveLogRQ.File();
            zipFile.setContent(zippedReport);
            zipFile.setName("FitNesse-results.zip");
            System.out.println("Attaching result zip");
            sendAttachment("FitNesse-results.zip", "INFO", null, launchId, zipFile);
        } catch (Exception e) {
            System.err.println("Zip failed: " + e.getMessage());
            logger.error("An exception occured when attempting to attach the html report to the launch", e);
        }
    }
}
