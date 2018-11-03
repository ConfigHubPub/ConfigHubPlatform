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

package com.confighub.api.repository.user.editor;

import com.confighub.api.repository.user.AUserAccessValidation;
import com.confighub.api.util.GsonHelper;
import com.confighub.core.error.ConfigException;
import com.confighub.core.repository.*;
import com.confighub.core.resolver.Context;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
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
import java.util.Collection;
import java.util.Date;

@Path("/keyProperties")
public class KeyProperties
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(KeyProperties.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @HeaderParam("Authorization") String token,
                        @QueryParam("key") String keyString,
                        @QueryParam("context") String contextString,
                        @QueryParam("sk") String secretKey,
                        @QueryParam("allValues") boolean allValues,
                        @QueryParam("keyView") boolean keyView,
                        @QueryParam("ts") Long ts,
                        @QueryParam("tag") String tagLabel,
                        @QueryParam("literal") boolean literal)
    {
        Gson gson = new Gson();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            Date dateObj = DateTimeUtils.dateFromTsOrTag(Utils.isBlank(tagLabel)
                                                                 ? null
                                                                 : store.getTag(repository.getId(), tagLabel),
                                                         ts,
                                                         repository.getCreateDate());

            PropertyKey key = store.getKey(repository, keyString, dateObj);
            if (null == key)
            {
                JsonObject json = new JsonObject();
                json.addProperty("no_key", true);
                json.addProperty("success", true);
                return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
            }

            store.begin();
            JsonObject json = GsonHelper.keyToGSON(key);
            Collection<CtxLevel> ctx = ContextParser.parseAndCreate( contextString, repository, store, user, dateObj);
            Context context = new Context(store, repository, ctx, dateObj);

            SecurityProfile ep = key.getSecurityProfile();
            if (null != ep)
            {
                boolean validSK = false;
                try { validSK = !Utils.isBlank(secretKey) && ep.isSecretValid(secretKey); }
                catch (Exception e) { ep = null; }

                if (validSK)
                    ep.sk = secretKey;
                else
                    ep = null;
            }

            if (keyView)
                json.add("properties",
                         GsonHelper.propertyListToGSON(repository,
                                                       repository.getRulesWrapper(user),
                                                       key.getProperties(),
                                                       ep));
            else if (literal)
                json.add("properties", getKeyLiteralProperties(user, repository, context, ep, keyString, allValues));
            else if (allValues)
                json.add("properties", getContextCategorizedProperties(user, repository, context, ep, keyString));
            else
                json.add("properties", getContextRelevantProperties(user, repository, context, ep, keyString));

            json.addProperty("success", true);

            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        catch (ConfigException e)
        {
            e.printStackTrace();
            JsonObject json = new JsonObject();

            json.addProperty("success", false);
            json.addProperty("message", e.getMessage());
            return Response.ok(gson.toJson(json), MediaType.APPLICATION_JSON).build();
        }
        finally
        {
            store.close();
        }
    }



    /**
     * @param user
     * @param repository
     * @param context
     * @param keys
     * @return
     * @throws ConfigException
     */
    public static JsonArray getContextCategorizedProperties(UserAccount user,
                                                            Repository repository,
                                                            Context context,
                                                            SecurityProfile ep,
                                                            String... keys)
            throws ConfigException
    {
        JsonArray config = new JsonArray();

        Collection<Property> properties = context.contextualSplitKeyResolver(keys);
        AccessRuleWrapper ruleWrapper = repository.getRulesWrapper(user);

        for (Property property : properties)
            config.add(GsonHelper.propertyToGSON(repository, property, ruleWrapper, ep));

        return config;
    }

    /**
     * @param user
     * @param repository
     * @param context
     * @param keys
     * @return
     * @throws ConfigException
     */
    public static JsonArray getContextRelevantProperties(UserAccount user,
                                                         Repository repository,
                                                         Context context,
                                                         SecurityProfile ep,
                                                         String... keys)
            throws ConfigException
    {
        return GsonHelper.propertyListToGSON(repository,
                                             repository.getRulesWrapper(user),
                                             context.keyResolver(keys),
                                             ep);
    }

    public static JsonArray getKeyLiteralProperties(UserAccount user,
                                                    Repository repository,
                                                    Context context,
                                                    SecurityProfile ep,
                                                    String key,
                                                    boolean allValues)
            throws ConfigException
    {
        return GsonHelper.propertyListToGSON(repository,
                                             repository.getRulesWrapper(user),
                                             context.literalKeyResolver(key, allValues),
                                             ep);
    }

}

