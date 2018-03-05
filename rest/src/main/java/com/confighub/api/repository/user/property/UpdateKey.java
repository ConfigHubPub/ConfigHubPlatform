/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.property;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Pair;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/updateKey")
public class UpdateKey
        extends AUserAccessValidation
{
    @POST
    @Path("/{account}/{repository}/{originalKey}")
    @Produces("application/json")
    public Response update(@PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @PathParam("originalKey") String originalKey,
                           @HeaderParam("Authorization") String token,
                           @FormParam("key") String key,
                           @FormParam("vdt") String vdt,
                           @FormParam("deprecated") boolean deprecated,
                           @FormParam("comment") String comment,
                           @FormParam("changeComment") String changeComment,
                           @FormParam("spName") String spName,
                           @FormParam("newSpPassword") String newSpPassword,
                           @FormParam("currentPassword") String currentPassword,
                           @FormParam("pushEnabled") boolean pushEnabled)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status)
                return Response.status(status).build();

            store.begin();
            Pair<PropertyKey, Store.KeyUpdateStatus> updateStatus = store.updatePropertyKey(repository,
                                                                                            originalKey,
                                                                                            key,
                                                                                            vdt,
                                                                                            comment,
                                                                                            deprecated,
                                                                                            pushEnabled,
                                                                                            user,
                                                                                            spName,
                                                                                            newSpPassword,
                                                                                            currentPassword,
                                                                                            changeComment);

            store.commit();
            json.addProperty("status", updateStatus.cdr.name());
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            switch (e.getErrorCode())
            {
                case PROP_DUPLICATION_CONTEXT:
                case PROP_PARENT_LOCK:
                case PROP_CHILD_LOCK:
                case PROP_CONTEXT_DUPLICATE_DEPTH:
                    json.addProperty("status", Store.KeyUpdateStatus.MERGE.name());
                    break;

                default:
                    json.addProperty("status", "ERROR");
                    break;
            }

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
