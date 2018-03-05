/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.context;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Level;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/getContextItem")
public class GetContextItem
        extends AUserAccessValidation
{
    @GET
    @Path("/{account}/{repository}/{depthLabel}/{contextItem}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @PathParam("depthLabel") String depthLabel,
                        @PathParam("contextItem") String contextItemName,
                        @HeaderParam("Authorization") String token)
    {

        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status)
                return Response.status(status).build();

            Depth depth = repository.getDepthFromLabel(depthLabel);
            if (null == depth)
                throw new ConfigException(Error.Code.MISSING_PARAMS);

            Level ci = store.getLevel(contextItemName, depth, repository, null);
            if (null == ci)
            {
                json.addProperty("message", "Specified context item cannot be found.  Please try again.");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            json.addProperty("success", true);
            json.addProperty("canManageContext", repository.canUserManageContext(user));
            json.add("ci", ciToJson(ci, repository.getLabel(ci.getDepth()), repository.isContextClustersEnabled()));

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

    public static JsonObject ciToJson(Level level, String label, boolean isContextClustersEnabled)
    {
        JsonObject jsonCi = new JsonObject();

        jsonCi.addProperty("contextClustersEnabled", isContextClustersEnabled);
        jsonCi.addProperty("id", level.getId());
        jsonCi.addProperty("count", null == level.getProperties() ? 0 : level.getProperties().size());
        jsonCi.addProperty("name", level.getName());
        jsonCi.addProperty("placement", level.getContextPlacement());
        jsonCi.addProperty("depthLabel", label);

        jsonCi.addProperty("type", level.getType().name());

        JsonArray assignments = new JsonArray();
        if (null != level.getGroups())
        {
            level.getGroups().forEach(p -> {
                JsonObject parent = new JsonObject();

                parent.addProperty("id", p.getId());
                parent.addProperty("name", p.getName());
                parent.addProperty("type", p.getType().name());
                parent.addProperty("state", 2);

                assignments.add(parent);
            });
        }

        if (null != level.getMembers())
        {
            level.getMembers().forEach(c -> {
                JsonObject child = new JsonObject();

                child.addProperty("id", c.getId());
                child.addProperty("name", c.getName());
                child.addProperty("type", c.getType().name());
                child.addProperty("state", 2);

                assignments.add(child);
            });
        }
        jsonCi.add("assignments", assignments);

        return jsonCi;
    }
}
