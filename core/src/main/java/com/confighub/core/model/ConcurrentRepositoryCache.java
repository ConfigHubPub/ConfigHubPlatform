package com.confighub.core.model;

import com.confighub.core.repository.Repository;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentRepositoryCache<K, V> extends HashMap<String, ConcurrentHashMap<K, V>> {

    public V put(Repository repository, K key, V value)
    {
        ConcurrentHashMap<K, V> map = get(repository.getName());
        if (Objects.isNull(map))
        {
            synchronized (this)
            {
                map = getOrDefault(repository.getName(), new ConcurrentHashMap<>());  // in case another call already made this
                put(repository.getName(), map);
            }
        }
        return map.put(key, value);
    }

    public V putIfAbsent(Repository repository, K key, V value)
    {
        ConcurrentHashMap<K, V> map = get(repository.getName());
        if (Objects.isNull(map))
        {
            synchronized (this)
            {
                map = getOrDefault(repository.getName(), new ConcurrentHashMap<>());  // in case another call already made this
                putIfAbsent(repository.getName(), map);
            }
        }
        return map.putIfAbsent(key, value);
    }

    public void removeByRepository(Repository repository)
    {
        if (containsKey(repository.getName()))
        {
            synchronized (this)
            {
                remove(repository.getName());
            }
        }
    }

    public V get(Repository repository, K key)
    {
        return getOrDefault(repository.getName(), new ConcurrentHashMap<>()).get(key);
    }

    public boolean containsKey(Repository repository, K key)
    {
        return Objects.nonNull(get(repository, key));
    }
}
