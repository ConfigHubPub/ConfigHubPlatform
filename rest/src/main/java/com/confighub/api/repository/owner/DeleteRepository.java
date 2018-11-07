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

import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path( "/deleteRepository" )
public class DeleteRepository
      extends AOwnerAccessValidation
{
    @POST
    @Path( "/{account}/{repository}" )
    @Produces( "application/json" )
    public Response delete( @HeaderParam( "Authorization" ) String token,
                            @PathParam( "account" ) String account,
                            @PathParam( "repository" ) String repositoryName,
                            @FormParam( "password" ) String password )
    {
        JsonObject json = new JsonObject();
        Store store = new Store();

        try
        {
            int status = validate( account, repositoryName, token, store, true );
            if ( 0 != status )
            {
                return Response.status( status ).build();
            }

            user = store.login( user.getUsername(), password );

            store.begin();
            store.deleteRepository( repository, user );
            store.commit();

            json.addProperty( "success", true );
        }
        catch ( ConfigException e )
        {
            store.rollback();

            json.addProperty( "message", e.getErrorCode().getMessage() );
            json.addProperty( "success", false );
        }
        finally
        {
            store.close();
        }

        Gson gson = new Gson();
        return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
    }
}
