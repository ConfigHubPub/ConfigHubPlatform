package com.confighub.api.system;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;

import javax.ws.rs.core.Response;

public abstract class ASysAdminAccessValidation
{
    protected UserAccount user;

    protected int validateCHAdmin(final String token, final Store store)
    {
        this.user = TokenState.getUser(token, store);

        if (null == this.user)
        {
            // Not authorized - 401
            return Response.Status.UNAUTHORIZED.getStatusCode();
        }

        if (!this.user.isConfigHubAdmin())
        {
            return Response.Status.FORBIDDEN.getStatusCode();
        }

        return 0;
    }


}
