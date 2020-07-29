package io.orangebeard.listener;

import io.orangebeard.listener.responders.run.OrangebeardEnabledSuiteResponder;
import io.orangebeard.listener.responders.run.OrangebeardEnabledTestResponder;

import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginFeatureFactoryBase;
import fitnesse.responders.ResponderFactory;

public class OrangebeardPluginFeatureFactory extends PluginFeatureFactoryBase {
    @Override
    public void registerResponders(ResponderFactory responderFactory) throws PluginException {
        super.registerResponders(responderFactory);
        LOG.info("[Orangebeard Plugin] Registering Test Responder (?otest).");
        responderFactory.addResponder("otest", OrangebeardEnabledTestResponder.class);
        LOG.info("[Orangebeard Plugin] Registering Suite Responder (?osuite).");
        responderFactory.addResponder("osuite", OrangebeardEnabledSuiteResponder.class);
    }
}
