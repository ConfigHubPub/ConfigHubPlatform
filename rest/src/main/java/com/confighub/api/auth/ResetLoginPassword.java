/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
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
