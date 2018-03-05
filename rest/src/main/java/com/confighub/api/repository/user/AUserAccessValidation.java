/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.store.diff.ADiffTracker;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.Response;

public abstract class AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(AUserAccessValidation.class);
    protected UserAccount user;
    protected Repository repository;

    protected int validate(String account,
                           String repositoryName,
                           String token,
                           Store store)
    {
        return validate(account, repositoryName, token, store, false);
    }

    protected int validateWrite(String account,
                                String repositoryName,
                                String token,
                                Store store,
                                boolean enableTracking)
    {
        int status = validate(account, repositoryName, token, store, enableTracking);
        if (0 != status)
            return status;

        if (null == this.user)
        {
            // Not authorized - 401
            return Response.Status.UNAUTHORIZED.getStatusCode();
        }

        if (!this.repository.hasWriteAccess(this.user))
        {
            // Forbidden - 403
            return Response.Status.FORBIDDEN.getStatusCode();
        }

        return 0;
    }

    protected int validate(String account,
                           String repositoryName,
                           String token,
                           Store store,
                           boolean enableTracking)
    {
        if (Utils.anyBlank(account, repositoryName))
        {
            // Bad request - 400
            return Response.Status.BAD_REQUEST.getStatusCode();
        }

        try
        {
            this.repository = store.getRepository(account, repositoryName);
            if (null == this.repository)
            {
                return Response.Status.NOT_FOUND.getStatusCode();
            }

            this.user = TokenState.getUser(token, store);
            if (!this.repository.hasReadAccess(this.user))
            {
                // Forbidden - 403
                return Response.Status.FORBIDDEN.getStatusCode();
            }

            // ToDo - should this be in write
            if (enableTracking) ADiffTracker.track();

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