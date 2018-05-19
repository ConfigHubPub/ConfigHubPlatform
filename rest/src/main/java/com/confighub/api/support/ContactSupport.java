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

package com.confighub.api.support;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.error.ConfigException;
import com.confighub.core.mail.SendMail;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/contactSupport")
@Produces("application/json")
public class ContactSupport
{
    @POST
    public Response send(@HeaderParam("Authorization") String token,
                         @FormParam("name") String name,
                         @FormParam("email") String email,
                         @FormParam("subject") String subject,
                         @FormParam("message") String message)
    {
        Store store = new Store();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        try
        {
            store.begin();

            UserAccount user = TokenState.getUser(token, store);
            SendMail.contactSupport(user, name, email, subject, message);

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            json.addProperty("success", false);
            json.addProperty("message", e.getErrorCode().getMessage());

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
