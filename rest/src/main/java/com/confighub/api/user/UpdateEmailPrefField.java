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
import com.confighub.core.store.UserStore;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/updateEmailPrefField")
public class UpdateEmailPrefField
{
    @POST
    @Produces("application/json")
    public Response get(@FormParam("field") String field,
                        @FormParam("val") boolean val,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        UserStore store = new UserStore();
        Gson gson = new Gson();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            if (!"repo".equals(field) && !"blog".equals(field))
            {
                json.addProperty("message", "Invalid selection for email preference specified.");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            if ("repo".equals(field))
                store.emailRepoCritical(user, val);
            else if ("blog".equals(field))
                store.emailBlog(user, val);
            store.commit();

            json.addProperty("success", true);
            json.addProperty("token", Auth.createUserToken(user));

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
