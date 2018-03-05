/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.auth.Auth;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.store.UserStore;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Validator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/changeEmail")
public class ChangeEmail
{
    @POST
    @Produces("application/json")
    public Response get(@FormParam("email") String email,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        UserStore store = new UserStore();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            if (!Validator.validEmail(email))
                throw new ConfigException(Error.Code.USER_BAD_EMAIL);

            store.begin();
            store.changeEmail(user, email);
            store.commit();

            json.addProperty("success", true);
            json.addProperty("token", Auth.createUserToken(user));

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            store.rollback();

            if (Error.Code.CONSTRAINT.equals(e.getErrorCode()))
                json.addProperty("message", "Email is already used.");
            else
                json.addProperty("message", e.getMessage());

            json.addProperty("success", false);

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
