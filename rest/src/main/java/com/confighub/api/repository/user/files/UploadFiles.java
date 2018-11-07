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

package com.confighub.api.repository.user.files;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;

@Path("/uploadFiles")
@Produces("application/json")
public class UploadFiles
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(UploadFiles.class);

    @POST
    @Path("/{account}/{repository}")
    public Response uploadFile(@PathParam("account") String account,
                               @PathParam("repository") String repositoryName,
                               @HeaderParam("Authorization") String token,
                               @HeaderParam("path") String path,
                               @HeaderParam("context") String fileContext,
                               @HeaderParam("fileName") String fileName,
                               @HeaderParam("sg") String securityGroupName,
                               @HeaderParam("password") String password,
                               @HeaderParam("changeComment") String changeComment,
                               @FormDataParam("file") InputStream uploadedInputStream,
                               @FormDataParam("file") FormDataContentDisposition fileDetail)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();

        String content;

        try
        {
            StringWriter writer = new StringWriter();
            IOUtils.copy(uploadedInputStream, writer, "UTF-8");
            content = writer.toString();
        }
        catch (IOException e)
        {
            json.addProperty("message", "Failed to process file upload request");
            json.addProperty("success", false);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }

        Store store = new Store();

        int status = validateWrite(account, repositoryName, token, store, true);
        if (0 != status)
            return Response.status(status).build();

        try
        {
            Collection<CtxLevel> context = ContextParser.parseAndCreate( fileContext, repository, store, user, null);

            store.begin();
            RepoFile newFile = store.createRepoFile(user,
                                                    repository,
                                                    path,
                                                    fileName,
                                                    content,
                                                    context,
                                                    true,
                                                    securityGroupName,
                                                    password,
                                                    changeComment);
            store.commit();

            json.add("file", newFile.toJson());
            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            store.rollback();

            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }

    }
}
