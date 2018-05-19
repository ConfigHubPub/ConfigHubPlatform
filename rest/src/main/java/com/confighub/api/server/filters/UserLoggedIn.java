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

package com.confighub.api.server.filters;

import com.confighub.core.auth.Auth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import java.io.IOException;

@PreMatching
public class UserLoggedIn
    implements ContainerResponseFilter
{
    private static final Logger log = LogManager.getLogger(UserLoggedIn.class);

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext)
            throws IOException
    {
        String token = requestContext.getHeaderString("Authorization");
        if (null == token)
        {
            responseContext.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            return;
        }

        try {
            Auth.validateUser(token);
        } catch (Exception e) {
            responseContext.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            return;
        }
    }
}
