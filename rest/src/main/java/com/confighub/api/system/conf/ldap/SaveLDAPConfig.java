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
        JsonObject ldap = new JsonObject();

        ldap.addProperty("system_username", systemUsername);
        ldap.addProperty("system_password", systemPassword);
        ldap.addProperty("ldap_uri", ldapUriString);
        ldap.addProperty("use_start_tls", useStartTls);
        ldap.addProperty("trust_all_certificates", trustAllCertificates);
        ldap.addProperty("active_directory", activeDirectory);
        ldap.addProperty("search_base", searchBase);
        ldap.addProperty("search_pattern", searchPattern);
        ldap.addProperty("display_name", displayName);
        ldap.addProperty("group_search_base", groupSearchBase);
        ldap.addProperty("group_id_attribute", groupIdAttribute);
        ldap.addProperty("group_search_pattern", groupSearchPattern);

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
                                   ldap.toString());
            store.commit();

            ldap.addProperty("success", true);
            return Response.ok(gson.toJson(ldap), MediaType.APPLICATION_JSON).build();
        }
        catch (final ConfigException e)
        {
            store.rollback();

            ldap.addProperty("message", e.getMessage());
            ldap.addProperty("success", false);

            return Response.ok(gson.toJson(ldap), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

    }
}
