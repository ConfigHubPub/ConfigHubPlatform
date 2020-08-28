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

package com.confighub.api.repository.client.v1;

import com.confighub.api.repository.client.AClientAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.model.ConcurrentContextFilenameResponseCache;
import com.confighub.core.repository.AbsoluteFilePath;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.resolver.Context;
import com.confighub.core.resolver.RepositoryFilesResolver;
import com.confighub.core.store.Store;
import com.confighub.core.utils.FileUtils;
import com.google.gson.Gson;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.Objects;

@Path("/rawFile")
public class APIPullFile
        extends AClientAccessValidation {
    private static final ConcurrentContextFilenameResponseCache cache = ConcurrentContextFilenameResponseCache.getInstance();

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
            checkToken(null, store);
            Context context = resolveContext(contextString, store);
            validatePull(context, appName, remoteIp, store, gson, securityProfiles, cache.containsKey(context, absPath));
            return getResponse(context, absPath, store);
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
            checkToken(clientToken, store);
            Context context = resolveContext(contextString, store);
            validatePull(context, appName, remoteIp, store, gson, securityProfiles, cache.containsKey(context, absPath));
            return getResponse(context, absPath, store);
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

    private Response getResponse(Context context, String absPath, Store store)
    {
        Response response = cache.get(context, absPath);
        if (Objects.isNull(response))
        {
            AbsoluteFilePath absoluteFilePath = store.getAbsFilePath(repository, absPath, date);
            RepoFile file = RepositoryFilesResolver.fullContextResolveForPath(absoluteFilePath, context);
            response = fileResponse(context, file, absoluteFilePath);
        }

        if (repository.isCachingEnabled())
        {
            cache.putIfAbsent(context, absPath, response);
        }

        return response;
    }

    private Response fileResponse(Context context, RepoFile file, AbsoluteFilePath absoluteFilePath)
    {
        if (Objects.isNull(file))
        {
            return Response.noContent().build();
        }
        else
        {
            return Response.ok(FileUtils.resolveFile(context, file, resolved, passwords),
                    absoluteFilePath.getContentType())
                    .build();
        }
    }

}