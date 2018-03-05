/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.admin.repo;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/cleanup")
public class Cleanup
        extends AAdminAccessValidation
{
    @GET
    @Path("/{account}/{repository}/keys")
    @Produces("application/json")
    public Response getKeys(@PathParam("account") String account,
                            @PathParam("repository") String repositoryName,
                            @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            int status = validate(account, repositoryName, token, store, false);
            if (0 != status)
                return Response.status(status).build();

            Set<PropertyKey> keys = repository.getKeys();
            JsonArray keysJson = new JsonArray();
            if (null != keys)
                keys.stream().forEach(k -> {

                    if ((null == k.getProperties() || 0 == k.getProperties().size())
                        && (null == k.getFiles() || 0 == k.getFiles().size()))
                        keysJson.add(GsonHelper.keyToGSON(k));

                });

            json.add("keys", keysJson);
            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
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
