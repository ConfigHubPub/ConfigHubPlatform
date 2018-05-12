package com.confighub.api.system.conf.ldap;

import com.confighub.core.auth.Auth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("localAccountsAvailable")
public class LocalAccountsAvailable
{
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response areLocalAccountsEnabled()
    {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.addProperty("enabled", Auth.isLocalAccountsEnabled());

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}
