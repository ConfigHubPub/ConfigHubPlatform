/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.confighub.core.resolver;

import com.confighub.core.error.Error;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.utils.Pair;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Depth;
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
public class UIKeyCategoryResolver
        extends AResolver
{
    private static final Logger log = LogManager.getLogger(UIKeyCategoryResolver.class);

    public UIKeyCategoryResolver(final Store store)
    {
        super(store);
    }


    protected Collection<Property> resolve(final Context context, final String... keys)
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
            properties.addAll(pair.cdr);
        }

        if (context.isFullContext())
            fullContext(properties, context);
        else
            partialContext(properties, context);

        return properties;
    }


    private void fullContext(Collection<Property> properties, Context context)
            throws ConfigException
    {
        Property heaviest = null;
        for (Property property : properties)
        {
            if (isContextualMatchAudit(property.getDepthMap(), context))
            {
                property.type = Context.PropertyType.override;
                if (property.isActive() && (null == heaviest || property.getContextWeight() > heaviest
                        .getContextWeight()))
                    heaviest = property;
            } else
            {
                property.type = Context.PropertyType.outOfContext;
            }
        }

        if (null != heaviest)
            heaviest.type = Context.PropertyType.match;
    }

    private void partialContext(Collection<Property> properties, Context context)
            throws ConfigException
    {
        Property heaviest = null;

        for (Property property : properties)
        {
            Map<Depth, CtxLevel> propertyContextMap = property.getContextMap();

            if (isContextualMatchAudit(property.getDepthMap(), context))
            {
                boolean propertyIsWildcardMatch = false;
                for (Depth depth : context.wildcards)
                {
                    if (propertyContextMap.containsKey(depth))
                    {
                        propertyIsWildcardMatch = true;
                        property.type = Context.PropertyType.match;

                        break;
                    }
                }

                // Properties that are not matched based on wildcards in the context,
                // are treated as full-context resolution, and are therefore weight
                // compared.
                if (!propertyIsWildcardMatch)
                {
                    if (property.isActive() && (null == heaviest || property.getContextWeight() > heaviest
                            .getContextWeight()))
                        heaviest = property;

                    property.type = Context.PropertyType.override;
                }

            } else
            {
                property.type = Context.PropertyType.outOfContext;
            }
        }

        if (null != heaviest)
            heaviest.type = Context.PropertyType.match;
    }

}
