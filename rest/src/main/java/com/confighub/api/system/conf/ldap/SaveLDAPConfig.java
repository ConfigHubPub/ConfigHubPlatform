package com.confighub.api.system.conf.ldap;

import com.confighub.api.system.ASysAdminAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.system.SystemConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/saveLDAPConfig")
public class SaveLDAPConfig
        extends ASysAdminAccessValidation
{
    @POST
    @Produces("application/json")
    public Response saveLdapConfiguration(@FormParam("system_username") String systemUsername,
                                          @FormParam("system_password") String systemPassword,
                                          @FormParam("ldap_uri") String ldapUriString,
                                          @FormParam("use_start_tls") boolean useStartTls,
                                          @FormParam("trust_all_certificates") boolean trustAllCertificates,
                                          @FormParam("active_directory") boolean activeDirectory,
                                          @FormParam("search_base") String searchBase,
                                          @FormParam("search_pattern") String searchPattern,
                                          @FormParam("display_name") String displayName,
                                          @FormParam("group_search_base") String groupSearchBase,
                                          @FormParam("group_id_attribute") String groupIdAttribute,
                                          @FormParam("group_search_pattern") String groupSearchPattern,
                                          @HeaderParam("Authorization") final String token)
    {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();

        json.addProperty("system_username", systemUsername);
        json.addProperty("system_password", systemPassword);
        json.addProperty("ldap_uri", ldapUriString);
        json.addProperty("use_start_tls", useStartTls);
        json.addProperty("trust_all_certificates", trustAllCertificates);
        json.addProperty("active_directory", activeDirectory);
        json.addProperty("search_base", searchBase);
        json.addProperty("search_pattern", searchPattern);
        json.addProperty("display_name", displayName);
        json.addProperty("group_search_base", groupSearchBase);
        json.addProperty("group_id_attribute", groupIdAttribute);
        json.addProperty("group_search_pattern", groupSearchPattern);

        Store store = new Store();

        try
        {
            int status = validateCHAdmin(token, store);
            if (0 != status)
                return Response.status(status).build();

            store.begin();
            store.saveSystemConfig(user,
                                   SystemConfig.ConfigGroup.LDAP,
                                   "ldap",
                                   json.toString());
            store.commit();

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (final ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

    }
}
