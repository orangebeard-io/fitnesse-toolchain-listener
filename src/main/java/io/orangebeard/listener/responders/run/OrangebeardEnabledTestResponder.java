package io.orangebeard.listener.responders.run;

import io.orangebeard.listener.OrangebeardTestSystemListener;

import fitnesse.testrunner.MultipleTestsRunner;

public class OrangebeardEnabledTestResponder extends fitnesse.responders.run.TestResponder {
    private final OrangebeardTestSystemListener orangebeardListener = new OrangebeardTestSystemListener("orangebeard.properties", true);

    @Override
    protected void addFormatters(MultipleTestsRunner runner) {
        runner.addTestSystemListener(orangebeardListener);
        super.addFormatters(runner);
    }
}
