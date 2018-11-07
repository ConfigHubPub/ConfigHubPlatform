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
import com.confighub.core.error.Error;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Set;

@Path("/createAccessRule")
public class CreateAccessRule
    extends AAdminAccessValidation
{
    private static final Logger log = LogManager.getLogger(CreateAccessRule.class);

    @POST
    @Path("/{account}/{repository}/{team}")
    @Produces("application/json")
    public Response create(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @PathParam("team") String teamName,
                           @FormParam("type") String type,
                           @FormParam("match") String match,
                           @FormParam("key") String key,
                           @FormParam("context") String contextString,
                           @FormParam("access") String access)
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

            if (Utils.isBlank(type))
            {
                json.addProperty("message", "Rule type has to be specified.");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            // Always put new rule at the bottom of priority list
            int priority = team.getRuleCount() + 1;
            boolean canEdit = "rw".equals(access);

            AccessRule rule;

            switch (type)
            {
                case "Key":
                    if (Utils.anyBlank(match, key)) {
                        throw new ConfigException(Error.Code.MISSING_PARAMS);
                    }

                    AccessRule.KeyMatchType keyMatchType;
                    switch (match)
                    {
                        case "Is": keyMatchType = AccessRule.KeyMatchType.Is; break;
                        case "StartsWith": keyMatchType = AccessRule.KeyMatchType.StartsWith; break;
                        case "EndsWith": keyMatchType = AccessRule.KeyMatchType.EndsWith; break;
                        case "Contains": keyMatchType = AccessRule.KeyMatchType.Contains; break;
                        default:
                            throw new ConfigException(Error.Code.INVALID_TYPE);
                    }

                    rule = new AccessRule(team,
                                          AccessRule.RuleTarget.Key,
                                          keyMatchType,
                                          key,
                                          canEdit,
                                          priority);
                    team.addRule(rule);
                    break;

                case "Value":

                    Collection<CtxLevel> context = null;
                    try
                    {
                        context = ContextParser.parseAndCreate(contextString, repository, store, user, null);
                    } catch (NumberFormatException e)
                    {
                        json.addProperty("message", "Invalid context element specified.");
                        json.addProperty("success", false);

                        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                    }

                    AccessRule.ContextMatchType contextMatchType;
                    switch (match)
                    {
                        case "ContainsAny": contextMatchType = AccessRule.ContextMatchType.ContainsAny; break;
                        case "ContainsAll": contextMatchType = AccessRule.ContextMatchType.ContainsAll; break;
                        case "DoesNotContain": contextMatchType = AccessRule.ContextMatchType.DoesNotContain; break;
                        case "Resolves": contextMatchType = AccessRule.ContextMatchType.Resolves; break;
                        case "DoesNotResolve": contextMatchType = AccessRule.ContextMatchType.DoesNotResolve; break;
                        default:
                            throw new ConfigException(Error.Code.INVALID_TYPE);
                    }

                    rule = new AccessRule(team,
                                          AccessRule.RuleTarget.Value,
                                          contextMatchType,
                                          (Set<CtxLevel>)context,
                                          canEdit,
                                          priority);
                    team.addRule(rule);
                    break;

                default:
                    json.addProperty("message", "Invalid rule type specified.");
                    json.addProperty("success", false);
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            store.updateAccessRule(repository, user, rule);
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
