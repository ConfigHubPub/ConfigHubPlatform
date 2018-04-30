package com.confighub.api.system.conf.ldap;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.auth.LdapConnector;
import com.confighub.core.auth.LdapEntry;
import com.confighub.core.auth.TrustAllX509TrustManager;
import com.confighub.core.store.Store;
import com.confighub.core.system.SystemConfig;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;

//@Slf4j
@Path("/getLDAPConfig")
public class GetLDAPConfig
{
    private static final Logger log = LogManager.getLogger(GetLDAPConfig.class);

    @GET
    @Produces("application/json")
    public Response create(@HeaderParam("Authorization") final String token)
    {
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            final UserAccount user = TokenState.getUser(token, store);
            List<SystemConfig> config = store.getSystemConfig(SystemConfig.ConfigGroup.LDAP);

            JsonArray conf = new JsonArray();
            config.forEach(p -> conf.add(p.toJson()));

            return Response.ok(gson.toJson(conf), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

    @POST
    @Produces("application/json")
    @Path("/testLdap")
    public Response testLdapConfiguration(@FormParam("system_username") String systemUsername,
                                          @FormParam("system_password") String systemPassword,
                                          @FormParam("ldap_uri") String ldapUriString,
                                          @FormParam("use_start_tls") boolean useStartTls,
                                          @FormParam("trust_all_certificates") boolean trustAllCertificates,
                                          @FormParam("active_directory") boolean activeDirectory,
                                          @FormParam("search_base") String searchBase,
                                          @FormParam("search_pattern") String searchPattern,
                                          @FormParam("display_name") String displayName,
                                          @FormParam("principal") String principal,
                                          @FormParam("password") String password,
                                          @FormParam("test_connect_only") boolean testConnectOnly,
                                          @FormParam("group_search_base") String groupSearchBase,
                                          @FormParam("group_id_attribute") String groupIdAttribute,
                                          @FormParam("group_search_pattern") String groupSearchPattern)
    {
        Gson gson = new Gson();


        final LdapConnectionConfig config = new LdapConnectionConfig();
        final URI ldapUri = URI.create(ldapUriString);
        config.setLdapHost(ldapUri.getHost());
        config.setLdapPort(ldapUri.getPort());
        config.setUseSsl(ldapUri.getScheme().startsWith("ldaps"));
        config.setUseTls(useStartTls);

        if (trustAllCertificates)
        {
            config.setTrustManagers(new TrustAllX509TrustManager());
        }

        config.setName(systemUsername);
        config.setCredentials(systemPassword);

        LdapNetworkConnection connection = null;
        try
        {
            LdapConnector ldapConnector = new LdapConnector();

            try
            {
                connection = ldapConnector.connect(config);
            }
            catch (LdapException e)
            {
                log.error("Failed to connect to LDAP: " + e.getMessage());

                JsonObject json = new JsonObject();
                json.addProperty("success", false);
                JsonObject response = new JsonObject();
                response.addProperty("connected", false);
                response.addProperty("authenticated", false);
                json.add("errorMessage", response);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            boolean connected = null != connection && connection.isConnected();
            boolean systemAuthenticated = connection.isAuthenticated();

            // the web interface allows testing the connection only, in that case we can bail out early.
            if (null == connection || testConnectOnly)
            {
                JsonObject json = new JsonObject();
                json.addProperty("success", connected && systemAuthenticated);

                JsonObject response = new JsonObject();
                response.addProperty("connected", connected);
                response.addProperty("authenticated", systemAuthenticated);

                json.add("errorMessage", response);

                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            String userPrincipalName = null;
            boolean loginAuthenticated = false;

            JsonObject json = new JsonObject();

            try
            {
                final LdapEntry entry = ldapConnector.search(connection,
                                                             searchBase,
                                                             searchPattern,
                                                             "*",
                                                             principal,
                                                             activeDirectory,
                                                             groupSearchBase,
                                                             groupIdAttribute,
                                                             groupSearchPattern);

                if (entry != null)
                {
                    userPrincipalName = entry.getBindPrincipal();

                    json.addProperty("entry", entry.toString());
                    json.addProperty("displayName", entry.getAttributes().getOrDefault(displayName, principal));
                }
            }
            catch (CursorException | LdapException e)
            {
                log.error(e.getMessage());
                json.addProperty("errorMessage", e.getMessage());
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            try
            {
                loginAuthenticated = ldapConnector.authenticate(connection, userPrincipalName, password);
            }
            catch (Exception e)
            {
                log.error(e.getMessage());
                json.addProperty("errorMessage", e.getMessage());
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            json.addProperty("success", loginAuthenticated);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();

        }
        finally
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (IOException e)
                {
                    log.error("Unable to close LDAP connection.", e);
                }
            }
        }
    }

}
