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
import com.confighub.core.security.CipherTransformation;
import com.confighub.core.store.SecurityStore;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/createSecurityProfile")
public class CreateSecurityProfile
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(CreateSecurityProfile.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response create(@HeaderParam("Authorization") String token,
                           @PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @FormParam("name") String name,
                           @FormParam("password") String password,
                           @FormParam("password2") String password2,
                           @FormParam("cipher") String cipher)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        SecurityStore store = new SecurityStore();

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            if (!Utils.same(password, password2))
            {
                json.addProperty("message", "Passwords do not match.");
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            CipherTransformation ct = null;
            if (!Utils.isBlank(cipher))
                ct = CipherTransformation.get(cipher);

            store.begin();
            store.createEncryptionProfile(user, repository, name, password, ct);
            store.commit();

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

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
