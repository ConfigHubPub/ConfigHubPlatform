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

@Path("/validatePassChangeToken")
@Produces("application/json")
public class ValidatePassChangeToken
{
    private static final Logger log = LogManager.getLogger(ValidatePassChangeToken.class);

    @AuthenticationNotRequired
    @POST
    public Response updatePassword(@FormParam("t") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        try
        {
            Map<String, Object> claims = Auth.verifyPasswordChangeToken(token);
            if (null == claims || !claims.containsKey("email"))
                throw new ConfigException(Error.Code.INVALID_PASS_CHANGE_TOKEN);

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
    }
}