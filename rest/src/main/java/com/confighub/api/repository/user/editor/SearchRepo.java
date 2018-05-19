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
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.store.Store;
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
import java.util.Collection;
import java.util.Date;
import java.util.Map;

@Path("/searchRepo")
public class SearchRepo
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getFormatterLogger("EditorResolver");

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @QueryParam("ts") Long ts,
                        @QueryParam("tag") String tagLabel,
                        @QueryParam("searchTerm") String searchTerm,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            Date dateObj = DateTimeUtils.dateFromTsOrTag(Utils.isBlank(tagLabel)
                                                                 ? null
                                                                 : store.getTag(repository.getId(), tagLabel),
                                                         ts,
                                                         repository.getCreateDate());


            Long start = System.currentTimeMillis();

            Map<PropertyKey, Collection<Property>> keyListMap = store.searchKeysAndValues(user,
                                                                                          repository,
                                                                                          dateObj,
                                                                                          searchTerm);

            log.info("[%s] Editor search found %d objects in %d/ms",
                     null == user ? "guest" : user.getUsername(),
                     keyListMap.size(),
                     (System.currentTimeMillis() - start));


            AccessRuleWrapper rulesWrapper = repository.getRulesWrapper(user);

            JsonArray config = new JsonArray();
            keyListMap.forEach((k, v) -> {
                config.add(GsonHelper.keyAndPropertiesToGSON(repository, rulesWrapper, k, null, v));
            });

            json.add("config", config);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            log.error("Editor error: " + e.getMessage());
            json.addProperty("error", e.getMessage());
        }
        finally
        {
            store.close();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }

}
