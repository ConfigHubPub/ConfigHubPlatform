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
import com.confighub.core.error.Error;
import com.confighub.core.repository.*;
import com.confighub.core.resolver.Context;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.security.SecurityProfile;
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

@Path("/compareResolver")
public class CompareResolver
        extends AUserAccessValidation
{
    private static final Logger log = LogManager.getLogger(EditorResolver.class);

    @GET
    @Path("/{account}/{repository}")
    @Produces("application/json")
    public Response get(@PathParam("account") String account,
                        @PathParam("repository") String repositoryName,
                        @QueryParam("aContext") String aContextString,
                        @QueryParam("aTag") String aTagLabel,
                        @QueryParam("aTs") Long aTs,

                        @QueryParam("bContext") String bContextString,
                        @QueryParam("bTag") String bTagLabel,
                        @QueryParam("bTs") Long bTs,

                        @QueryParam("all") boolean allKeys,
                        @QueryParam("diffOnly") boolean diffOnly,
                        @QueryParam("allValues") boolean allValues,
                        @QueryParam("key") String keyString,
                        @QueryParam("aPass") String aPass,
                        @QueryParam("bPass") String bPass,
                        @QueryParam("json") boolean json,
                        @HeaderParam("Authorization") String token)
    {
        JsonObject data = new JsonObject();
        Store store = new Store();

        try
        {
            int status = validate(account, repositoryName, token, store);
            if (0 != status) return Response.status(status).build();

            if (diffOnly)
                allKeys = false;

            Tag aTag = Utils.isBlank(aTagLabel) ? null : store.getTag(repository.getId(), aTagLabel);

            Date aDateObj = DateTimeUtils.dateFromTsOrTag(aTag,
                                                          aTs,
                                                          repository.getCreateDate());

            Tag bTag = Utils.isBlank(bTagLabel) ? null : store.getTag(repository.getId(), bTagLabel);
            Date bDateObj = DateTimeUtils.dateFromTsOrTag(bTag,
                                                          bTs,
                                                          repository.getCreateDate());

            Repository aRepo = repository, bRepo = repository;
            if (null != aDateObj)
                aRepo = store.getRepository(repository.getId(), aDateObj);
            if (null != bDateObj)
                bRepo = store.getRepository(repository.getId(), bDateObj);

            Collection<CtxLevel> aContextCtxLevels = ContextParser.parseAndCreate( aContextString,
                                                                                   aRepo,
                                                                                   store,
                                                                                   user,
                                                                                   aDateObj,
                                                                                   true);
            Collection<CtxLevel> bContextCtxLevels = ContextParser.parseAndCreate( bContextString,
                                                                                   bRepo,
                                                                                   store,
                                                                                   user,
                                                                                   bDateObj,
                                                                                   true);

            AccessRuleWrapper accessRuleWrapper = repository.getRulesWrapper(user);

            Context aContext = new Context( store, aRepo, aContextCtxLevels, aDateObj, allKeys);
            Context bContext = new Context( store, bRepo, bContextCtxLevels, bDateObj, allKeys);

            Map<PropertyKey, Collection<Property>> aResolved;
            Map<PropertyKey, Collection<Property>> bResolved;

            Map<String, SecurityProfile> aSecurityProfiles = new HashMap<>();
            Map<String, SecurityProfile> bSecurityProfiles = new HashMap<>();

            if (!Utils.isBlank(keyString))
            {
                aResolved = new HashMap<>();
                bResolved = new HashMap<>();
                Collection<Property> aProps;
                Collection<Property> bProps;

                if (!Utils.isBlank(aPass))
                {
                    PropertyKey aKey = store.getKey(aRepo, keyString, aDateObj);
                    if (null != aKey && aKey.isEncrypted())
                    {
                        SecurityProfile aSp = aKey.getSecurityProfile();
                        boolean validSK = false;
                        try { validSK = !Utils.isBlank(aPass) && aSp.isSecretValid(aPass); }
                        catch (Exception ignore) { }

                        if (validSK)
                        {
                            aSp.sk = aPass;
                            aSecurityProfiles.put(keyString, aSp);
                            data.addProperty("aDecrypted", true);
                        }
                    }
                }

                if (!Utils.isBlank(bPass))
                {
                    PropertyKey bKey = store.getKey(bRepo, keyString, bDateObj);
                    if (null != bKey && bKey.isEncrypted())
                    {
                        SecurityProfile bSp = bKey.getSecurityProfile();
                        boolean validSK = false;
                        try { validSK = !Utils.isBlank(bPass) && bSp.isSecretValid(bPass); }
                        catch (Exception ignore) { }

                        if (validSK)
                        {
                            bSp.sk = bPass;
                            bSecurityProfiles.put(keyString, bSp);
                            data.addProperty("bDecrypted", true);
                        }
                    }
                }

                if (allValues)
                    aProps = aContext.contextualSplitKeyResolver(keyString);
                else
                    aProps = aContext.keyResolver(keyString);

                if (null != aProps && aProps.size() > 0)
                    aResolved.put(aProps.iterator().next().getPropertyKey(), aProps);


                if (allValues)
                    bProps = bContext.contextualSplitKeyResolver(keyString);
                else
                    bProps = bContext.keyResolver(keyString);

                if (null != bProps && bProps.size() > 0)
                    bResolved.put(bProps.iterator().next().getPropertyKey(), bProps);

            } else
            {
                aResolved = aContext.resolve();
                bResolved = bContext.resolve();
            }


            List<Cmp> organized = organize(aResolved, bResolved, aSecurityProfiles, bSecurityProfiles);

            JsonArray config = new JsonArray();

            long start = System.currentTimeMillis();
            for (Cmp comparisonEntry : organized)
            {
                JsonObject entryJson = comparisonEntry.toJson(aRepo, bRepo, accessRuleWrapper, diffOnly, allValues);
                if (null != entryJson)
                    config.add(entryJson);
            }
            log.info(String.format("Json gen: %d/ms;", (System.currentTimeMillis() - start)));

            if (null != aDateObj && null != bDateObj)
                data.addProperty("date", aDateObj.compareTo(bDateObj));
            else if (null == aDateObj && null != bDateObj)
                data.addProperty("date", -1);
            else if (null == bDateObj && null != aDateObj)
                data.addProperty("date", 1);
            else
                data.addProperty("date", 0);

            data.add("diff", config);


        }
        catch (ConfigException e)
        {
            data.addProperty("error", e.getMessage());
            if (e.getErrorCode().equals(Error.Code.CONTEXT_SCOPE_MISMATCH))
                data.addProperty("resetContext", true);

            log.error("Compare error: " + e.getMessage());

            e.printStackTrace();
        }
        finally
        {
            store.close();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(data), MediaType.APPLICATION_JSON).build();
    }

    /**
     * @param aResolved
     * @param bResolved
     * @return
     */
    private List<Cmp> organize(Map<PropertyKey, Collection<Property>> aResolved,
                               Map<PropertyKey, Collection<Property>> bResolved,
                               Map<String, SecurityProfile> aSecurityProfiles,
                               Map<String, SecurityProfile> bSecurityProfiles)
    {
        List<Cmp> data = new ArrayList<>();

        Map<String, PropertyKey> aKeys = new HashMap<>();
        Map<String, PropertyKey> bKeys = new HashMap<>();

        for (PropertyKey key : aResolved.keySet())
            aKeys.put(key.getKey(), key);

        for (PropertyKey key : bResolved.keySet())
            bKeys.put(key.getKey(), key);

        for (String keyString : aKeys.keySet())
        {
            PropertyKey aKey, bKey;
            Collection<Property> aProps = null, bProps = null;

            aKey = aKeys.get(keyString);
            bKey = bKeys.remove(keyString);

            if (null != aKey)
                aProps = aResolved.get(aKey);
            if (null != bKey)
                bProps = bResolved.get(bKey);

            data.add(new Cmp(keyString,
                             aKey,
                             bKey,
                             aProps,
                             bProps,
                             aSecurityProfiles.get(keyString),
                             bSecurityProfiles.get(keyString)));
        }

        for (String keyString : bKeys.keySet())
        {
            PropertyKey bKey = bKeys.get(keyString);
            data.add(new Cmp(keyString,
                             null,
                             bKey,
                             null,
                             bResolved.get(bKey),
                             aSecurityProfiles.get(keyString),
                             bSecurityProfiles.get(keyString)));
        }

        return data;
    }


    /**
     *
     */
    private class Cmp
    {
        private final PropertyKey aKey, bKey;
        private final Collection<Property> aProps, bProps;
        private final String keyString;
        private final SecurityProfile aSp;
        private final SecurityProfile bSp;

        public Cmp(String keyString,
                   PropertyKey aKey,
                   PropertyKey bKey,
                   Collection<Property> aProps,
                   Collection<Property> bProps,
                   SecurityProfile aSp,
                   SecurityProfile bSp)
        {
            this.keyString = keyString;
            this.aKey = aKey;
            this.bKey = bKey;
            this.aProps = aProps;
            this.bProps = bProps;
            this.aSp = aSp;
            this.bSp = bSp;
        }

        @Override
        public String toString () {
            StringBuilder sb = new StringBuilder();
            sb.append("--------\n");
            sb.append("aKey: " + aKey + "\n");
            sb.append("bKey: " + bKey + "\n");
            sb.append("-- props --\n");
            if (null != aProps)
            {
                for (Property p : aProps)
                    sb.append(" ap: " + p + "\n");
            }

            if (null != bProps)
            {
                for (Property p : bProps)
                    sb.append(" bp: " + p + "\n");
            }

            return sb.toString();
        }

        public JsonObject toJson(final Repository aRepo,
                                 final Repository bRepo,
                                 final AccessRuleWrapper accessRuleWrapper,
                                 boolean diffOnly,
                                 boolean allValues)
        {
            JsonObject json = new JsonObject();

            JsonArray propertiesJson = new JsonArray();
            boolean hasDiff = false;

            // ToDo, if two properties' contexts are only different in the
            // depth that is different as per specified search contexts, then
            // show these on the same line, highlighted
            //
            // i.e.
            // Searching for:
            // A: foo, DEV,  bar
            // B: foo, PROD, bar
            //
            // properties with these contexts should be on the same line

            // Define property pairs
            if (null == aProps)
            {
                for (Property bProp : bProps)
                {
                    JsonObject pairJson = new JsonObject();

                    pairJson.addProperty("score", bProp.getContextWeight());
                    pairJson.add("2",
                                 GsonHelper.propertyToGSON(bRepo,
                                                           bProp,
                                                           accessRuleWrapper,
                                                           bSp /* SecurityProfile*/,
                                                           null,
                                                           null)); // ep
                    pairJson.addProperty("diff", -1);
                    hasDiff = true;
                    propertiesJson.add(pairJson);
                }
            } else if (null == bProps)
            {
                for (Property aProp : aProps)
                {
                    JsonObject pairJson = new JsonObject();

                    pairJson.addProperty("score", aProp.getContextWeight());
                    pairJson.add("0",
                                 GsonHelper.propertyToGSON(aRepo,
                                                           aProp,
                                                           accessRuleWrapper,
                                                           aSp /* SecurityProfile*/,
                                                           null,
                                                           null)); // ep
                    pairJson.addProperty("diff", -1);
                    hasDiff = true;
                    propertiesJson.add(pairJson);
                }
            } else
            {
                for (Property aProp : aProps)
                {
                    JsonObject pairJson = new JsonObject();

                    pairJson.addProperty("score", aProp.getContextWeight());
                    pairJson.add("0",
                                 GsonHelper.propertyToGSON(aRepo,
                                                           aProp,
                                                           accessRuleWrapper,
                                                           aSp /* SecurityProfile*/,
                                                           null,
                                                           null)); // ep

                    Iterator<Property> bPropsI = bProps.iterator();
                    int diff = -1;
                    while (bPropsI.hasNext())
                    {
                        Property bProp = bPropsI.next();
                        if (Utils.equal(aProp.getContextJson(), bProp.getContextJson()))
                        {
                            // ToDo if same, just add one

                            boolean equal;

                            if (allValues)
                                equal = Utils.same(aProp.getValue(), bProp.getValue()) &&
                                        aProp.isActive() == bProp.isActive() &&
                                        Utils.equal(aProp.type, bProp.type);
                            else
                                equal = Utils.same(aProp.getValue(), bProp.getValue()) &&
                                        aProp.isActive() == bProp.isActive();

                            if (equal)
                                diff = 0;
                            else
                            {
                                hasDiff = true;
                                diff = 1;
                            }

                            pairJson.add("2",
                                         GsonHelper.propertyToGSON(bRepo,
                                                                   bProp,
                                                                   accessRuleWrapper,
                                                                   bSp /* SecurityProfile*/,
                                                                   null,
                                                                   null));
                            bPropsI.remove();
                            break;
                        }
                    }

                    pairJson.addProperty("diff", diff);
                    propertiesJson.add(pairJson);
                }

                for (Property bProp : bProps)
                {
                    JsonObject pairJson = new JsonObject();

                    pairJson.addProperty("score", bProp.getContextWeight());
                    pairJson.add("2", GsonHelper.propertyToGSON(bRepo, bProp, accessRuleWrapper, bSp, null, null));
                    pairJson.addProperty("diff", -1);
                    hasDiff = true;
                    propertiesJson.add(pairJson);
                }
            }

            if (!allValues && (!hasDiff && diffOnly))
                return null;

            json.add("properties", propertiesJson);
            json.addProperty("key", keyString);

            if (null != aKey)
                json.add("0", GsonHelper.keyAttributesToGSON(aKey));

            if (null != bKey)
                json.add("2", GsonHelper.keyAttributesToGSON(bKey));

            return json;
        }
    }
}
