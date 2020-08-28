package com.confighub.core.model;

import com.confighub.core.repository.Repository;
import com.confighub.core.resolver.Context;
import com.confighub.core.utils.Pair;

import javax.ws.rs.core.Response;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentContextFilenameResponseCache extends ConcurrentHashMap<Pair<Context, String>, Response>
{
    private static final ConcurrentContextFilenameResponseCache instance = new ConcurrentContextFilenameResponseCache();

    public static ConcurrentContextFilenameResponseCache getInstance()
    {
        return instance;
    }

    public Response put(Context context, String str, Response response)
    {
        return put(new Pair<>(context, str), response);
    }

    public Response get(Context context, String str)
    {
        return get(new Pair<>(context, str));
    }

    public boolean containsKey(Context context, String str)
    {
        return containsKey(new Pair<>(context, str));
    }

    public Response putIfAbsent(Context context, String str, Response response)
    {
        return putIfAbsent(new Pair<>(context, str), response);
    }

    public void removeByRepository(Repository repository)
    {
        keySet().stream()
                .filter(ctx -> ctx.car.getRepository().equals(repository))
                .forEach(this::remove);
    }
}
