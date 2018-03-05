/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.org;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/deleteOrganization")
public class DeleteOrganization
        extends AOrgOwnerAccessValidation
{
    @POST
    @Path("/{accountName}")
    @Produces("application/json")
    public Response update(@PathParam("accountName") String accountName,
                           @FormParam("password") String password,
                           @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(accountName, token, store);
            if (0 != status) return Response.status(status).build();

            if (!user.isPasswordValid(password))
                throw new ConfigException(Error.Code.INVALID_PASSWORD);

            store.begin();
            store.deleteOrganization(organization, user);
            store.commit();

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

}
