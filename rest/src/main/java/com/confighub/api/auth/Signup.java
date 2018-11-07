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

package com.confighub.api.auth;

import com.confighub.api.server.AuthenticationNotRequired;
import com.confighub.core.auth.Auth;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;


@Path( "/signup" )
@Produces( "application/json" )
public class Signup
{

    private static final Logger log = LogManager.getLogger( Signup.class );


    @AuthenticationNotRequired
    @POST
    public Response signup( @FormParam( "email" ) String email,
                            @FormParam( "username" ) String username,
                            @FormParam( "password" ) String password )
    {
        Store store = new Store();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        try
        {
            // -------------------------------------------------------------------------------------------- //
            // Form validation
            // --------------------------------------------------------------------------------------------
            Map<String, String> errors = new HashMap<>();

            if ( Utils.isBlank( email ) )
            {
                errors.put( "error_email", "Please enter email address" );
            }

            if ( Utils.isBlank( username ) )
            {
                errors.put( "error_username", "Username required" );
            }

            if ( Utils.isBlank( password ) )
            {
                errors.put( "error_password", "Please enter password" );
            }

            if ( store.isEmailRegistered( email ) )
            {
                errors.put( "message", "Email is already registered" );
            }

            if ( store.isUsernameTaken( username ) )
            {
                errors.put( "error_username", "Username is already registered" );
            }


            if ( errors.size() != 0 )
            {
                for ( String k : errors.keySet() )
                {
                    json.addProperty( k, errors.get( k ) );
                }
                json.addProperty( "success", false );

                return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
            }

            store.begin();
            UserAccount user = store.createUser( email, username, password, UserAccount.AccountType.LOCAL );
            store.commit();

            final String token = Auth.createUserToken( user );
            json.addProperty( "token", token );
            json.addProperty( "success", true );

            return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
        }
        catch ( ConfigException e )
        {
            System.out.println( "---------------------------" );
            e.printStackTrace();
            System.out.println( "---------------------------" );

            store.rollback();
            json.addProperty( "success", false );
            json.addProperty( "message", e.getErrorCode().getMessage() );

            return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
        }
        finally
        {
            store.close();
        }
    }
}
