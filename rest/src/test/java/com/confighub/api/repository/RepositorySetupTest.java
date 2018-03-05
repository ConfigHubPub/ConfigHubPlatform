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
import static org.junit.Assert.*;

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

    }

    @After
    public void cleanup()
    {
        Common.deleteRepository(userToken, accountName, repoName, accountPass);
    }

    @Test
    public void privateRepoDefaultSettings()
    {
        RepositoryInfo repositoryInfoAPI = new RepositoryInfo();
        Response response = repositoryInfoAPI.get(accountName, repoName, userToken);

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        String responseString = (String)response.getEntity();
        JsonObject json = gson.fromJson(responseString, JsonObject.class);
        assertEquals(repoName, json.get("name").getAsString());

        assertEquals(true, json.get("isPrivate").getAsBoolean());
        assertEquals(true, json.get("isPersonal").getAsBoolean());

        assertEquals(false, json.get("accessControlEnabled").getAsBoolean());
        assertEquals(true, json.get("securityProfilesEnabled").getAsBoolean());
        assertEquals(true, json.get("valueTypeEnabled").getAsBoolean());
        assertEquals(false, json.get("contextClustersEnabled").getAsBoolean());
        assertEquals(false, json.get("adminContextControlled").getAsBoolean());
        assertEquals(false, json.get("demo").getAsBoolean());
        assertEquals("owner", json.get("ut").getAsString());
    }

    @Test
    public void duplicateRepoName()
    {
        Response response = Common.createRepository(accountName,
                                           repoName,
                                           "Description",
                                           userToken,
                                           accountPass,
                                           true,
                                           Depth.D2,
                                           "Environment,Application,Instance");
        assertEquals(200, response.getStatus());
        JsonObject json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
    }
}