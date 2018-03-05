/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.Response;

public abstract class AUserAccAccessValidation
{
    private static final Logger log = LogManager.getLogger(AUserAccAccessValidation.class);
    protected UserAccount user;

    protected int validate(String account,
                           String token,
                           Store store)
    {
        if (Utils.anyBlank(account))
        {
            // Bad request - 400
            return Response.Status.BAD_REQUEST.getStatusCode();
        }

        if (Utils.isBlank(token))
        {
            // Not authorized - 401
            return Response.Status.UNAUTHORIZED.getStatusCode();
        }

        try
        {

            this.user = TokenState.getUser(token, store);
            if (null == this.user)
            {
                // Not authorized - 401
                return Response.Status.UNAUTHORIZED.getStatusCode();
            }

        }
        catch (ConfigException e)
        {
            log.error(e.getMessage());
            e.printStackTrace();

            // Server error - 500
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }

        return 0;
    }
}