/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user;

import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/userSearch")
public class UserSearch
{
    @GET
    @Produces("application/json")
    public Response getAssignments(@QueryParam("t") String searchTerm)
    {
        Store store = new Store();

        try
        {
            JsonArray jsonUsers = new JsonArray();

            searchTerm = searchTerm.contains(",") ? searchTerm.split(",")[0] : searchTerm;
            List<UserAccount> users = store.searchUsers(searchTerm, 10);
            if (null != users)
            {
                for (UserAccount user : users)
                {
                    JsonObject jsonUser = new JsonObject();
                    jsonUser.addProperty("un", user.getUsername());
                    jsonUser.addProperty("name", user.getName());
                    jsonUsers.add(jsonUser);
                }
            }

            Gson gson = new Gson();
            return Response.ok(gson.toJson(jsonUsers), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
