/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.admin.settings;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Level;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

@Path("/updateRepositoryFeatures")
public class UpdateRepositoryFeatures
        extends AAdminAccessValidation
{
    private static final Logger log = LogManager.getLogger(UpdateRepositoryFeatures.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("password") String password,
                           @FormParam("accessControl") boolean accessControl,
                           @FormParam("securityProfiles") boolean securityProfiles,
                           @FormParam("valueType") boolean valueType,
                           @FormParam("contextClusters") boolean contextClusters,
                           @FormParam("adminContextControlled") boolean adminContextControlled,
                           @FormParam("tokenlessAPIPull") boolean tokenlessAPIPull,
                           @FormParam("tokenlessAPIPush") boolean tokenlessAPIPush)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            if (!user.isPasswordValid(password))
            {
                json.addProperty("message", "Invalid authentication credentials specified.");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            boolean hasErrors = false;
            JsonArray messages = new JsonArray();


            if (!valueType)
            {
                List<PropertyKey> typedKeys = store.nonTextTypeKeys(repository, user);
                if (null != typedKeys && typedKeys.size() > 0)
                {
                    JsonObject err = new JsonObject();

                    err.addProperty("keys", typedKeys.size());
                    err.addProperty("type", "valueType");
                    err.addProperty("message", "Typed keys exist.");

                    messages.add(err);
                    hasErrors = true;
                }
            }

            if (!contextClusters)
            {
                Collection<Level> levels = repository.getLevels();
                for (Level level : levels)
                {
                    if (!level.isStandalone())
                    {
                        JsonObject err = new JsonObject();
                        err.addProperty("type", "contextClusters");
                        err.addProperty("message", "Group/Member context items exist.");

                        messages.add(err);
                        hasErrors = true;
                        break;
                    }
                }
            }

            if (hasErrors)
            {
                json.addProperty("success", false);
                json.add("err", messages);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            repository.setAccessControlEnabled(accessControl);
            repository.setSecurityProfilesEnabled(securityProfiles);
            repository.setValueTypeEnabled(valueType);
            repository.setContextClustersEnabled(contextClusters);
            repository.setAdminContextControlled(adminContextControlled);
            repository.setAllowTokenFreeAPIPull(tokenlessAPIPull);
            repository.setAllowTokenFreeAPIPush(tokenlessAPIPush);

            store.begin();
            store.update(repository, user);
            store.commit();

            json.addProperty("success", true);
            json.add("repository", GsonHelper.repositoryToJSON(repository));
        }
        catch (ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}
