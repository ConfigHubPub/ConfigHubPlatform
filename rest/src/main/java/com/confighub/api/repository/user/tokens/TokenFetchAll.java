/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.confighub.api.repository.user.tokens;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Team;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.SecurityStore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/tokenFetchAll")
public class TokenFetchAll
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(TokenFetchAll.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @QueryParam("all") boolean all,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        SecurityStore store = new SecurityStore();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status)
                return Response.status(status).build();

            Collection<Token> tokens = store.getTokens(repository);
            JsonArray tokensJson = new JsonArray();

            if (null != tokens && tokens.size() > 0)
            {
                for (Token t : tokens)
                {
                    boolean isAllowedToEdit = t.isAllowedToEdit(user);
                    boolean viewable = t.isAllowedToViewToken(user);

                    if (all || viewable)
                    {
                        JsonObject tkJson = GsonHelper.tokenToJson(t, viewable);

                        tkJson.addProperty("editable", isAllowedToEdit);
                        tkJson.addProperty("deletable", t.isAllowedToDelete(user));
                        tokensJson.add(tkJson);
                    }
                }
            }
            json.add("tokens", tokensJson);

            if (repository.isSecurityProfilesEnabled())
            {
                Collection<SecurityProfile> groups = store.getAllRepoEncryptionProfiles(user, repository);
                JsonArray groupJson = new JsonArray();

                boolean hasEncryptedGroups = false;
                for (SecurityProfile group : groups)
                {
                    if (group.encryptionEnabled())
                    {
                        hasEncryptedGroups = true;

                        JsonObject g = new JsonObject();
                        g.addProperty("name", group.getName());
                        g.addProperty("cipher", null == group.getCipher() ? "" : group.getCipher().getName());
                        groupJson.add(g);
                    }
                }

                if (hasEncryptedGroups)
                    json.add("groups", groupJson);
            }

            JsonObject teamsJson = new JsonObject();
            if (null != repository.getTeams() && repository.getTeams().size() > 0)
            {
                for (Team team : repository.getTeams())
                {
                    JsonObject teamJson = new JsonObject();
                    teamJson.addProperty("memberCount", team.getMemberCount());
                    teamJson.addProperty("ruleCount", team.getRuleCount());

                    teamsJson.add(team.getName(), teamJson);
                }
            }
            json.add("teams", teamsJson);

            Team userTeam = store.getTeamForMember(repository, user);
            json.addProperty("isAdmin", repository.isAdminOrOwner(user));
            json.addProperty("accessControlEnabled", repository.isAccessControlEnabled());
            json.addProperty("teamMember", null == userTeam ? null : userTeam.getName());

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
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
