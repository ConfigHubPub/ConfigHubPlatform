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

package com.confighub.api.common;

import com.confighub.api.auth.Login;
import com.confighub.api.auth.Signup;
import com.confighub.api.repository.admin.settings.UpdateRepositoryFeatures;
import com.confighub.api.repository.owner.DeleteRepository;
import com.confighub.api.user.CreateRepository;
import com.confighub.core.repository.Depth;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


/**
 * Common REST API operations used by unit tests.
 */
public class Common
{
    public static final Gson gson;

    public static final Logger log;

    static
    {
        System.setProperty( "log.path", "target/test-logs" );
        log = LogManager.getLogger( "Test" );
        gson = new Gson();
    }

    /**
     * @param username
     * @param password
     * @return
     */
    public static String createOrGetUser( String username,
                                          String password )
    {
        final Signup signupAPI = new Signup();
        Response response = signupAPI.signup( username + "@confighub.com", username, password );

        assertNotNull( response );
        assertEquals( 200, response.getStatus() );

        JsonObject json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        if ( !json.get( "success" ).getAsBoolean() )
        {
            final Login loginAPI = new Login();
            response = loginAPI.login( username, password );

            assertNotNull( response );
            assertNotNull( response.getEntity() );
            assertEquals( 200, response.getStatus() );
            json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
            assertNotNull( json );
        }

        assertNotNull( json.get( "token" ) );
        final String token = json.get( "token" ).getAsString();
        assertNotNull( token );

        return token;
    }


    /**
     * @param accountName
     * @param repoName
     * @param repoDescription
     * @param userToken
     * @param isPrivate
     * @param depth
     * @param labels
     * @return
     */
    public static Response createRepository( String accountName,
                                             String repoName,
                                             String repoDescription,
                                             String userToken,
                                             String accountPass,
                                             boolean isPrivate,
                                             Depth depth,
                                             String labels )
    {
        CreateRepository createRepositoryAPI = new CreateRepository();
        Response response = createRepositoryAPI.createRepository( userToken,
                                                                  accountName,
                                                                  repoName,
                                                                  repoDescription,
                                                                  isPrivate,
                                                                  depth.getIndex(),
                                                                  labels );
        assertNotNull( response );
        assertEquals( 200, response.getStatus() );

        {
            UpdateRepositoryFeatures repoUpdateAPI = new UpdateRepositoryFeatures();
            Response updateResponse = repoUpdateAPI.update( userToken,
                                                            accountName,
                                                            repoName,
                                                            accountPass,
                                                            false,
                                                            true,
                                                            true,
                                                            false,
                                                            false,
                                                            true,
                                                            true );

            assertNotNull( updateResponse );
            assertEquals( 200, updateResponse.getStatus() );
        }

        return response;
    }


    /**
     * @param userToken
     * @param accountName
     * @param repositoryName
     * @param accountPassword
     * @return
     */
    public static Response deleteRepository( final String userToken,
                                             final String accountName,
                                             final String repositoryName,
                                             final String accountPassword )
    {
        final DeleteRepository deleteRepositoryAPI = new DeleteRepository();
        Response response = deleteRepositoryAPI.delete( userToken, accountName, repositoryName, accountPassword );

        assertNotNull( response );
        assertEquals( 200, response.getStatus() );

        JsonObject json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertTrue( json.get( "success" ).getAsBoolean() );

        return response;
    }


    /**
     * 640:GSD ; 1280:Development ; 2560:360T                          ; 5120:360T-Commerzban
     * 640:GSD ; 1280:Development ; 2560:360T,AccretionHostDatFileSync ; 5120:360T-Commerzbank,360T-SEB
     * 640:GSD                    ; 2560:360T,AccretionHostDatFileSync ; 5120:360T-Commerzbank,360T-SEB
     *
     * @param context
     * @return
     */
    public static String buildUIContextString( String[] context )
    {
        List<String> levels = new ArrayList<>();

        int arrayIndex = 0;
        for ( int depthIndex = context.length; depthIndex > 0; depthIndex-- )
        {
            String elements = context[arrayIndex++];
            if ( Utils.isBlank( elements ) )
            {
                continue;
            }

            levels.add( String.format( "%d:%s", Depth.getByIndex( depthIndex ).getPlacement(), elements ) );
        }

        return Utils.join( levels, ";" );
    }
}
