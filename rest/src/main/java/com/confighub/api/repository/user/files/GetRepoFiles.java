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
import com.confighub.core.error.Error;
import com.confighub.core.repository.AbsoluteFilePath;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.resolver.Context;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
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
import java.util.*;

@Path("/getRepoFiles")
public class GetRepoFiles
    extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(GetRepoFiles.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @QueryParam("context") String contextString,
                        @QueryParam("all") boolean all,
                        @QueryParam("ts") Long ts,
                        @QueryParam("tag") String tagLabel,
                        @QueryParam("searchTerm") String searchTerm,
                        @QueryParam("searchResolved") boolean searchResolved,
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

            if (null != dateObj)
                repository = store.getRepository(repository.getId(), dateObj);

            Collection<CtxLevel> ctx = ContextParser.parseAndCreate( contextString, repository, store, user, dateObj, true);
            Context context = new Context(store, repository, ctx, dateObj, all);

            Map<AbsoluteFilePath, Collection<RepoFile>> resolved = context.resolveFiles(user, searchTerm, searchResolved);

            Map<String, JsonObject> directoryMap = new HashMap<>();
            Set<String> absPaths = new HashSet<>();

            AccessRuleWrapper rulesWrapper = repository.getRulesWrapper(user);

            for (AbsoluteFilePath absoluteFilePath : resolved.keySet())
            {
                absPaths.add(absoluteFilePath.getPath());

                JsonObject directory = directoryMap.get(absoluteFilePath.getPath());
                if (null == directory)
                {
                    directory = new JsonObject();
                    String folderName;
                    int index = -1;
                    if (null != absoluteFilePath.getPath())
                        index = absoluteFilePath.getPath().lastIndexOf("/");
                    if (-1 == index)
                        folderName = absoluteFilePath.getPath();
                    else
                        folderName = absoluteFilePath.getPath().substring(index + 1,
                                                                          absoluteFilePath.getPath().length());

                    directory.addProperty("name", folderName);
                    directory.addProperty("path", absoluteFilePath.getPath());
                    directory.add("files", new JsonArray());
                    directoryMap.put(absoluteFilePath.getPath(), directory);
                }

                JsonArray files = directory.getAsJsonArray("files");

                resolved.get(absoluteFilePath).stream().forEach(f -> {
                    JsonObject file = new JsonObject();

                    file.addProperty("id", f.getId());
                    file.addProperty("name", absoluteFilePath.getFilename());
                    file.addProperty("fullPath", absoluteFilePath.getAbsPath());

                    if (repository.isAccessControlEnabled())
                    {
                        if (null == rulesWrapper)
                            file.addProperty("editable", false);
                        else
                        {
                            rulesWrapper.executeRuleFor(f);
                            file.addProperty("editable", f.isEditable);
                        }
                    } else
                        file.addProperty("editable", true);

                    if (f.isSecure())
                        file.addProperty("spName", f.getSecurityProfile().getName());

                    file.add("levels", gson.fromJson(f.getContextJson(), JsonArray.class));
                    file.addProperty("active", f.isActive());
                    file.addProperty("score", f.getContextWeight());

                    files.add(file);
                });
            }

            // Fill in the blank paths
            absPaths.stream().forEach(path -> {
                if (!Utils.isBlank(path))
                {
                    String[] folders = path.split("/");
                    String folder = "";

                    for (int i = 0; i < folders.length; i++)
                    {
                        if (i == 0)
                            folder = folders[0];
                        else
                            folder += "/" + folders[i];

                        if (!directoryMap.containsKey(folder))
                        {
                            JsonObject directory = new JsonObject();
                            directory.addProperty("name", folders[i]);
                            directory.addProperty("path", folder);
                            directory.add("files", new JsonArray());

                            directoryMap.put(folder, directory);
                        }
                    }
                }
            });

            JsonArray directories = new JsonArray();
            directoryMap.values().stream().forEach(d -> directories.add(d));
            json.add("data", directories);
            json.addProperty("success", true);

        }
        catch (ConfigException e)
        {
            e.printStackTrace();

            if (e.getErrorCode().equals(Error.Code.CONTEXT_SCOPE_MISMATCH))
                json.addProperty("resetContext", true);

            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
        }
        finally
        {
            store.close();
        }

        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
    }

}
