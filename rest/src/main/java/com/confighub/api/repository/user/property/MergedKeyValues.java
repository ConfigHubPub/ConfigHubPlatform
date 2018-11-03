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

package com.confighub.api.repository.user.property;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.repository.user.editor.KeyProperties;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.resolver.Context;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.DateTimeUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;

@Path("/mergedKeyValues")
public class MergedKeyValues
        extends AUserAccessValidation
{

    @GET
    @Path("/{account}/{repository}/{keys}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @PathParam("keys") String keyStrings,
                        @QueryParam("context") String contextString,
                        @QueryParam("includeSiblings") boolean includeSiblings,
                        @QueryParam("ts") Long ts,
                        @HeaderParam("Authorization") String token)
    {
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            Date dateObj = DateTimeUtils.dateFromTs(ts, repository.getCreateDate());
            String[] keys = keyStrings.split(",");
            JsonObject json = new JsonObject();

            for (String keyString : keys)
            {
                PropertyKey key = store.getKey(repository, keyString, dateObj);
                if (null == key)
                {
                    json.addProperty("success", true);
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }
            }

            Collection<CtxLevel> ctx = ContextParser.parseAndCreate( contextString, repository, store, user, dateObj);
            Context context = new Context(store, repository, ctx, dateObj);
            SecurityProfile ep = null;

            if (includeSiblings)
                json.add("properties", KeyProperties.getContextCategorizedProperties(user,
                                                                                     repository,
                                                                                     context,
                                                                                     ep,
                                                                                     keys));
            else
                json.add("properties", KeyProperties.getContextRelevantProperties(user,
                                                                                  repository,
                                                                                  context,
                                                                                  ep,
                                                                                  keys));

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();


        }
        catch (ConfigException e)
        {
            JsonObject json = new JsonObject();

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
