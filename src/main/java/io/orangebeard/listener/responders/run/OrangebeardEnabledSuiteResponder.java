package io.orangebeard.listener.responders.run;

import io.orangebeard.listener.OrangebeardTestSystemListener;
import io.orangebeard.listener.helper.OrangebeardPropertyHelper;

import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.testrunner.MultipleTestsRunner;

public class OrangebeardEnabledSuiteResponder extends fitnesse.responders.run.SuiteResponder {
    private OrangebeardTestSystemListener orangebeardListener;

    @Override
    public Response makeResponse(FitNesseContext context, Request request) throws Exception {
        OrangebeardPropertyHelper.setOrangebeardSystemProperties(context.variableSource);
        OrangebeardPropertyHelper.setTestSetName(request.getResource());
        OrangebeardPropertyHelper.setAttributesFromQueryString(request.getQueryString());

        orangebeardListener = new OrangebeardTestSystemListener("orangebeard.properties", true);

        return super.makeResponse(context, request);
    }

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
