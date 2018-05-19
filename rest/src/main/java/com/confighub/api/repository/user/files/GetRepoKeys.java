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

package com.confighub.api.repository.user.files;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/getRepoKeys")
public class GetRepoKeys
        extends AUserAccessValidation
{
    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response add(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Authorization") String token)
    {
        JsonArray json = new JsonArray();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store, false);
            if (0 != status)
                return Response.status(status).build();

            Set<PropertyKey> keys = repository.getKeys();
            if (null != keys)
                keys.stream().forEach(k -> json.add(k.getKey()));
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
        }
        finally
        {
            store.close();
        }

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}