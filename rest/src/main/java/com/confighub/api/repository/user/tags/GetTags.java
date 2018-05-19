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

package com.confighub.api.repository.user.tags;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Tag;
import com.confighub.core.store.TagStore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/getTags")
public class GetTags
        extends AUserAccessValidation
{
    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@HeaderParam("Authorization") String token,
                        @PathParam("account") String account,
                        @PathParam("repository") String repositoryName)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        TagStore store = new TagStore();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status)
                return Response.status(status).build();

            JsonArray tagsArr = new JsonArray();
            Set<Tag> tags = repository.getTags();
            if (null != tags)
                tags.forEach(tag -> tagsArr.add(tag.toJson()));

            json.add("tags", tagsArr);
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
