/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.util;

import com.confighub.core.error.ConfigException;
import com.confighub.core.organization.Team;
import com.confighub.core.repository.*;
import com.confighub.core.resolver.Context;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.security.Encryption;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Pair;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 *
 */
public abstract class GsonHelper
{
    private static final Logger log = LogManager.getLogger(GsonHelper.class);

    public static JsonObject keyAndPropertiesToGSON(Repository repository,
                                                    AccessRuleWrapper accessRuleWrapper,
                                                    PropertyKey key,
                                                    SecurityProfile ep,
                                                    Collection<Property> properties)
    {
        JsonObject json = keyToGSON(key);
        json.add("properties", propertyListToGSON(repository, accessRuleWrapper, properties, ep));

        return json;
    }

    public static JsonObject keyAuditToGSON(PropertyKey key)
    {
        JsonObject json = keyToGSON(key);

        String diff = key.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        return json;

    }

    public static JsonObject keyToGSON(PropertyKey key)
    {
        JsonObject json = new JsonObject();
        json.addProperty("key", key.getKey());
        json.addProperty("id", key.getId());
        json.add("1", keyAttributesToGSON(key));

        return json;
    }


    public static JsonObject keyAttributesToGSON(PropertyKey key)
    {
        JsonObject attr = new JsonObject();

        attr.addProperty("vdt", key.getValueDataType().name());
        attr.addProperty("pushEnabled", key.isPushValueEnabled());
        attr.addProperty("readme", Utils.jsonString(key.getReadme()));
        attr.addProperty("deprecated", key.isDeprecated());
        attr.addProperty("uses", 0 == key.propertyCount ? key.getProperties().size() : key.propertyCount);
        attr.addProperty("files", null == key.getFiles() ? 0 : key.getFiles().size());

        SecurityProfile ep = key.getSecurityProfile();

        if (null != ep)
        {
            attr.addProperty("spName", ep.getName());
            attr.addProperty("encrypted", null != ep.getCipher());
            attr.addProperty("spCipher", null == ep.getCipher() ? "" : ep.getCipher().getName());
        }

        return attr;
    }





    public static JsonArray propertyListToGSON(Repository repository,
                                               AccessRuleWrapper accessRuleWrapper,
                                               Collection<Property> properties,
                                               SecurityProfile ep,
                                               Pair<String, String>... attributes)
    {
        JsonArray jsonArray = new JsonArray();

        if (null == properties)
            return jsonArray;

        for (Property property : properties)
            jsonArray.add(propertyToGSON(repository, property, accessRuleWrapper, ep, null, attributes));

        return jsonArray;
    }


    public static void setValue(JsonObject json, PropertyKey.ValueDataType type, String value, final Property property)
    {
        switch (type)
        {
            case List:
                if (null == value)
                    json.add("value", null);
                else if (Utils.isBlank(value))
                    json.addProperty("value", "");
                else
                {
                    try
                    {
                        JsonArray arr = new Gson().fromJson(value, JsonArray.class);
                        json.add("value", arr);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        log.error("Unable to get value for property: " + property);
                        json.addProperty("value", Utils.jsonString(property.getValue()));
                        json.addProperty("valueErr", true);
                    }
                }
                break;

            case Map:
                if (null == value)
                    json.add("value", null);
                else if (Utils.isBlank(value))
                    json.addProperty("value", "");
                else
                {
                    try
                    {
                        JsonObject obj = new Gson().fromJson(value, JsonObject.class);
                        json.add("value", obj);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        log.error("Unable to get value for property: " + property);
                        json.addProperty("value", Utils.jsonString(property.getValue()));
                        json.addProperty("valueErr", true);
                    }
                }

                break;

            case FileRef:
            case FileEmbed:
                AbsoluteFilePath absoluteFilePath = property.getAbsoluteFilePath();
                if (null == absoluteFilePath)
                    json.add("value", null);
                else
                    json.addProperty("value", absoluteFilePath.getAbsPath());
                break;

            default:
                if (null == value)
                    json.add("value", null);
                else
                    json.addProperty("value", Utils.jsonString(value));
                break;
        }
    }

    public static JsonObject propertyToGSON(Repository repository,
                                            Property prop,
                                            AccessRuleWrapper accessRuleWrapper,
                                            SecurityProfile ep)
    {
        return propertyToGSON(repository, prop, accessRuleWrapper, ep, null, false, null);
    }


    public static JsonObject propertyToGSON(Repository repository,
                                            Property property,
                                            AccessRuleWrapper accessRuleWrapper,
                                            SecurityProfile ep,
                                            PropertyAttn attention,
                                            Pair<String, String>... attributes)
    {
        return propertyToGSON(repository, property, accessRuleWrapper, ep, attention, false, attributes);
    }

    public static JsonObject propertyToGSON(Repository repository,
                                            Property property,
                                            AccessRuleWrapper accessRuleWrapper,
                                            SecurityProfile ep,
                                            PropertyAttn attention,
                                            boolean includeDiffJson,
                                            Pair<String, String>... attributes)
    {
        JsonObject json = new JsonObject();

        if (includeDiffJson)
        {
            String diff = property.getDiffJson();
            if (Utils.isBlank(diff))
                json.add("diff", null);
            else
                json.add("diff", new Gson().fromJson(diff, JsonObject.class));

            json.addProperty("key", property.getKey());
            if (null != ep)
                json.addProperty("spName", ep.getName());
        }

        json.addProperty("id", property.getId());

        if (property.isEncrypted()) // repository.isSecurityProfilesEnabled()
        {
            int encryptionState; // = property.isEncrypted() ? 1 : 0;
            if (null != ep)
            {
                try
                {
                    if (ep.encryptionEnabled() && !Utils.isBlank(ep.sk))
                    {
                        setValue(json,
                                 property.getPropertyKey().getValueDataType(),
                                 ep.decrypt(property.getValue(), ep.sk),
                                 property);
                        encryptionState = 2;
                    } else
                    {
                        setValue(json, property.getPropertyKey().getValueDataType(), "", property);
                        encryptionState = 1;
                    }
                }
                catch (ConfigException e)
                {
                    e.printStackTrace();
                    log.error("Unable to get value for property: " + property);
                    json.addProperty("value", Utils.jsonString(property.getValue()));
                    encryptionState = 1;
                }
            } else if (property.isEncrypted())
            {
                encryptionState = 1;
                json.addProperty("value", "");
            } else
            {
                encryptionState = 0;
                setValue(json, property.getPropertyKey().getValueDataType(), property.getValue(), property);
            }

            json.addProperty("encryptionState", encryptionState);
            json.addProperty("secure", property.isSecure());
        } else
        {
            setValue(json, property.getPropertyKey().getValueDataType(), property.getValue(), property);
        }


        if (repository.isAccessControlEnabled())
        {
            if (null == accessRuleWrapper)
                json.addProperty("editable", false);
            else
            {
                accessRuleWrapper.executeRuleFor(property);
                json.addProperty("editable", property.isEditable);
            }
        } else
            json.addProperty("editable", true); // ToDO: OOh, this needs love - repository.hasWriteAccess(user)

        json.addProperty("active", property.isActive());

        if (null == property.type)
            property.type = Context.PropertyType.match;

        json.addProperty("score", property.getContextWeight() + (property.isActive() ? property.type.scoreSupplement : 0));
        json.addProperty("type", property.type.name());

        if (null != attention)
            json.addProperty("attn", attention.name());

        // Property.levelsToJson(property.getDepthMap())
        json.add("levels", property.getContextJsonObj());

        if (null != attributes && attributes.length > 0)
        {
            for (Pair<String, String> pair : attributes)
                json.addProperty(pair.car, pair.cdr);
        }

        return json;
    }

    public static JsonObject levelAuditToGSON(Level l)
    {
        JsonObject json = levelToGSON(l);

        String diff = l.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        switch (l.getType())
        {
            case Group:
            {
                JsonArray assignments = new JsonArray();
                l.getMembers().forEach(c -> assignments.add(c.getName()));
                json.add("assignments", assignments);
                break;
            }
            case Member:
            {
                JsonArray assignments = new JsonArray();
                l.getGroups().forEach(c -> assignments.add(c.getName()));
                json.add("assignments", assignments);
                break;
            }
        }
        return json;
    }

    public static JsonObject levelToGSON(Level l)
    {
        JsonObject json = new JsonObject();

        json.addProperty("id", l.getId());
        json.addProperty("name", l.getName());
        json.addProperty("type", l.getType().name());
        json.addProperty("score", l.getContextScore());
        json.addProperty("p", l.getDepth().getPlacement());

        return json;
    }

    //---------------------------------------------------------------------------------
    // SecurityProfile
    //---------------------------------------------------------------------------------

    public static JsonObject securityProfileAuditGSON(SecurityProfile sp)
    {
        JsonObject json = new JsonObject();
        if (null == sp) return json;

        json.addProperty("spName", sp.getName());
        json.addProperty("encrypted", null != sp.getCipher());
        json.addProperty("spCipher", null == sp.getCipher() ? "" : sp.getCipher().getName());

        String diff = sp.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        return json;
    }

    public static JsonObject toJson(final SecurityProfile ep, String profile, Collection<PropertyKey> keys)
    {
        JsonObject jProfile = new JsonObject();
        jProfile.addProperty("name", ep.getName());


        if (null != ep.getCipher())
        {
            jProfile.addProperty("cipher", ep.getCipher().getName());
            jProfile.addProperty("max", ep.getCipher().getMax());
            jProfile.addProperty("min", ep.getCipher().getMin());
        }
        else
            jProfile.addProperty("cipher", "None");

        JsonArray jKeys = new JsonArray();

        if (null != keys)
        {
            for (PropertyKey key : keys)
                jKeys.add(keyToGSON(key));
            jProfile.add("keys", jKeys);
        }

        return jProfile;
    }

    //---------------------------------------------------------------------------------
    // Repository
    //---------------------------------------------------------------------------------
    public static JsonObject repositoryAuditToJSON(Repository repository)
    {
        JsonObject json = repositoryJson(repository);

        String diff = repository.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        return json;
    }

    public static JsonObject repositoryToJSON(Repository repository)
    {
        JsonObject json = repositoryJson(repository);
        json.addProperty("creationTs", repository.getCreateDate().getTime());
        json.addProperty("demo", repository.isDemo());

        return json;
    }

    private static JsonObject repositoryJson(Repository repository)
    {
        JsonObject json = new JsonObject();

        json.addProperty("name", repository.getName());
        json.addProperty("description", Utils.jsonString(repository.getDescription()));
        json.addProperty("isPrivate", repository.isPrivate());
        json.addProperty("id", repository.getId());
        json.addProperty("accessControlEnabled", repository.isAccessControlEnabled());
        json.addProperty("valueTypeEnabled", repository.isValueTypeEnabled());
        json.addProperty("securityProfilesEnabled", repository.isSecurityProfilesEnabled());
        json.addProperty("contextClustersEnabled", repository.isContextClustersEnabled());
        json.addProperty("owner", repository.getAccountName());
        json.addProperty("isPersonal", repository.isPersonal());
        json.addProperty("adminContextControlled", repository.isAdminContextControlled());
        json.addProperty("scopeSize", repository.getDepth().getIndex());
        json.addProperty("tokenlessAPIPull", repository.isAllowTokenFreeAPIPull());
        json.addProperty("tokenlessAPIPush", repository.isAllowTokenFreeAPIPush());
        json.add("labels", getRepositoryDepthLabels(repository));

        return json;
    }

    public static JsonObject getRepositoryDepthLabels(Repository repository)
    {
        JsonObject json = new JsonObject();
        for (Depth depth : repository.getDepth().getDepths())
            json.addProperty(String.valueOf(depth.getPlacement()), repository.getLabel(depth));
        return json;
    }


    //---------------------------------------------------------------------------------
    // Teams
    //---------------------------------------------------------------------------------
    public static JsonArray getTeams(Set<Team> teams)
    {
        JsonArray teamsJ = new JsonArray();
        if (null != teams)
        {
            for (Team team : teams)
            {
                JsonObject teamJ = new JsonObject();
                teamJ.addProperty("name", team.getName());
                teamJ.addProperty("description", Utils.jsonString(team.getDescription()));
                teamJ.addProperty("stopOnFirstMatch", team.isStopOnFirstMatch());
                teamJ.addProperty("unmatchedEditable", team.isUnmatchedEditable());

                teamsJ.add(teamJ);
            }
        }
        return teamsJ;
    }

    public static JsonArray teamMembers(Team team)
    {
        JsonArray membersJ = new JsonArray();
        Set<UserAccount> members = team.getMembers();
        if (null == members)
            return membersJ;

        for (UserAccount member : members)
        {
            JsonObject memberJ = new JsonObject();
            memberJ.addProperty("un", member.getUsername());
            memberJ.addProperty("name", member.getName());

            membersJ.add(memberJ);
        }

        return membersJ;
    }

    public static JsonArray allMembers(Repository repository)
    {
        Set<Team> teams = repository.getTeams();
        JsonArray json = new JsonArray();

        if (null == teams)
            return json;

        for (Team team : teams)
        {
            for (UserAccount member : team.getMembers())
            {
                JsonObject u = new JsonObject();
                u.addProperty("un", member.getUsername());
                u.addProperty("name", member.getName());
                u.addProperty("team", team.getName());

                json.add(u);
            }
        }

        return json;
    }

    public static JsonObject teamToJson(Team team)
    {
        JsonObject teamJ = new JsonObject();
        if (null == team)
            return teamJ;

        teamJ.addProperty("name", team.getName());
        // Members
        teamJ.add("members", teamMembers(team));

        // Rules
        if (team.getRepository().isAccessControlEnabled())
        {
            teamJ.add("accessRules", ruleToJson(team));
            teamJ.addProperty("unmatchedEditable", team.isUnmatchedEditable());
            teamJ.addProperty("stopOnFirstMatch", team.isStopOnFirstMatch());
        }

        return teamJ;
    }

    public static JsonObject teamAuditToJSON(Team team)
    {
        JsonObject json = new JsonObject();

        json.addProperty("name", team.getName());


        String diff = team.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        return json;
    }

    // TODO
    public static JsonObject accessRuleAuditToJSON(AccessRule rule)
    {
        JsonObject json = accessRuleToJson(rule);

        String diff = rule.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        json.addProperty("team", rule.getTeam() == null ? "null":rule.getTeam().getName());

        return json;
    }

    public static JsonArray ruleToJson(Team team)
    {
        List<AccessRule> accessRules = team.getAccessRules();
        JsonArray json = new JsonArray();

        if (null != accessRules)
        {
            for (AccessRule accessRule : accessRules)
                json.add(accessRuleToJson(accessRule));
        }

        return json;
    }

    public static JsonObject accessRuleToJson(AccessRule accessRule)
    {
        JsonObject json = new JsonObject();
        json.addProperty("priority", accessRule.getPriority());
        json.addProperty("id", accessRule.getId());
        json.addProperty("type", accessRule.getRuleTarget().name());
        json.addProperty("access", accessRule.isCanEdit() ? "rw" : "ro");

        if (AccessRule.RuleTarget.Key.equals(accessRule.getRuleTarget()))
        {
            json.addProperty("match", Utils.jsonString(accessRule.getKeyMatchType()));
            json.addProperty("key", Utils.jsonString(accessRule.getMatchValue()));
        }
        else if (AccessRule.RuleTarget.Value.equals(accessRule.getRuleTarget()))
        {
            json.addProperty("match", Utils.jsonString(accessRule.getContextMatchType()));
            json.add("context", accessRule.getContextJsonObj());
        }

        return json;
    }


    public enum PropertyAttn {
        equalPriority,
        highPriority,
        lowPriority,
        conflict
    }

    public static JsonObject tokenAuditToJSON(Token token)
    {
        JsonObject json = tokenToJson(token, false);

        String diff = token.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        return json;
    }

    public static JsonObject tokenToJson(Token token, boolean includeToken)
    {
        JsonObject json = new JsonObject();

        if (includeToken)
            json.addProperty("token", token.getToken());

        json.addProperty("id", token.getId());
        json.addProperty("createdOn", token.getCreatedOn());
        json.addProperty("expires", token.getExpires());
        json.addProperty("expired", token.isExpired());
        json.addProperty("name", Utils.jsonString(token.getName()));
        json.addProperty("active", token.isActive());
        json.addProperty("forceKeyPushEnabled", token.isForceKeyPushEnabled());
        json.addProperty("user", null == token.getUser() ? null : token.getUser().getUsername());
        json.addProperty("rulesTeam", null == token.getTeamRules() ? null : token.getTeamRules().getName());
        json.addProperty("managedBy", null == token.getManagedBy() ? "All" : token.getManagedBy().name());
        json.addProperty("managingTeam", null == token.getManagingTeam() ? null : token.getManagingTeam().getName());

        Set<SecurityProfile> securityProfiles = token.getSecurityProfiles();
        if (null != securityProfiles && securityProfiles.size() > 0)
        {
            try
            {
                JsonArray sps = new JsonArray();
                for (SecurityProfile sp : securityProfiles)
                    sps.add(sp.getName());

                json.add("sps", sps);
            }
            catch (Exception e)
            {
                json.addProperty("error", "Token error:  " + e.getMessage());
            }
        }

        return json;
    }

    public static JsonObject tagAuditToJSON(Tag tag)
    {
        JsonObject json = tag.toJson();

        String diff = tag.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        return json;
    }

    public static JsonObject absFileAuditToJSON(AbsoluteFilePath absoluteFilePath)
    {
        JsonObject json = absoluteFilePath.toJson();

        String diff = absoluteFilePath.getDiffJson();
        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
            json.add("diff", new Gson().fromJson(diff, JsonObject.class));

        return json;
    }

    public static JsonObject fileAuditToJSON(RepoFile configFile,
                                             SecurityProfile ep,
                                             Store store,
                                             UserAccount user,
                                             Repository repository,
                                             Long ts)
    {
        JsonObject json = new JsonObject();
        json.addProperty("fileName", configFile.getAbsFilePath().getFilename());
        json.addProperty("absPath", configFile.getAbsPath());

        boolean diffEncrypted = false,
                entryEncrypted = configFile.isEncrypted();

        boolean showContent = true; //entryEncrypted;

        String diff = configFile.getDiffJson();

        if (Utils.isBlank(diff))
            json.add("diff", null);
        else
        {
            JsonObject diffJson = new Gson().fromJson(diff, JsonObject.class);
            JsonElement encEl = diffJson.get("encrypted");
            diffEncrypted = null != encEl && encEl.getAsBoolean();

            showContent = (!diffEncrypted && !entryEncrypted) || !entryEncrypted;

            if (!showContent)
            {
                diffJson.remove("content");
                diffJson.addProperty("content", "");
                diffJson.addProperty("encryptionState", 1);
            }
            else {
                if (diffEncrypted) {

                    String oldSpName = diffJson.get("spName").getAsString().trim();

                    Date dateObj = DateTimeUtils.dateFromTs(ts, null);
                    SecurityProfile oldSp = store.getSecurityProfile(user, repository, dateObj, oldSpName);

                    String oldDecrypted = Encryption.decrypt(oldSp.getCipher(),
                                                             diffJson.get("content").getAsString().trim(),
                                                             oldSp.getDecodedPassword());


                    diffJson.addProperty("content", oldDecrypted);
                    diffJson.remove("encrypted");
                }
            }

            json.add("diff", diffJson);
        }

        json.addProperty("spName",
                         null == configFile.getSecurityProfile() ? "" : configFile.getSecurityProfile().getName());

        json.addProperty("score", configFile.getContextWeight());
        json.add("levels", configFile.getContextJsonObj());

        json.addProperty("id", configFile.getId());
        json.addProperty("active", configFile.isActive());

        if (entryEncrypted)
        {
            int encryptionState;
            if (null != ep)
            {
                try
                {
                    if (ep.encryptionEnabled() && !Utils.isBlank(ep.sk))
                    {
                        configFile.decryptFile(ep.sk);
                        json.addProperty("content", configFile.getContent());
                        encryptionState = 2;
                    } else
                    {
                        json.addProperty("content", "");
                        encryptionState = 1;
                    }
                }
                catch (ConfigException e)
                {
                    e.printStackTrace();
                    log.error("Unable to get contents of file: " + configFile);
                    json.addProperty("content", "");
                    encryptionState = 1;
                }
            } else if (diffEncrypted || entryEncrypted)
            {
                encryptionState = 1;
                json.addProperty("content", "");
            } else
            {
                encryptionState = 0;
                json.addProperty("content", configFile.getContent());
            }

            json.addProperty("encryptionState", encryptionState);
            json.addProperty("secure", configFile.isSecure());
        }
        else
            json.addProperty("content", !showContent ? "" : configFile.getContent());


        return json;
    }
}
