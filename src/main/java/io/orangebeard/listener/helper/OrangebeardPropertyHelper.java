package io.orangebeard.listener.helper;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.log.LogLevel;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fitnesse.http.Request.decodeContent;

public class OrangebeardPropertyHelper {
    private static final Pattern queryStringPattern = Pattern.compile("([^=&]*)=?([^&]*)&?");

    private OrangebeardPropertyHelper() {
    }

    public static Set<Attribute> getAttributesFromQueryString(String queryString) {
        Matcher match = queryStringPattern.matcher(queryString);

        Set<Attribute> attributes = new HashSet<>();
        while (match.find()) {
            String key = decodeContent(match.group(1));
            String value = decodeContent(match.group(2));
            if (value != null && !value.isEmpty()) {
                attributes.add(new Attribute(key, value));
            }
        }
        return attributes;
    }

    public static LogLevel getLogLevelFromStringOrElse(String level, LogLevel defaultLevel) {
        return level != null ? LogLevel.valueOf(level.toUpperCase()) : defaultLevel;
    }
}
