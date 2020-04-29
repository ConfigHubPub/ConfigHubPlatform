package com.confighub.core.model;

import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.resolver.Context;
import com.google.common.collect.ImmutableMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentContextPropertiesCache
{
    private static final ConcurrentMap<Context, ImmutableMap<PropertyKey, Property>> instance = new ConcurrentHashMap<>();

    public static ConcurrentMap<Context, ImmutableMap<PropertyKey, Property>> getInstance()
    {
        return instance;
    }
}
