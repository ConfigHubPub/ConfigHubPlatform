/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.property;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.*;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.collections4.CollectionUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Set;

/**
 * When property's context changes, find out if it has a conflict with an existing property
 */
@Path("/contextChange")
public class ContextChange
        extends AUserAccessValidation
{

    @POST
    @Path("/value/{account}/{repository}")
    @Produces("application/json")
    public Response checkValue(@PathParam("account") String account,
                               @PathParam("repository") String repositoryName,
                               @FormParam("propertyId") Long propertyId,
                               @FormParam("key") String keyString,
                               @FormParam("context") String propertyContext,
                               @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status)
                return Response.status(status).build();

            json.addProperty("success", true);
            PropertyKey key = store.getKey(repository, keyString);
            if (null == key)
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

            Set<Property> all = key.getProperties();
            Collection<Level> context = ContextParser.parseAndCreate(propertyContext, repository, store, user, null);
            SecurityProfile ep = null;

            int score = 0;
            for (Level l : context)
                score += l.getContextScore();

            for (Property prop : all)
            {
                if (prop.getId().equals(propertyId))
                    continue;

                if (prop.isActive() && prop.getContextWeight() == score && CollectionUtils.isEqualCollection(context,
                                                                                                             prop.getContext()))
                {
                    json.addProperty("conflict", prop.getId());
                    json.add("conflictProperty",
                             GsonHelper.propertyToGSON(repository,
                                                       prop,
                                                       repository.getRulesWrapper(user),
                                                       ep,
                                                       GsonHelper.PropertyAttn.conflict));
                    break;
                }
            }

            json.addProperty("score", score);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

    @POST
    @Path("/file/{account}/{repository}")
    @Produces("application/json")
    public Response checkFile(@PathParam("account") String account,
                              @PathParam("repository") String repositoryName,
                              @FormParam("fileId") Long fileId,
                              @FormParam("path") String path,
                              @FormParam("name") String fileName,
                              @FormParam("context") String propertyContext,
                              @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status)
                return Response.status(status).build();

            json.addProperty("success", true);

            Collection<Level> context = ContextParser.parseAndCreate(propertyContext, repository, store, user, null);

            String cleanPath = !Utils.isBlank(path) && path.startsWith("/") ? path.substring(1) : path;
            String absPath = Utils.isBlank(cleanPath) ? fileName : cleanPath + "/" + fileName;

            AbsoluteFilePath absoluteFilePath = store.getAbsFilePath(repository, absPath, null);
            if (null != absoluteFilePath)
            {
                int score = 0;
                for (Level l : context)
                    score += l.getContextScore();

                Set<RepoFile> files = absoluteFilePath.getFiles();
                for (RepoFile file : files)
                {
                    if (file.getId().equals(fileId))
                        continue;

                    if (file.isActive() && file.getContextWeight() == score &&
                            CollectionUtils.isEqualCollection(context, file.getContext()))
                    {
                        json.addProperty("conflict", true);
                        break;
                    }
                }

                json.addProperty("score", score);
            }

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
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
