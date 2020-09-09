package com.confighub.core.model;

import com.confighub.core.repository.Repository;

import java.util.HashMap;
import java.util.Objects;

public class ConcurrentRepositoryCache<K, V> extends HashMap<String, HashMap<K, V>> {

    public V put(Repository repository, K key, V value)
    {
        synchronized (this)
        {
            HashMap<K, V> map = getOrDefault(repository.getName(), new HashMap<>());
            put(repository.getName(), map);
            return map.put(key, value);
        }
    }

    public V putIfAbsent(Repository repository, K key, V value)
    {
        synchronized (this)
        {
            HashMap<K, V> map = getOrDefault(repository.getName(), new HashMap<>());
            putIfAbsent(repository.getName(), map);
            return map.putIfAbsent(key, value);
        }
    }

    public void removeByRepository(Repository repository)
    {
        synchronized (this)
        {
            remove(repository.getName());
        }
    }

    public V get(Repository repository, K key)
    {
        HashMap<K, V> map = getOrDefault(repository.getName(), new HashMap<>());
        return map.get(key);
    }

    public boolean containsKey(Repository repository, K key)
    {
        return Objects.nonNull(get(repository, key));
    }
}
