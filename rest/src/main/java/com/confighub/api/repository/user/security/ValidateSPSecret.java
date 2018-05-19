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

package com.confighub.api.repository.user.security;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.SecurityStore;
import com.confighub.core.utils.DateTimeUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

@Path("/validateSPSecret")
public class ValidateSPSecret
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(ValidateSPSecret.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("ts") Long ts,
                           @FormParam("spName") String spName,
                           @FormParam("spPassword") String spPassword)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        SecurityStore store = new SecurityStore();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();


            Date dateObj = DateTimeUtils.dateFromTs(ts, repository.getCreateDate());

            SecurityProfile ep = store.getSecurityProfile(user, repository, dateObj, spName);
            if (null == ep)
            {
                json.addProperty("message", "Could not find Value-Encryption profile");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            if (ep.isSecretValid(spPassword))
            {
                json.addProperty("success", true);
            }
            else
            {
                json.addProperty("message", "Invalid secret specified");
                json.addProperty("success", false);
            }
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
