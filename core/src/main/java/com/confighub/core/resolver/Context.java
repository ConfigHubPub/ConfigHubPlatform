/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.resolver;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.*;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class Context
{
    private static final Logger log = LogManager.getLogger(Context.class);

    protected final Map<Depth, Collection<Level>> elements = new HashMap<>();
    protected final Map<Depth, Collection<Level>> parents = new HashMap<>();
    protected final Set<Depth> wildcards = new HashSet<>();
    protected final Repository repository;
    protected final Store store;
    protected final Date date;
    protected final boolean all;

    private final EnumSet<Depth> depths;

    public Context(final Store store, final Date date, final Repository repository, final boolean allKeys)
            throws ConfigException
    {
        if (null == repository)
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        this.store = store;
        this.depths = repository.getDepth().getDepths();
        this.repository = repository;
        this.date = date;
        this.all = allKeys;

        for (Depth depth : this.depths)
        {
            elements.put(depth, new HashSet<>());
            parents.put(depth, new HashSet<>());
        }

        updateWildcards(this.elements, this.wildcards);
    }

    public Repository getRepository()
    {
        return repository;
    }

    public Date getDate()
    {
        return this.date;
    }

    public Context(final Store store,
                   final Repository repository,
                   final Collection<Level> contextElements,
                   final Date date)
            throws ConfigException
    {
        this(store, repository, contextElements, date, false);
    }

    /**
     * @param repository
     * @param contextElements
     * @param date
     * @throws ConfigException
     */
    public Context(final Store store,
                   final Repository repository,
                   final Collection<Level> contextElements,
                   final Date date,
                   boolean allKeys)
            throws ConfigException
    {
        this(store, date, repository, allKeys);
        if (null != contextElements)
        {
            for (Level l : contextElements) add(l);
            updateWildcards(this.elements, this.wildcards);
        }
    }


    protected boolean containsLevelAtDepth(Depth d, String levelName)
    {
        Collection<Level> dls = elements.get(d);
        Collection<Level> pls = parents.get(d);

        if ((null == dls || dls.size() == 0) && (null == pls || pls.size() == 0))
            return false;

        for (Level l : dls)
            if (l.getName().equalsIgnoreCase(levelName))
                return true;

        for (Level l : pls)
            if (l.getName().equalsIgnoreCase(levelName))
                return true;

        return false;
    }

    public boolean isAudit() { return null != this.date; }

    private void add(final Level level)
            throws ConfigException
    {
        // check depth since repository depth might have changed, and the user
        // could be loading stale data
        if (elements.containsKey(level.getDepth()))
        {
            if (level.isMember() && null != level.getGroups())
                parents.get(level.getDepth()).addAll(level.getGroups());

            elements.get(level.getDepth()).add(level);
        }
        else
            throw new ConfigException(Error.Code.CONTEXT_SCOPE_MISMATCH);
    }

    public static void updateWildcards(Map<Depth, Collection<Level>> e, Set<Depth> w)
    {
        for (Depth depth : e.keySet())
        {
            if (e.get(depth).size() == 1)
                w.remove(depth);
            else
                w.add(depth);
        }
    }

    public Collection<Level> getContextItems()
    {
        Collection<Level> contextItems = new ArrayList<>();
        this.elements.values().forEach(contextItems::addAll);

        return contextItems;
    }

    /**
     * Resolves for key among given properties, and returns properties that are categorized.
     *
     * @param keys
     * @return
     * @throws ConfigException
     */
    public Collection<Property> contextualSplitKeyResolver(final String... keys)
            throws ConfigException
    {
        UIKeyCategoryResolver resolver = new UIKeyCategoryResolver(store);
        return resolver.resolve(this, keys);
    }

    /**
     * Resolve for contextually relevant property collection
     *
     * @param keys to resolve context for
     * @return Collection of Properties
     * @throws ConfigException
     */
    public Collection<Property> keyResolver(final String... keys)
            throws ConfigException
    {
        UIKeyResolver resolver = new UIKeyResolver(store);
        return resolver.resolve(this, keys);
    }

    /**
     *
     * @return
     */
    public boolean isFullContext()
    {
        return wildcards.isEmpty() && repository.getDepth().getIndex() == this.elements.size();
    }

    /**
     *
     * @return
     * @throws ConfigException
     */
    public Map<PropertyKey, Property> resolveForClient()
            throws ConfigException
    {
        RepositoryPropertiesResolver resolver = new RepositoryPropertiesResolver(store, true);
        return resolver.resolveClient(this);
    }

    /**
     * Full repository resolver.  Depending on the state of the context,
     * this will resolver for either full/partial context.
     *
     * @return Map of key, Collection of properties resolved
     * @throws ConfigException
     */
    public Map<PropertyKey, Collection<Property>> resolve()
            throws ConfigException
    {
        RepositoryPropertiesResolver resolver = new RepositoryPropertiesResolver(store, false);
        return resolver.resolve(this);
    }

    public Map<PropertyKey, Collection<Property>> resolveFile(final Collection<PropertyKey> keys, boolean isClient)
            throws ConfigException
    {
        FilePropertiesResolver resolver = new FilePropertiesResolver(store, keys, isClient);
        return resolver.resolve(this);
    }


    public Map<AbsoluteFilePath, Collection<RepoFile>> resolveFiles(UserAccount user,
                                                                    String searchQuery,
                                                                    boolean searchResolved)
        throws ConfigException
    {
        RepositoryFilesResolver resolver = new RepositoryFilesResolver(store, user, false, searchQuery, searchResolved);
        return resolver.resolve(this);
    }

    public Map<AbsoluteFilePath, RepoFile> resolveAPIFiles()
            throws ConfigException
    {
        RepositoryFilesResolver resolver = new RepositoryFilesResolver(store);
        return resolver.resolveClient(this);
    }

    public RepoFile resolveFullContextFilePath(final AbsoluteFilePath absoluteFilePath)
            throws ConfigException
    {
        RepositoryFilesResolver resolver = new RepositoryFilesResolver(store);
        return resolver.resolveFullContextFilePath(this, absoluteFilePath);
    }

    public Collection<RepoFile> resolvePartialContextFilePath(final AbsoluteFilePath absoluteFilePath)
            throws ConfigException
    {
        RepositoryFilesResolver resolver = new RepositoryFilesResolver(store);
        return resolver.resolvePartialContextFilePath(this, absoluteFilePath);
    }

    public Collection<Property> partialContextKeyResolver(final PropertyKey key)
            throws ConfigException
    {
        UIKeyResolver resolver = new UIKeyResolver(store);
        return resolver.partialContextKeyResolver(this, key);
    }


    /**
     *
     * @return
     * @throws ConfigException
     */
    public Map<PropertyKey, Collection<Property>> literalContextResolver()
            throws ConfigException
    {
        LiteralContextResolver resolver = new LiteralContextResolver(store);
        return resolver.resolve(this);
    }

    /**
     *
     * @param key
     * @param allValues
     * @return
     * @throws ConfigException
     */
    public Collection<Property> literalKeyResolver(String key, boolean allValues)
            throws ConfigException
    {
        LiteralKeyResolver resolver = new LiteralKeyResolver(store);
        return resolver.resolve(this, key, allValues);
    }

    @Override
    public String toString()
    {
        List<String> ctx = new ArrayList<>();

        for (Depth depth : depths)
        {
            Collection<Level> levels = elements.get(depth);
            String toAdd;

            if (null == levels)
                toAdd = "*";
            else
            {
                List<String> lls = levels.stream().map(Level::getName).collect(Collectors.toList());
                toAdd = Utils.join(lls, ", ");
            }

            levels = parents.get(depth);

            if (null != levels)
            {
                List<String> lls = levels.stream().map(Level::getName).collect(Collectors.toList());
                if (lls.size() > 0)
                    toAdd += "[" + Utils.join(lls, ", ") + "]";
            }

            ctx.add(toAdd);
        }

        return null == this.date
                ? Utils.join(ctx, " > ")
                : DateTimeUtils.standardDTFormatter.get().format(this.date) + " | " + Utils.join(ctx, " > ");
    }

    public enum PropertyType
    {
        match(3000),
        override(2000),
        outOfContext(0),
        self(0);

        public final int scoreSupplement;

        PropertyType(int scoreSupplement)
        {
            this.scoreSupplement = scoreSupplement;
        }
    }
}
