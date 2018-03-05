/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.audit;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Property;
import com.confighub.core.security.Encryption;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/decryptAuditValue")
public class DecryptAuditValue
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(DecryptAuditValue.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @FormParam("id") long id,
                        @FormParam("revId") long revId,
                        @FormParam("password") String password,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();
        Gson gson = new Gson();

        try {

            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            Property property = store.getAuditProperty(repository, user, id, revId);
            if (null == property)
                throw new ConfigException(Error.Code.NOT_FOUND);

            SecurityProfile sp = property.getPropertyKey().getSecurityProfile();
            if (!sp.isSecretValid(password))
                throw new ConfigException(Error.Code.INVALID_PASSWORD);

            if (property.isEncrypted())
                property.decryptValue(password);

            String dj = property.getDiffJson();
            if (Utils.isBlank(dj))
                json.addProperty("old", "");
            else
            {
                JsonObject diffJson = new Gson().fromJson(dj, JsonObject.class);
                json.addProperty("old",
                                 Encryption.decrypt(sp.getCipher(), diffJson.get("value").getAsString(), password));
            }

            json.addProperty("value", property.getValue());
            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

}
