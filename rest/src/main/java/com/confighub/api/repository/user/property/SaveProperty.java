/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.property;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Level;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/saveProperty")
public class SaveProperty
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(SaveProperty.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @HeaderParam("Authorization") String token,
                           @FormParam("key") String key,
                           @FormParam("comment") String comment,
                           @FormParam("deprecated") boolean deprecated,
                           @FormParam("vdt") String vdt,
                           @FormParam("pushEnabled") boolean pushEnabled,
                           @FormParam("value") String value,
                           @FormParam("context") String propertyContext,
                           @FormParam("changeComment") String changeComment,
                           @FormParam("active") boolean active,
                           @FormParam("spPassword") String spPassword,
                           @FormParam("spName") String spName,
                           @FormParam("propertyId") Long propertyId)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status)
                return Response.status(status).build();

            Collection<Level> context = ContextParser.parseAndCreate(propertyContext, repository, store, user, null);

            if (null != propertyId && propertyId >= 0)
            {
                store.begin();
                store.updateProperty(propertyId, value, context, changeComment, active, spPassword, user);
                store.commit();
            }
            else
            {
                if (Utils.isBlank(key))
                {
                    json.addProperty("message", "Key cannot be blank.");
                    json.addProperty("success", false);
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }

                PropertyKey.ValueDataType valueDataType;
                try
                {
                    valueDataType = PropertyKey.ValueDataType.valueOf(vdt);
                } catch (Exception e) {
                    valueDataType = PropertyKey.ValueDataType.Text;
                }

                store.begin();
                propertyId = store.createProperty(key,
                                                  value,
                                                  active,
                                                  comment,
                                                  deprecated,
                                                  pushEnabled,
                                                  valueDataType,
                                                  context,
                                                  changeComment,
                                                  spPassword,
                                                  spName,
                                                  repository,
                                                  user).getId();
                store.commit();
            }

            json.addProperty("success", true);
            json.addProperty("id", propertyId);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            store.rollback();

            switch (e.getErrorCode())
            {
                case PROP_DUPLICATION_CONTEXT:
                case PROP_PARENT_LOCK:
                case PROP_CHILD_LOCK:
                case PROP_CONTEXT_DUPLICATE_DEPTH:
                    json.addProperty("status", Store.KeyUpdateStatus.MERGE.name());
                    break;

                case FILE_CIRCULAR_REFERENCE:
                    json.add("circularRef", e.getJson());

                default:
                    json.addProperty("status", "ERROR");
            }

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
