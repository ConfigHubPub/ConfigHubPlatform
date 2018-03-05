/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.auth;

import com.confighub.api.server.AuthenticationNotRequired;
import com.confighub.api.server.auth.TokenState;
import com.confighub.core.auth.Auth;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("refreshToken")
@Produces("application/json")
public class RefreshToken
{
    @AuthenticationNotRequired
    @GET
    public Response get(@HeaderParam("Authorization") String oldToken)
    {
        if (null == oldToken)
            return Response.ok().build();

        Store store = new Store();
        try
        {
            JsonObject json = new JsonObject();
            Gson gson = new Gson();

            json.addProperty("token", Auth.createUserToken(TokenState.getUser(oldToken, store)));
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        } catch (Exception ignore)
        {
            return Response.ok().build();
        }
        finally
        {
            store.close();
        }
    }
}
