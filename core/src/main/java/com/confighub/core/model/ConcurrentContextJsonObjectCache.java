package com.confighub.core.model;

import com.confighub.core.repository.Repository;
import com.confighub.core.resolver.Context;
import com.google.gson.JsonObject;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentContextJsonObjectCache extends ConcurrentHashMap<Context, JsonObject>
{
    private static final ConcurrentContextFilenameResponseCache instance = new ConcurrentContextFilenameResponseCache();

    public static ConcurrentContextFilenameResponseCache getInstance()
    {
        return instance;
    }

    public void removeByRepository(Repository repository)
    {
        keySet().stream()
                .filter(ctx -> ctx.getRepository().equals(repository))
                .forEach(this::remove);
    }
}
