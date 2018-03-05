/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.repository.user.files;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.AbsoluteFilePath;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.store.Store;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

@Path("/getFile")
public class GetFile
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetFile.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @QueryParam("id") long id,
                        @QueryParam("all") boolean allKeys,
                        @QueryParam("ts") Long ts,
                        @QueryParam("tag") String tagLabel,
                        @QueryParam("password") String password,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store, false);
            if (0 != status)
                return Response.status(status).build();

            Date dateObj = DateTimeUtils.dateFromTsOrTag(Utils.isBlank(tagLabel)
                                                                 ? null
                                                                 : store.getTag(repository.getId(), tagLabel),
                                                         ts,
                                                         repository.getCreateDate());

            RepoFile file = store.getRepoFile(user, repository, id, dateObj);
            AccessRuleWrapper rulesWrapper = repository.getRulesWrapper(user);

            if (null != file)
            {
                AbsoluteFilePath absoluteFilePath = file.getAbsFilePath();

                json.addProperty("siblings", absoluteFilePath.getFiles().size());
                json.addProperty("refs",
                                 null == absoluteFilePath.getProperties()
                                         ? 0
                                         : absoluteFilePath.getProperties().size());

                json.addProperty("filename", absoluteFilePath.getFilename());
                json.addProperty("id", file.getId());
                json.addProperty("success", true);
                json.add("levels", gson.fromJson(file.getContextJsonObj(), JsonArray.class));
                json.addProperty("active", file.isActive());
                json.addProperty("path", absoluteFilePath.getPath());
                if (repository.isAccessControlEnabled())
                {
                    if (null == rulesWrapper)
                        json.addProperty("editable", false);
                    else
                    {
                        rulesWrapper.executeRuleFor(file);
                        json.addProperty("editable", file.isEditable);
                    }
                } else
                    json.addProperty("editable", true);

                if (file.isSecure())
                {
                    json.add("sp", file.getSecurityProfile().toJson());

                    if (!Utils.isBlank(password))
                    {
                        if (!file.getSecurityProfile().isSecretValid(password))
                            throw new ConfigException(Error.Code.INVALID_PASSWORD);

                        if (file.isEncrypted())
                            file.decryptFile(password);

                        json.addProperty("content", file.getContent());
                        json.addProperty("unlocked", true);
                    }
                    else
                    {
                        if (!file.isEncrypted())
                            json.addProperty("content", file.getContent());
                    }
                }
                else
                {
                    json.addProperty("content", file.getContent());
                }
            }
            else
            {
                json.addProperty("message", "Specified file cannot be found.");
                json.addProperty("success", false);
            }
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
            json.addProperty("message", e.getMessage());
            json.addProperty("success", false);
        }
        finally
        {
            store.close();
        }

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }
}