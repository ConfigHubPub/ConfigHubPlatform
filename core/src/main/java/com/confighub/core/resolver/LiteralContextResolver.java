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
import com.confighub.core.repository.CtxLevel;
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
            for ( CtxLevel ci : context.getContextItems())
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
            for ( CtxLevel ci : context.getContextItems())
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
