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
