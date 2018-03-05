/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
