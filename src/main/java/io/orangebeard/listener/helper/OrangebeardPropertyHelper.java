package io.orangebeard.listener.helper;

import io.orangebeard.client.OrangebeardProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import fitnesse.wiki.SystemVariableSource;

import static fitnesse.http.Request.decodeContent;

public class OrangebeardPropertyHelper {
    private static final Pattern queryStringPattern = Pattern.compile("([^=&]*)=?([^&]*)&?");

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

    public static void setAttributesFromQueryString(String queryString) {
        Matcher match = queryStringPattern.matcher(queryString);
        StringBuilder attrs = new StringBuilder();
        while (match.find()) {
            String key = decodeContent(match.group(1));
            String value = decodeContent(match.group(2));
            if (value != null && !value.isEmpty()) {
                attrs.append(key).append(":").append(value).append(";");
            }
        }
        System.setProperty(OrangebeardProperty.ATTRIBUTES.getPropertyName(), attrs.toString());
    }
}
