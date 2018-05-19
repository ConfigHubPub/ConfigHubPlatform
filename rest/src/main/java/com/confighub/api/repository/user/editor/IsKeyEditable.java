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
