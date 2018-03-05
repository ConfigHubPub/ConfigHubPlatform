package com.confighub.api.tokens;

import com.confighub.api.common.Common;
import com.confighub.api.common.Files;
import com.confighub.api.common.KVStore;
import com.confighub.api.repository.admin.accessRules.CreateAccessRule;
import com.confighub.api.repository.admin.accessRules.UpdateStopOnFirstMatchedRule;
import com.confighub.api.repository.admin.accessRules.UpdateUnmatchedEditableRule;
import com.confighub.api.repository.admin.settings.UpdateRepositoryFeatures;
import com.confighub.api.repository.admin.users.AddMember;
import com.confighub.api.repository.admin.users.CreateTeam;
import com.confighub.api.repository.user.team.TeamInfo;
import com.confighub.api.repository.client.v1.APIPull;
import com.confighub.api.repository.client.v1.APIPush;
import com.confighub.api.repository.user.tokens.SaveOrUpdateToken;
import com.confighub.api.repository.user.tokens.TokenFetchAll;
import com.confighub.core.repository.Depth;
import com.confighub.core.security.Token;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.confighub.api.common.Common.gson;
import static org.junit.Assert.*;

/**
 *
 */
public class TokenTest
{
    String accountName = "aUser";
    String accountPass = "password";
    String repoName = "TokenTest";
    String userToken = null;

    /**
     * The objective of the test is to exercise private token functionality
     *
     * Test Steps:
     *
     * 1. Create repo and add config
     * 2. Disable token-less API push
     * 3. Create a team and add access controls
     * 4. Create a new user, and assign to the team
     * 5. Team member pulls config via UI - check correct UI indications for editable values
     * 6. Create a personal token for teamMember account and try and update access restricted value
     * 7. Make sure team member can pull configuration via API
     * 8. Try and change a Production value
     * 9. Change a Non-Production value
     * 10. Create a Production file
     * 11. Create a Non-Production file
     *
     */
    @Test
    public void userTokenSetup()
    {
        Response response;
        JsonObject json;
        boolean debugTest = false;

        // --------------------------------------------------------------------------
        // 1. Create repo and add config
        // --------------------------------------------------------------------------

        // Upload configuration to the repository
        String fileContent = Files.readLocalFile(this.getClass(), "repoConfigKV.json");
        APIPush apiPush = new APIPush();
        response = apiPush.pushStructuredData(accountName,
                                              repoName,
                                              null,
                                              "TokenTest",
                                              null,
                                              "localhost",
                                              fileContent);
        assertEquals(200, response.getStatus());

        // Make sure configuration is there
        response = KVStore.getUIConfig(accountName,
                                       repoName,
                                       "",
                                       null,
                                       true,
                                       null,
                                       false,
                                       userToken);

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        JsonArray config = json.get("config").getAsJsonArray();
        assertEquals(6, config.size());


        // --------------------------------------------------------------------------
        // 2. Disable token-less API push
        // --------------------------------------------------------------------------
        UpdateRepositoryFeatures repoUpdateAPI = new UpdateRepositoryFeatures();
        Response updateResponse = repoUpdateAPI.update(userToken,
                                                       accountName,
                                                       repoName,
                                                       accountPass,
                                                       true,
                                                       true,
                                                       true,
                                                       false,
                                                       false,
                                                       true,
                                                       false);

        assertNotNull(updateResponse);
        assertEquals(200, updateResponse.getStatus());

        // --------------------------------------------------------------------------
        // 3. Create a team and add access controls
        // --------------------------------------------------------------------------

        final String teamName = "Developers";

        // Create a repository team and some access restrictions
        CreateTeam teamAPI = new CreateTeam();
        response = teamAPI.create(userToken, accountName, repoName, teamName, null);

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        // --------------------------------------------------------------------------
        // Create access rules:
        // - editing anything that specifies Env: "Production" or App: "Collector"
        //   context is disabled
        // - editing key "db.main.host" is disabled
        //
        // If a rule is not matched, editing is allowed;
        // Stop processing rules on first match
        // --------------------------------------------------------------------------
        CreateAccessRule ruleAPI = new CreateAccessRule();

        response = ruleAPI.create(userToken,
                                  accountName,
                                  repoName,
                                  teamName,
                                  "Value",
                                  "ContainsAny",
                                  "",
                                  Common.buildUIContextString(new String[]{ "", "Production", "Collector", "" }),
                                  "ro");

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        response = ruleAPI.create(userToken,
                                  accountName,
                                  repoName,
                                  teamName,
                                  "Key",
                                  "Is",
                                  "db.main.host",
                                  "",
                                  "ro");

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        // Stop on first match
        UpdateStopOnFirstMatchedRule onFirstAPI = new UpdateStopOnFirstMatchedRule();
        response = onFirstAPI.update(userToken, accountName, repoName, teamName, true);
        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        // Read/Write if no rule
        UpdateUnmatchedEditableRule onUnmatchedAPI = new UpdateUnmatchedEditableRule();
        response = onUnmatchedAPI.update(userToken, accountName, repoName, teamName, true);
        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());


        // --------------------------------------------------------------------------
        // 4. Create a new user, and assign to the team
        // --------------------------------------------------------------------------
        final String teamMemberAccount = "teamMember";
        final String teamMemberToken = Common.createOrGetUser(teamMemberAccount, teamMemberAccount);

        AddMember addMemberAPI = new AddMember();
        response = addMemberAPI.update(accountName, repoName, teamName, userToken, teamMemberAccount);
        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        if (debugTest)
        {
            TeamInfo teamInfoAPI = new TeamInfo();
            response = teamInfoAPI.get(accountName, repoName, teamName, userToken);
            json = gson.fromJson((String)response.getEntity(), JsonObject.class);
            System.out.println(">>>>>>>>>>>>>>>>>>");
            System.out.println("Team: " + json);
            System.out.println("<<<<<<<<<<<<<<<<<<");
        }
        // --------------------------------------------------------------------------
        // 5. Team member pulls config via UI - check correct UI indications for
        //    editable values
        // --------------------------------------------------------------------------
        response = KVStore.getUIConfig(accountName,
                                       repoName,
                                       "",
                                       null,
                                       true,
                                       null,
                                       false, teamMemberToken);

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        config = json.get("config").getAsJsonArray();
        assertEquals(6, config.size());

        for (JsonElement el : config)
        {
            JsonObject entry = el.getAsJsonObject();
            if (entry.get("key").getAsString().equalsIgnoreCase("db.main.host"))
            {
                JsonArray props = entry.get("properties").getAsJsonArray();
                assertEquals(2, props.size());
                assertFalse(props.get(0).getAsJsonObject().get("editable").getAsBoolean());
                assertFalse(props.get(1).getAsJsonObject().get("editable").getAsBoolean());
            }
            else
            {
                JsonArray props = entry.get("properties").getAsJsonArray();
                for (int i =0; i<props.size(); i++)
                {
                    JsonObject prop = props.get(i).getAsJsonObject();
                    JsonArray levels = prop.get("levels").getAsJsonArray();

                    boolean isEditable = prop.get("editable").getAsBoolean();
                    JsonObject env = levels.get(0).getAsJsonObject();
                    JsonObject app = levels.get(1).getAsJsonObject();

                    if ((env.has("n") && env.get("n").getAsString().equals("Production")) ||
                        (app.has("n") && app.get("n").getAsString().equals("Collector")))
                        assertFalse(isEditable);
                    else
                        assertTrue(isEditable);
                }
            }
        }


        // --------------------------------------------------------------------------
        // 6. Create a personal token for teamMember account and try and update
        // access restricted value
        // --------------------------------------------------------------------------

        SaveOrUpdateToken tokenAPI = new SaveOrUpdateToken();
        response = tokenAPI.create(accountName,
                                   repoName,
                                   null,
                                   null,
                                   "MyToken",
                                   true,
                                   null,
                                   false,
                                   null,
                                   null,
                                   teamName,
                                   teamName,
                                   Token.ManagedBy.User.name(),
                                   accountName,
                                   teamMemberToken);

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        String teamMemberAPIToken = json.get("token").getAsString();
        assertNotNull(teamMemberAPIToken);


        if (debugTest)
        {
            TokenFetchAll getTokensAPI = new TokenFetchAll();
            response = getTokensAPI.get(accountName, repoName, false, userToken);
            json = gson.fromJson((String)response.getEntity(), JsonObject.class);
            System.out.println(">>>>>>>>>>>>>>>>>>");
            System.out.println("Tokens: " + json);
            System.out.println("<<<<<<<<<<<<<<<<<<");
        }


        // --------------------------------------------------------------------------
        // 7. Make sure team member can pull configuration via API
        // --------------------------------------------------------------------------
        APIPull apiPull = new APIPull();
        response = apiPull.get(teamMemberAPIToken,
                               "Production;Collector;Default",
                               null,
                               null,
                               null,
                               "TokenTest",
                               null,
                               false,
                               "none",
                               false,
                               false,
                               false,
                               false);

        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        JsonObject props = json.get("properties").getAsJsonObject();
        assertEquals(6, props.size());

        // --------------------------------------------------------------------------
        // 8. Try and change a Production value
        // --------------------------------------------------------------------------
        apiPush = new APIPush();
        response = apiPush.pushStructuredData(teamMemberAPIToken,
                                              null,
                                              null,
                                              null,
                                              null,
                                              "{\"data\":[{\"key\":\"app.name\",\"values\":[{\"context\":\"*;" +
                                                      "Collector;*\",\"value\":\"NewCollector\"}]}]}");

        assertEquals(304, response.getStatus());
        assertNotNull(response.getEntityTag());

        if (debugTest)
        {
            response = KVStore.getUIConfig(accountName, repoName, "", null, true, null, false, teamMemberToken);

            assertEquals(200, response.getStatus());
            json = gson.fromJson((String)response.getEntity(), JsonObject.class);
            config = json.get("config").getAsJsonArray();
            System.out.println(">>>>>>>>>>>>>>>>>>");
            System.out.println("UI Config: " + config);
            System.out.println("<<<<<<<<<<<<<<<<<<");
        }

        // --------------------------------------------------------------------------
        // 9. Change a Non-Production value
        // --------------------------------------------------------------------------
        apiPush = new APIPush();
        response = apiPush.pushStructuredData(teamMemberAPIToken,
                                              null,
                                              null,
                                              null,
                                              null,
                                              "{\"data\":[{\"key\":\"app.name\",\"values\":[{\"context\":\"*;" +
                                                      "WebDashboard;*\",\"value\":\"New WebDashboard\"}]}]}");

        assertEquals(200, response.getStatus());
        assertNull(response.getEntityTag());

        // --------------------------------------------------------------------------
        // 10. Create a Production file
        // --------------------------------------------------------------------------
        apiPush = new APIPush();
        response = apiPush.pushStructuredData(teamMemberAPIToken,
                                              null,
                                              null,
                                              null,
                                              null,
                                              "{\"data\":[{\"content\":\"This is a test file\",\"file\": \"test" +
                                                      ".conf\",\"context\":\"Production;*;*\"}]}");

        assertEquals(304, response.getStatus());
        assertNotNull(response.getEntityTag());

        // --------------------------------------------------------------------------
        // 11. Create a Non-Production file
        // --------------------------------------------------------------------------
        apiPush = new APIPush();
        response = apiPush.pushStructuredData(teamMemberAPIToken,
                                              null,
                                              null,
                                              null,
                                              null,
                                              "{\"data\":[{\"content\": \"This is a test file\",\"file\": \"test.conf\"" +
                                                      ",\"context\": \"*;*;*\"}]}");

        assertEquals(200, response.getStatus());
        assertNull(response.getEntityTag());

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
                                                       true,
                                                       true,
                                                       true,
                                                       false,
                                                       false,
                                                       true,
                                                       true);

        assertNotNull(updateResponse);
        assertEquals(200, updateResponse.getStatus());

    }

    @After
    public void cleanup()
    {
        Common.deleteRepository(userToken, accountName, repoName, accountPass);
    }
}
