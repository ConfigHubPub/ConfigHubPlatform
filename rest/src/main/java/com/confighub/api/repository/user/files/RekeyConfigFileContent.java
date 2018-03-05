/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.files;

import com.confighub.core.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/rekeyConfigFileContent")
public class RekeyConfigFileContent
{
    private static final Logger log = LogManager.getLogger(DeleteDir.class);

    @POST
    @Produces("text/plain")
    public Response add(@FormParam("content") String content,
                        @FormParam("from") String from,
                        @FormParam("to") String to)
    {

        String text = FileUtils.replaceKey(content, from, to);
        return Response.ok(text, MediaType.TEXT_PLAIN).build();
    }
}