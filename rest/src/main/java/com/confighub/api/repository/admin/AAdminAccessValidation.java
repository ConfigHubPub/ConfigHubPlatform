package com.confighub.api.repository.admin;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.store.diff.ADiffTracker;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;

@Slf4j
public abstract class AAdminAccessValidation
{
    protected UserAccount user;
    protected Repository repository;

    protected int validate(final String account,
                           final String repositoryName,
                           final String token,
                           final Store store)
    {
        return validate(account, repositoryName, token, store, false);
    }

    protected int validateWrite(final String account,
                                final String repositoryName,
                                final String token,
                                final Store store,
                                final boolean enableTracking)
    {
        if (enableTracking) ADiffTracker.track();

        int status = validate(account, repositoryName, token, store, enableTracking);

        if (0 != status)
            return status;

        if (null == this.user)
        {
            // Not authorized - 401
            return Response.Status.UNAUTHORIZED.getStatusCode();
        }

        if (!this.repository.hasWriteAccess(this.user) || !this.repository.isAdminOrOwner(this.user))
        {
            // Forbidden - 403
            return Response.Status.FORBIDDEN.getStatusCode();
        }

        return 0;
    }

    protected int validate(final String account,
                           final String repositoryName,
                           final String token,
                           final Store store,
                           final boolean enableTracking)
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

            if (!repository.isDemo())
            {

                if (null == this.user)
                {
                    // Not authorized - 401
                    return Response.Status.UNAUTHORIZED.getStatusCode();
                }

                if (!this.repository.isAdminOrOwner(this.user))
                {
                    // Forbidden - 403
                    return Response.Status.FORBIDDEN.getStatusCode();
                }
            }
        }
        catch (final ConfigException e)
        {
            log.error(e.getMessage());
            e.printStackTrace();

            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }

        return 0;
    }
}