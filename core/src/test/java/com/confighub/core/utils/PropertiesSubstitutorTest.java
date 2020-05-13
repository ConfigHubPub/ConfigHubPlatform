package com.confighub.core.utils;

import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.repository.Repository;
import com.confighub.core.user.Account;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PropertiesSubstitutorTest {

    private static final Repository testRepository =
            new Repository("TestRepository", Depth.D0, false, new Account());

    private void addProperty(String key, String value, Map<PropertyKey, Property> properties, Map<String, PropertyKey> keys) {
        PropertyKey propertyKey = new PropertyKey(testRepository, key, PropertyKey.ValueDataType.Text);
        Property property = new Property(testRepository);
        try {
            Field valueField = Property.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(property, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        properties.put(propertyKey, property);
        keys.put(key, propertyKey);
    }

    @Test
    public void substitutionBasicTest()
    {
        Map<PropertyKey, Property> properties = new HashMap<>();
        Map<String, PropertyKey> keys = new HashMap<>();

        addProperty("key0", "value0:${key1}", properties, keys);
        addProperty("key1", "value1:${key0}:${key2}", properties, keys);
        addProperty("key2", "value2", properties, keys);
        addProperty("key3", "value3:${key2}", properties, keys);
        addProperty("key4", "value4:${key3}", properties, keys);

        Map<PropertyKey, String> result = PropertiesSubstitutor.resolveTextSubstitutions(properties, new HashMap<>());

        assertEquals(properties.size(), result.size());
        assertEquals("value0:${key1}", result.get(keys.get("key0")));
        assertEquals("value1:${key0}:value2", result.get(keys.get("key1")));
        assertEquals("value2", result.get(keys.get("key2")));
        assertEquals("value3:value2", result.get(keys.get("key3")));
        assertEquals("value4:value3:value2", result.get(keys.get("key4")));
    }
}
