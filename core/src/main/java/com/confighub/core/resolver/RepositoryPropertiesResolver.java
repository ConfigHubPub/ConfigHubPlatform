/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.resolver;

import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.LevelCtx;
import com.confighub.core.repository.Property;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RepositoryPropertiesResolver
        extends AResolver
{
    private static final Logger log = LogManager.getLogger("RepositoryResolver");
    private final boolean isClient;

    public RepositoryPropertiesResolver(final Store store, final boolean isClient)
    {
        super(store);
        this.isClient = isClient;
    }

    protected Map<PropertyKey, Property> resolveClient(final Context context)
            throws ConfigException
    {
        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        if (!context.isFullContext())
            throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

        Map<PropertyKey, Collection<Property>> propertiesByKey = getPropertiesByKey(context);
        Map<PropertyKey, Property> resolved = new HashMap<>();

        for (PropertyKey propertyKey : propertiesByKey.keySet())
        {
            Property heaviest = null;

            for (Property property : propertiesByKey.get(propertyKey))
            {
                if (null == heaviest || property.getContextWeight() > heaviest.getContextWeight())
                    heaviest = property;
            }

            if (null != heaviest)
                resolved.put(propertyKey, heaviest);
        }

        return resolved;
    }

    /**
     * Full or partial context resolver
     *
     * @param context
     * @return
     * @throws ConfigException
     */
    protected Map<PropertyKey, Collection<Property>> resolve(final Context context)
            throws ConfigException
    {
        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        Map<PropertyKey, Collection<Property>> propertiesByKey = getPropertiesByKey(context);
        Map<PropertyKey, Collection<Property>> resolved;

        if (context.isFullContext())
            resolved = fullContext(propertiesByKey);
        else
            resolved = partialContext(propertiesByKey, context.wildcards);

        return resolved;
    }


    /**
     * Full context resolver for the UI which includes:
     * - key property counts
     * - non-active properties
     */
    protected static Map<PropertyKey, Collection<Property>> fullContext(Map<PropertyKey,
                                                                        Collection<Property>> propertiesByKey)
            throws ConfigException
    {

        // final filtering
        Map<PropertyKey, Collection<Property>> resolved = new HashMap<>();
        for (PropertyKey propertyKey : propertiesByKey.keySet())
        {
            List<Property> matchedProperties = new ArrayList<>();
            Property heaviest = null;

            for (Property property : propertiesByKey.get(propertyKey))
            {
                if (!property.isActive())
                    matchedProperties.add(property);
                else if (null == heaviest || property.getContextWeight() > heaviest.getContextWeight())
                    heaviest = property;
            }

            if (null != heaviest)
                matchedProperties.add(heaviest);

            resolved.put(propertyKey, matchedProperties);
        }

        return resolved;
    }


    /**
     * Resolve repository properties for a partially specified context
     */
    protected static Map<PropertyKey, Collection<Property>> partialContext(Map<PropertyKey, Collection<Property>>
                                                                           propertiesByKey,
                                                                           final Set<Depth> wildcards)
            throws ConfigException
    {
        Map<PropertyKey, Collection<Property>> resolved = new HashMap<>();

        for (PropertyKey propertyKey : propertiesByKey.keySet())
        {
            Set<Property> matchedProperties = new HashSet<>();
            Property heaviest = null;

            Collection<Property> keyProperties = propertiesByKey.get(propertyKey);
            for (Property property : keyProperties)
            {
                boolean propertyIsWildcardMatch = false;
                Map<String, LevelCtx> propDepthMap = property.getDepthMap();
                for (Depth depth : wildcards)
                {
                    // property has context &&
                    // property has context at the depth context is wildcard-ed
                    if (null != propDepthMap &&
                        propDepthMap.containsKey(String.valueOf(depth.getPlacement())))
                    {
                        propertyIsWildcardMatch = true;
                        matchedProperties.add(property);
                        break;
                    }
                }

                // Properties that are not matched based on wildcards in the context,
                // are treated as full-context resolution, and are therefore weight
                // compared.
                if (!propertyIsWildcardMatch)
                {
                    if (!property.isActive())
                        matchedProperties.add(property);
                    if (null == heaviest || property.getContextWeight() > heaviest.getContextWeight())
                        heaviest = property;
                }
            }

            if (null != heaviest)
                matchedProperties.add(heaviest);

            resolved.put(propertyKey, matchedProperties);
        }

        return resolved;
    }

    // - Initial filtering by context;
    // - organizing by key
    // - counting siblings
    private Map<PropertyKey, Collection<Property>> getPropertiesByKey(Context context)
            throws ConfigException
    {
        Collection<Property> properties;
        if (null == context.date)
            properties = context.repository.getProperties();
        else
            properties = store.getProperties(context.repository, null == context.date ? new Date() : context.date);

        Map<Long, Integer> counters = new HashMap<>();

        Map<PropertyKey, Collection<Property>> propertiesByKey = new HashMap<>();
        if (context.all)
        {
            if (null != context.repository.getKeys())
            {
                for (PropertyKey k : context.repository.getKeys())
                {
                    propertiesByKey.put(k, new ArrayList<>());
                    counters.put(k.getId(), 0);
                }
            }
        }

        for (Property property : properties)
        {
            boolean match = isContextualMatchAudit(property.getDepthMap(), context);
            boolean inMap = propertiesByKey.containsKey(property.getPropertyKey());

            PropertyKey key = property.getPropertyKey();

            int cnt = 0;
            if (counters.containsKey(key.getId()))
                cnt = counters.get(key.getId());
            counters.put(key.getId(), ++cnt);

            if (!inMap && (context.all || match))
                propertiesByKey.put(key, new ArrayList<>());

            if (this.isClient && !property.isActive())
                continue;

            if (match)
                propertiesByKey.get(key).add(property);
        }

        for (PropertyKey key : propertiesByKey.keySet())
            key.propertyCount = counters.get(key.getId());

        return propertiesByKey;
    }

}
