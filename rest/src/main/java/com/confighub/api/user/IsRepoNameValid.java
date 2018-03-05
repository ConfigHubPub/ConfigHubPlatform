/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
