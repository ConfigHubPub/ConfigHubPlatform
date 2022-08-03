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
import com.confighub.core.store.AuditRecord;
import com.confighub.core.store.RevisionEntry;
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

@Path("/getPropertyAudit")
public class GetPropertyAudit
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetPropertyAudit.class);

    @POST
    @Path("/{account}/{repository}/{propertyId}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @PathParam("propertyId") long propertyId,
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

            List<AuditRecord> audit = store.getPropertyHistory(repository,
                                                             user,
                                                             max,
                                                             starting,
                                                             direction,
                                                             null,
                                                             propertyId,
                                                             attention,
                                                             commitGroup);

            json.addProperty("success", true);
            json.add("audit", GetRepositoryAudit.getAuditList(audit, gson, user, repository, store, false));
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
}
