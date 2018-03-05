/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Depth;
import com.confighub.core.store.Store;
import com.confighub.core.user.Account;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/createRepository")
public class CreateRepository
{

    @POST
    @Produces("application/json")
    public Response createRepository(@HeaderParam("Authorization") String token,
                                     @FormParam("owner") String accountName,
                                     @FormParam("name") String name,
                                     @FormParam("description") String description,
                                     @FormParam("private") boolean isPrivate,
                                     @FormParam("contextSize") int contextSize,
                                     @FormParam("labels") String labelList)
    {
        JsonObject json = new JsonObject();
        UserAccount user;
        Store store = new Store();

        try
        {
            user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            JsonObject errors = new JsonObject();
            boolean hasErrors = false;

            if (Utils.isBlank(name))
            {
                hasErrors = true;
                errors.addProperty("name", "Name required");
            }

            Depth d = null;
            try
            {
                d = Depth.getByIndex(contextSize);
            }
            catch (Exception ignore)
            {
            }

            if (null == d)
            {
                hasErrors = true;
                errors.addProperty("depth", "Context size not specified");
            }

            if (!hasErrors)
            {
                Account acc = store.getAccount(accountName);
                if (null == acc)
                    throw new ConfigException(Error.Code.ACCOUNT_NOT_FOUND);

                store.begin();

                Map<Depth, String> depthLabels = new HashMap<>();
                String[] labels = labelList.split(",");

                int i = contextSize;
                for (String label : labels)
                    depthLabels.put(Depth.getByIndex(i--), label);

                if (acc.isPersonal())
                {
                    if (!acc.getUser().equals(user))
                        throw new ConfigException(Error.Code.ACCOUNT_INVALID);

                    store.createRepository(name, description, d, isPrivate, user, depthLabels);

                } else
                {
                    store.createRepository(name, description, d, isPrivate, acc.getOrganization(), depthLabels, user);
                }

                store.commit();
                json.addProperty("success", true);
            } else
            {
                json.addProperty("success", false);
                json.add("errors", errors);
                json.addProperty("message", "Required fields are not filled.");
            }
        }
        catch (ConfigException e)
        {
            store.rollback();

            switch (e.getErrorCode())
            {
                case CONSTRAINT:
                    json.addProperty("message", "Repository name is already used in this account");
                    break;

                default:
                    json.addProperty("message", e.getErrorCode().getMessage());
            }


            json.addProperty("success", false);
        }
        finally
        {
            store.close();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}
