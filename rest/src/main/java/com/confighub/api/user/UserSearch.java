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
