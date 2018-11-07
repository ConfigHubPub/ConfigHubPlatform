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

package com.confighub.api.repository;

import com.confighub.api.common.Common;
import com.confighub.api.repository.user.editor.RepositoryInfo;
import com.confighub.core.repository.Depth;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.confighub.api.common.Common.gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Basic sunny-day repository operations.
 */
public class RepositorySetupTest
{
    String accountName = "aUser";

    String accountPass = "password";

    String repoName = "aRepo";

    String userToken = null;


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


    @Test
    public void privateRepoDefaultSettings()
    {
        RepositoryInfo repositoryInfoAPI = new RepositoryInfo();
        Response response = repositoryInfoAPI.get( accountName, repoName, userToken );

        assertNotNull( response );
        assertEquals( 200, response.getStatus() );

        String responseString = (String) response.getEntity();
        JsonObject json = gson.fromJson( responseString, JsonObject.class );
        assertEquals( repoName, json.get( "name" ).getAsString() );

        assertEquals( true, json.get( "isPrivate" ).getAsBoolean() );
        assertEquals( true, json.get( "isPersonal" ).getAsBoolean() );

        assertEquals( false, json.get( "accessControlEnabled" ).getAsBoolean() );
        assertEquals( true, json.get( "securityProfilesEnabled" ).getAsBoolean() );
        assertEquals( true, json.get( "valueTypeEnabled" ).getAsBoolean() );
        assertEquals( false, json.get( "contextClustersEnabled" ).getAsBoolean() );
        assertEquals( false, json.get( "adminContextControlled" ).getAsBoolean() );
        assertEquals( false, json.get( "demo" ).getAsBoolean() );
        assertEquals( "owner", json.get( "ut" ).getAsString() );
    }


    @Test
    public void duplicateRepoName()
    {
        Response response = Common.createRepository( accountName,
                                                     repoName,
                                                     "Description",
                                                     userToken,
                                                     accountPass,
                                                     true,
                                                     Depth.D2,
                                                     "Environment,Application,Instance" );
        assertEquals( 200, response.getStatus() );
        JsonObject json = gson.fromJson( (String) response.getEntity(), JsonObject.class );
        assertFalse( json.get( "success" ).getAsBoolean() );
    }
}