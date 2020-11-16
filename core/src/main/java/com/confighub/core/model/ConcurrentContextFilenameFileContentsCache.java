package com.confighub.core.model;

import com.confighub.core.repository.Repository;
import com.confighub.core.utils.Pair;

public class ConcurrentContextFilenameFileContentsCache extends ConcurrentRepositoryCache<Pair<String, String>, ContentsAndType>
{
    private static final ConcurrentContextFilenameFileContentsCache instance = new ConcurrentContextFilenameFileContentsCache();

    public static ConcurrentContextFilenameFileContentsCache getInstance()
    {
        return instance;
    }

    public ContentsAndType put(Repository repository, String context, String str, ContentsAndType value)
    {
        return super.put(repository, new Pair<>(context.toLowerCase(), str), value);
    }

    public ContentsAndType putIfAbsent(Repository repository, String context, String str, ContentsAndType value)
    {
        return super.putIfAbsent(repository, new Pair<>(context.toLowerCase(), str), value);
    }

    public ContentsAndType get(Repository repository, String context, String str)
    {
        return super.get(repository, new Pair<>(context.toLowerCase(), str));
    }

    public boolean containsKey(Repository repository, String context, String str)
    {
        return super.containsKey(repository, new Pair<>(context.toLowerCase(), str));
    }
}
