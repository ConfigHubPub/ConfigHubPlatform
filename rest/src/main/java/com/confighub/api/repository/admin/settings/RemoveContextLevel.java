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

package com.confighub.api.repository.admin.settings;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Depth;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/removeContextLevel")
public class RemoveContextLevel
        extends AAdminAccessValidation
{
    private static final Logger log = LogManager.getLogger(RemoveContextLevel.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("password") String password,
                           @FormParam("rank") int rank)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            if (!user.isPasswordValid(password))
            {
                json.addProperty("message", "Invalid authentication credentials specified.");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            Depth depth = Depth.getByPlacement(rank);

            store.begin();
            store.removeContextRank(repository, user, depth);
            store.commit();

            json.addProperty("success", true);
            json.add("repo", GsonHelper.repositoryToJSON(repository));
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
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
