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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.confighub.api.files;

import com.confighub.api.common.Common;
import com.confighub.api.common.Files;
import com.confighub.api.common.KVStore;
import com.confighub.api.repository.user.files.GetKeyFiles;
import com.confighub.core.repository.Depth;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.confighub.api.common.Common.gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test @Path("/getKeyFiles") - resolving which files reference a given property key.
 */
public class GetKeyFilesTest
{
    String accountName = "aUser";
    String accountPass = "password";
    String repoName = "GetKeyFilesTest";
    String userToken = null;

    @Test
    public void keyUsedInFile()
    {
        Response response = KVStore.addOrUpdateProperty(accountName, repoName, userToken,
                                                        "key1", "value1",
                                                        Common.buildUIContextString(new String[]{ "", "", "" }));
        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        response = Files.saveOrUpdateFile(accountName,
                                          repoName,
                                          userToken,
                                          "/config",
                                          "app.properties",
                                          null,
                                          "prop=${key1}",
                                          Common.buildUIContextString(new String[]{ "", "", "" }),
                                          true,
                                          "Adding a new file",
                                          null,
                                          null,
                                          null,
                                          false,
                                          false);
        json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        GetKeyFiles keyFilesAPI = new GetKeyFiles();
        response = keyFilesAPI.get(accountName, repoName, "key1", userToken);
        assertEquals(200, response.getStatus());

        json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        JsonArray files = json.get("files").getAsJsonArray();
        assertEquals(1, files.size());
        assertEquals("config/app.properties", files.get(0).getAsJsonObject().get("fullPath").getAsString());
    }

    @Test
    public void keyNotUsedInAnyFile()
    {
        Response response = KVStore.addOrUpdateProperty(accountName, repoName, userToken,
                                                        "unusedKey", "value1",
                                                        Common.buildUIContextString(new String[]{ "", "", "" }));
        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        GetKeyFiles keyFilesAPI = new GetKeyFiles();
        response = keyFilesAPI.get(accountName, repoName, "unusedKey", userToken);
        assertEquals(200, response.getStatus());

        json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(0, json.get("files").getAsJsonArray().size());
    }

    @Test
    public void nonExistentKey()
    {
        GetKeyFiles keyFilesAPI = new GetKeyFiles();
        Response response = keyFilesAPI.get(accountName, repoName, "doesNotExist", userToken);
        assertEquals(200, response.getStatus());

        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(0, json.get("files").getAsJsonArray().size());
    }

    @Before
    public void setup()
    {
        userToken = Common.createOrGetUser(accountName, accountPass);

        Response response = Common.createRepository(accountName,
                                                    repoName,
                                                    "Description",
                                                    userToken,
                                                    accountPass,
                                                    true,
                                                    Depth.D2,
                                                    "Environment,Application,Instance");

        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
    }

    @After
    public void cleanup()
    {
        Common.deleteRepository(userToken, accountName, repoName, accountPass);
    }
}
