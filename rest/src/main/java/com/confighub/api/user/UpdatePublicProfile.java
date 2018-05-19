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

package com.confighub.api.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.Account;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/updatePublicProfile")
public class UpdatePublicProfile
{
    @POST
    @Produces("application/json")
    public Response get(@FormParam("account") String account,
                        @FormParam("name") String name,
                        @FormParam("company") String company,
                        @FormParam("website") String website,
                        @FormParam("city") String city,
                        @FormParam("country") String country,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            store.begin();
            user = store.updatePublicProfile(user, account, name, company, website, city, country);
            store.commit();

            json.addProperty("success", true);
            json.add("u", userJson(user));

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            e.printStackTrace();

            store.rollback();

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        finally
        {
            store.close();
        }
    }

    public static JsonObject userJson(UserAccount owner)
    {
        JsonObject json = new JsonObject();

        json.addProperty("t", "u"); // account type
        json.addProperty("un", owner.getUsername());
        json.addProperty("name", owner.getName());
        json.addProperty("orgs", owner.getOrganizationCount());
        json.addProperty("repos", owner.getRepositoryCount());
        json.addProperty("creationTs", owner.getCreateDate().getTime());
        json.addProperty("email", owner.getEmail());
        json.addProperty("blogSub", owner.isEmailBlog());
        json.addProperty("repoSub", owner.isEmailRepoCritical());

        Account account = owner.getAccount();
        json.addProperty("company", account.getCompany());
        json.addProperty("website", account.getWebsite());
        json.addProperty("city", account.getCity());
        json.addProperty("country", account.getCountry());

        return json;
    }
}
