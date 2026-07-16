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

package com.confighub.api.editor;

import com.confighub.api.common.Common;
import com.confighub.api.common.KVStore;
import com.confighub.api.repository.user.editor.KeySearchDetails;
import com.confighub.core.repository.Depth;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

import static com.confighub.api.common.Common.gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test @Path("/keySearchDetails") - bulk key search returning id + creation date per match,
 * with a higher result cap than the autocomplete-oriented @Path("/keySearch").
 */
public class KeySearchDetailsTest
{
    String accountName = "aUser";
    String accountPass = "password";
    String repoName = "KeySearchDetailsTest";
    String userToken = null;

    private JsonObject search(String searchTerm, int max)
    {
        KeySearchDetails searchAPI = new KeySearchDetails();
        Response response = searchAPI.get(searchTerm, max, accountName, repoName, userToken);
        assertEquals(200, response.getStatus());

        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        return json;
    }

    @Test
    public void findsMatchingKeysWithCreationDateAndExcludesNonMatches()
    {
        long beforeCreate = System.currentTimeMillis();

        createKey("FeatureFlag.A");
        createKey("FeatureFlag.B");
        createKey("Other.Key");

        JsonObject result = search("FeatureFlag", 0);
        JsonArray keys = result.get("keys").getAsJsonArray();
        assertEquals(2, keys.size());

        Set<String> foundKeys = new HashSet<>();
        for (int i = 0; i < keys.size(); i++)
        {
            JsonObject key = keys.get(i).getAsJsonObject();
            foundKeys.add(key.get("key").getAsString());

            assertNotNull(key.get("id"));
            long createdOn = key.get("createdOn").getAsLong();
            assertTrue(createdOn >= beforeCreate);
            assertTrue(createdOn <= System.currentTimeMillis());
        }

        assertTrue(foundKeys.contains("FeatureFlag.A"));
        assertTrue(foundKeys.contains("FeatureFlag.B"));
    }

    @Test
    public void defaultCapIsHigherThanTheAutocompleteEndpoint()
    {
        for (int i = 0; i < 15; i++)
            createKey("BulkFlag." + i);

        // max <= 0 defaults to 100 here, vs the hardcoded 10 on /keySearch
        JsonObject result = search("BulkFlag", 0);
        assertEquals(15, result.get("keys").getAsJsonArray().size());
    }

    private void createKey(String key)
    {
        Response response = KVStore.addOrUpdateProperty(accountName, repoName, userToken,
                                                        key, "value",
                                                        Common.buildUIContextString(new String[]{ "", "", "" }));
        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
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
