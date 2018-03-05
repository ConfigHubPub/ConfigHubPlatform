/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.auth;

import com.confighub.api.server.AuthenticationNotRequired;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/logout")
public class Logout
{
    @AuthenticationNotRequired
    @POST
    public Response login(@HeaderParam("Authorization") String token)
    {
        return Response.status(401).build();
    }
}