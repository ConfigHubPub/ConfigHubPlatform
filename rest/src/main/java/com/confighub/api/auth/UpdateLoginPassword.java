/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.auth;

import com.confighub.api.server.AuthenticationNotRequired;
import com.confighub.core.auth.Auth;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/updateLoginPassword")
@Produces("application/json")
public class UpdateLoginPassword
{
    private static final Logger log = LogManager.getLogger(UpdateLoginPassword.class);

    @AuthenticationNotRequired
    @POST
    public Response updatePassword(@FormParam("password") String password,
                                   @FormParam("password2") String password2,
                                   @FormParam("t") String token)
    {
        Store store = new Store();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        if (Utils.anyBlank(password, password2) || !password.equals(password2))
        {
            json.addProperty("success", false);
            json.addProperty("message", "Passwords do not match.");

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }

        try
        {
            Map<String, Object> claims = Auth.verifyPasswordChangeToken(token);
            if (null == claims || !claims.containsKey("email"))
                throw new ConfigException(Error.Code.INVALID_PASS_CHANGE_TOKEN);

            String email = (String) claims.get("email");

            UserAccount user = store.geUserByEmail(email);
            store.begin();
            store.updateUserPassword(user, password);
            store.commit();

            json.addProperty("token", Auth.createUserToken(user));
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