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
