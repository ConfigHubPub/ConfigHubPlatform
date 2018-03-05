/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.UserStore;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/changePassword")
public class ChangePassword
{
//    @EJB
//    private UserStore store;

    @POST
    @Produces("application/json")
    public Response get(@FormParam("password") String oldPassword,
                        @FormParam("newPassword1") String newPassword1,
                        @FormParam("newPassword2") String newPassword2,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        UserStore store = new UserStore();

        try
        {
            if (Utils.anyBlank(token, oldPassword, newPassword1, newPassword2))
            {
                json.addProperty("message", "Missing required fields.");
                json.addProperty("success", false);

                Gson gson = new Gson();
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            if (!newPassword1.equals(newPassword2))
            {
                json.addProperty("message", "New passwords are not the same.");
                json.addProperty("success", false);

                Gson gson = new Gson();
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            store.changePassword(user, oldPassword, newPassword1);
            store.commit();

            json.addProperty("success", true);

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            store.rollback();

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
