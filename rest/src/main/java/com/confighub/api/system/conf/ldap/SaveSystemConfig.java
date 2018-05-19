package com.confighub.api.system.conf.ldap;

import com.confighub.api.system.ASysAdminAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/addSystemAdmin")
public class SaveSystemConfig
        extends ASysAdminAccessValidation
{
    @POST
    @Produces("application/json")
    public Response create(@HeaderParam("Authorization") final String token,
                           @FormParam("group") String group,
                           MultivaluedMap<String, String> formParams)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateCHAdmin(token, store);
            if (0 != status)
                return Response.status(status).build();

            // ToDo: implement
            return null;
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