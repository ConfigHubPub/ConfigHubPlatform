/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.editor;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.store.KeyUtils;
import com.confighub.core.store.Store;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/isKeyEditable")
public class IsKeyEditable
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(KeyUtils.class);

    @GET
    @Path("/{account}/{repository}/{key}")
    public boolean isEditable(@PathParam("account") String account,
                              @PathParam("repository") String repositoryName,
                              @PathParam("key") String keyString,
                              @HeaderParam("Authorization") String token)
    {
        Store store = new Store();

        try {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return false;

            return KeyUtils.isKeyEditable(repository, store.getKey(repository, keyString), keyString, user);
        }
        finally
        {
            store.close();
        }
    }
}
