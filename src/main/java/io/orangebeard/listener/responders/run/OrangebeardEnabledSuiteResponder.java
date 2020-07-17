package io.orangebeard.listener.responders.run;


import fitnesse.testrunner.MultipleTestsRunner;
import io.orangebeard.listener.OrangebeardTestSystemListener;

public class OrangebeardEnabledSuiteResponder extends fitnesse.responders.run.SuiteResponder {
    private final OrangebeardTestSystemListener orangebeardListener = new OrangebeardTestSystemListener("orangebeard.properties", true);

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
