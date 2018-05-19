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
