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
import com.confighub.core.mail.SendMail;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
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
import java.util.HashMap;

@Path("/resetLoginPassword")
@Produces("application/json")
public class ResetLoginPassword
{
    private static final int tokenTTL = 60; // one hour
    private static final Logger log = LogManager.getLogger(ResetLoginPassword.class);

    @AuthenticationNotRequired
    @POST
    public Response requestPassReset(@FormParam("email") String email)
    {
        log.info("Password reset requested for: " + email);

        Store store = new Store();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        try
        {
            UserAccount user = store.geUserByEmail(email);

            HashMap<String, Object> claims = new HashMap<>();
            claims.put("email", user.getEmail());

            SendMail.sendPasswordReset(user.getEmail(), Auth.getPassResetToken(claims));
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
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
