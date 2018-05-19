package com.confighub.api.system;

import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/addSystemAdmin")
public class AddSystemAdmin
        extends ASysAdminAccessValidation
{
    @POST
    @Produces("application/json")
    public Response create(@HeaderParam("Authorization") final String token,
                           @FormParam("un") final String username)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateCHAdmin(token, store);
            if (0 != status) return Response.status(status).build();

            final UserAccount sysAdmin = store.getUserByUsername(username);
            if (null == sysAdmin)
            {
                json.addProperty("success", false);
                json.addProperty("message", "Can not find specified user.");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            store.addSystemAdmin(this.user, sysAdmin);
            store.commit();

            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (final ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

    }


}
