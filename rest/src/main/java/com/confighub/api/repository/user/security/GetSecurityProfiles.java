/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.security;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.SecurityStore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/getSecurityProfiles")
public class GetSecurityProfiles
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetSecurityProfiles.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@HeaderParam("Authorization") String token,
                        @PathParam("account") String account,
                        @PathParam("repository") String repositoryName)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        SecurityStore store = new SecurityStore();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status)
                return Response.status(status).build();

            Collection<SecurityProfile> groups = store.getAllRepoEncryptionProfiles(user, repository);
            JsonArray groupJson = new JsonArray();

            for (SecurityProfile group : groups)
            {
                JsonObject g = new JsonObject();
                g.addProperty("name", group.getName());
                g.addProperty("cipher", null == group.getCipher() ? "" : group.getCipher().getName());
                g.addProperty("tokens", null == group.getTokens() ? 0 : group.getTokens().size());
                g.addProperty("files", null == group.getFiles() ? 0 : group.getFiles().size());
                g.addProperty("keys", null == group.getKeys() ? 0 : group.getKeys().size());
                groupJson.add(g);
            }

            json.add("groups", groupJson);
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
