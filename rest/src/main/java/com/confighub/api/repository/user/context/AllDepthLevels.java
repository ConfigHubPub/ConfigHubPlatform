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

            Level ci = null;
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
            Level.LevelType t = Level.LevelType.valueOf(type);
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

    protected static JsonArray getAssignments(Level ci,
                                              Depth depth,
                                              Level.LevelType t,
                                              boolean all,
                                              Store store,
                                              Repository repository,
                                              UserAccount user,
                                              Long id)
    {
        boolean isCluster = t.equals(Level.LevelType.Group);
        boolean isNode = t.equals(Level.LevelType.Member);

        Set<Level> assigned = null;
        if (null != ci)
            assigned = isCluster ? ci.getMembers() : ci.getGroups();

        JsonArray cis = new JsonArray();

        if (all)
        {
            Collection<Level> levels = store.getLevelsForDepth(repository, user, depth);
            if (null == levels)
                return null;

            for (Level l : levels)
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