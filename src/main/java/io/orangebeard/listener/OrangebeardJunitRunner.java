package io.orangebeard.listener;

import fitnesse.junit.DescriptionFactory;
import fitnesse.testrunner.MultipleTestsRunner;
import nl.hsac.fitnesse.junit.HsacFitNesseRunner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

public class OrangebeardJunitRunner extends HsacFitNesseRunner {

    public OrangebeardJunitRunner(Class<?> suiteClass) throws InitializationError {
        super(suiteClass);
        System.getProperties().setProperty("nodebug", "true");
    }

    @Override
    protected void addTestSystemListeners(RunNotifier notifier, MultipleTestsRunner testRunner, Class<?> suiteClass, DescriptionFactory descriptionFactory) {
        super.addTestSystemListeners(notifier, testRunner, suiteClass, descriptionFactory);

        testRunner.addTestSystemListener(new OrangebeardTestSystemListener(null, FITNESSE_RESULTS_PATH));
    }
}
