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

package com.confighub.api.audit;

import com.confighub.api.common.Common;
import com.confighub.api.common.Files;
import com.confighub.api.common.KVStore;
import com.confighub.api.repository.user.audit.GetRepositoryAudit;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.PropertyKey;
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
 * Test the Revisions "Search" box: @Path("/getRepositoryAudit")'s searchTerm matching against
 * property key names and file paths.
 */
public class RevisionSearchTest
{
    String accountName = "aUser";
    String accountPass = "password";
    String repoName = "RevisionSearchTest";
    String userToken = null;

    private JsonObject search(String recordTypes, String searchTerm)
    {
        GetRepositoryAudit auditAPI = new GetRepositoryAudit();
        Response response = auditAPI.get(accountName,
                                         repoName,
                                         recordTypes,
                                         50,
                                         0,
                                         0,
                                         false,
                                         null,
                                         searchTerm,
                                         userToken);
        assertEquals(200, response.getStatus());

        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        return json;
    }

    /**
     * Repro of the bug where searching for a key only surfaced the earliest matching revision:
     * a single property flipped false -> true -> false must return all 3 revisions, not just one.
     */
    @Test
    public void searchFindsEveryRevisionOfARepeatedlyChangedKey()
    {
        String context = Common.buildUIContextString(new String[]{ "", "", "" });

        Response response = KVStore.addOrUpdateProperty(accountName, repoName, userToken,
                                                        "Flag.NewFlag", "", false,
                                                        PropertyKey.ValueDataType.Boolean.name(), false,
                                                        "false", context, null, true, null, null, null);
        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        long propertyId = json.get("id").getAsLong();

        response = KVStore.addOrUpdateProperty(accountName, repoName, userToken,
                                               "Flag.NewFlag", "", false,
                                               PropertyKey.ValueDataType.Boolean.name(), false,
                                               "true", context, null, true, null, null, propertyId);
        json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        response = KVStore.addOrUpdateProperty(accountName, repoName, userToken,
                                               "Flag.NewFlag", "", false,
                                               PropertyKey.ValueDataType.Boolean.name(), false,
                                               "false", context, null, true, null, null, propertyId);
        json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        JsonObject result = search("Config", "NewFlag");
        JsonArray audit = result.get("audit").getAsJsonArray();

        // 1 revision for creating the key + property, plus 2 for the value flips
        assertEquals(3, audit.size());
    }

    @Test
    public void searchByFileNameFindsFileRevisionsButNotConfigRevisions()
    {
        String context = Common.buildUIContextString(new String[]{ "", "", "" });

        Response response = Files.saveOrUpdateFile(accountName, repoName, userToken,
                                                   "/config/prod", "searchable-app.properties", null,
                                                   "prop=value", context, true,
                                                   "Adding a new file", null, null, null, false, false);
        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        JsonObject filesResult = search("Files", "searchable-app");
        assertEquals(1, filesResult.get("audit").getAsJsonArray().size());

        JsonObject configResult = search("Config", "searchable-app");
        assertEquals(0, configResult.get("audit").getAsJsonArray().size());
    }

    @Test
    public void searchByFileNameFindsRenamedFileByOldOrNewName()
    {
        String context = Common.buildUIContextString(new String[]{ "", "", "" });

        Response response = Files.saveOrUpdateFile(accountName, repoName, userToken,
                                                   "/config/prod", "original-name.properties", null,
                                                   "prop=value", context, true,
                                                   "Adding a new file", null, null, null, false, false);
        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        long fileId = json.get("id").getAsLong();

        response = Files.saveOrUpdateFile(accountName, repoName, userToken,
                                          "/config/prod", "renamed.properties", fileId,
                                          "prop=value", context, true,
                                          "Renaming the file", null, null, null, false, false);
        json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        // Old name: matches both the original add and the rename commit
        assertEquals(2, search("Files", "original-name").get("audit").getAsJsonArray().size());

        // New name: matches only the rename commit
        assertEquals(1, search("Files", "renamed").get("audit").getAsJsonArray().size());
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
