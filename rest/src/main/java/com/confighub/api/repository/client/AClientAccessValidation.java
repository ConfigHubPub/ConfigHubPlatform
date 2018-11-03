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

package com.confighub.api.repository.client;

import com.confighub.core.auth.Auth;
import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.*;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.Store;
import com.confighub.core.utils.ContextParser;
import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class AClientAccessValidation
{
    private static final Logger log = LogManager.getFormatterLogger(AClientAccessValidation.class);

    @Context
    private HttpServletRequest request;

    protected Repository repository;
    protected com.confighub.core.resolver.Context context;
    protected Token token;
    protected Tag tag;
    protected Date date;
    protected JsonObject json = new JsonObject();
    protected Map<PropertyKey, Property> resolved;
    protected Map<String, String> passwords = new HashMap<>();

    protected void getRepositoryFromToken(String clientToken, Store store)
            throws ConfigException
    {
        getRepositoryFromToken(clientToken, null, null, store);
    }

    protected void getRepositoryFromToken(String clientToken, String dateString, String tagString, Store store)
            throws ConfigException
    {
        Map<String, Object> payload;
        try
        {
            payload = Auth.validateApiToken(clientToken);
        }
        catch (ConfigException e)
        {
            log.error("Bad API token from " + (null == request ? "0.0.0.0" : request.getRemoteAddr()));
            throw e;
        }

        Long rid = Long.valueOf((int)payload.get("rid"));
        if (null == rid)
            throw new ConfigException(Error.Code.REPOSITORY_NOT_FOUND);

        if (!Utils.isBlank(tagString))
        {
            this.tag = store.getTag(rid, tagString);
            if (null == this.tag)
                throw new ConfigException(Error.Code.TAG_NOT_FOUND);
        }

        date = DateTimeUtils.parseAPIDate(tag, dateString, null);

        repository = store.getRepository(rid, date);
        if (null == repository || repository.isDeleted())
            throw new ConfigException(Error.Code.REPOSITORY_NOT_FOUND);

    }

    protected void getRepositoryFromUrl(final String account,
                                        final String repositoryName,
                                        final Store store,
                                        final boolean isPull)
    {
        getRepositoryFromUrl(account, repositoryName, null, null, store, isPull);
    }

    protected void getRepositoryFromUrl(final String account,
                                        final String repositoryName,
                                        final String tagString,
                                        final String dateString,
                                        final Store store,
                                        final boolean isPull)
            throws ConfigException
    {
        if (Utils.anyBlank(account, repositoryName))
            throw new ConfigException(Error.Code.REPOSITORY_NOT_FOUND);

        this.repository = store.getRepository(account, repositoryName);
        if (isPull && !this.repository.isAllowTokenFreeAPIPull())
            throw new ConfigException(Error.Code.TOKEN_FREE_PULL_ACCESS_DENIED);

        if (!isPull && !this.repository.isAllowTokenFreeAPIPush())
            throw new ConfigException(Error.Code.TOKEN_FREE_PUSH_ACCESS_DENIED);

        if (!Utils.isBlank(tagString))
            this.tag = store.getTag(this.repository.getId(), tagString);

        this.date = DateTimeUtils.parseAPIDate(tag, dateString, null);
        if (null != this.date)
            this.repository = store.getRepository(this.repository.getId(), this.date);

        if (null == this.repository)
            throw new ConfigException(Error.Code.REPOSITORY_NOT_FOUND);

    }

    /////////////////////////////////////////////////////

    public void validatePush(final String clientToken,
                             final String version,
                             final String appName,
                             final String remoteIp,
                             Store store)
            throws ConfigException
    {
        checkToken(clientToken, store);
        setPasswords(store, repository, token, date, passwords);
    }


    public void validateExport(final String clientToken,
                               final String version,
                               final String appName,
                               final String remoteIp,
                               Store store)
            throws ConfigException
    {
        checkToken(clientToken, store);
        setPasswords(store, repository, token, date, passwords);
    }


    public void validatePull(String clientToken,
                             String contextString,
                             String version,
                             String appName,
                             String remoteIp,
                             Store store,
                             Gson gson,
                             String securityProfiles)
            throws ConfigException
    {

        // ToDo check context size is the same as the repository from the time as requested by the Tag/Label
        Collection<CtxLevel> ctx = ContextParser.contextFromApi( contextString, repository, store, null);

        checkToken(clientToken, store);

        try
        {
            context = new com.confighub.core.resolver.Context(store, repository, ctx, date);
        }
        catch (ConfigException e)
        {
            log.info(repository.getLogId() + ": " + e.getMessage());
            throw e;
        }

        if (!context.isFullContext())
        {
            throw new ConfigException(Error.Code.PARTIAL_CONTEXT);
        }

        addJsonHeader(json, repository, contextString, context);
        Long start = System.currentTimeMillis();
        resolved = context.resolveForClient();

        log.info("Client [%s] from [%s] resolved %d keys in %d/ms > %s",
                 appName,
                 remoteIp,
                 resolved.size(),
                 (System.currentTimeMillis() - start),
                 contextString);


        processAuth(store, gson, securityProfiles, token, repository, date, passwords);
    }

    public static void addJsonHeader(final JsonObject json,
                                     final Repository repository,
                                     final String contextString,
                                     final com.confighub.core.resolver.Context context)
    {
        json.addProperty("generatedOn", DateTimeUtils.standardDTFormatter.get().format(new Date()));
        json.addProperty("account", repository.getAccountName());
        json.addProperty("repo", repository.getName());
        json.addProperty("context", contextString);

        if (null != context.getDate())
            json.addProperty("repoDate", DateTimeUtils.standardDTFormatter.get().format(context.getDate()));

    }

    public static void processAuth(Store store,
                                   Gson gson,
                                   String securityProfiles,
                                   Token token,
                                   Repository repository,
                                   Date date,
                                   Map<String, String> passwords)
    {

        if (!Utils.isBlank(securityProfiles))
        {
            try
            {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> spMap = gson.fromJson(securityProfiles, type);

                if (null != spMap)
                {
                    for (String name : spMap.keySet())
                    {
                        name = name.replaceAll(" ", "");
                        SecurityProfile sp = store.getSecurityProfile(repository, date, name);
                        if (null != sp)
                        {
                            String pass = spMap.get(name);
                            if (sp.isSecretValid(pass))
                                passwords.put(sp.getName(), pass);
                        }

                    }
                }
            }
            catch (ConfigException e) { throw e; }
            catch (Exception e)
            {
                throw new ConfigException(Error.Code.INVALID_PASSWORD);
            }
        }

        setPasswords(store, repository, token, date, passwords);
    }


    private void checkToken(final String clientToken, final Store store)
            throws ConfigException
    {
        if (!Utils.isBlank(clientToken))
        {
            token = store.getToken(repository, clientToken);
            if (null == token)
            {
                throw new ConfigException(Error.Code.INVALID_CLIENT_TOKEN);
            }

            if (token.isExpired())
            {
                throw new ConfigException(Error.Code.EXPIRED_TOKEN);
            }

            if (!token.isActive())
            {
                throw new ConfigException(Error.Code.NON_ACTIVE_TOKEN);
            }

        }
    }


    private static void setPasswords(final Store store,
                                     final Repository repository,
                                     final Token token,
                                     final Date date,
                                     final Map<String, String> passwords)
    {
        if (null != token && null != token.getSecurityProfiles())
        {
            if (null == date)
            {
                for (SecurityProfile sp : token.getSecurityProfiles())
                    passwords.put(sp.getName(), sp.getDecodedPassword());
            } else
            {
                for (SecurityProfile sp : token.getSecurityProfiles())
                {
                    SecurityProfile osp = store.getSecurityProfile(repository, date, sp.getName());
                    if (null != osp)
                        passwords.put(sp.getName(), osp.getDecodedPassword());
                }
            }
        }
    }

}
