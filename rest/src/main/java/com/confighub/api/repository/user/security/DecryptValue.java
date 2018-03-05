/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.security;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.SecurityStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/decryptValue")
public class DecryptValue
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(DecryptValue.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("propertyId") Long propertyId,
                           @FormParam("spPassword") String spPassword)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        SecurityStore store = new SecurityStore();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            Property property = store.getProperty(user, repository, propertyId);
            if (null == property)
            {
                json.addProperty("message", "Could not find specified property.");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            PropertyKey key = property.getPropertyKey();
            SecurityProfile ep = key.getSecurityProfile();
            if (null == ep)
            {
                json.addProperty("decoded", property.getValue());
                json.addProperty("success", true);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            if (!ep.isSecretValid(spPassword))
            {
                json.addProperty("message", "Invalid secret specified");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            GsonHelper.setValue(json,
                                property.getPropertyKey().getValueDataType(),
                                ep.decrypt(property.getValue(), spPassword),
                                property);
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
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
