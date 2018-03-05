/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.resolver;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Level;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class LiteralContextResolver
        extends AResolver
{
    private static final Logger log = LogManager.getLogger(LiteralContextResolver.class);

    protected LiteralContextResolver(Store store)
    {
        super(store);
    }

    protected Map<PropertyKey, Collection<Property>> resolve(final Context context)
            throws ConfigException
    {
        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        Map<PropertyKey, Collection<Property>> propertiesByKey = new HashMap<>();

        Collection<Property> properties = new HashSet<>();
        if (context.all)
        {
            properties = context.repository.getProperties();

            Set<Long> pids = new HashSet<>();
            for (Level ci : context.getContextItems())
            {
                if (null != ci.getProperties())
                    ci.getProperties().forEach(p -> pids.add(p.getId()));
            }

            properties.stream().forEach(property -> {
                boolean inMap = propertiesByKey.containsKey(property.getPropertyKey());
                boolean match = pids.contains(property.getId());

                PropertyKey key = property.getPropertyKey();

                if (!inMap && (context.all || match))
                    propertiesByKey.put(key, new ArrayList<>());

                if (match)
                    propertiesByKey.get(key).add(property);
            });
        }
        else
        {
            for (Level ci : context.getContextItems())
                properties.addAll(ci.getProperties());

            properties.stream().forEach(p -> {
                PropertyKey key = p.getPropertyKey();
                Collection<Property> props = propertiesByKey.get(key);
                if (null == props)
                {
                    props = new ArrayList<>();
                    propertiesByKey.put(p.getPropertyKey(), props);
                }
                props.add(p);
            });
        }


        return propertiesByKey;
    }
}
