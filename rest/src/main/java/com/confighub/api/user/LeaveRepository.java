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
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.UserStore;
import com.confighub.core.store.diff.ADiffTracker;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/leaveRepository")
public class LeaveRepository
{
    @POST
    @Produces("application/json")
    public Response get(@FormParam("account") String account,
                        @FormParam("repositoryName") String repositoryName,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        UserStore store = new UserStore();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            ADiffTracker.track();
            store.begin();
            boolean removed = store.leaveRepository(user, account, repositoryName);
            store.commit();

            json.addProperty("success", true);
            json.addProperty("removed", removed);

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
