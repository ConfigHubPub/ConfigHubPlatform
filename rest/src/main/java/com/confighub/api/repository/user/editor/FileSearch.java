/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.editor;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.repository.AbsoluteFilePath;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Used for auto-complete RepoFile searching
 */
@Path("/fileSearch")
public class FileSearch
        extends AUserAccessValidation
{

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response getAssignments(@QueryParam("t") String searchTerm,
                                   @PathParam("account") String account,
                                   @PathParam("repository") String repositoryName,
                                   @HeaderParam("Authorization") String token)
    {
        JsonArray json = new JsonArray();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            List<AbsoluteFilePath> AbsoluteFilePaths = store.searchFile(searchTerm, 10, repository);
            if (null != AbsoluteFilePaths)
            {
                for (AbsoluteFilePath absoluteFilePath : AbsoluteFilePaths)
                    if (!absoluteFilePath.getAbsPath().equals(searchTerm))
                        json.add(absoluteFilePath.getAbsPath());
            }

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
