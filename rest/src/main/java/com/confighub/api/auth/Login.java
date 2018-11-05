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
    public Response login( @FormParam( "email" ) String login,
                           @FormParam( "password" ) String password )
    {
        Store store = new Store();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        try
        {
            UserAccount user = null;

            if ( Auth.isLdapEnabled() )
            {
                user = Auth.ldapAuth( login, password, store );
            }

            if ( null == user && Auth.isLocalAccountsEnabled() )
            {
                user = store.login( login, password );
            }

            if ( null == user )
            {
                throw new ConfigException( Error.Code.USER_AUTH );
            }

            json.addProperty( "token", Auth.createUserToken( user ) );
            json.addProperty( "success", true );

            return Response.ok( gson.toJson( json ), MediaType.APPLICATION_JSON ).build();
        }
        catch ( ConfigException e )
        {
            e.printStackTrace();

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