/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.org;

import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/removeAdminOrOwner")
public class RemoveAdminOrOwner
        extends AOrgOwnerAccessValidation
{
    @POST
    @Path("/{orgAccName}")
    @Produces("application/json")
    public Response update(@PathParam("orgAccName") String orgAccName,
                           @FormParam("un") String userAccountName,
                           @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        if (Utils.anyBlank(orgAccName, userAccountName))
        {
            json.addProperty("success", false);
            json.addProperty("message", "Missing required field.");
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }

        try
        {
            UserAccount userAccount = store.getUserByUsername(userAccountName);
            if (null == userAccount)
            {
                json.addProperty("success", true);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            int status = validate(orgAccName, token, store);
            if (0 != status) return Response.status(status).build();

            if (!organization.isOwnerOrAdmin(userAccount))
            {
                json.addProperty("success", true);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            if (organization.isOwner(userAccount))
                store.removeOwner(organization, user);
            else
                store.removeAdministrator(organization, user);
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
