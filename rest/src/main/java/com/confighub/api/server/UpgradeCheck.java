/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.server;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("upgradeCheck")
@Produces("application/json")
public class UpgradeCheck
{
    private static final Logger log = LogManager.getLogger(UpgradeCheck.class);

    @AuthenticationNotRequired
    @GET
    public Response get()
    {
        Gson gson = new Gson();
        Response.ResponseBuilder response;

        if (Maintenance.hasUpgrade())
            response = Response.ok(gson.toJson(Maintenance.getUpgrade()), MediaType.APPLICATION_JSON);
        else
            response = Response.ok("{}", MediaType.APPLICATION_JSON);

        response.status(200);
        return response.build();
    }
}
