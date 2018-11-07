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
import com.confighub.core.error.Error;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
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


@Path( "/login" )
@Produces( "application/json" )
public class Login
{
    private static final Logger log = LogManager.getLogger( Login.class );


    @AuthenticationNotRequired
    @POST
    public Response login( @FormParam( "email" ) String username,
                           @FormParam( "password" ) String password )
    {
        Store store = new Store();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        try
        {
            UserAccount user = store.getUserAccount( username );
            if ( null == user )
            {
                // if a user is not stored in local DB, the only option is to check
                // if they can auth via LDAP.
                if ( Auth.isLdapEnabled() )
                {
                    user = Auth.ldapAuth( username, password, true, store );
                }
                else
                {
                    throw new ConfigException( Error.Code.USER_AUTH );
                }
            }
            else
            {
                switch ( user.getAccountType() )
                {
                    case LDAP:
                        if ( Auth.isLdapEnabled() )
                        {
                            Auth.ldapAuth( username, password, false, store );
                        }
                        else
                        {
                            throw new ConfigException( Error.Code.USER_AUTH );
                        }
                        break;

                    case LOCAL:
                        if ( Auth.isLocalAccountsEnabled() )
                        {
                            store.login( username, password );
                        }
                        else
                        {
                            throw new ConfigException( Error.Code.USER_AUTH );
                        }
                        break;
                }
            }

            if ( null == user )
            {
                throw new ConfigException( Error.Code.USER_AUTH );
            }

            final String token = Auth.createUserToken( user );
            json.addProperty( "token", token );
            json.addProperty( "success", true );

            return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
        }
        catch ( ConfigException e )
        {
            json.addProperty( "success", false );
            json.addProperty( "message", e.getMessage() );

            return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
        }
        finally
        {
            store.close();
        }
    }
}