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

package com.confighub.api.repository.admin.repo;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/deleteUnusedKeys")
public class DeleteUnusedKeys
        extends AAdminAccessValidation
{
    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response getKeys(@PathParam("account") String account,
                            @PathParam("repository") String repositoryName,
                            @FormParam("keys") String keys,
                            @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            int status = validate(account, repositoryName, token, store, false);
            if (0 != status)
                return Response.status(status).build();

            store.begin();
            if (!Utils.isBlank(keys))
            {
                List<String> keyList = Utils.split(keys, ",");
                store.deleteUnusedKeys(user, repository, keyList);
            }
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
