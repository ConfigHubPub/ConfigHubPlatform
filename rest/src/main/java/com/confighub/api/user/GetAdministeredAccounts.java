/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.organization.Organization;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/getAdministeredAccounts")
public class GetAdministeredAccounts
{

    @GET
    @Produces("application/json")
    public Response get(@HeaderParam("Authorization") String token)
    {
        Store store = new Store();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            JsonArray json = new JsonArray();

            JsonObject i = new JsonObject();
            i.addProperty("un", user.getUsername());
            i.addProperty("type", "user");
            json.add(i);

            Set<Organization> orgs = user.getOrganizations();
            if (null != orgs)
            {
                for (Organization o : orgs)
                {
                    i = new JsonObject();
                    i.addProperty("un", o.getAccountName());
                    i.addProperty("type", "org");
                    json.add(i);
                }
            }

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

    }
}
