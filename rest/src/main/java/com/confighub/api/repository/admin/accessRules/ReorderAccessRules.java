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

package com.confighub.api.repository.admin.accessRules;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Team;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Path("/reorderAccessRules")
public class ReorderAccessRules
        extends AAdminAccessValidation
{
    @POST
    @Path("/{account}/{repository}/{team}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @PathParam("team") String teamName,
                           @FormParam("order") String order)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            Team team = repository.getTeam(teamName);
            if (null == team)
            {
                json.addProperty("message", "Unable to find specified team.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            if (Utils.isBlank(order))
            {
                json.addProperty("message", "New order has to be specified.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();

            try
            {
                String[] ids = order.split(",");
                List<AccessRule> rules = team.getAccessRules();
                Set<AccessRule> reordered = new HashSet<>();

                int priority = 1;
                for (String _id : ids)
                {
                    Long id = Long.valueOf(_id.trim());
                    Iterator<AccessRule> itt = rules.iterator();
                    while (itt.hasNext())
                    {
                        AccessRule ar = itt.next();
                        if (ar.getId().equals(id))
                        {
                            ar.setPriority(priority);
                            reordered.add(ar);
                            itt.remove();
                            priority++;
                            store.updateAccessRule(repository, user, ar);
                            break;
                        }
                    }
                }

                if (rules.size() > 0)
                {
                    for (AccessRule ar : rules)
                    {
                        reordered.add(ar);
                        store.updateAccessRule(repository, user, ar);
                        priority++;
                    }
                }

            }
            catch (Exception e) {
                json.addProperty("message", "Failed to process new order.  If this problem persists, please context support.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

//            store.updateTeam(repository, user, team);
            store.commit();

            json.addProperty("success", true);
            json.add("accessRules", GsonHelper.ruleToJson(team));

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