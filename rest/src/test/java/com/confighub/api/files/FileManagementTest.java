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

package com.confighub.api.files;

import com.confighub.api.common.Common;
import com.confighub.api.common.Files;
import com.confighub.api.common.KVStore;
import com.confighub.api.repository.client.v1.APIPullFile;
import com.confighub.core.repository.Depth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

import static com.confighub.api.common.Common.gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test file creation, editing, deletion operations
 */
public class FileManagementTest
{
    String accountName = "aUser";

    String accountPass = "password";

    String repoName = "FileManagementTest";

    String userToken = null;


    @Test
    public void sunnyDay()
    {
        // Create a file
        Response response = Files.saveOrUpdateFile( accountName,
                                                    repoName,
                                                    userToken,
                                                    "/var/test",
                                                    "sunnyDay1.xml",
                                                    null,
                                                    Files.readLocalFile( this.getClass(), "sunnyDay1.xml" ),
                                                    Common.buildUIContextString( new String[] { "Development",
                                                                                                "",
                                                                                                "MyApp" } ),
                                                    true,
                                                    "Adding a new file for FileManagementTest",
                                                    null,
                                                    null,
                                                    null,
                                                    false,
                                                    false );

        JsonObject json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertTrue( json.get( "success" ).getAsBoolean() );

        // -------------------------------------------------------------------- //
        // Get all files
        // -------------------------------------------------------------------- //
        response = Files.getRepositoryFiles( accountName,
                                             repoName,
                                             "",
                                             true,
                                             null,
                                             null,
                                             null,
                                             true,
                                             userToken );

        json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertTrue( json.get( "success" ).getAsBoolean() );

        // -------------------------------------------------------------------- //
        // Validate newly minted file is returned, and its defined as expected
        // -------------------------------------------------------------------- //
        JsonArray data = json.get( "data" ).getAsJsonArray();
        for ( JsonElement el : data )
        {
            JsonObject element = el.getAsJsonObject();
            if ( "var/test".equals( element.get( "path" ).getAsString() ) )
            {
                JsonArray fileList = element.get( "files" ).getAsJsonArray();
                assertEquals( 1, fileList.size() );

                JsonObject file = fileList.get( 0 ).getAsJsonObject();
                assertEquals( "sunnyDay1.xml", file.get( "name" ).getAsString() );
                assertEquals( "var/test/sunnyDay1.xml", file.get( "fullPath" ).getAsString() );
                assertTrue( file.get( "active" ).getAsBoolean() );
                assertTrue( file.get( "editable" ).getAsBoolean() );

                // check context
                JsonArray context = file.get( "levels" ).getAsJsonArray();
                assertEquals( 3, context.size() );

                for ( JsonElement contextEl : context )
                {
                    JsonObject contextElement = contextEl.getAsJsonObject();
                    int p = contextElement.get( "p" ).getAsInt();
                    Depth depth = Depth.getByPlacement( p );

                    switch ( depth )
                    {
                        case D2:
                            assertEquals( "Development", contextElement.get( "n" ).getAsString() );
                            assertEquals( Depth.D2.getPlacement(), contextElement.get( "w" ).getAsInt() );
                            break;

                        case D1:
                            assertFalse( contextElement.has( "n" ) );
                            assertFalse( contextElement.has( "w" ) );
                            break;

                        case D0:
                            assertEquals( "MyApp", contextElement.get( "n" ).getAsString() );
                            assertEquals( Depth.D0.getPlacement(), contextElement.get( "w" ).getAsInt() );
                            break;

                        default:
                            fail( "Unexpected depth specified" );
                            break;
                    }
                }
            }
        }

        // -------------------------------------------------------------------- //
        // We have declared 3 keys in the file.  Check they are defined.
        // -------------------------------------------------------------------- //
        response = KVStore.getUIConfig( accountName,
                                        repoName,
                                        "",
                                        null,
                                        true,
                                        null,
                                        false, userToken );

        assertEquals( 200, response.getStatus() );

        json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        JsonArray config = json.get( "config" ).getAsJsonArray();

        assertEquals( 3, config.size() );
        Set<String> expectedKeys = new HashSet<>();
        expectedKeys.add( "username" );
        expectedKeys.add( "password" );
        expectedKeys.add( "db.host" );

        for ( JsonElement el : config )
        {
            expectedKeys.remove( el.getAsJsonObject().get( "key" ).getAsString() );
        }

        assertEquals( 0, expectedKeys.size() );

        // -------------------------------------------------------------------- //
        // Add values to the keys
        // -------------------------------------------------------------------- //
        response = KVStore.addOrUpdateProperty( accountName, repoName, userToken,
                                                "username", "admin",
                                                Common.buildUIContextString( new String[] { "",
                                                                                            "",
                                                                                            "" } ) );
        json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertEquals( 200, response.getStatus() );
        assertTrue( json.get( "success" ).getAsBoolean() );

        response = KVStore.addOrUpdateProperty( accountName, repoName, userToken,
                                                "db.host", "127.0.0.1",
                                                Common.buildUIContextString( new String[] { "",
                                                                                            "",
                                                                                            "" } ) );
        json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertEquals( 200, response.getStatus() );
        assertTrue( json.get( "success" ).getAsBoolean() );

        response = KVStore.addOrUpdateProperty( accountName, repoName, userToken,
                                                "password", "s!e@c#r$e%t^P&a*s(s)w{o}r|d\"':;?/>.<,~`",
                                                Common.buildUIContextString( new String[] { "",
                                                                                            "",
                                                                                            "" } ) );
        json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertEquals( 200, response.getStatus() );
        assertTrue( json.get( "success" ).getAsBoolean() );

        // -------------------------------------------------------------------- //
        // Pull resolved file
        // -------------------------------------------------------------------- //
        APIPullFile fileAPI = new APIPullFile();
        response = fileAPI.get( accountName,
                                repoName,
                                "Development;Default;MyApp",
                                "var/test/sunnyDay1.xml",
                                null,
                                null,
                                null,
                                "SunnyDay File Test",
                                null,
                                "127.0.0.1" );

        assertEquals( 200, response.getStatus() );
        String resolvedFile = (String) response.getEntity();
        String expectedFile = Files.readLocalFile( this.getClass(), "sunnyDay1_resolved.xml" );

        assertEquals( expectedFile, resolvedFile );


        // Update it


        // Delete it
    }


    @Before
    public void setup()
    {
        userToken = Common.createOrGetUser( accountName, accountPass );

        Response response = Common.createRepository( accountName,
                                                     repoName,
                                                     "Description",
                                                     userToken,
                                                     accountPass,
                                                     true,
                                                     Depth.D2,
                                                     "Environment,Application,Instance" );

        JsonObject json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertTrue( json.get( "success" ).getAsBoolean() );
    }


    @After
    public void cleanup()
    {
        Common.deleteRepository( userToken, accountName, repoName, accountPass );
    }
}
