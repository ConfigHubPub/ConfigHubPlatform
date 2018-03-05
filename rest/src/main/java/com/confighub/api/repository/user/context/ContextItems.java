/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.context;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Level;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;

@Path("/contextItems")
public class ContextItems
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(ContextItems.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response getContextElements(@PathParam("account") String account,
                                       @PathParam("repository") String repositoryName,
                                       @HeaderParam("Authorization") String token)
    {
        JsonObject data = new JsonObject();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            JsonObject depthData = new JsonObject();

            Map<Depth, Collection<Level>> levels = store.getLevelsByDepth(repository);
            for (Depth depth : levels.keySet())
            {
                JsonArray jsonLevels = new JsonArray();
                for (Level level : levels.get(depth))
                {
                    JsonObject o = new JsonObject();
                    o.addProperty("id", level.getId());
                    o.addProperty("name", level.getName());
                    if (!Level.LevelType.Standalone.equals(level.getType()))
                        o.addProperty("type", level.getType().name());

                    jsonLevels.add(o);
                }

                JsonObject jsonDepth = new JsonObject();
                jsonDepth.addProperty("label", repository.getLabel(depth));
                jsonDepth.addProperty("depth", depth.name());
                jsonDepth.add("levels", jsonLevels);

                depthData.add(String.valueOf(depth.getPlacement()), jsonDepth);
            }

            data.add("depthData", depthData);
            data.addProperty("canManageContext", repository.canUserManageContext(user));

            JsonArray depthScores = new JsonArray();
            for (Depth d : repository.getDepth().getDepths())
                depthScores.add(d.getPlacement());

            data.add("depthScores", depthScores);
        }
        catch (ConfigException e)
        {
            data.addProperty("error", e.getMessage());
        }
        finally
        {
            store.close();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(data), MediaType.APPLICATION_JSON).build();
    }
}
