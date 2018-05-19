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

package com.confighub.api.user.dashboard;

import com.confighub.api.server.auth.TokenState;
import com.confighub.core.organization.Organization;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Path("/myRepositories")
public class MyRepositories
{
    private static final Logger log = LogManager.getLogger(MyRepositories.class);

//    @EJB
//    private Store store;

    @GET
    @Produces("application/json")
    public Response getAssignments(@HeaderParam("Authorization") String token)
    {
        JsonArray json = new JsonArray();
        Store store = new Store();

        try
        {
            UserAccount user = TokenState.getUser(token, store);
            if (null == user)
                return Response.status(401).build();

            if (null != user.getRepositories())
            {
                for (Repository repository : user.getRepositories())
                {
                    JsonObject repositoryJ = new JsonObject();

                    repositoryJ.addProperty("name", repository.getName());
                    repositoryJ.addProperty("description",
                                    null == repository.getDescription() ? "" : repository.getDescription());
                    repositoryJ.addProperty("owner", repository.getAccountName());
                    repositoryJ.addProperty("isPrivate", repository.isPrivate());
                    repositoryJ.addProperty("userCount", repository.getUserCount());

                    json.add(repositoryJ);
                }
            }

            // if a user is registered as admin/owner of an organization, then
            // they have access to all their repositories
            Set<Organization> orgs = user.getOrganizations();
            if (null != orgs)
            {
                for (Organization org : orgs)
                {
                    Set<Repository> orgRepos = org.getRepositories();
                    if (null == orgRepos) continue;

                    for (Repository repository : orgRepos)
                    {
                        JsonObject repositoryJ = new JsonObject();

                        repositoryJ.addProperty("name", repository.getName());
                        repositoryJ.addProperty("description",
                                        null == repository.getDescription() ? "" : repository.getDescription());
                        repositoryJ.addProperty("owner", repository.getAccountName());
                        repositoryJ.addProperty("isPrivate", repository.isPrivate());
                        repositoryJ.addProperty("userCount", repository.getUserCount());

                        json.add(repositoryJ);
                    }
                }
            }

            Gson gson = new Gson();
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
