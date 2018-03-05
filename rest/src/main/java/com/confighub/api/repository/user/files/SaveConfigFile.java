/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.files;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Level;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/saveConfigFile")
public class SaveConfigFile
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(SaveConfigFile.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response saveOrUpdate(@PathParam("account") String account,
                                 @PathParam("repository") String repositoryName,
                                 @HeaderParam("Authorization") String token,
                                 @FormParam("path") String path,
                                 @FormParam("name") String name,
                                 @FormParam("id") Long id,
                                 @FormParam("content") String content,
                                 @FormParam("context") String fileContext,
                                 @FormParam("active") boolean active,
                                 @FormParam("changeComment") String changeComment,
                                 @FormParam("currentPassword") String currentPassword,
                                 @FormParam("newProfilePassword") String newProfilePassword,
                                 @FormParam("spName") String spName,
                                 @FormParam("renameAll") boolean renameAll,
                                 @FormParam("updateRefs") boolean updateRefs)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status)
                return Response.status(status).build();

            Collection<Level> context = ContextParser.parseAndCreate(fileContext, repository, store, user, null);
            RepoFile file;
            store.begin();
            if (null != id && id > 0)
            {
                file = store.updateRepoFile(user,
                                            repository,
                                            id,
                                            path,
                                            name,
                                            renameAll,
                                            updateRefs,
                                            content,
                                            context,
                                            active,
                                            spName,
                                            newProfilePassword,
                                            currentPassword,
                                            changeComment);
            } else
            {
                file = store.createRepoFile(user,
                                            repository,
                                            path,
                                            name,
                                            content,
                                            context,
                                            active,
                                            spName,
                                            newProfilePassword,
                                            changeComment);
            }
            store.commit();

            json.addProperty("success", true);
            json.addProperty("id", file.getId());

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            switch (e.getErrorCode())
            {
                case FILE_CIRCULAR_REFERENCE:
                    json.add("circularRef", e.getJson());

                default:
                    json.addProperty("status", "ERROR");
            }

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}