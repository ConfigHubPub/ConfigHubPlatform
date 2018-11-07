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

import com.confighub.api.repository.client.AClientAccessValidation;
import com.confighub.api.repository.client.v1.APIPull;
import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.*;
import com.confighub.core.resolver.Context;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/getJSONDiff")
@Produces("application/json")
public class GetJSONDiff
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger("API");

    @POST
    @Path("/{account}/{repository}")
    public Response getLocal(@PathParam("account") String account,
                             @PathParam("repository") String repositoryName,

                             @FormParam("aContext") String aContextString,
                             @FormParam("aRepository-Date") String aDateString,
                             @FormParam("aTag") String aTagString,
                             @FormParam("aSecurity-Profile-Auth") String aSecurityProfiles,

                             @FormParam("bContext") String bContextString,
                             @FormParam("bRepository-Date") String bDateString,
                             @FormParam("bTag") String bTagString,
                             @FormParam("bSecurity-Profile-Auth") String bSecurityProfiles,
                             @HeaderParam("Authorization") String token)

    {
        Store store = new Store();
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        JsonObject json = new JsonObject();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            Repository repository = store.getRepository(account, repositoryName);

            json.add("left",  getJson(store, repository, gson, aContextString, aDateString, aTagString, aSecurityProfiles));
            json.add("right", getJson(store, repository, gson, bContextString, bDateString, bTagString, bSecurityProfiles));

            Response.ResponseBuilder response = Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON);
            response.status(200);

            return response.build();
        }
        catch (ConfigException e)
        {
            json.addProperty("error", e.getMessage());
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).tag(e.getMessage()).build();
        }
        finally
        {
            store.close();
        }
    }


    private JsonObject getJson(final Store store,
                               final Repository repository,
                               final Gson gson,
                               String contextString,
                               String dateString,
                               String tagString,
                               String securityProfiles)
            throws ConfigException
    {
        Tag tag = null;
        if (!Utils.isBlank(tagString))
            tag = store.getTag(repository.getId(), tagString);

        Date date = DateTimeUtils.parseAPIDate(tag, dateString, null);
        Repository specificRepo = repository;
        if (null != date)
            specificRepo = store.getRepository(repository.getId(), date);

        Collection<CtxLevel> aCtx =
                ContextParser.parseAndCreate(contextString,
                                             specificRepo,
                                             store,
                                             user,
                                             date,
                                             true);
        Context context = new Context(store, specificRepo, aCtx, date, false);

        if (!context.isFullContext())
            throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

        JsonObject json = new JsonObject();
        AClientAccessValidation.addJsonHeader(json, specificRepo, contextString, context);

        Map<String, String> aPasswords = new HashMap<>();
        AClientAccessValidation.processAuth(store, gson, securityProfiles, null, specificRepo, date, aPasswords);

        Map<PropertyKey, Property> aResolved = context.resolveForClient();
        Map<PropertyKey, Property> sorted = new TreeMap<>(
                (Comparator<PropertyKey>) (o1, o2) -> o1.getKey().compareTo(o2.getKey())
        );

        sorted.putAll(aResolved);
        APIPull.getConfiguration(repository, context, sorted, aPasswords, json, true, false, true, false, gson);

        return json;
    }

}
