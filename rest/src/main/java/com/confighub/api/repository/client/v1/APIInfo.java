/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.client.v1;

import com.confighub.api.repository.client.AClientAccessValidation;
import com.confighub.api.util.ServiceConfiguration;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.*;
import com.confighub.core.store.Store;
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
import java.util.*;

@Path("/info")
@Produces("application/json")
public class APIInfo
        extends AClientAccessValidation
{
    private static final Logger log = LogManager.getLogger("API");

    /**
     * - Given a repo, get all contexts
     * - Given a repo and context type, get all context values
     * (e.g. Demo, Environment -> [ Development, Production, Test ])
     * - Given a repo, a list of available files
     */
    @GET
    public Response get(@HeaderParam("Client-Token") String clientToken,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("X-Forwarded-For") String remoteIp,
                        @HeaderParam("Files") boolean includeFiles,
                        @HeaderParam("Files-Glob") String filesGlob,
                        @HeaderParam("Context-Elements") boolean includeCIs,
                        @HeaderParam("Context-Labels") String contextLabels,
                        @HeaderParam("Tags") boolean tags,
                        @HeaderParam("Pretty") boolean pretty)
    {
        Store store = new Store();
        Gson gson;

        if (pretty)
            gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        else
            gson = new GsonBuilder().serializeNulls().create();

        try
        {
            getRepositoryFromToken(clientToken, dateString, tagString, store);

            return getRepoInfo(repository.getAccountName(),
                               repository.getName(),
                               includeFiles,
                               filesGlob,
                               includeCIs,
                               contextLabels,
                               tags,
                               gson,
                               store);
        }
        catch (ConfigException e)
        {
            return Response.status(Response.Status.EXPECTATION_FAILED).tag(e.getErrorCode().getMessage()).build();

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

    /**
     * - Given a repo, get all contexts
     * - Given a repo and context type, get all context values
     * (e.g. Demo, Environment -> [ Development, Production, Test ])
     * - Given a repo, a list of available files
     */
    @GET
    @Path("/{account}/{repository}")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("X-Forwarded-For") String remoteIp,
                        @HeaderParam("Files") boolean includeFiles,
                        @HeaderParam("Files-Glob") String filesGlob,
                        @HeaderParam("Context-Elements") boolean includeCIs,
                        @HeaderParam("Context-Labels") String contextLabels,
                        @HeaderParam("Tags") boolean tags,
                        @HeaderParam("Pretty") boolean pretty)
    {
        Store store = new Store();
        Gson gson;

        if (pretty)
            gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        else
            gson = new GsonBuilder().serializeNulls().create();

        try
        {
            getRepositoryFromUrl(account, repositoryName, tagString, dateString, store, true);
            return getRepoInfo(account,
                               repositoryName,
                               includeFiles,
                               filesGlob,
                               includeCIs,
                               contextLabels,
                               tags,
                               gson,
                               store);
        }
        catch (ConfigException e)
        {
            return Response.status(Response.Status.EXPECTATION_FAILED).tag(e.getErrorCode().getMessage()).build();

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

    @GET
    @Path("/all")
    public Response get(@HeaderParam("Client-Version") String version,
                        @HeaderParam("Pretty") boolean pretty)
    {
        Store store = new Store();
        Gson gson;

        if (pretty)
            gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        else
            gson = new GsonBuilder().serializeNulls().create();

        JsonArray json = new JsonArray();

        try {
            List<Repository> repositories = store.getAllRepositories();

            for (Repository repository : repositories)
            {
                JsonObject jsonRepo = new JsonObject();
                jsonRepo.addProperty("account", repository.getAccountName());
                jsonRepo.addProperty("name", repository.getName());
                jsonRepo.addProperty("isPrivate", repository.isPrivate());
                jsonRepo.addProperty("isPersonal", repository.isPersonal());
                jsonRepo.addProperty("description", repository.getDescription());
                jsonRepo.addProperty("created", DateTimeUtils.iso8601.get().format(repository.getCreateDate()));
                jsonRepo.addProperty("accessControlsEnabled", repository.isAccessControlEnabled());
                jsonRepo.addProperty("vdtEnabled", repository.isValueTypeEnabled());
                jsonRepo.addProperty("securityEnabled", repository.isSecurityProfilesEnabled());
                jsonRepo.addProperty("contextGroupsEnabled", repository.isContextClustersEnabled());
                jsonRepo.addProperty("keyCount", repository.getKeys().size());
                jsonRepo.addProperty("valueCount", repository.getProperties().size());
                jsonRepo.addProperty("userCount", repository.getUserCount());

                JsonArray jsonContextLabels = new JsonArray();
                for (Depth d : repository.getDepth().getDepths())
                    jsonContextLabels.add(repository.getLabel(d));
                jsonRepo.add("context", jsonContextLabels);

                json.add(jsonRepo);
            }

            Response.ResponseBuilder response = Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON);
            response.status(200);

            return response.build();
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
            return Response.status(Response.Status.EXPECTATION_FAILED).tag(e.getErrorCode().getMessage()).build();
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
    @Path("/system")
    public Response getSystemInfo(@HeaderParam("Client-Version") String version,
                                  @HeaderParam("Pretty") boolean pretty)
    {
        Gson gson;

        if (pretty)
            gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        else
            gson = new GsonBuilder().serializeNulls().create();

        JsonObject json = new JsonObject();
        Store store = new Store();
        try
        {
            json.addProperty("version", ServiceConfiguration.getVersion());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            store.close();
        }

        Response.ResponseBuilder response = Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON);
        response.status(200);

        return response.build();
    }

    private Response getRepoInfo(final String account,
                                 final String repositoryName,
                                 final boolean includeFiles,
                                 final String filesGlob,
                                 final boolean includeCIs,
                                 final String contextLabels,
                                 final boolean tags,
                                 final Gson gson,
                                 final Store store)
    {
        JsonObject json = new JsonObject();

        // Global repository information
        json.addProperty("account", account);
        json.addProperty("repository", repositoryName);
        json.addProperty("generatedOn", DateTimeUtils.standardDTFormatter.get().format(new Date()));
        if (null != this.date)
            json.addProperty("date", DateTimeUtils.standardDTFormatter.get().format(this.date));


        // Repository context labels
        JsonArray jsonContextLabels = new JsonArray();
        for (Depth d : repository.getDepth().getDepths())
            jsonContextLabels.add(repository.getLabel(d));
        json.add("context", jsonContextLabels);

        if (includeCIs || !Utils.isBlank(contextLabels))
        {
            Map<Depth, Collection<Level>> levels;

            if (null == this.date)
                levels = store.getLevelsByDepth(repository);
            else
                levels = store.getLevelsByDepth(this.repository, this.date);

            Set<Depth> depths = null;

            if (!Utils.isBlank(contextLabels))
            {
                depths = new HashSet<>();
                for (String label : contextLabels.trim().split(","))
                {
                    if (!Utils.isBlank(label))
                        depths.add(this.repository.getDepthFromLabel(label));
                }
            }

            JsonObject jsonDepth = new JsonObject();

            for (Depth depth : null == depths ? levels.keySet() : depths)
            {
                JsonArray jsonDepthLevels = new JsonArray();

                for (Level level : levels.get(depth))
                    jsonDepthLevels.add(level.getName());

                jsonDepth.add(repository.getLabel(depth), jsonDepthLevels);
            }

            json.add("contextElements", jsonDepth);
        }

        // Files
        if (includeFiles || !Utils.isBlank(filesGlob))
        {
            String regex = null;

            if (!Utils.isBlank(filesGlob))
                regex = Utils.convertGlobToRegEx(filesGlob);

            JsonArray jsonFiles = new JsonArray();
            if (null != repository.getFiles())
            {
                for (RepoFile file : repository.getFiles())
                {
                    if (null == regex || file.getAbsPath().matches(regex))
                    {
                        JsonObject jsonFile = new JsonObject();
                        jsonFile.addProperty("name", file.getAbsFilePath().getFilename());
                        jsonFile.addProperty("path", file.getAbsPath());
                        if (null != file.getSecurityProfile())
                            jsonFile.addProperty("secureGroup", file.getSecurityProfile().getName());

                        jsonFiles.add(jsonFile);
                    }
                }
            }
            json.add("files", jsonFiles);
        }

        if (tags)
        {
            List<Tag> tagList = store.getTags(this.repository);
            JsonArray jsonTags = new JsonArray();
            tagList.forEach(t -> jsonTags.add(t.toJson()));
            json.add("tags", jsonTags);
        }

        Response.ResponseBuilder response = Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON);
        response.status(200);

        return response.build();

    }

}
