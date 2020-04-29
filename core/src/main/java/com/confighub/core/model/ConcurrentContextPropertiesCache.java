package com.confighub.core.model;

import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.repository.Repository;
import com.confighub.core.resolver.Context;
import com.google.common.collect.ImmutableMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentContextPropertiesCache extends ConcurrentHashMap<Context, ImmutableMap<PropertyKey, Property>>
{
    private static final ConcurrentContextPropertiesCache instance = new ConcurrentContextPropertiesCache();

    public static ConcurrentContextPropertiesCache getInstance()
    {
        return instance;
    }

    public void removeByRepository(Repository repository)
    {
        keySet().stream()
                .filter(ctx -> ctx.getRepository().equals(repository))
                .forEach(ConcurrentContextPropertiesCache.getInstance()::remove);
    }
}
