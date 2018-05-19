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

package com.confighub.api.repository.admin.users;

import com.confighub.api.repository.admin.AAdminAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.mail.InvitationEmail;
import com.confighub.core.organization.Team;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/addMember")
public class AddMember
        extends AAdminAccessValidation
{
    private static final Logger log = LogManager.getLogger(AddMember.class);

    @POST
    @Path("/{account}/{repository}/{team}")
    @Produces("application/json")
    public Response update(@PathParam("account") String account,
                           @PathParam("repository") String repositoryName,
                           @PathParam("team") String teamName,
                           @HeaderParam("Authorization") String token,
                           @FormParam("un") String username)
    {

        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        if (Utils.anyBlank(account, repositoryName, teamName))
        {
            json.addProperty("success", false);
            json.addProperty("message", "Missing required field.");
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }

        UserAccount member = null;

        try
        {
            int status = validateWrite(account, repositoryName, token, store, true);
            if (0 != status) return Response.status(status).build();

            member = store.getUserByUsername(username);
            if (null == member)
            {
                json.addProperty("success", false);
                json.addProperty("message", "Can not find specified user.");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            store.addTeamMember(repository, user, teamName, member);
            store.commit();

            InvitationEmail.sendRepositoryInvitation(user, member.getEmail());

            json.addProperty("success", true);
            json.add("members", GsonHelper.teamMembers(repository.getTeam(teamName)));

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            if (e.getErrorCode().equals(Error.Code.MULTIPLE_TEAM_MEMBERSHIPS))
            {
                Team team = store.getTeamForMember(repository, member);
                if (team.getName().equals(teamName))
                {
                    json.addProperty("success", false);
                    json.addProperty("message", member.getName() + " is already a member.");
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }

                json.addProperty("success", false);
                json.addProperty("multiple", true);
                json.addProperty("team", team.getName());
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

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
