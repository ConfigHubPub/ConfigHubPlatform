/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.resolver;

import com.confighub.core.error.Error;
import com.confighub.core.utils.Pair;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.LevelCtx;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class UIKeyResolver
    extends AResolver
{
    private static final Logger log = LogManager.getLogger(UIKeyResolver.class);

    public UIKeyResolver(final Store store)
    {
        super(store);
    }


    protected Collection<Property> partialContextKeyResolver(final Context context, PropertyKey key)
        throws ConfigException
    {
        if (Utils.anyNull(context, key))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        return partialContext(key.getProperties(), context);
    }


    /**
     * UI Resolver for full or partial context, for a specific key.
     *
     * @param context
     * @param keys
     * @return
     * @throws ConfigException
     */
    protected Collection<Property> resolve(final Context context, String... keys)
            throws ConfigException
    {
        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        Collection<Property> properties = new ArrayList<>();
        for (String key : keys)
        {
            Pair<PropertyKey, Collection<Property>> pair = store.getPropertiesForKey(context.repository,
                                                                                     context.date,
                                                                                     key);
            if (null != pair)
                properties.addAll(pair.cdr);
        }

        if (context.isFullContext())
            return fullContext(properties, context);

        return partialContext(properties, context);
    }

    private Collection<Property> fullContext(Collection<Property> properties, Context context)
            throws ConfigException
    {
        Collection<Property> matchedProperties = new ArrayList<>();

        Property heaviest = null;
        for (Property property : properties)
        {
            if (isContextualMatchAudit(property.getDepthMap(), context))
            {
                if (!property.isActive())
                    matchedProperties.add(property);
                else if (null == heaviest || property.getContextWeight() > heaviest.getContextWeight())
                    heaviest = property;
            }
        }

        if (null != heaviest)
            matchedProperties.add(heaviest);

        return matchedProperties;
    }

    /**
     *
     * @param properties
     * @param context
     * @return
     * @throws ConfigException
     */
    protected static Collection<Property> partialContext(Collection<Property> properties, Context context)
        throws ConfigException
    {
        Collection<Property> matchedProperties = new ArrayList<>();
        Property heaviest = null;

        for (Property property : properties)
        {
            if (isContextualMatchAudit(property.getDepthMap(), context))
            {
                boolean propertyIsWildcardMatch = false;
                for (Depth depth : context.wildcards)
                {
                    Map<String, LevelCtx> propDepthMap = property.getDepthMap();
                    if (null != propDepthMap && propDepthMap.containsKey(String.valueOf(depth.getPlacement())))
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
                    if (null == heaviest || property.getContextWeight() > heaviest.getContextWeight())
                        heaviest = property;
                }
            }
        }

        if (null != heaviest)
            matchedProperties.add(heaviest);

        return matchedProperties;
    }
}
