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

package com.confighub.core.mail;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.user.UserAccount;

public class InvitationEmail
{
    public static void sendRepositoryInvitation(UserAccount from, String toEmail)
        throws ConfigException
    {
        if (null == from)
            throw new ConfigException(Error.Code.MISSING_PARAMS);

        // XXX
//        SendMail.sendMail(toEmail, "Come join my repository!", "Here's some info about it!");
    }

}
