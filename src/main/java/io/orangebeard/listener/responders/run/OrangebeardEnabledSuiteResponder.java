package io.orangebeard.listener.responders.run;

import io.orangebeard.client.OrangebeardProperty;
import io.orangebeard.listener.OrangebeardTestSystemListener;

import java.util.HashMap;
import java.util.Map;
import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.testrunner.MultipleTestsRunner;
import fitnesse.wiki.SystemVariableSource;

public class OrangebeardEnabledSuiteResponder extends fitnesse.responders.run.SuiteResponder {
    private OrangebeardTestSystemListener orangebeardListener;
    private final Map<String,String> orangebeardProperties = new HashMap<>();

    @Override
    public Response makeResponse(FitNesseContext context, Request request) throws Exception {
        SystemVariableSource fitNesseProperties = context.variableSource;

        orangebeardProperties.put(OrangebeardProperty.ENDPOINT.getPropertyName(), fitNesseProperties.getProperty(OrangebeardProperty.ENDPOINT.getPropertyName()));
        orangebeardProperties.put(OrangebeardProperty.ACCESS_TOKEN.getPropertyName(), fitNesseProperties.getProperty(OrangebeardProperty.ACCESS_TOKEN.getPropertyName()));
        orangebeardProperties.put(OrangebeardProperty.PROJECT.getPropertyName(), fitNesseProperties.getProperty(OrangebeardProperty.PROJECT.getPropertyName()));
        orangebeardProperties.put(OrangebeardProperty.TESTSET.getPropertyName(), request.getResource());

        for (Map.Entry<String, String> property : orangebeardProperties.entrySet()) {
            if(property.getValue() != null) {
                System.setProperty(property.getKey(), property.getValue());
            }
        }

        orangebeardListener = new OrangebeardTestSystemListener("orangebeard.properties", true);
        return super.makeResponse(context, request);
    }

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
