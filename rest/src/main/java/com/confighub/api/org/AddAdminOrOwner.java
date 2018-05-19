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

import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/addAdminOrOwner")
public class AddAdminOrOwner
        extends AOrgOwnerAccessValidation
{
    @POST
    @Path("/{orgAccName}")
    @Produces("application/json")
    public Response update(@PathParam("orgAccName") String orgAccName,
                           @FormParam("un") String newOwnerName,
                           @FormParam("role") String role,
                           @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        if (Utils.anyBlank(orgAccName, newOwnerName, role) ||
            !(role.equalsIgnoreCase("own") || role.equalsIgnoreCase("adm")))
        {
            json.addProperty("success", false);
            json.addProperty("message", "Missing required field.");
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }

        try
        {
            UserAccount newOwner = store.getUserByUsername(newOwnerName);
            if (null == newOwner)
            {
                json.addProperty("success", false);
                json.addProperty("message", "Requested user not found.");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            int status = validate(orgAccName, token, store);
            if (0 != status) return Response.status(status).build();

            store.begin();

            if (role.equals("adm"))
                organization.addAdministrator(newOwner);
            else
                organization.addOwner(newOwner);

            store.update(organization, user);
            store.commit();

            JsonObject owner = new JsonObject();
            owner.addProperty("un", newOwner.getUsername());
            owner.addProperty("name", newOwner.getName());
            owner.addProperty("email", newOwner.getEmail());

            json.add("no", owner);
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
