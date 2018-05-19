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
            return null;
        }
    }

}
