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

package com.confighub.api.audit;

import com.confighub.api.common.Common;
import com.confighub.api.common.Files;
import com.confighub.api.common.KVStore;
import com.confighub.api.repository.admin.settings.UpdateRepositoryFeatures;
import com.confighub.api.repository.client.v1.APIPull;
import com.confighub.api.repository.user.tags.CreateTag;
import com.confighub.api.repository.user.tokens.SaveOrUpdateToken;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.security.Token;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.confighub.api.common.Common.gson;
import static org.junit.Assert.*;

/**
 *
 */
public class AuditPullTest
{
    String accountName = "aUser";
    String accountPass = "password";
    String repoName = "AuditPullTest";
    String userToken = null;

    @Test
    public void pullByTagTest()
    {
        JsonObject json;
        Response response;

        // --------------------------------------------------------------------------
        // 1. Create a token
        // --------------------------------------------------------------------------
        SaveOrUpdateToken tokenAPI = new SaveOrUpdateToken();
        response = tokenAPI.create(accountName,
                                   repoName,
                                   null,
                                   null,
                                   "MyToken",
                                   true,
                                   null,
                                   true,
                                   null,
                                   null,
                                   null,
                                   null,
                                   Token.ManagedBy.User.name(),
                                   accountName,
                                   userToken);

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        String token = json.get("token").getAsString();
        assertNotNull(token);

        // --------------------------------------------------------------------------
        // 2. Create a few properties, and change them 3 times.  At each interval,
        //    create a new tag.
        // --------------------------------------------------------------------------
        Map<String, Long> entries = new HashMap<>();
        Long fileId = null;
        entries.put("db.admin", null);
        entries.put("db.passwords", null);
        entries.put("url", null);

        int timeSegments = 3;

        // Stores times at which tags are created.  Will be used to validate audit data using
        // date as well as tag
        long[] times = new long[timeSegments];


        for (int i = 0; i < timeSegments; i++)
        {
            try
            {
                TimeUnit.SECONDS.sleep(1);
            }
            catch (Exception ignore)
            {
            }

            for (String key : entries.keySet())
            {
                response = KVStore.addOrUpdateProperty(accountName,
                                                       repoName,
                                                       userToken,
                                                       key,
                                                       PropertyKey.ValueDataType.Text.name(),
                                                       key + "_" + i,
                                                       Common.buildUIContextString(new String[]{ "", "", "" }),
                                                       entries.get(key));

                assertEquals(200, response.getStatus());
                json = gson.fromJson((String)response.getEntity(), JsonObject.class);
                assertTrue(json.get("success").getAsBoolean());
                entries.put(key, json.get("id").getAsLong());

                response = Files.saveOrUpdateFile(accountName,
                                                  repoName,
                                                  userToken,
                                                  "/var/test",
                                                  "file.xml",
                                                  fileId,
                                                  "file content: " + i,
                                                  Common.buildUIContextString(new String[]{ "", "", "" }),
                                                  true,
                                                  "Updating a file",
                                                  null,
                                                  null,
                                                  null,
                                                  false,
                                                  false);

                json = gson.fromJson((String)response.getEntity(), JsonObject.class);
                assertTrue(json.get("success").getAsBoolean());
                assertTrue(json.has("id"));
                fileId = json.get("id").getAsLong();
            }

            try
            {
                TimeUnit.SECONDS.sleep(1);
            }
            catch (Exception ignore)
            {
            }

            times[i] = System.currentTimeMillis();

            CreateTag createTagAPI = new CreateTag();
            response = createTagAPI.create(userToken, accountName, repoName, "tag_" + i, null, times[i]);
            assertEquals(200, response.getStatus());
            json = gson.fromJson((String)response.getEntity(), JsonObject.class);
            assertTrue(json.get("success").getAsBoolean());

        }


        // --------------------------------------------------------------------------
        // 3. API Pull config using tags
        // --------------------------------------------------------------------------

        for (int i = 0; i < timeSegments; i++)
        {
            APIPull pullAPI = new APIPull();
            response = pullAPI.get(token,
                                   "Development;Default;Default",
                                   null,
                                   "tag_" + i,
                                   null,
                                   null,
                                   null,
                                   false,
                                   null,
                                   false,
                                   false,
                                   false,
                                   false);
            assertEquals(200, response.getStatus());
            json = gson.fromJson((String)response.getEntity(), JsonObject.class);

            JsonObject props = json.get("properties").getAsJsonObject();
            assertEquals(3, props.size());

            for (String key : entries.keySet())
                assertEquals(key + "_" + i, props.get(key).getAsJsonObject().get("val").getAsString());

            JsonObject files = json.get("files").getAsJsonObject();
            assertEquals(1, files.size());

            assertEquals("file content: " + i,
                         files.get("/var/test/file.xml").getAsJsonObject().get("content").getAsString());
        }

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

        JsonObject json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        // Enable access controls for this repository
        UpdateRepositoryFeatures repoUpdateAPI = new UpdateRepositoryFeatures();
        Response updateResponse = repoUpdateAPI.update(userToken,
                                                       accountName,
                                                       repoName,
                                                       accountPass,
                                                       false,
                                                       true,
                                                       true,
                                                       false,
                                                       false,
                                                       false,
                                                       false);

        assertNotNull(updateResponse);
        assertEquals(200, updateResponse.getStatus());

    }

    @After
    public void cleanup()
    {
        Common.deleteRepository(userToken, accountName, repoName, accountPass);
    }

}
