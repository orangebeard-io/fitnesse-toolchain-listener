package io.orangebeard.listener.orangebeardv3client;
import io.orangebeard.client.entity.Attachment;
import io.orangebeard.client.entity.FinishTestItem;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.StartTestItem;

import io.orangebeard.listener.orangebeardv3client.entities.FinishTest;
import io.orangebeard.listener.orangebeardv3client.entities.FinishTestRun;
import io.orangebeard.listener.orangebeardv3client.entities.StartSuiteRQ;

import io.orangebeard.listener.orangebeardv3client.entities.StartTest;

import io.orangebeard.listener.orangebeardv3client.entities.StartTestRun;

import io.orangebeard.listener.orangebeardv3client.entities.Suite;
import io.orangebeard.listener.orangebeardv3client.entities.TestRunUUID;

import io.orangebeard.listener.orangebeardv3client.entities.TestUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class OrangebeardV3Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardV3Client.class);
    private final String endpoint;
    private final RestTemplate restTemplate;
    private final String projectName;
    private boolean connectionWithOrangebeardIsValid;

    protected final UUID accessToken;


    protected HttpHeaders getAuthorizationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken.toString());
        headers.setContentType(APPLICATION_JSON);
        return headers;
    }

    public OrangebeardV3Client(String endpoint, UUID accessToken, String projectName, boolean connectionWithOrangebeardIsValid) {
        this.accessToken = accessToken;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
        this.endpoint = endpoint;
        this.projectName = projectName == null ? null : projectName.toLowerCase();
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public UUID startTestRun(StartTestRun testRun) {
        if (this.connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartTestRun> request = new HttpEntity(testRun, this.getAuthorizationHeaders());
                return this.restTemplate.exchange(
                        String.format("%s/listener/v3/%s/test-run/start", this.endpoint, this.projectName),
                        HttpMethod.POST,
                        request,
                        TestRunUUID.class).getBody().getTestRunUUID();

            } catch (Exception var3) {
                LOGGER.error("The connection with Orangebeard could not be established! Check the properties and try again!");
                this.connectionWithOrangebeardIsValid = false;
            }
        }
        return null;
    }

    public List<Suite> startSuite(StartSuiteRQ suiteRQ) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestItem> request = new HttpEntity(suiteRQ, this.getAuthorizationHeaders());
            ResponseEntity<Suite[]> response = this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/suite/start", this.endpoint, this.projectName),
                    HttpMethod.POST,
                    request,
                    Suite[].class);

            return Arrays.asList(response.getBody());
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
            return null;
        }
    }

    public UUID startTestItem(StartTest startTest) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<StartTestItem> request = new HttpEntity(startTest, this.getAuthorizationHeaders());
            return  this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/test/start", this.endpoint, this.projectName),
                    HttpMethod.POST,
                    request,
                    TestUUID.class).getBody().getGetTestUUID();
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
            return null;
        }
    }

    public void FinishTest(UUID testUUId, FinishTest finishTest) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestItem> request = new HttpEntity(finishTest, this.getAuthorizationHeaders());
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/test/finish/%s", this.endpoint, this.projectName, testUUId),
                    HttpMethod.PUT,
                    request,
                    Void.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }

    }

    public void finishTestRun(UUID testRunUUID, FinishTestRun finishTestRun) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestRun> request = new HttpEntity(finishTestRun, this.getAuthorizationHeaders());
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/test-run/finish/%s", this.endpoint, this.projectName, testRunUUID),
                    HttpMethod.PUT,
                    request,
                    Void.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }

    }

    public void log(Log log) {
        this.log(Collections.singleton(log));
    }

    public void log(Set<Log> logs) {
        LOGGER.warn("Log api for v3client not defined yet");
    }

    public void sendAttachment(Attachment attachment) {
        LOGGER.warn(" sendAttachemnt api for v3client not defined yet");
    }
}

