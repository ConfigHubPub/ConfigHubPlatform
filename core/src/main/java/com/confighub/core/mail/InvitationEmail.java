/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
