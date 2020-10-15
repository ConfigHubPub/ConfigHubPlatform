package com.confighub.core.model;

import com.confighub.core.repository.Repository;
import com.confighub.core.utils.Pair;

import javax.ws.rs.core.Response;

public class ConcurrentContextFilenameResponseCache extends ConcurrentRepositoryCache<Pair<String, String>, Response>
{
    private static final ConcurrentContextFilenameResponseCache instance = new ConcurrentContextFilenameResponseCache();

    public static ConcurrentContextFilenameResponseCache getInstance()
    {
        return instance;
    }

    public Response put(Repository repository, String context, String str, Response response)
    {
        return super.put(repository, new Pair<>(context.toLowerCase(), str), response);
    }

    public Response putIfAbsent(Repository repository, String context, String str, Response response)
    {
        return super.putIfAbsent(repository, new Pair<>(context.toLowerCase(), str), response);
    }

    public Response get(Repository repository, String context, String str)
    {
        return super.get(repository, new Pair<>(context.toLowerCase(), str));
    }

    public boolean containsKey(Repository repository, String context, String str)
    {
        return super.containsKey(repository, new Pair<>(context.toLowerCase(), str));
    }
}
