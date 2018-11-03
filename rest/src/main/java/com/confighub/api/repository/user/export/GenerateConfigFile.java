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

package com.confighub.api.repository.user.export;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.FileHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.resolver.Context;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;

@Deprecated
@Path("/generateConfigFile")
public class GenerateConfigFile
        extends AUserAccessValidation
{
    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Authorization") String token,
                        @FormParam("context") String ctx,
                        @FormParam("comments") boolean comments,
                        @FormParam("formatType") String formatType)
    {

        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            Collection<CtxLevel> ctxLevels = ContextParser.parseAndCreate( ctx, repository, store, user, null);
            Context context = new Context( store, repository, ctxLevels, null);

            if (!context.isFullContext())
                throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

            FileHelper.FileOutputFormatType format;
            try
            {
                format = FileHelper.FileOutputFormatType.valueOf(formatType);
            }
            catch (Exception ignore)
            {
                format = FileHelper.FileOutputFormatType.Text;
            }

            Map<PropertyKey, Property> resolved = context.resolveForClient();

//            switch (format)
//            {
//                case Text:
//                    json.addProperty("file", FileHelper.getPlain(resolved, comments));
//                    break;
//
//                case JSON_Array:
//                    json.add("file", FileHelper.getJsonArray(resolved, comments));
//                    break;
//
//                case JSON_Map:
//                    json.add("file", FileHelper.getJsonMap(resolved, comments));
//                    break;
//
//                case JSON_Simple_Map:
//                    json.add("file", FileHelper.getJsonMapSimple(resolved, comments));
//                    break;
//            }


            json.addProperty("success", true);
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
