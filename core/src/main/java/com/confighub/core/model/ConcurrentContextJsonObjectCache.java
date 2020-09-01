package com.confighub.core.model;

import com.confighub.core.repository.Repository;
import com.confighub.core.resolver.Context;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Objects;

public class ConcurrentContextJsonObjectCache extends HashMap<String, HashMap<Context, JsonObject>>
{
    private static final ConcurrentContextFilenameResponseCache instance = new ConcurrentContextFilenameResponseCache();

    public static ConcurrentContextFilenameResponseCache getInstance()
    {
        return instance;
    }

    public JsonObject put(Context context, JsonObject value)
    {
        synchronized (this)
        {
            HashMap<Context, JsonObject> map = getOrDefault(context.getRepository().getName(), new HashMap<>());
            put(context.getRepository().getName(), map);
            return map.put(context, value);
        }
    }

    public JsonObject putIfAbsent(Context context, JsonObject value)
    {
        synchronized (this)
        {
            HashMap<Context, JsonObject> map = getOrDefault(context.getRepository().getName(), new HashMap<>());
            put(context.getRepository().getName(), map);
            return map.putIfAbsent(context, value);
        }
    }

    public void removeByRepository(Repository repository)
    {
        synchronized (this)
        {
            remove(repository.getName());
        }
    }

    public JsonObject get(Context context)
    {
        HashMap<Context, JsonObject> map = getOrDefault(context.getRepository().getName(), new HashMap<>());
        return map.get(context);
    }

    public boolean containsKey(Context context)
    {
        return Objects.nonNull(get(context));
    }
}
