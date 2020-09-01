package com.confighub.core.model;

import com.confighub.core.repository.Repository;
import com.confighub.core.resolver.Context;
import com.confighub.core.utils.Pair;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Objects;

public class ConcurrentContextFilenameResponseCache extends HashMap<String, HashMap<Pair<Context, String>, Response>>
{
    private static final ConcurrentContextFilenameResponseCache instance = new ConcurrentContextFilenameResponseCache();

    public static ConcurrentContextFilenameResponseCache getInstance()
    {
        return instance;
    }

    public Response put(Context context, String str, Response response)
    {
        synchronized (this)
        {
            HashMap<Pair<Context, String>, Response> map = getOrDefault(context.getRepository().getName(), new HashMap<>());
            put(context.getRepository().getName(), map);
            return map.put(new Pair<>(context, str), response);
        }
    }

    public Response putIfAbsent(Context context, String str, Response response)
    {
        synchronized (this)
        {
            HashMap<Pair<Context, String>, Response> map = getOrDefault(context.getRepository().getName(), new HashMap<>());
            putIfAbsent(context.getRepository().getName(), map);
            return map.putIfAbsent(new Pair<>(context, str), response);
        }
    }

    public void removeByRepository(Repository repository)
    {
        synchronized (this)
        {
            remove(repository.getName());
        }
    }

    public Response get(Context context, String str)
    {
        HashMap<Pair<Context, String>, Response> map = getOrDefault(context.getRepository().getName(), new HashMap<>());
        return map.get(new Pair<>(context, str));
    }

    public boolean containsKey(Context context, String str)
    {
        return Objects.nonNull(get(context, str));
    }
}
