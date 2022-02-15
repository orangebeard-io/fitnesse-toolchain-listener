package io.orangebeard.listener.responders.run;

import io.orangebeard.listener.OrangebeardTestSystemListener;

import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.testrunner.MultipleTestsRunner;

public class OrangebeardEnabledTestResponder extends fitnesse.responders.run.TestResponder {
    private OrangebeardTestSystemListener orangebeardListener;

    @Override
    public Response makeResponse(FitNesseContext context, Request request) throws Exception {
        OrangebeardPropertyHelper.setOrangebeardSystemProperties(context.variableSource);
        OrangebeardPropertyHelper.setTestSetName(request.getResource());
        OrangebeardPropertyHelper.setDescription("Single test executed from wiki");
        orangebeardListener = new OrangebeardTestSystemListener("orangebeard.properties", true);

        return super.makeResponse(context, request);
    }

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
