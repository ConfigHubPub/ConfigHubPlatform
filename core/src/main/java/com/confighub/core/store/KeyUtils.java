/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
