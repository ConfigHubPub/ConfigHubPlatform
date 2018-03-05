/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.security;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.security.CipherTransformation;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/updateSecurityProfile")
public class UpdateSecurityProfile
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(UpdateSecurityProfile.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("profile") String profile,
                           @FormParam("newName") String newName,
                           @FormParam("oldPass") String oldPass,
                           @FormParam("cipher") String cipher)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        int status = validateWrite(account, repositoryName, token, store, true);
        if (0 != status) return Response.status(status).build();

        try {
            CipherTransformation.get(cipher);
        } catch (ConfigException e) {
            cipher = null;
        }

        try
        {
            store.begin();
            store.updateSecureProfile(user, repository, profile, newName, oldPass, cipher);
            store.commit();

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            if (e.getErrorCode().equals(Error.Code.CONSTRAINT))
                json.addProperty("message", "Name is already used");
            else
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