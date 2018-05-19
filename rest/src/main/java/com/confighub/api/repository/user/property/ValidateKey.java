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

package com.confighub.api.repository.user.property;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.KeyUtils;
import com.confighub.core.store.Store;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.Set;

@Path("/validateKey")
public class ValidateKey
        extends AUserAccessValidation
{
    @POST
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @FormParam("fromKey") String fromKeyString,
                        @FormParam("toKey") String toKeyString,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            if (!Utils.isKeyValid(toKeyString))
            {
                json.addProperty("success", false);
                json.addProperty("error", "invalidKey");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            PropertyKey fromKey = store.getKey(repository, fromKeyString);
            PropertyKey toKey = store.getKey(repository, toKeyString);

            if (null != fromKey && fromKey.equals(toKey))
            {
                json.addProperty("success", true);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            // If changing from a key, make sure user has permissions to change this key
            if (null != fromKey)
            {
                if (!KeyUtils.isKeyEditable(repository, fromKey, fromKeyString, user))
                {
                    json.addProperty("success", false);
                    json.addProperty("keyEditable", false);
                    json.addProperty("error", "fromKeyCannotChange");
                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }
            }

            // Make sure you can change to this key
            if (!KeyUtils.isKeyEditable(repository, null, toKeyString, user))
            {
                json.addProperty("success", false);
                json.addProperty("keyEditable", false);
                json.addProperty("error", "toKeyCannotChange");
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            // if fromKey is not blank, than user wants to merge two keys to the new one (toKey)
            boolean isMerging = null != fromKey && null != toKey;

            if (isMerging)
            {
                // For merging to be valid, user has to:
                //
                // 1. EncryptionProfiles for both keys have to be the same
                // 2. EncryptionProfiles for both keys have to be the same
                // 3. have permission to change from key
                // 4. have permission to change all values assigned to the fromKey
                // 5. have access to the toKey
                // 6. there cannot be any context conflicts between properties of the two keys

                // 1
                if (repository.isSecurityProfilesEnabled() &&
                    !Utils.same(fromKey.getSecurityProfile(), toKey.getSecurityProfile()))
                {
                    json.addProperty("success", false);
                    json.addProperty("error", "encryption");

                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }

                // 2
                if (repository.isValueTypeEnabled() &&
                    !Utils.same(fromKey.getValueDataType(), toKey.getValueDataType()))
                {
                    json.addProperty("success", false);
                    json.addProperty("error", "vdt");

                    return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                }

                // 3, 4, 5 - already checked

                // 6
                Set<Property> fromProperties = fromKey.getProperties();
                Iterator<Property> itt = fromProperties.iterator();
                while(itt.hasNext())
                {
                    Property p = itt.next();
                    itt.remove();

                    p.setPropertyKey(toKey);
                    try {
                        p.enforce();
                    }
                    catch (ConfigException ee) {
                        json.addProperty("success", false);
                        json.addProperty("keyEditable", false);
                        json.addProperty("error", "conflict");
                        json.addProperty("readme", Utils.jsonString(fromKey.getReadme()));
                        json.addProperty("message", ee.getErrorCode().getMessage());

                        return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
                    }
                }
            }

            json.addProperty("success", true);
            json.addProperty("keyEditable", true);
            json.addProperty("merging", isMerging);
            json.addProperty("readme", null == toKey ? "" : Utils.jsonString(toKey.getReadme()));
            json.addProperty("uses", isMerging
                    ? toKey.getProperties().size() + fromKey.getProperties().size()
                    : null == toKey ? 0 : toKey.getProperties().size());
            json.addProperty("attributeLock", null != toKey);
            if (null != toKey)
            {
                json.addProperty("vdt", toKey.getValueDataType().name());
                SecurityProfile ep = toKey.getSecurityProfile();
                if (null != ep)
                {
                    json.addProperty("spName", ep.getName());
                    json.addProperty("encrypted", null != ep.getCipher());
                    json.addProperty("spCipher", null == ep.getCipher() ? "" : ep.getCipher().getName());
                }
            }

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }
}
