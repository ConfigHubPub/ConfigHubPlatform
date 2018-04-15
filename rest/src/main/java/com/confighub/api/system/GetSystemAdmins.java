package com.confighub.api.system;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/getSystemAdmins")
public class GetSystemAdmins
{
    @GET
    @Produces("application/json")
    public Response create(@HeaderParam("Authorization") final String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            final UserAccount user = TokenState.getUser(token, store);
            final JsonArray admins = new JsonArray();

            List<UserAccount> sysAdmins = store.getSystemAdmins();
            if (null != sysAdmins)
            {
                sysAdmins.forEach(admin -> {
                    JsonObject j = new JsonObject();
                    j.addProperty("un", admin.getUsername());
                    j.addProperty("name", admin.getName());

                    admins.add(j);
                });
            }

            json.add("admins", admins);
            json.addProperty("isAdmin", null == user ? false : user.isConfigHubAdmin());
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

}
