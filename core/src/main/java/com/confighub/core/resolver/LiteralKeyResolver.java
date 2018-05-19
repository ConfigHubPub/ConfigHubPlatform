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