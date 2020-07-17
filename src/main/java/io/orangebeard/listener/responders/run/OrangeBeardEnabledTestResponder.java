package io.orangebeard.testlisteners.fitnesse.responders.run;

import fitnesse.testrunner.MultipleTestsRunner;
import io.orangebeard.testlisteners.fitnesse.OrangebeardTestSystemListener;

public class OrangeBeardEnabledTestResponder extends fitnesse.responders.run.TestResponder {
    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(new OrangebeardTestSystemListener("orangebeard.properties", true));
        super.addFormatters(runner);
    }
}
