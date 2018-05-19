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
