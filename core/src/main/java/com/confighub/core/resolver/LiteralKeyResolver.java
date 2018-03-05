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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class LiteralKeyResolver
        extends AResolver
{
    private static final Logger log = LogManager.getLogger(LiteralKeyResolver.class);

    protected LiteralKeyResolver(Store store)
    {
        super(store);
    }

    protected Collection<Property> resolve(final Context context, String key, boolean allValues)
            throws ConfigException
    {
        if (Utils.anyNull(context))
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        PropertyKey propertyKey = store.getKey(context.repository, key, context.date);

        Collection<Property> properties = propertyKey.getProperties();
        Collection<Property> resolved = new HashSet<>();

        properties.stream().forEach(p -> {

            boolean match = !Collections.disjoint(p.getContext(), context.getContextItems());

            if (allValues)
            {
                p.type = match ? Context.PropertyType.match : Context.PropertyType.outOfContext;
                resolved.add(p);
            }
            else if (match)
            {
                resolved.add(p);
            }
        });

        return resolved;
    }

}