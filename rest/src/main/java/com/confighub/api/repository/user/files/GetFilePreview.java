/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.files;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Level;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.resolver.Context;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.FileUtils;
import com.confighub.core.utils.Utils;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Path("/getFilePreview")
public class GetFilePreview
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetFilePreview.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response add(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Authorization") String token,
                        MultivaluedMap<String, String> formParams,
                        @FormParam("ts") Long ts,
                        @FormParam("tag") String tagLabel,
                        @FormParam("context") String contextString,
                        @FormParam("fileContent") String fileContent,
                        @FormParam("password") String password)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status)
                return Response.status(status).build();

            Date dateObj = DateTimeUtils.dateFromTsOrTag(Utils.isBlank(tagLabel)
                                                                 ? null
                                                                 : store.getTag(repository.getId(), tagLabel),
                                                         ts,
                                                         repository.getCreateDate());

            Collection<Level> ctx = ContextParser.parseAndCreate(contextString, repository, store, user, dateObj);
            Context context = new Context(store, repository, ctx, dateObj, false);
            if (!context.isFullContext())
                throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

            Map<PropertyKey, Collection<Property>> keyListMap = context.resolve();
            Map<String, Property> keyValueMap = new HashMap<>();

            if (null != keyListMap)
            {
                for (PropertyKey key : keyListMap.keySet())
                    keyValueMap.put(key.getKey(), Iterables.getLast(keyListMap.get(key)));
            }

            json.addProperty("content", FileUtils.previewFile(context, fileContent, keyValueMap, formParams));
            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
            json.add("culprit", e.getJson());
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
