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

package com.confighub.api.repository.user.audit;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.*;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.AuditRecord;
import com.confighub.core.store.RevisionEntry;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/getRepositoryAudit")
public class GetRepositoryAudit
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetRepositoryAudit.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @FormParam("recordTypes") String recordTypes,
                        @FormParam("max") int max,
                        @FormParam("starting") long starting,
                        @FormParam("direction") int direction,
                        @FormParam("attention") boolean attention,
                        @FormParam("forUser") String forUser,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            List<RevisionEntry.CommitGroup> commitGroup = null;
            if (!Utils.isBlank(recordTypes))
            {
                commitGroup = new ArrayList<>();
                try
                {
                    for (String s : recordTypes.split(","))
                        commitGroup.add(RevisionEntry.CommitGroup.valueOf(s));
                } catch (Exception e)
                {
                    json.addProperty("success", true);
                    json.addProperty("message", "Invalid audit commit types specified: " + recordTypes);
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }
            }

            List<AuditRecord> audit = store.getCommitHistory(repository,
                                                             user,
                                                             max,
                                                             starting,
                                                             direction,
                                                             null,
                                                             attention,
                                                             commitGroup);

            json.addProperty("success", true);
            json.add("audit", getAuditList(audit, gson, user, repository, store, false));
            json.add("labels", GsonHelper.getRepositoryDepthLabels(repository));


            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            e.printStackTrace();

            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

    }

    public static JsonArray getAuditList(List<AuditRecord> audit, Gson gson, UserAccount user, Repository repository, Store store, boolean overrideSizeLimitFilter)
    {
        JsonArray commitsJson = new JsonArray();
        for (AuditRecord commit : audit)
        {
            JsonObject commitJson = new JsonObject();

            commitJson.addProperty("ts", commit.getRevEntry().getTimestamp());
            commitJson.addProperty("group",
                                   null == commit.getRevEntry().getCommitGroup()
                                           ? ""
                                           : commit.getRevEntry().getCommitGroup().name());

            if (null != commit.getAuthor())
                commitJson.addProperty("author", commit.getAuthor().getUsername());
            else
                commitJson.addProperty("appId", commit.getAppId());

            commitJson.addProperty("rev", commit.getRevEntry().getId());

            commitJson.addProperty("comment",
                                   null == commit.getRevEntry().getChangeComment()
                                           ? ""
                                           : commit.getRevEntry().getChangeComment());


            Map<String, String> types = commit.getRevEntry().getRevTypes(gson);
            List<APersisted> objList = commit.getAudited();

            if (!overrideSizeLimitFilter && objList.size() > 100)
            {
                commitJson.addProperty("overloaded", true);
                commitJson.addProperty("count", objList.size());
                commitJson.addProperty("limit", 50);
            }
            else
            {
                JsonArray jsonRecords = new JsonArray();

                for (APersisted obj : objList)
                {
                    String revType = types.get(String.valueOf(obj.getId()));
                    if (null == revType)
                        continue;

                    JsonObject jsonRecord = new JsonObject();

                    jsonRecord.addProperty("revType", revType);

                    if (obj instanceof Property)
                    {
                        Property property = (Property)obj;
                        jsonRecord.addProperty("type", "property");
                        jsonRecord.addProperty("pr", 10);

                        SecurityProfile sp = null;
                        if (property.isEncrypted())
                            sp = property.getPropertyKey().getSecurityProfile();
                        jsonRecord.add("entry",
                                       GsonHelper.propertyToGSON(repository, property, null, sp, null, true, null));
                    } else if (obj instanceof PropertyKey)
                    {
                        jsonRecord.addProperty("type", "propertyKey");
                        jsonRecord.addProperty("pr", 100);
                        jsonRecord.add("entry", GsonHelper.keyAuditToGSON((PropertyKey)obj));
                    } else if (obj instanceof CtxLevel )
                    {
                        jsonRecord.addProperty("type", "contextItem");
                        jsonRecord.addProperty("pr", 5);
                        jsonRecord.add("entry", GsonHelper.levelAuditToGSON((CtxLevel)obj));
                    } else if (obj instanceof SecurityProfile)
                    {
                        jsonRecord.addProperty("type", "securityProfile");
                        jsonRecord.addProperty("pr", 10);
                        jsonRecord.add("entry", GsonHelper.securityProfileAuditGSON((SecurityProfile)obj));
                    } else if (obj instanceof Repository)
                    {
                        jsonRecord.addProperty("type", "repository");
                        jsonRecord.addProperty("pr", 100);
                        jsonRecord.add("entry", GsonHelper.repositoryAuditToJSON((Repository)obj));
                    } else if (obj instanceof Token)
                    {
                        jsonRecord.addProperty("type", "token");
                        jsonRecord.addProperty("pr", 10);
                        jsonRecord.add("entry", GsonHelper.tokenAuditToJSON((Token)obj));
                    } else if (obj instanceof Team)
                    {
                        jsonRecord.addProperty("type", "team");
                        jsonRecord.addProperty("pr", 20);
                        jsonRecord.add("entry", GsonHelper.teamAuditToJSON((Team)obj));
                    } else if (obj instanceof AccessRule)
                    {
                        jsonRecord.addProperty("type", "accessRule");
                        jsonRecord.addProperty("pr", 10);
                        jsonRecord.add("entry", GsonHelper.accessRuleAuditToJSON((AccessRule)obj));
                    }else if (obj instanceof Tag)
                    {
                        jsonRecord.addProperty("type", "tag");
                        jsonRecord.addProperty("pr", 10);
                        jsonRecord.add("entry", GsonHelper.tagAuditToJSON((Tag)obj));
                    } else if (obj instanceof RepoFile)
                    {
                        jsonRecord.addProperty("type", "repoFile");
                        jsonRecord.addProperty("pr", 10);

                        RepoFile configFile = (RepoFile)obj;
                        SecurityProfile sp = null;

                        if (configFile.isEncrypted())
                            sp = configFile.getSecurityProfile();

                        jsonRecord.add("entry",
                                       GsonHelper.fileAuditToJSON(configFile,
                                                                  sp,
                                                                  store,
                                                                  user,
                                                                  repository,
                                                                  commit.getRevEntry().getTimestamp()));
                    } else if (obj instanceof AbsoluteFilePath)
                    {
                        jsonRecord.addProperty("type", "absFilePath");
                        jsonRecord.addProperty("pr", 20);
                        jsonRecord.add("entry", GsonHelper.absFileAuditToJSON((AbsoluteFilePath)obj));
                    }
                    else
                    {
                        jsonRecord.addProperty("type", obj.getClassName().name());
                    }

                    jsonRecords.add(jsonRecord);
                }
                commitJson.add("records", jsonRecords);
            }

            commitsJson.add(commitJson);
        }

        return commitsJson;
    }

}
