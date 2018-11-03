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

package com.confighub.api.repository.user.context;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.CtxLevel;
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

            Map<Depth, Collection<CtxLevel>> levels = store.getLevelsByDepth( repository);
            for (Depth depth : levels.keySet())
            {
                JsonArray jsonLevels = new JsonArray();
                for ( CtxLevel ctxLevel : levels.get( depth))
                {
                    JsonObject o = new JsonObject();
                    o.addProperty( "id", ctxLevel.getId());
                    o.addProperty( "name", ctxLevel.getName());
                    if (!CtxLevel.LevelType.Standalone.equals( ctxLevel.getType()))
                        o.addProperty( "type", ctxLevel.getType().name());

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
