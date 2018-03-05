/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.tokens;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.Store;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Path("/saveOrUpdateToken")
public class SaveOrUpdateToken
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(SaveOrUpdateToken.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response create(@PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           MultivaluedMap<String, String> formParams,
                           @FormParam("id") Long id,
                           @FormParam("name") String name,
                           @FormParam("active") boolean active,
                           @FormParam("expires") Long expires,
                           @FormParam("forceKeyPushEnabled") boolean forceKeyPushEnabled,
                           @FormParam("addSps") String addSps,
                           @FormParam("removeSps") String removeSps,
                           @FormParam("rulesTeam") String rulesTeam,
                           @FormParam("managingTeam") String managingTeam,
                           @FormParam("managedBy") String managedByName,
                           @FormParam("newOwner") String newOwner,
                           @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            List<SecurityProfile> spsToAdd = new ArrayList<>();
            if (!Utils.isBlank(addSps))
            {
                String[] profiles = addSps.split(",");
                for (String profile : profiles)
                {
                    String password = formParams.get(profile).get(0);

                    SecurityProfile sp = store.getSecurityProfile(user, repository, null, profile);
                    if (null == sp)
                    {
                        json.addProperty("message", "Security profile '" + profile + "' cannot be found.");
                        json.addProperty("success", false);
                        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                    }

                    if (!sp.isSecretValid(password))
                    {
                        json.addProperty("message", "Password for security profile '" + profile + "' is not valid.");
                        json.addProperty("success", false);
                        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                    }

                    spsToAdd.add(sp);
                }
            }

            List<SecurityProfile> spsToRemove = new ArrayList<>();
            if (!Utils.isBlank(removeSps))
            {
                String[] profiles = removeSps.split(",");
                for (String profile : profiles)
                {
                    String password = formParams.get(profile).get(0);

                    SecurityProfile sp = store.getSecurityProfile(user, repository, null, profile);
                    if (null == sp)
                        continue;

                    if (!sp.isSecretValid(password))
                    {
                        json.addProperty("message", "Password for security profile '" + profile + "' is not valid.");
                        json.addProperty("success", false);
                        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                    }

                    spsToRemove.add(sp);
                }
            }

            Long expirationTs = null;
            if (null != expires)
            {
                try
                {
                    Date dateObj = DateTimeUtils.dateFromTs(expires, new Date());
                    expirationTs = dateObj.getTime();
                }
                catch (Exception e) {
                    json.addProperty("message", "Invalid expiration date specified.");
                    json.addProperty("success", false);
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }
            }



            Token.ManagedBy managedBy;
            try {
                managedBy = Token.ManagedBy.valueOf(managedByName);
            } catch (Exception e) {
                json.addProperty("message", "Invalid specification of token's management.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            Token apiToken;
            if (null == id)
            {
                apiToken = new Token(repository,
                                     name,
                                     expirationTs,
                                     forceKeyPushEnabled,
                                     spsToAdd,
                                     rulesTeam,
                                     managingTeam,
                                     user,
                                     managedBy);
            }
            else
            {
                // ToDo -  Need to implement token updates for teams, and personal

                apiToken = store.getToken(repository, user, id);
                if (null == apiToken)
                {
                    json.addProperty("message", "Selected token is no longer available.");
                    json.addProperty("success", false);
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }

                apiToken.updateToken(active,
                                     name,
                                     expirationTs,
                                     forceKeyPushEnabled,
                                     spsToAdd,
                                     spsToRemove,
                                     rulesTeam,
                                     managingTeam,
                                     managedBy,
                                     newOwner,
                                     user);

            }

            store.begin();
            store.saveToken(user, apiToken);
            store.commit();

            json.addProperty("token", apiToken.getToken());
            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            store.rollback();

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
