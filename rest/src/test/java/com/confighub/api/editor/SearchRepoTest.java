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
import com.confighub.api.repository.user.editor.SearchRepo;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.PropertyKey;
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
import static org.junit.Assert.assertTrue;

/**
 * Test the Properties page "All" search's "key=value" syntax: @Path("/searchRepo") should
 * scope a value match to a specific key (AND semantics) instead of matching keys/values independently.
 */
public class SearchRepoTest
{
    String accountName = "aUser";
    String accountPass = "password";
    String repoName = "SearchRepoTest";
    String userToken = null;

    private Set<String> searchKeys(String searchTerm, String valueTerm)
    {
        SearchRepo searchAPI = new SearchRepo();
        Response response = searchAPI.get(accountName, repoName, null, null, searchTerm, valueTerm, userToken);
        assertEquals(200, response.getStatus());

        JsonObject json = gson.fromJson((String) response.getEntity(), JsonObject.class);
        JsonArray config = json.get("config").getAsJsonArray();

        Set<String> keys = new HashSet<>();
        config.forEach(el -> keys.add(el.getAsJsonObject().get("key").getAsString()));
        return keys;
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

        String context = Common.buildUIContextString(new String[]{ "", "", "" });

        for (String[] kv : new String[][]{{ "Flag.A", "true" }, { "Flag.B", "true" }, { "Flag.C", "false" }})
        {
            response = KVStore.addOrUpdateProperty(accountName, repoName, userToken,
                                                   kv[0], "", false,
                                                   PropertyKey.ValueDataType.Boolean.name(), false,
                                                   kv[1], context, null, true, null, null, null);
            json = gson.fromJson((String) response.getEntity(), JsonObject.class);
            assertTrue(json.get("success").getAsBoolean());
        }
    }

    @Test
    public void keyEqualsValueScopesToThatKeyAndValue()
    {
        assertEquals(new HashSet<>(java.util.Arrays.asList("Flag.A")), searchKeys("Flag.A", "true"));
    }

    @Test
    public void keyEqualsMismatchedValueReturnsNothing()
    {
        assertEquals(0, searchKeys("Flag.A", "false").size());
    }

    @Test
    public void blankKeyWithValueMatchesAnyKeyWithThatValue()
    {
        assertEquals(new HashSet<>(java.util.Arrays.asList("Flag.A", "Flag.B")), searchKeys("", "true"));
    }

    @After
    public void cleanup()
    {
        Common.deleteRepository(userToken, accountName, repoName, accountPass);
    }
}
