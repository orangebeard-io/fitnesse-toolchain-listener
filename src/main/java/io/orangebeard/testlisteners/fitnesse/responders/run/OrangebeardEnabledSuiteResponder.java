package io.orangebeard.testlisteners.fitnesse.responders.run;

import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.testrunner.MultipleTestsRunner;
import io.orangebeard.testlisteners.fitnesse.OrangebeardTestSystemListener;

public class OrangebeardEnabledSuiteResponder extends fitnesse.responders.run.SuiteResponder {
    private final OrangebeardTestSystemListener orangebeardListener = new OrangebeardTestSystemListener("orangebeard.properties", true);

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
