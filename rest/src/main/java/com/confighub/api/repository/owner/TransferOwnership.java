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

package com.confighub.api.repository.owner;

import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.store.Store;
import com.confighub.core.user.Account;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/transferOwnership")
public class TransferOwnership
        extends AOwnerAccessValidation
{
    private static final Logger log = LogManager.getLogger(TransferOwnership.class);

    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response update(@FormParam("toAccount") String toAccount,
                           @PathParam("account") String fromAccount,
                           @PathParam("repository") String repositoryName,
                           @FormParam("password") String password,
                           @HeaderParam("Authorization") String token)
    {

        log.info("Transferring ownership from " + fromAccount + " to " + toAccount + " repo: " + repositoryName);
        Gson gson = new Gson();
        JsonObject json = new JsonObject();

        if (Utils.anyBlank(toAccount, fromAccount, repositoryName))
        {
            json.addProperty("success", false);
            json.addProperty("message", "Missing required field.");
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }

        Store store = new Store();

        try
        {
            int status = validate(fromAccount, repositoryName, token, store, true);
            log.info("status: " + status);
            if (0 != status)
                return Response.status(status).build();

            // If user is not re-authed, store will throw an exception
            user = store.login(user.getUsername(), password);

            Account newAccount = store.getAccount(toAccount);
            if (null == newAccount)
            {
                json.addProperty("message", "Unable to find the account you want to transfer the ownership to.");
                json.addProperty("success", false);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            store.transferOwnership(repository, user, newAccount);
            store.commit();

            json.addProperty("success", true);
            if (repository.hasWriteAccess(user))
                json.add("repository", GsonHelper.repositoryToJSON(repository));

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        catch (ConfigException e)
        {
            store.rollback();

            if (Error.Code.CONSTRAINT.equals(e.getErrorCode()))
            {
                json.addProperty("message",
                                 "This repository cannot be received by '" +
                                 toAccount +
                                 "'.  This account might already have a repository with the same name.");
            }
            else
            {
                json.addProperty("message", e.getErrorCode().getMessage());
            }

            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}

