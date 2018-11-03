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
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.CtxLevel;
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

            CtxLevel ci = store.getLevel( contextItemName, depth, repository, null);
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

    public static JsonObject ciToJson( CtxLevel ctxLevel, String label, boolean isContextClustersEnabled)
    {
        JsonObject jsonCi = new JsonObject();

        jsonCi.addProperty("contextClustersEnabled", isContextClustersEnabled);
        jsonCi.addProperty( "id", ctxLevel.getId());
        jsonCi.addProperty("count", null == ctxLevel.getProperties() ? 0 : ctxLevel.getProperties().size());
        jsonCi.addProperty( "name", ctxLevel.getName());
        jsonCi.addProperty( "placement", ctxLevel.getContextPlacement());
        jsonCi.addProperty("depthLabel", label);

        jsonCi.addProperty( "type", ctxLevel.getType().name());

        JsonArray assignments = new JsonArray();
        if ( null != ctxLevel.getGroups())
        {
            ctxLevel.getGroups().forEach( p -> {
                JsonObject parent = new JsonObject();

                parent.addProperty("id", p.getId());
                parent.addProperty("name", p.getName());
                parent.addProperty("type", p.getType().name());
                parent.addProperty("state", 2);

                assignments.add(parent);
            });
        }

        if ( null != ctxLevel.getMembers())
        {
            ctxLevel.getMembers().forEach( c -> {
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
