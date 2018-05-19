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

package com.confighub.core.store;

import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.repository.Repository;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.user.UserAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeyUtils
{
    private static final Logger log = LogManager.getLogger(KeyUtils.class);

    public static boolean isKeyEditable(final Repository repository,
                                        final PropertyKey key,
                                        final String keyString,
                                        final UserAccount user)
    {
        if (!repository.isAccessControlEnabled())
            return true;

        if (null == key)
        {
            AccessRuleWrapper accessRuleWrapper = repository.getRulesWrapper(user, AccessRule.RuleTarget.Key);
            if (null == accessRuleWrapper)
                return false;

            return accessRuleWrapper.executeRuleFor(keyString);
        }

        AccessRuleWrapper accessRuleWrapper = repository.getRulesWrapper(user);
        // User is not a team member
        if (null == accessRuleWrapper)
            return false;

        for (Property property : key.getProperties())
        {
            accessRuleWrapper.executeRuleFor(property);
            if (!property.isEditable)
                return false;
        }

        return true;
    }
}
