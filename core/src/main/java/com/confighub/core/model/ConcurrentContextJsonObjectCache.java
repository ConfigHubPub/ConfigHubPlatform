package com.confighub.core.model;

import com.confighub.core.repository.Repository;
import com.google.gson.JsonObject;

public class ConcurrentContextJsonObjectCache extends ConcurrentRepositoryCache<String, JsonObject>
{
    private static final ConcurrentContextJsonObjectCache instance = new ConcurrentContextJsonObjectCache();

    public static ConcurrentContextJsonObjectCache getInstance()
    {
        return instance;
    }

    public JsonObject put(Repository repository, String context, JsonObject value)
    {
        return super.put(repository, context.toLowerCase(), value);
    }

    public JsonObject putIfAbsent(Repository repository, String context, JsonObject value)
    {
         return super.putIfAbsent(repository, context.toLowerCase(), value);
    }

    public JsonObject get(Repository repository, String context)
    {
        return super.get(repository, context.toLowerCase());
    }

    public boolean containsKey(Repository repository, String context)
    {
        return super.containsKey(repository, context.toLowerCase());
    }
}
