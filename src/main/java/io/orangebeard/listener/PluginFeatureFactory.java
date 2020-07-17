package io.orangebeard.testlisteners.fitnesse;

import fitnesse.Responder;
import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginFeatureFactoryBase;
import fitnesse.responders.ResponderFactory;
import io.orangebeard.testlisteners.fitnesse.responders.run.OrangeBeardEnabledTestResponder;
import io.orangebeard.testlisteners.fitnesse.responders.run.OrangebeardEnabledSuiteResponder;

public class PluginFeatureFactory extends PluginFeatureFactoryBase {
    @Override
    public void registerResponders(ResponderFactory responderFactory) throws PluginException {
        super.registerResponders(responderFactory);
        LOG.info("[Orangebeard Plugin] Registering Test Responder (?otest).");
        add(responderFactory, "otest", OrangeBeardEnabledTestResponder.class);
        LOG.info("[Orangebeard Plugin] Registering Test Responder (?osuite).");
        add(responderFactory, "osuite", OrangebeardEnabledSuiteResponder.class);
    }

    private void add(ResponderFactory factory, String key, Class<? extends Responder> responder) {
        factory.addResponder(key, responder);
    }
}
