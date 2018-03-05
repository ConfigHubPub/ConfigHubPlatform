/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.owner;

import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/togglePrivacy")
public class TogglePrivacy
        extends AOwnerAccessValidation
{

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("isPrivate") boolean isPrivate)
    {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            store.begin();
            repository.setPrivate(isPrivate);
            store.update(repository, user);
            store.commit();

            json.addProperty("success", true);
            json.add("repository", GsonHelper.repositoryToJSON(repository));
        }
        catch (ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);
        }
        finally
        {
            store.close();
        }

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}
