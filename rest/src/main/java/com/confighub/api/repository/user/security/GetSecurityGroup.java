/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.security;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/getSecurityGroup")
public class GetSecurityGroup
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetSecurityGroup.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String userToken,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("profile") String profile,
                           @FormParam("all") boolean allKeys)
    {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().serializeNulls().create();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, userToken, store);
            if (0 != status) return Response.status(status).build();

            SecurityProfile ep = store.getSecurityProfile(user, repository, null, profile);
            if (null == ep)
            {
                json.addProperty("message", "Unable to find specified profile");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            json.add("profile", GsonHelper.toJson(ep, profile, null));
            json.addProperty("privAcc", repository.isOwner(user));

            JsonArray files = new JsonArray();
            if (null != ep.getFiles())
            {
                for (RepoFile file : ep.getFiles())
                {

                    JsonObject fileJson = file.toJson();
                    fileJson.addProperty("fullPath", file.getAbsPath());
                    files.add(fileJson);
                }
            }

            json.add("files", files);

            JsonArray tokens = new JsonArray();
            if (null != ep.getTokens())
                for (Token token : ep.getTokens())
                    tokens.add(GsonHelper.tokenToJson(token, false));
            json.add("tokens", tokens);

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
