package com.confighub.api.properties;

import com.confighub.api.common.Common;
import com.confighub.api.common.Files;
import com.confighub.api.common.KVStore;
import com.confighub.api.repository.admin.settings.UpdateRepositoryFeatures;
import com.confighub.api.repository.user.security.CreateSecurityProfile;
import com.confighub.api.repository.user.security.GetSecurityProfiles;
import com.confighub.core.repository.Depth;
import com.confighub.core.security.CipherTransformation;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.confighub.api.common.Common.gson;
import static com.confighub.core.repository.PropertyKey.ValueDataType;
import static org.junit.Assert.*;

/**
 *
 */
public class KVOpsTest
{
    String accountName = "aUser";
    String accountPass = "password";
    String repoName = "KVOpsTest";
    String userToken = null;

    private class VDTTestCondition
    {
        VDTTestCondition(String[] _valid, String[] _invalid, ValueDataType[] _canSwitchTo)
        {
            valid = Arrays.asList(_valid);
            invalid = Arrays.asList(_invalid);
            canSwitchTo = Arrays.asList(_canSwitchTo);
        }

        List<String> valid;
        List<String> invalid;
        List<ValueDataType> canSwitchTo;
        List<ValueDataType> canNotSwitchTo;
    }

    @Test
    public void vdtNonSecureTest()
    {
        vdtTest(null, null, null);
    }


    @Test
    public void vdtSecureTest()
    {
        String spName = "Passwords";
        String spPass = "12345678";

        CreateSecurityProfile createSecurityProfileAPI = new CreateSecurityProfile();
        Response response = createSecurityProfileAPI.create(userToken,
                                                            accountName,
                                                            repoName,
                                                            spName,
                                                            spPass,
                                                            spPass,
                                                            CipherTransformation.DES_ECB_PKCS5Padding.getName());
        assertEquals(200, response.getStatus());
        JsonObject json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        GetSecurityProfiles securityProfilesAPI = new GetSecurityProfiles();
        response = securityProfilesAPI.get(userToken, accountName, repoName);
        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());

        assertTrue(json.has("groups"));
        assertEquals(1, json.get("groups").getAsJsonArray().size());

        vdtTest(spName, spPass, spPass);
    }



    private void vdtTest(String spName, String currentPassword, String newProfilePassword)
    {
        Response response;
        JsonObject json;

        Map<ValueDataType, VDTTestCondition> vdtConditions = new HashMap<>();
        vdtConditions.put(ValueDataType.Text,
                          new VDTTestCondition(new String[]{ "test\n\r\t" },
                                               new String[]{},
                                               new ValueDataType[]{ ValueDataType.Code, ValueDataType.JSON }));

        vdtConditions.put(ValueDataType.Code,
                          new VDTTestCondition(new String[]{ "test\n\r\t" },
                                               new String[]{},
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.JSON }));

        vdtConditions.put(ValueDataType.JSON,
                          new VDTTestCondition(new String[]{ "1", "{ \n }", "[\"1\", \"a\"]" },
                                               new String[]{ "", " " },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.JSON }));

        // Numbers
        vdtConditions.put(ValueDataType.Integer,
                          new VDTTestCondition(new String[]{ "0", String.valueOf(Integer.MAX_VALUE),
                                                             String.valueOf(-1 * Integer.MAX_VALUE) },
                                               new String[]{ "1a", "asd", "" },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.Code,
                                                                    ValueDataType.Double, ValueDataType.Float,
                                                                    ValueDataType.Long }));

        vdtConditions.put(ValueDataType.Long,
                          new VDTTestCondition(new String[]{ "0", String.valueOf(Long.MAX_VALUE),
                                                             String.valueOf(-1 * Long.MAX_VALUE) },
                                               new String[]{ "1a", "asd", "" },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.Code,
                                                                    ValueDataType.Double, ValueDataType.Float,
                                                                    ValueDataType.Integer }));

        vdtConditions.put(ValueDataType.Float,
                          new VDTTestCondition(new String[]{ "0", "1.1234567890", "-1.1234567890",
                                                             String.valueOf(Float.MAX_VALUE),
                                                             String.valueOf(-1 * Float.MAX_VALUE) },
                                               new String[]{ "1a", "asd", "" },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.Code,
                                                                    ValueDataType.Double, ValueDataType.Long,
                                                                    ValueDataType.Integer }));

        vdtConditions.put(ValueDataType.Double,
                          new VDTTestCondition(new String[]{ "0", "1.1234567890", "-1.1234567890",
                                                             String.valueOf(Float.MAX_VALUE),
                                                             String.valueOf(-1 * Float.MAX_VALUE) },
                                               new String[]{ "1a", "asd", "" },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.Code,
                                                                    ValueDataType.Float, ValueDataType.Long,
                                                                    ValueDataType.Integer }));


        vdtConditions.put(ValueDataType.Boolean,
                          new VDTTestCondition(new String[]{ "true", "false" },
                                               new String[]{ "1", "0", "1a", "asd", "" },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.Code }));

        // Structured
        vdtConditions.put(ValueDataType.Map,
                          new VDTTestCondition(new String[]{ "", "{}", "{\"a\": \"b\" }" },
                                               new String[]{ "1", "0", "1a", "asd" },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.Code,
                                                                    ValueDataType.JSON }));

        vdtConditions.put(ValueDataType.List,
                          new VDTTestCondition(new String[]{ "", "[]", "[\"1\", \"\", \"a\"]" },
                                               new String[]{ "1", "0", "1a", "asd" },
                                               new ValueDataType[]{ ValueDataType.Text, ValueDataType.Code,
                                                                    ValueDataType.JSON }));

        // Files
        vdtConditions.put(ValueDataType.FileRef,
                          new VDTTestCondition(new String[]{ "foo.cfg" },
                                               new String[]{ "", "asd" },
                                               new ValueDataType[]{ ValueDataType.Text }));

        vdtConditions.put(ValueDataType.FileEmbed,
                          new VDTTestCondition(new String[]{ "foo.cfg" },
                                               new String[]{ "", "asd" },
                                               new ValueDataType[]{ ValueDataType.Text }));

        // Add a repository file

        response = Files.saveOrUpdateFile(accountName,
                                          repoName,
                                          userToken,
                                          "/",
                                          "foo.cfg",
                                          null,
                                          "this is file content",
                                          Common.buildUIContextString(new String[]{ "", "", "" }),
                                          true,
                                          null,
                                          currentPassword,
                                          newProfilePassword,
                                          spName,
                                          false,
                                          false);
        assertEquals(200, response.getStatus());
        json = gson.fromJson((String)response.getEntity(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());


        for (ValueDataType vdt : vdtConditions.keySet())
        {
            VDTTestCondition condition = vdtConditions.get(vdt);

            response = KVStore.addOrUpdateProperty(accountName,
                                                   repoName,
                                                   userToken,
                                                   vdt.name(), // key
                                                   null,
                                                   false,
                                                   vdt.name(), // vdt
                                                   false,
                                                   null,
                                                   Common.buildUIContextString(new String[]{ "", "", "" }),
                                                   null,
                                                   true,
                                                   currentPassword,
                                                   spName,
                                                   null);

            assertEquals(200, response.getStatus());
            json = gson.fromJson((String)response.getEntity(), JsonObject.class);
            assertTrue(json.get("success").getAsBoolean());
            assertNotNull(json.get("id").getAsLong());

            Long propertyId = json.get("id").getAsLong();

            // Test back-and-forth between changing the valid value and null
            for (String validValue : condition.valid)
            {
                response = KVStore.addOrUpdateProperty(accountName,
                                                       repoName,
                                                       userToken,
                                                       vdt.name(),
                                                       null,
                                                       false,
                                                       vdt.name(),
                                                       false,
                                                       validValue,
                                                       Common.buildUIContextString(new String[]{ "", "", "" }),
                                                       null,
                                                       true,
                                                       currentPassword,
                                                       spName,
                                                       propertyId);

                assertEquals(200, response.getStatus());
                json = gson.fromJson((String)response.getEntity(), JsonObject.class);
                assertTrue(json.get("success").getAsBoolean());

                response = KVStore.addOrUpdateProperty(accountName,
                                                       repoName,
                                                       userToken,
                                                       vdt.name(),
                                                       null,
                                                       false,
                                                       vdt.name(),
                                                       false,
                                                       null,
                                                       Common.buildUIContextString(new String[]{ "", "", "" }),
                                                       null,
                                                       true,
                                                       currentPassword,
                                                       spName,
                                                       propertyId);
                assertEquals(200, response.getStatus());
                json = gson.fromJson((String)response.getEntity(), JsonObject.class);
                assertTrue(json.get("success").getAsBoolean());
            }

            // Test switching the key type to other VDTs when value set and value null
            for (ValueDataType toVdt : condition.canSwitchTo)
            {
                response = KVStore.updateKey(accountName, repoName, vdt.name(), userToken, vdt.name(), // key
                                             toVdt.name(), // switchTo
                                             false, null, null, spName, null, currentPassword, false);

                assertEquals(200, response.getStatus());
                json = gson.fromJson((String)response.getEntity(), JsonObject.class);
                assertTrue(json.get("success").getAsBoolean());
                response = KVStore.updateKey(accountName, repoName, vdt.name(), userToken, vdt.name(), // key
                                             vdt.name(), // back to original
                                             false, null, null, spName, null, currentPassword, false);

                assertEquals(200, response.getStatus());
                json = gson.fromJson((String)response.getEntity(), JsonObject.class);
                assertTrue(json.get("success").getAsBoolean());
            }


            // Test invalid input is not accepted
            for (String invalidValue : condition.invalid)
            {
                response = KVStore.addOrUpdateProperty(accountName,
                                                       repoName,
                                                       userToken,
                                                       vdt.name(),
                                                       null,
                                                       false,
                                                       vdt.name(),
                                                       false,
                                                       invalidValue,
                                                       Common.buildUIContextString(new String[]{ "", "", "" }),
                                                       null,
                                                       true,
                                                       currentPassword,
                                                       spName,
                                                       propertyId);

                assertEquals(200, response.getStatus());
                json = gson.fromJson((String)response.getEntity(), JsonObject.class);
                assertFalse(json.get("success").getAsBoolean());
            }
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
