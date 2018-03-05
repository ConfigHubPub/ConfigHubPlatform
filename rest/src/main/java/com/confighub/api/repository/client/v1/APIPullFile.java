/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.client.v1;

import com.confighub.api.repository.client.AClientAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.AbsoluteFilePath;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.resolver.RepositoryFilesResolver;
import com.confighub.core.store.Store;
import com.confighub.core.utils.FileUtils;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/rawFile")
public class APIPullFile
        extends AClientAccessValidation
{
    private static final Logger log = LogManager.getLogger("API");

    @GET
    @Path("/{account}/{repository}")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Context") String contextString,
                        @HeaderParam("File") String absPath,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("Application-Name") String appName,
                        @HeaderParam("Security-Profile-Auth") String securityProfiles,
                        @HeaderParam("X-Forwarded-For") String remoteIp)
    {
        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            getRepositoryFromUrl(account, repositoryName, tagString, dateString, store, true);
            validatePull(null, contextString, version, appName, remoteIp, store, gson, securityProfiles);

            AbsoluteFilePath absoluteFilePath = store.getAbsFilePath(repository, absPath, date);
            RepoFile file = RepositoryFilesResolver.fullContextResolveForPath(absoluteFilePath, context);

            return fileResponse(file, absoluteFilePath, store);
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).tag(e.getMessage()).build();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).tag(e.getMessage()).build();
        }
        finally
        {
            store.close();
        }
    }

    @GET
    public Response get(@HeaderParam("Client-Token") String clientToken,
                        @HeaderParam("Context") String contextString,
                        @HeaderParam("File") String absPath,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("Application-Name") String appName,
                        @HeaderParam("Security-Profile-Auth") String securityProfiles,
                        @HeaderParam("X-Forwarded-For") String remoteIp)
    {
        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            getRepositoryFromToken(clientToken, dateString, tagString, store);
            validatePull(clientToken, contextString, version, appName, remoteIp, store, gson, securityProfiles);

            AbsoluteFilePath absoluteFilePath = store.getAbsFilePath(repository, absPath, date);
            RepoFile file = RepositoryFilesResolver.fullContextResolveForPath(absoluteFilePath, context);

            return fileResponse(file, absoluteFilePath, store);
        }
        catch (ConfigException e)
        {
            return Response.status(Response.Status.NOT_ACCEPTABLE).tag(e.getMessage()).build();
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

    private Response fileResponse(RepoFile file, AbsoluteFilePath absoluteFilePath, final Store store)
    {
        if (null != file)
        {
            Response.ResponseBuilder response = Response.ok(FileUtils.resolveFile(context, file, resolved, passwords),
                                                            absoluteFilePath.getContentType());
            response.status(200);

            return response.build();
        } else
        {
            Response.ResponseBuilder response = Response.noContent();
            return response.build();
        }
    }

}