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
import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Team;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/renameTeam")
public class RenameTeam
        extends AAdminAccessValidation
{
    @POST
    @Path("/{account}/{repository}/{team}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @PathParam("team") String team,
                           @FormParam("newName") String newName)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            if (Utils.isBlank(newName))
            {
                json.addProperty("success", false);
                json.addProperty("message", "New team name is required.");

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            Team aTeam = repository.getTeam(team);
            if (null == aTeam)
            {
                json.addProperty("success", false);
                json.addProperty("message", "Specified team cannot be found.");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            Team existing = repository.getTeam(newName);
            if (null != existing)
            {
                json.addProperty("success", false);
                json.addProperty("message", "Team " + newName + " already exists in this repository.  Teams need unique names.");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            aTeam.setName(newName);
            store.begin();
            store.update(aTeam, user);
            store.commit();

            json.addProperty("success", true);
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
