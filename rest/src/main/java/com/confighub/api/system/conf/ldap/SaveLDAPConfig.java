package com.confighub.api.system.conf.ldap;

import com.confighub.api.system.ASysAdminAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.system.conf.LdapConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger log = LogManager.getLogger(SaveLDAPConfig.class);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveLdapConfiguration(final LdapConfig config,
                                          @HeaderParam("Authorization") final String token)
    {
        Gson gson = new Gson();
        JsonObject ldap = new JsonObject();

        Store store = new Store();

        try
        {
            int status = validateCHAdmin(token, store);
            if (0 != status)
                return Response.status(status).build();

            store.begin();
            store.save(user, config);
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
