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
