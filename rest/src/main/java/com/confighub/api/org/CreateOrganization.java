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

package com.confighub.api.org;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Organization;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/createOrganization")
public class CreateOrganization
{
    @POST
    @Path("/{accountName}")
    @Produces("application/json")
    public Response update(@PathParam("accountName") String accountName,
                           @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            if (Utils.isBlank(accountName))
            {
                json.addProperty("message", "Organization account name has to be specified.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            Organization organization = store.createOrganization(accountName, user);
            store.commit();

            json.addProperty("success", true);
            JsonObject jsonOrg = new JsonObject();

            jsonOrg.addProperty("un", organization.getAccountName());
            jsonOrg.addProperty("name", organization.getName());
            jsonOrg.addProperty("id", organization.getId());
            json.add("organization", jsonOrg);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getErrorCode().getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
