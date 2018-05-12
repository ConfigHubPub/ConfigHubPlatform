package com.confighub.api.system;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/makeMeAnAdmin")
public class MakeMeAnAdmin
{
    @POST
    public Response create(@HeaderParam("Authorization") final String token)
    {
        Store store = new Store();

        try
        {
            final UserAccount user = TokenState.getUser(token, store);
            store.begin();
            store.addSystemAdmin(user);
            store.commit();
        }
        catch (ConfigException e)
        {
            store.rollback();
        }
        finally
        {
            store.close();
        }

        return Response.ok().build();
    }

}
