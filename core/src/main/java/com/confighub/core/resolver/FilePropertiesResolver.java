/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.resolver;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static com.confighub.core.resolver.RepositoryPropertiesResolver.fullContext;
import static com.confighub.core.resolver.RepositoryPropertiesResolver.partialContext;

public class FilePropertiesResolver
        extends AResolver
{
    private static final Logger log = LogManager.getLogger(FilePropertiesResolver.class);
    private final Collection<PropertyKey> keys;
    private final boolean isClient;

    protected FilePropertiesResolver(Store store, Collection<PropertyKey> keys, final boolean isClient)
    {
        super(store);
        this.keys = keys;
        this.isClient = isClient;
    }

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

    private Map<PropertyKey, Collection<Property>> getPropertiesByKey(Context context)
            throws ConfigException
    {
        Map<PropertyKey, Collection<Property>> propertiesByKey = new HashMap<>();
        if (context.all)
        {
            for (PropertyKey k : context.repository.getKeys())
            {
                propertiesByKey.put(k, new ArrayList<>());
            }
        }

        if (null == this.keys)
            return propertiesByKey;

        for (PropertyKey key : this.keys)
        {
            int cnt = 0;

            List<Property> props = new ArrayList<>();
            for (Property property : key.getProperties())
            {
                boolean match = isContextualMatchAudit(property.getDepthMap(), context);
                cnt++;

                if (this.isClient && !property.isActive())
                    continue;

                if (match)
                    props.add(property);
            }

            key.propertyCount = cnt;
            propertiesByKey.put(key, props);
        }

        return propertiesByKey;
    }
}
