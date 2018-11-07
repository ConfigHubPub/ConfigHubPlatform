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
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Set;

@Path("/allDepthLevels")
public class AllDepthLevels
        extends AUserAccessValidation
{
    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @FormParam("id") Long id,
                        @FormParam("type") String type,
                        @FormParam("depthLabel") String depthLabel,
                        @FormParam("all") boolean all,
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

            CtxLevel ci = null;
            if (null != id)
            {
                ci = store.getLevel(id, repository);
                if (null == ci)
                {
                    json.addProperty("message", "Specified context item cannot be found.");
                    json.addProperty("success", false);

                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }
            }

            Depth depth = repository.getDepthFromLabel(depthLabel);
            CtxLevel.LevelType t = CtxLevel.LevelType.valueOf( type);
            JsonArray cis = getAssignments(ci, depth, t, all, store, repository, user, id);
            if (null == cis)
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

            json.add("levels", cis);
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException | IllegalArgumentException e)
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

    protected static JsonArray getAssignments( CtxLevel ci,
                                               Depth depth,
                                               CtxLevel.LevelType t,
                                               boolean all,
                                               Store store,
                                               Repository repository,
                                               UserAccount user,
                                               Long id)
    {
        boolean isCluster = t.equals( CtxLevel.LevelType.Group);
        boolean isNode = t.equals( CtxLevel.LevelType.Member);

        Set<CtxLevel> assigned = null;
        if (null != ci)
            assigned = isCluster ? ci.getMembers() : ci.getGroups();

        JsonArray cis = new JsonArray();

        if (all)
        {
            Collection<CtxLevel> ctxLevels = store.getLevelsForDepth( repository, user, depth);
            if ( null == ctxLevels )
                return null;

            for ( CtxLevel l : ctxLevels )
            {
                if (!l.getId().equals(id))
                {
                    // 0 - not assignable
                    // 1 - assignable
                    // 2 - assigned
                    int state = 0;

                    if (null != assigned && assigned.contains(l))
                        state = 2;

                    else if (isCluster)
                    {
                        if (l.isStandalone() || l.isMember())
                            state = 1;
                    } else if (isNode)
                    {
                        if (l.isGroup())
                            state = 1;
                    }

                    if (0 != state)
                    {
                        JsonObject o = new JsonObject();

                        o.addProperty("name", l.getName());
                        o.addProperty("id", l.getId());
                        o.addProperty("type", l.getType().name());
                        o.addProperty("state", state);

                        cis.add(o);
                    }
                }
            }
        }
        else
        {
            if (null != assigned)
            {
                assigned.forEach(l -> {
                    JsonObject o = new JsonObject();

                    o.addProperty("name", l.getName());
                    o.addProperty("id", l.getId());
                    o.addProperty("type", l.getType().name());
                    o.addProperty("state", 2);

                    cis.add(o);
                });
            }
        }

        return cis;
    }
}