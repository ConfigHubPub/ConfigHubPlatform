/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.team;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.organization.Team;
import com.confighub.core.security.Token;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/teamInfo")
public class TeamInfo
        extends AUserAccessValidation
{
    @GET
    @Path("/{account}/{repository}/{team}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @PathParam("team") String teamName,
                        @HeaderParam("Authorization") String userToken)
    {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().serializeNulls().create();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, userToken, store);
            if (0 != status)
                return Response.status(status).build();

            Team team = repository.getTeam(teamName);
            if (null == team)
            {
                json.addProperty("message", "Failed to find the requested team: " + teamName);
                json.addProperty("success", false);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            json.addProperty("success", true);
            json.add("team", GsonHelper.teamToJson(team));

            JsonArray tokens = new JsonArray();
            if (null != team.getTokens())
                for (Token token : team.getTokens())
                    tokens.add(GsonHelper.tokenToJson(token, false));
            json.add("tokens", tokens);

        }
        finally
        {
            store.close();
        }

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}
