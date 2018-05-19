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
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
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
import java.util.Set;

@Path("/export")
@Produces("application/json")
public class APIExport
        extends AClientAccessValidation
{
    private static final Logger log = LogManager.getLogger("API");

    @GET
    @Path("/{account}/{repository}")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Application-Name") String appName,
                        @HeaderParam("Omit-Values") boolean omitValues,
                        @HeaderParam("Omit-Files") boolean omitFiles,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("X-Forwarded-For") String remoteIp,
                        @HeaderParam("Pretty") boolean pretty)
    {
        Store store = new Store();
        Gson gson;

        if (pretty)
            gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        else
            gson = new GsonBuilder().serializeNulls().create();

        try
        {

            getRepositoryFromUrl(account, repositoryName, tagString, dateString, store, true);
            validateExport(null, version, appName, remoteIp, store);

            EnumSet<Depth> depths = repository.getDepth().getDepths();
            JsonArray data = new JsonArray();

            Set<PropertyKey> keys = this.repository.getKeys();
            for (PropertyKey key : keys)
            {
                JsonObject keyJson = new JsonObject();
                keyJson.addProperty("key", key.getKey());
                keyJson.addProperty("readme", key.getReadme());
                keyJson.addProperty("deprecated", key.isDeprecated());
                keyJson.addProperty("vdt", key.getValueDataType().name());
                keyJson.addProperty("push", key.isPushValueEnabled());
                if (key.isSecure())
                {
                    keyJson.addProperty("securityGroup", key.getSecurityProfile().getName());
                    // add encrypted security group password
                }

                if (!omitValues)
                {
                    JsonArray propertiesJson = new JsonArray();
                    Set<Property> properties = key.getProperties();
                    if (null != properties)
                    {
                        for (Property property : properties)
                        {
                            JsonObject propertyJson = new JsonObject();
                            propertyJson.addProperty("context", ContextParser.getContextForExport(depths, property.getContextMap()));
                            propertyJson.addProperty("value", property.getValue());
                            propertyJson.addProperty("active", property.isActive());

                            propertiesJson.add(propertyJson);
                        }
                    }

                    keyJson.add("values", propertiesJson);
                }

                data.add(keyJson);
            }

            if (!omitFiles)
            {
                Set<RepoFile> files = this.repository.getFiles();
                for (RepoFile file : files)
                {
                    JsonObject fileJson = new JsonObject();
                    fileJson.addProperty("content", file.getContent());
                    fileJson.addProperty("file", file.getAbsPath());
                    fileJson.addProperty("context", ContextParser.getContextForExport(depths, file.getContextMap()));

                    if (file.isSecure())
                    {
                        fileJson.addProperty("securityGroup", file.getSecurityProfile().getName());
                        // add encrypted security group password
                    }

                    data.add(fileJson);
                }
            }

            json.add("data", data);


            Response.ResponseBuilder response = Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON);
            response.status(200);

            return response.build();
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

}
