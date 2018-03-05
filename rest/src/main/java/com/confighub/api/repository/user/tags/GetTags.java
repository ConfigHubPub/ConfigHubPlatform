/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
