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

package com.confighub.api.repository.user.files;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.resolver.Context;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/getFileKeys")
public class GetFileKeys
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetFileKeys.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response add(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @FormParam("context") String contextString,
                        @FormParam("keys") String keysString,
                        @FormParam("ts") Long ts,
                        @FormParam("tag") String tagLabel,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        JsonArray config = new JsonArray();
        Gson gson = new Gson();

        if (Utils.isBlank(keysString))
        {
            json.add("config", config);
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }

        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store, false);
            if (0 != status)
                return Response.status(status).build();

            Set<String> ks = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            ks.addAll(Arrays.asList(keysString.replaceAll(" ", "").split(",")));

            store.begin();
            AccessRuleWrapper rulesWrapper = repository.getRulesWrapper(user);

            Date dateObj = DateTimeUtils.dateFromTsOrTag(Utils.isBlank(tagLabel)
                                                                 ? null
                                                                 : store.getTag(repository.getId(), tagLabel),
                                                         ts,
                                                         repository.getCreateDate());

            json.addProperty("canManageContext", repository.canUserManageContext(user));
            Collection<CtxLevel> ctx = ContextParser.parseAndCreate( contextString, repository, store, user, dateObj, true);

            Context context = new Context(store, repository, ctx, dateObj, false);

            List<PropertyKey> keys = store.getKeys(user, repository, ks, dateObj);
            Map<PropertyKey, Collection<Property>> keyListMap = context.resolveFile(keys, false);

            keyListMap.forEach((k, v) -> {
                config.add(GsonHelper.keyAndPropertiesToGSON(repository, rulesWrapper, k, null, v));
            });

            if (null == dateObj && ks.size() > 0)
            {
                for (PropertyKey key : keys)
                    ks.remove(key.getKey());

                for (String k : ks)
                {
                    PropertyKey pk = new PropertyKey(repository, k);
                    config.add(GsonHelper.keyAndPropertiesToGSON(repository, rulesWrapper, pk, null, null));
                }
            }

            json.add("config", config);
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();
            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);
            if (e.getErrorCode().equals(Error.Code.CONTEXT_SCOPE_MISMATCH))
                json.addProperty("resetContext", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}