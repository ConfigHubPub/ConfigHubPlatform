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

package com.confighub.api.user;

import com.confighub.api.server.auth.TokenState;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Organization;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.user.Account;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/getAccount")
public class GetAccount
{
    private static final Logger log = LogManager.getLogger(GetAccount.class);

    @GET
    @Path("/{account}")
    @Produces("application/json")
    public Response get(@PathParam("account") String accountName,
                        @HeaderParam("Authorization") String token)
    {
        Store store = new Store();
        Gson gson = new Gson();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            Account account = store.getAccount(accountName);

            JsonObject json = new JsonObject();

            if (null == account)
                return accountNotFound();

            // public information
            json.addProperty("un", account.getName());
            json.addProperty("company", account.getCompany());
            json.addProperty("website", account.getWebsite());
            json.addProperty("city", account.getCity());
            json.addProperty("country", account.getCountry());


            if (account.isPersonal())
                getPersonalAccountData(json, user, account, store);
            else
                getOrganizationAccountData(json, user, account, store);

            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("error", e.getMessage());
            jsonObject.addProperty("success", false);

            return Response.ok(gson.toJson(jsonObject), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }

    /**
     *
     * @return
     */
    public static Response accountNotFound()
    {
        JsonObject json = new JsonObject();

        json.addProperty("message", "Requested account not found");
        json.addProperty("success", false);

        Gson gson = new Gson();
        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }


    private void getPersonalAccountData(JsonObject json, UserAccount loggedInUser, Account account, Store store)
            throws ConfigException
    {
        UserAccount accUser = account.getUser();

        json.addProperty("t", "u");
        json.addProperty("name", accUser.getName());
        json.addProperty("creationTs", accUser.getCreateDate().getTime());

        boolean isLoggedInUsersViewingPersonalAccount = null ==
                loggedInUser ? false : loggedInUser.getAccount().equals(account);
        json.addProperty("own", isLoggedInUsersViewingPersonalAccount);

        if (isLoggedInUsersViewingPersonalAccount)
        {
            json.addProperty("email", accUser.getEmail());
            json.addProperty("blogSub", accUser.isEmailBlog());
            json.addProperty("repoSub", accUser.isEmailRepoCritical());
        }


        // Organizations
        if (isLoggedInUsersViewingPersonalAccount && null != accUser.getOrganizations())
        {
            JsonArray organizationsJson = new JsonArray();
            for (Organization o : accUser.getOrganizations())
            {
                JsonObject oj = new JsonObject();
                oj.addProperty("un", o.getAccountName());
                oj.addProperty("name", o.getName());
                oj.addProperty("id", o.getId());
                oj.addProperty("creationTs", o.getCreateDate().getTime());
                oj.addProperty("role", o.isOwner(accUser) ? "Owner" : "Admin");

                getOrganizationManagement(oj, o);

                if (null != o.getRepositories())
                {
                    JsonArray repos = new JsonArray();
                    o.getRepositories().parallelStream().forEach(r -> repos.add(r.getName()));
                    oj.add("repos", repos);
                }

                organizationsJson.add(oj);
            }
            json.add("orgs", organizationsJson);

        }

        // Repositories
        Set<Repository> repositories = new HashSet<>();
        repositories.addAll(accUser.getRepositories());
        Set<Organization> organizations = accUser.getOrganizations();

        if (null != organizations)
            organizations.forEach(o -> repositories.addAll(o.getRepositories()));

        JsonArray repos = new JsonArray();
        if (null != repositories)
        {
            for (Repository repo : repositories)
            {

                if (repo.hasReadAccess(loggedInUser))
                {
                    JsonObject rj = getRepoJson(repo);

                    if (repo.isOwner(accUser))
                        rj.addProperty("role", "Owner");
                    else if (repo.isAdmin(accUser))
                        rj.addProperty("role", "Admin");
                    else if (repo.isMember(loggedInUser))
                        rj.addProperty("role", "Team Member");

                    if (isLoggedInUsersViewingPersonalAccount)
                        rj.addProperty("canLeave", !repo.isAdminOrOwner(accUser));

                    rj.addProperty("ut", repo.getUserType(loggedInUser).name());
                    rj.addProperty("demo", repo.isDemo());

                    Team team = store.getTeamForMember(repo, accUser);
                    if (null != team)
                        rj.add("team", GsonHelper.teamToJson(team));

                    repos.add(rj);
                }
            }
        }

        json.add("repos", repos);
    }

    private JsonObject getRepoJson(Repository repo)
    {
        JsonObject json = new JsonObject();

        json.addProperty("name", repo.getName());
        json.addProperty("description", Utils.jsonString(repo.getDescription()));
        json.addProperty("isPrivate", repo.isPrivate());
        json.addProperty("owner", repo.getAccountName());
        json.addProperty("isPersonal", repo.isPersonal());
        json.addProperty("creationTs", repo.getCreateDate().getTime());

        json.addProperty("accessControlEnabled", repo.isAccessControlEnabled());
        json.addProperty("securityProfilesEnabled", repo.isSecurityProfilesEnabled());
        json.addProperty("valueTypeEnabled", repo.isValueTypeEnabled());
        json.addProperty("contextClusters", repo.isContextClustersEnabled());

        Map<Depth, String> cLabels = repo.getContextLabels();
        JsonObject ctxj = new JsonObject();
        for (Depth d : cLabels.keySet())
            ctxj.addProperty(String.valueOf(d.getPlacement()), cLabels.get(d));
        json.add("ctx", ctxj);

        return json;
    }

    private void getOrganizationManagement(JsonObject json, Organization organization)
    {
        JsonArray adminsJson = new JsonArray();
        if (null != organization.getAdministrators())
            organization.getAdministrators().forEach(admin -> adminsJson.add(getUser(admin)));

        JsonArray ownersJson = new JsonArray();
        if (null != organization.getOwners())
            organization.getOwners().forEach(owner -> ownersJson.add(getUser(owner)));

        json.add("admins", adminsJson);
        json.add("owners", ownersJson);
    }

    private JsonObject getUser(UserAccount user) {
        JsonObject u = new JsonObject();
        u.addProperty("un", user.getUsername());
        u.addProperty("name", user.getName());
        return u;
    }






    private void getOrganizationAccountData(JsonObject json, UserAccount loggedInUser, Account account, Store store)
            throws ConfigException
    {
        Organization organization = account.getOrganization();

        json.addProperty("t", "o");

        json.addProperty("un", organization.getAccountName());
        json.addProperty("name", organization.getName());
        json.addProperty("creationTs", organization.getCreateDate().getTime());

        boolean adminOrOwner = false;

        if (organization.isAdmin(loggedInUser))
        {
            adminOrOwner = true;
            json.addProperty("own", "adm");
        }
        else if (organization.isOwner(loggedInUser))
        {
            adminOrOwner = true;
            json.addProperty("own", "own");
        }

        if (adminOrOwner)
        {
            getOrganizationManagement(json, organization);
        }

        JsonArray repos = new JsonArray();
        if (null != account.getRepositories())
        {
            for (Repository repo : account.getRepositories())
            {
                if (adminOrOwner || repo.hasReadAccess(loggedInUser))
                {
                    JsonObject rj = getRepoJson(repo);

                    boolean canLeave = false;
                    if (repo.isOwner(loggedInUser))
                        rj.addProperty("role", "Owner");
                    else if (repo.isAdmin(loggedInUser))
                        rj.addProperty("role", "Admin");
                    else if (repo.isMember(loggedInUser))
                    {
                        rj.addProperty("role", "Team Member");
                        canLeave = true;
                    }

                    rj.addProperty("ut", repo.getUserType(loggedInUser).name());
                    rj.addProperty("demo", repo.isDemo());

                    rj.addProperty("canLeave", canLeave);

                    Team team = store.getTeamForMember(repo, loggedInUser);
                    if (null != team)
                        rj.add("team", GsonHelper.teamToJson(team));

                    repos.add(rj);
                }
            }
        }

        json.add("repos", repos);
    }
}
