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
import com.confighub.core.repository.Tag;
import com.confighub.core.store.Store;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Path("/contextHistory")
public class ContextItemsHistory
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(ContextItemsHistory.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response getContextElements(@PathParam("account") String account,
                                       @PathParam("repository") String repositoryName,
                                       @QueryParam("ts") Long ts,
                                       @QueryParam("tag") String tag,
                                       @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            JsonObject depthData = new JsonObject();
            Map<Depth, Collection<CtxLevel>> levels;

            JsonArray tags = new JsonArray();
            List<Tag> tagList = store.getTags(repository);
            if (null != tagList)
                tagList.forEach(t -> tags.add(t.toJson()));

            json.add("tags", tags);

            if (!Utils.isBlank(tag))
            {
                Tag t = store.getTag(repository.getId(), tag);
                if (null != t)
                    ts = t.getTs();
            }

            if (null == ts)
                levels = store.getLevelsByDepth(repository);
            else
            {
                Date dateObj = DateTimeUtils.dateFromTs(ts, repository.getCreateDate());
                repository = store.getRepository(repository.getId(), dateObj);
                if (null == repository)
                {
                    Gson gson = new Gson();
                    json.addProperty("error", null == dateObj
                                                ? "Cannot find specified repository."
                                                : "Repository not found at specified time.");
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }
                levels = store.getLevelsByDepth(repository, dateObj);
            }

            for (Depth depth : levels.keySet())
            {
                JsonArray depthLevelsArr = new JsonArray();

                for ( CtxLevel ctxLevel : levels.get( depth))
                {
                    JsonObject o = new JsonObject();
                    o.addProperty( "name", ctxLevel.getName());
                    if (!CtxLevel.LevelType.Standalone.equals( ctxLevel.getType()))
                        o.addProperty( "type", ctxLevel.getType().name());

                    depthLevelsArr.add(o);
                }

                JsonObject jsonDepth = new JsonObject();
                jsonDepth.addProperty("label", repository.getLabel(depth));
                jsonDepth.addProperty("depth", depth.name());
                jsonDepth.add("levels", depthLevelsArr);

                depthData.add(String.valueOf(depth.getPlacement()), jsonDepth);
            }

            json.add("depthData", depthData);

            JsonArray depthScores = new JsonArray();
            for (Depth d : repository.getDepth().getDepths())
                depthScores.add(d.getPlacement());

            json.add("depthScores", depthScores);

        }
        catch (ConfigException e)
        {
            json.addProperty("error", e.getErrorCode().getMessage());
        }
        finally
        {
            store.close();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}
