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

package com.confighub.api.repository.owner;

import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/togglePrivacy")
public class TogglePrivacy
        extends AOwnerAccessValidation
{

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("isPrivate") boolean isPrivate)
    {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            store.begin();
            repository.setPrivate(isPrivate);
            store.update(repository, user);
            store.commit();

            json.addProperty("success", true);
            json.add("repository", GsonHelper.repositoryToJSON(repository));
        }
        catch (ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);
        }
        finally
        {
            store.close();
        }

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}
