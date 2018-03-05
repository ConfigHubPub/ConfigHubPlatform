/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.admin.accessRules;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.Level;
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

@Path("/updateAccessRule")
public class UpdateAccessRule
        extends AAdminAccessValidation
{
    private static final Logger log = LogManager.getLogger(UpdateAccessRule.class);

    @POST
    @Path("/{account}/{repository}/{team}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @PathParam("team") String teamName,
                           @FormParam("id") long ruleId,
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

            AccessRule rule = store.getRule(team, ruleId);
            if (null == rule)
            {
                json.addProperty("message", "Cannot find specified rule.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            if (Utils.isBlank(type))
            {
                json.addProperty("message", "Rule type has to be specified.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            boolean canEdit = "rw".equals(access);

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

                    rule.setRuleTarget(AccessRule.RuleTarget.Key);
                    rule.setKeyMatchType(keyMatchType);
                    rule.setMatchValue(key);
                    rule.setCanEdit(canEdit);

                    break;

                case "Value":

                    Collection<Level> context = null;
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

                    rule.setRuleTarget(AccessRule.RuleTarget.Value);
                    rule.setContextMatchType(contextMatchType);
                    rule.setContext((Set<Level>)context);
                    rule.setCanEdit(canEdit);

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
