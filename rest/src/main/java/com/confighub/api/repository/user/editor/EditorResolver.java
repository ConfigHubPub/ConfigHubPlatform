/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.editor;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Level;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.resolver.Context;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

@Path("/editorResolver")
public class EditorResolver
    extends AUserAccessValidation
{
    private static final Logger log = LogManager.getFormatterLogger("EditorResolver");

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @QueryParam("context") String contextString,
                        @QueryParam("ts") Long ts,
                        @QueryParam("allKeys") boolean allKeys,
                        @QueryParam("tag") String tagLabel,
                        @QueryParam("literal") boolean literal,
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

            Collection<Level> ctx = ContextParser.parseAndCreate(contextString, repository, store, user, dateObj, true);

            json.addProperty("canManageContext", repository.canUserManageContext(user));

            Context context = new Context(store, repository, ctx, dateObj, allKeys);
            Map<PropertyKey, Collection<Property>> keyListMap;

            Long start = System.currentTimeMillis();

            if (literal)
                keyListMap = context.literalContextResolver();
            else
                keyListMap = context.resolve();

            log.info("[%s] Editor resolved %d keys in %d/ms",
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
        catch (ConfigException e)
        {
            log.error("Editor error: " + e.getMessage());

            if (e.getErrorCode().equals(Error.Code.CONTEXT_SCOPE_MISMATCH))
                json.addProperty("resetContext", true);

            json.addProperty("error", e.getMessage());
        }
        finally
        {
            store.close();
        }

        Gson gson = new GsonBuilder().serializeNulls().create();
        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }

}
