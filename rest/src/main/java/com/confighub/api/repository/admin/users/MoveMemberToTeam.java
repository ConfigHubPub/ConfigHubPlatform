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

package com.confighub.api.repository.admin.users;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/moveMemberToTeam")
public class MoveMemberToTeam
        extends AAdminAccessValidation
{
    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @HeaderParam("Authorization") String token,
                           @FormParam("all") boolean all,
                           @FormParam("un") String username,
                           @FormParam("toTeam") String toTeam,
                           @FormParam("fromTeam") String fromTeam)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            UserAccount collaborator = store.getUserByUsername(username);
            if (null == collaborator)
            {
                json.addProperty("success", false);
                json.addProperty("message", "Can not find specified user.");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            store.moveMemberToAnotherTeam(repository, user, collaborator, toTeam);
            store.commit();

            json.addProperty("success", true);
            if (all)
                json.add("members", GsonHelper.allMembers(repository));
            else
                json.add("members", GsonHelper.teamMembers(repository.getTeam(fromTeam)));

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
