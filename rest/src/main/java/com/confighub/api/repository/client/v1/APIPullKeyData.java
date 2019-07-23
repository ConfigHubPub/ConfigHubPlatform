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
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/keyData")
public class APIPullKeyData
        extends AClientAccessValidation
{
    private static final Logger log = LogManager.getLogger("API");

    @GET
    @Path("/{account}/{repository}")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("KeyName") String keyName,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("Application-Name") String appName,
                        @HeaderParam("X-Forwarded-For") String remoteIp,
                        @HeaderParam("Pretty") boolean pretty)
    {

        Store store = new Store();
        try
        {
            getRepositoryFromUrl(account, repositoryName, tagString, dateString, store, true);
            validateKeyDataPull(null, version, appName, remoteIp, store);
            return keyResponse(store.getKey(repository, keyName, date), pretty);
        }
        catch (ConfigException e)
        {
            log.error("A ConfigException occurred", e);
            return Response.status(Response.Status.NOT_ACCEPTABLE).tag(e.getMessage()).build();
        }
        catch (Exception e)
        {
            log.error("An unexpected exception occurred", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).tag(e.getMessage()).build();
        }
        finally
        {
            store.close();
        }
    }

    @GET
    public Response get(@HeaderParam("Client-Token") String clientToken,
                        @HeaderParam("KeyName") String keyName,
                        @HeaderParam("Repository-Date") String dateString,
                        @HeaderParam("Tag") String tagString,
                        @HeaderParam("Client-Version") String version,
                        @HeaderParam("Application-Name") String appName,
                        @HeaderParam("X-Forwarded-For") String remoteIp,
                        @HeaderParam("Pretty") boolean pretty)
    {
        Store store = new Store();

        try
        {
            getRepositoryFromToken(clientToken, dateString, tagString, store);
            validateKeyDataPull(clientToken, version, appName, remoteIp, store);
            return keyResponse(store.getKey(repository, keyName, date), pretty);
        }
        catch (ConfigException e)
        {
            log.error("A ConfigException occurred", e);
            return Response.status(Response.Status.NOT_ACCEPTABLE).tag(e.getMessage()).build();
        }
        catch (Exception e)
        {
            log.error("An unexpected exception occurred", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).tag(e.getMessage()).build();
        }
        finally
        {
            store.close();
        }
    }

    private Response keyResponse(PropertyKey propertyKey, boolean pretty)
    {
        Gson gson;
        if (pretty)
            gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        else
            gson = new GsonBuilder().serializeNulls().create();

        JsonObject keyJson = new JsonObject();
        keyJson.addProperty("key", propertyKey.getKey());
        keyJson.addProperty("readme", propertyKey.getReadme());
        keyJson.addProperty("deprecated", propertyKey.isDeprecated());
        keyJson.addProperty("vdt", propertyKey.getValueDataType().name());
        keyJson.addProperty("push", propertyKey.isPushValueEnabled());

        Response.ResponseBuilder response = Response.ok(gson.toJson(keyJson), MediaType.APPLICATION_JSON);
        response.status(200);
        return response.build();
    }
}