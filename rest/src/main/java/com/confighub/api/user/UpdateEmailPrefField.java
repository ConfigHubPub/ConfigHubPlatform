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
