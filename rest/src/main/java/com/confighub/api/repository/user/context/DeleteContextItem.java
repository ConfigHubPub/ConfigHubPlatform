/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.context;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Level;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/deleteContextItem")
public class DeleteContextItem
        extends AUserAccessValidation
{
    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("id") Long contextItemId,
                           @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status)
                return Response.status(status).build();

            Level ci = store.getLevel(contextItemId, repository);
            if (null != ci)
            {
                if (null != ci.getProperties() && ci.getProperties().size() > 0)
                {
                    json.addProperty("message",
                                     "Context item cannot be deleted while there are properties assigned to it.");
                    json.addProperty("success", false);

                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }

                store.begin();
                store.deleteLevel(user, repository, ci);
                store.commit();
            }

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            store.rollback();

            switch (e.getErrorCode())
            {
                case CONSTRAINT:
                    json.addProperty("message", "This context item is currently in use and cannot be removed.");
                    break;

                default:
                    json.addProperty("message", e.getMessage());
                    break;
            }

            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

}
