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
import com.confighub.core.repository.*;
import com.confighub.core.resolver.Context;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.FileUtils;
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
import java.util.EnumSet;
import java.util.Map;

@Path("/pull")
@Produces("application/json")
public class APIPull
        extends AClientAccessValidation
{
    private static final Logger log = LogManager.getLogger("API");

    @GET
    public Response get(@HeaderParam("Client-Token") String clientToken,
                        @HeaderParam("Context") String contextString,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("Application-Name") String appName,
                        @HeaderParam("Security-Profile-Auth") String securityProfiles,
                        @HeaderParam("Include-Comments") boolean includeComments,
                        @HeaderParam("X-Forwarded-For") String remoteIp,
                        @HeaderParam("Pretty") boolean pretty,
                        @HeaderParam("No-Files") boolean noFiles,
                        @HeaderParam("No-Properties") boolean noProperties,
                        @HeaderParam("Include-Value-Context") boolean includeContext)
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

            validatePull(clientToken,
                         contextString,
                         version,
                         appName,
                         remoteIp,
                         store,
                         gson,
                         securityProfiles);

            getConfiguration(repository,
                             context,
                             resolved,
                             passwords,
                             json,
                             noFiles,
                             noProperties,
                             includeComments,
                             includeContext,
                             gson);

            Response.ResponseBuilder response = Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON);
            response.status(200);

            return response.build();
        }
        catch (ConfigException e)
        {
            System.out.println("-- 1 --");
            e.printStackTrace();
            return Response.status(Response.Status.NOT_ACCEPTABLE).tag(e.getMessage()).build();
        }
        catch (Exception e)
        {
            System.out.println("-- 2 --");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).tag(e.getMessage()).build();
        }
        finally
        {
            store.close();
        }
    }


    @GET
    @Path("/{account}/{repository}")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Context") String contextString,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("Application-Name") String appName,
                        @HeaderParam("Security-Profile-Auth") String securityProfiles,
                        @HeaderParam("Include-Comments") boolean includeComments,
                        @HeaderParam("X-Forwarded-For") String remoteIp,
                        @HeaderParam("Pretty") boolean pretty,
                        @HeaderParam("Include-Value-Context") boolean includeContext,
                        @HeaderParam("No-Files") boolean noFiles,
                        @HeaderParam("No-Properties") boolean noProperties)
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
            validatePull(null,
                         contextString,
                         version,
                         appName,
                         remoteIp,
                         store,
                         gson,
                         securityProfiles);

            getConfiguration(repository,
                             context,
                             resolved,
                             passwords,
                             json,
                             noFiles,
                             noProperties,
                             includeComments,
                             includeContext,
                             gson);

            Response.ResponseBuilder response = Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON);
            response.status(200);

            return response.build();

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

    public static JsonObject getConfiguration(final Repository repository,
                                              final Context context,
                                              final Map<PropertyKey, Property> resolved,
                                              final Map<String, String> passwords,
                                              final JsonObject json,
                                              final boolean noFiles,
                                              final boolean noProperties,
                                              final boolean includeComments,
                                              final boolean includeContext,
                                              final Gson gson)
    {
        EnumSet<Depth> depths = repository.getDepth().getDepths();
        JsonObject config = new JsonObject();

        if (!noFiles)
        {
            JsonObject filesJson = new JsonObject();
            Map<AbsoluteFilePath, RepoFile> fileMap = context.resolveAPIFiles();

            for (AbsoluteFilePath path : fileMap.keySet())
            {
                JsonObject fileJson = new JsonObject();
                fileJson.addProperty("content", FileUtils.resolveFile(context, fileMap.get(path), resolved, passwords));
                fileJson.addProperty("content-type", path.getContentType());

                filesJson.add(path.getAbsPath(), fileJson);
            }

            json.add("files", filesJson);
        }


        if (!noProperties)
        {
            for (PropertyKey key : resolved.keySet())
            {
                JsonObject o = new JsonObject();

                if (!PropertyKey.ValueDataType.Text.equals(key.getValueDataType()))
                    o.addProperty("type", key.getValueDataType().name());

                if (key.isDeprecated())
                    o.addProperty("deprecated", true);

                if (includeComments && !Utils.isBlank(key.getReadme()))
                    o.addProperty("comment", key.getReadme());

                Property property = resolved.get(key);

                try
                {
                    if (key.isEncrypted())
                    {
                        SecurityProfile sp = key.getSecurityProfile();
                        String pass = passwords.get(sp.getName());

                        if (null == pass)
                        {
                            o.addProperty("encryption", sp.getName());
                            o.addProperty("cipher", sp.getCipher().getName());
                        } else
                            property.decryptValue(pass);
                    }

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                if (key.getValueDataType().equals(PropertyKey.ValueDataType.List))
                    o.add("val", gson.fromJson(property.getValue(), JsonArray.class));
                else if (key.getValueDataType().equals(PropertyKey.ValueDataType.Map))
                    o.add("val", gson.fromJson(property.getValue(), JsonObject.class));
                else
                    o.addProperty("val", property.getValue());

                if (includeContext)
                {
                    JsonArray ctx = new JsonArray();

                    Map<String, LevelCtx> ctxMap = property.getDepthMap();
                    for (Depth depth : depths)
                    {
                        if (null == ctxMap)
                        {
                            ctx.add("*");
                            continue;
                        }

                        LevelCtx lc = ctxMap.get(String.valueOf(depth.getPlacement()));
                        if (null == lc)
                            ctx.add("*");
                        else
                            ctx.add(lc.name);
                    }

                    o.add("context", ctx);
                }

                config.add(key.getKey(), o);
            }

            json.add("properties", config);
        }

        return json;
    }
}
