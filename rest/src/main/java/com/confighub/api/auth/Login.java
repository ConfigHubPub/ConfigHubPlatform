package com.confighub.api.auth;

import com.confighub.api.server.AuthenticationNotRequired;
import com.confighub.core.auth.Auth;
import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/login")
@Produces("application/json")
public class Login
{
    private static final Logger log = LogManager.getLogger(Login.class);

    @AuthenticationNotRequired
    @POST
    public Response login(@FormParam("email") String login,
                          @FormParam("password") String password)
    {
        Store store = new Store();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        try
        {
            UserAccount user = null;

            if (Auth.isLdapEnabled())
            {
                user = Auth.ldapAuth(login, password, store);
            }

            if (null == user)
            {
                user = store.login(login, password);
            }

            json.addProperty("token", Auth.createUserToken(user));
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            e.printStackTrace();


            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}