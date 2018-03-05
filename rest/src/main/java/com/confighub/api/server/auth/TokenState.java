/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.server.auth;

import com.confighub.core.auth.Auth;
import com.confighub.core.store.AStore;
import com.confighub.core.user.UserAccount;

import java.util.Map;

public final class TokenState
{
    private TokenState() {}

    public static UserAccount getUser(final String token, final AStore store)
    {
        try
        {
            if (null == token)
                return null;

            Map<String, Object> attributes = Auth.validateUser(token);
            Long userId = Long.valueOf((int)attributes.get("userId"));
            UserAccount user = store.getUser(userId);

            if (null == user || !user.isActive())
                return null;

            return user;
        }
        catch (Exception ignore) {
            ignore.printStackTrace();
            return null;
        }
    }

}
