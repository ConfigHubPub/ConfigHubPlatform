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

package com.confighub.api.repository.user.files;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;


@Path( "/saveConfigFile" )
public class SaveConfigFile
      extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger( SaveConfigFile.class );


    @POST
    @Path( "/{account}/{repository}" )
    @Produces( "application/json" )
    public Response saveOrUpdate( @PathParam( "account" ) String account,
                                  @PathParam( "repository" ) String repositoryName,
                                  @HeaderParam( "Authorization" ) String token,
                                  @FormParam( "path" ) String path,
                                  @FormParam( "name" ) String name,
                                  @FormParam( "id" ) Long id,
                                  @FormParam( "content" ) String content,
                                  @FormParam( "context" ) String fileContext,
                                  @FormParam( "active" ) boolean active,
                                  @FormParam( "changeComment" ) String changeComment,
                                  @FormParam( "currentPassword" ) String currentPassword,
                                  @FormParam( "newProfilePassword" ) String newProfilePassword,
                                  @FormParam( "spName" ) String spName,
                                  @FormParam( "renameAll" ) boolean renameAll,
                                  @FormParam( "updateRefs" ) boolean updateRefs )
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validateWrite( account, repositoryName, token, store, true );
            if ( 0 != status )
            {
                return Response.status( status ).build();
            }

            store.begin();

            final Collection<CtxLevel> context = ContextParser.parseAndCreate( fileContext, repository, store, user, null );
            RepoFile file;

            if ( null != id && id > 0 )
            {
                file = store.updateRepoFile( user,
                                             repository,
                                             id,
                                             path,
                                             name,
                                             renameAll,
                                             updateRefs,
                                             content,
                                             context,
                                             active,
                                             spName,
                                             newProfilePassword,
                                             currentPassword,
                                             changeComment );
            }
            else
            {
                file = store.createRepoFile( user,
                                             repository,
                                             path,
                                             name,
                                             content,
                                             context,
                                             active,
                                             spName,
                                             newProfilePassword,
                                             changeComment );
            }
            store.commit();

            json.addProperty( "success", true );
            json.addProperty( "id", file.getId() );

            return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
        }
        catch ( ConfigException e )
        {
            store.rollback();

            switch ( e.getErrorCode() )
            {
                case FILE_CIRCULAR_REFERENCE:
                    json.add( "circularRef", e.getJson() );

                default:
                    json.addProperty( "status", "ERROR" );
            }

            json.addProperty( "message", e.getMessage() );
            json.addProperty( "success", false );

            return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
        }
        finally
        {
            store.close();
        }
    }
}