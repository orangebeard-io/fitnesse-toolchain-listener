package io.orangebeard.listener.responders.run;

import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.entity.log.LogLevel;
import io.orangebeard.listener.OrangebeardTestSystemListener;
import io.orangebeard.listener.helper.OrangebeardPropertyHelper;

import java.util.UUID;
import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.testrunner.MultipleTestsRunner;

import static io.orangebeard.client.OrangebeardProperty.ACCESS_TOKEN;
import static io.orangebeard.client.OrangebeardProperty.ENDPOINT;
import static io.orangebeard.client.OrangebeardProperty.LOGS_AT_END_OF_TEST;
import static io.orangebeard.client.OrangebeardProperty.LOG_LEVEL;
import static io.orangebeard.client.OrangebeardProperty.PROJECT;

public class OrangebeardEnabledTestResponder extends fitnesse.responders.run.TestResponder {
    private OrangebeardTestSystemListener orangebeardListener;

    @Override
    public Response makeResponse(FitNesseContext fitNesseProperties, Request request) throws Exception {
        var orangebeardProperties = new OrangebeardProperties(
                fitNesseProperties.getProperty(ENDPOINT.getPropertyName()),
                UUID.fromString(fitNesseProperties.getProperty(ACCESS_TOKEN.getPropertyName())),
                fitNesseProperties.getProperty(PROJECT.getPropertyName()),
                request.getResource(),
                "Single test executed from wiki",
                OrangebeardPropertyHelper.getAttributesFromQueryString(request.getQueryString()),
                OrangebeardPropertyHelper.getLogLevelFromStringOrElse(fitNesseProperties.getProperty(LOG_LEVEL.getPropertyName()), LogLevel.DEBUG),
                Boolean.parseBoolean(fitNesseProperties.getProperty(LOGS_AT_END_OF_TEST.getPropertyName())));

        orangebeardListener = new OrangebeardTestSystemListener(orangebeardProperties);

        return super.makeResponse(fitNesseProperties, request);
    }

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
