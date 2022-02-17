package io.orangebeard.listener.responders.run;

import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.OrangebeardProperty;
import io.orangebeard.client.entity.LogLevel;
import io.orangebeard.listener.OrangebeardTestSystemListener;
import io.orangebeard.listener.helper.OrangebeardPropertyHelper;

import java.util.UUID;
import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.testrunner.MultipleTestsRunner;

public class OrangebeardEnabledTestResponder extends fitnesse.responders.run.TestResponder {
    private OrangebeardTestSystemListener orangebeardListener;

    @Override
    public Response makeResponse(FitNesseContext fitNesseProperties, Request request) throws Exception {
        var orangebeardProperties = new OrangebeardProperties(
                fitNesseProperties.getProperty(OrangebeardProperty.ENDPOINT.getPropertyName()),
                UUID.fromString(fitNesseProperties.getProperty(OrangebeardProperty.ACCESS_TOKEN.getPropertyName())),
                fitNesseProperties.getProperty(OrangebeardProperty.PROJECT.getPropertyName()),
                fitNesseProperties.getProperty(OrangebeardProperty.TESTSET.getPropertyName()),
                "Single test executed from wiki",
                OrangebeardPropertyHelper.getAttributesFromQueryString(request.getQueryString()),
                LogLevel.debug,
                false);

        orangebeardListener = new OrangebeardTestSystemListener(orangebeardProperties);

        return super.makeResponse(fitNesseProperties, request);
    }

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
