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

package com.confighub.api.repository.user.editor;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/securityProfileAssignments")
public class SecurityProfileAssignments
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(SecurityProfileAssignments.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @QueryParam("profile") String profile,
                        @QueryParam("all") boolean allKeys,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            SecurityProfile sp = store.getSecurityProfile(user, repository, null, profile);
            if (null == sp)
            {
                json.addProperty("message", "Unable to find specified profile");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            Collection<PropertyKey> keys = allKeys ? repository.getKeys() : sp.getKeys();
            JsonArray config = new JsonArray();
            AccessRuleWrapper rulesWrapper = repository.getRulesWrapper(user);
            keys.forEach(k -> config.add(GsonHelper.keyAndPropertiesToGSON(repository, rulesWrapper, k, null, null)));

            json.addProperty("success", true);
            json.add("config", config);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
            json.addProperty("success", false);
            json.addProperty("error", e.getMessage());

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

    }

}
