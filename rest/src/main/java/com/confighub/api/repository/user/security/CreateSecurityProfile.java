/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.security;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.security.CipherTransformation;
import com.confighub.core.store.SecurityStore;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/createSecurityProfile")
public class CreateSecurityProfile
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(CreateSecurityProfile.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response create(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("name") String name,
                           @FormParam("password") String password,
                           @FormParam("password2") String password2,
                           @FormParam("cipher") String cipher)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        SecurityStore store = new SecurityStore();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            if (!Utils.same(password, password2))
            {
                json.addProperty("message", "Passwords do not match.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            CipherTransformation ct = null;
            if (!Utils.isBlank(cipher))
                ct = CipherTransformation.get(cipher);

            store.begin();
            store.createEncryptionProfile(user, repository, name, password, ct);
            store.commit();

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
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
