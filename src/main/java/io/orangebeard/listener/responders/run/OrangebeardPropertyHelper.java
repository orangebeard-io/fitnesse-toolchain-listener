package io.orangebeard.listener.responders.run;

import io.orangebeard.client.OrangebeardProperty;
import fitnesse.wiki.SystemVariableSource;

public class OrangebeardPropertyHelper {

    private OrangebeardPropertyHelper() {
    }

    public static void setOrangebeardSystemProperties(SystemVariableSource fitNesseProperties) {
        System.setProperty(OrangebeardProperty.ENDPOINT.getPropertyName(), fitNesseProperties.getProperty(OrangebeardProperty.ENDPOINT.getPropertyName()));
        System.setProperty(OrangebeardProperty.ACCESS_TOKEN.getPropertyName(), fitNesseProperties.getProperty(OrangebeardProperty.ACCESS_TOKEN.getPropertyName()));
        System.setProperty(OrangebeardProperty.PROJECT.getPropertyName(), fitNesseProperties.getProperty(OrangebeardProperty.PROJECT.getPropertyName()));
    }

    public static void setTestSetName(String testSetName) {
        System.setProperty(OrangebeardProperty.TESTSET.getPropertyName(), testSetName);
    }

    public static void setDescription(String description) {
        System.setProperty(OrangebeardProperty.DESCRIPTION.getPropertyName(), description);
    }
}
