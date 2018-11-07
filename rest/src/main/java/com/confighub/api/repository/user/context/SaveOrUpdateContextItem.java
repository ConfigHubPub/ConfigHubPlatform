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
import com.confighub.core.error.Error;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/saveOrUpdateContextItem")
public class SaveOrUpdateContextItem
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(SaveOrUpdateContextItem.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @FormParam("id") Long id,
                        @FormParam("name") String name,
                        @FormParam("type") String type,
                        @FormParam("assignments") String assignments,
                        @FormParam("depthLabel") String depthLabel,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status)
                return Response.status(status).build();

            if (!"Standalone".equals(type) && !repository.isContextClustersEnabled())
            {
                json.addProperty("message", Error.Code.CLUSTERING_DISABLED.getMessage());
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            List<Long> assignedIds = null;
            if (!Utils.isBlank(assignments))
            {
                try
                {
                    assignedIds = new ArrayList<>();
                    String[] ids = assignments.split(",");
                    for (String aId : ids)
                        assignedIds.add(Long.valueOf(aId));
                } catch (Exception e)
                {
                    json.addProperty("message", "Invalid assignments specified.");
                    json.addProperty("success", false);

                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }
            }

            store.begin();
            CtxLevel ci = store.updateOrCreateLevel( repository,
                                                     user,
                                                     id,
                                                     name,
                                                     CtxLevel.LevelType.valueOf( type),
                                                     assignedIds,
                                                     depthLabel);
            store.commit();

            json.addProperty("success", true);
            json.add("ci",
                     GetContextItem.ciToJson(ci,
                                             repository.getLabel(ci.getDepth()),
                                             repository.isContextClustersEnabled()));

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getMessage());
            if (null != e.getJson())
                json.add("obj", e.getJson());

            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
