package com.confighub.core.utils;

import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.security.SecurityProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

public class PropertiesSubstitutor {

    private static final Logger log = LogManager.getLogger(PropertiesSubstitutor.class);

    private static String replaceKey(String key, String value, String text)
    {
        return text.replaceAll("\\$\\{" + Pattern.quote(key) + "}", value);
    }

    private static boolean containsKey(String key, String text)
    {
        return text.contains("${" + key + "}");
    }

    public static Map<PropertyKey, String> resolveTextSubstitutions(final Map<PropertyKey, Property> resolved,
                                                                    final Map<String, String> passwords) {
        Map<PropertyKey, String> textPropertiesValues = new HashMap<>();
        for (PropertyKey key : resolved.keySet())
        {
            //We are only interested in text properties
            if(key.getValueDataType().equals(PropertyKey.ValueDataType.Text))
            {
                Property property = resolved.get(key);
                if(key.isEncrypted())
                {
                    SecurityProfile sp = key.getSecurityProfile();
                    String password = passwords.get(sp.getName());
                    if(password != null) {
                        property.decryptValue(password);
                        textPropertiesValues.put(key, property.getValue());
                    }
                }
                else
                {
                    textPropertiesValues.put(key, property.getValue());
                }
            }
        }

        Map<PropertyKey, Set<PropertyKey>> inboundDependencies = new HashMap<>();
        Map<PropertyKey, Set<PropertyKey>> outboundDependencies = new HashMap<>();
        Queue<PropertyKey> queue = new ArrayDeque<>();

        for (PropertyKey key : textPropertiesValues.keySet())
        {
            inboundDependencies.put(key, new HashSet<>());
            outboundDependencies.put(key, new HashSet<>());
        }

        for (PropertyKey keyA : textPropertiesValues.keySet())
        {
            String text = textPropertiesValues.get(keyA);
            for (PropertyKey keyB : textPropertiesValues.keySet())
            {
                if(containsKey(keyB.getKey(), text))
                {
                    outboundDependencies.get(keyA).add(keyB);
                    inboundDependencies.get(keyB).add(keyA);
                }
            }
        }

        for (PropertyKey key : textPropertiesValues.keySet())
        {
            if(outboundDependencies.get(key).isEmpty())
            {
                queue.offer(key);
            }
        }

        while (!queue.isEmpty()) {
            PropertyKey key = queue.poll();
            for(PropertyKey dependantKey : inboundDependencies.get(key)) {
                textPropertiesValues.put(
                        dependantKey,
                        replaceKey(
                                key.getKey(),
                                textPropertiesValues.get(key),
                                textPropertiesValues.get(dependantKey)));
                Set<PropertyKey> dependencies = outboundDependencies.get(dependantKey);
                dependencies.remove(key);
                if(dependencies.isEmpty()) {
                    queue.offer(dependantKey);
                }
            }
        }

        return textPropertiesValues;
    }
}
