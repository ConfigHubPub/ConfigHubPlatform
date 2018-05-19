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

import com.confighub.core.error.ConfigException;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("/isRepoNameValid")
public class IsRepoNameValid
{
//    @EJB
//    private Store store;

    @GET
    @Path("/{account}")
    public int getAssignments(@PathParam("account") String account,
                              @QueryParam("t") String searchTerm)
    {
        Store store = new Store();

        if (Utils.isBlank(searchTerm)) return 1;
        if (!Utils.isNameValid(searchTerm)) return 2;

        try
        {
            if (null != store.getRepository(account, searchTerm))
                return 3;
        }
        catch (ConfigException e) {
            e.printStackTrace();
        }
        finally
        {
            store.close();
        }

        return 1;
    }
}
