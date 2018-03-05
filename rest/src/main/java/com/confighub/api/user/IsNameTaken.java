/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.user;

import com.confighub.api.server.AuthenticationNotRequired;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/isNameTaken")
public class IsNameTaken
{
    @AuthenticationNotRequired
    @GET
    public int getAssignments(@QueryParam("t") String searchTerm)
    {
        Store store = new Store();

        if (Utils.isBlank(searchTerm)) return 1;
        if (!Utils.isNameValid(searchTerm)) return 2;

        try
        {
            if (store.isAccountNameUsed(searchTerm))
                return 3;
        }
        finally
        {
            store.close();
        }

        return 1;
    }
}
