/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.utils;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.Level;
import com.confighub.core.repository.Repository;
import com.confighub.core.store.Store;
import com.confighub.core.user.UserAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 *
 */
public class ContextParser
{
    private static final Logger log = LogManager.getLogger(ContextParser.class);

    public static Set<Level> parseAndCreateViaApi(String ctx,
                                                  final Repository repository,
                                                  final Store store,
                                                  final String appIdentifier,
                                                  final String changeComment)
            throws ConfigException
    {
        Set<Level> context = new HashSet<>();

        if (Utils.anyNull(ctx, repository, store) || Utils.isBlank(ctx))
            return context;

        try
        {
            String[] ciNames = ctx.trim().split(";");
            if (ciNames.length == 0)
                return context;

            Depth contextDepth = Depth.getByIndex(ciNames.length);

            if (repository.getDepth().getIndex() != contextDepth.getIndex())
                throw new ConfigException(Error.Code.CONTEXT_SCOPE_MISMATCH);

            EnumSet<Depth> depths = contextDepth.getDepths();

            int i = -1;
            for (Depth depth : depths)
            {
                i++;

                String ci = ciNames[i];
                if (Utils.isBlank(ci)) continue;

                ci = ci.trim();
                if ("*".equals(ci)) continue;

                Level level = store.getLevel(ci, depth, repository, null);
                if (null == level)
                    level = store.createLevelViaApi(ci, depth, repository, appIdentifier, changeComment);

                context.add(level);
            }
        }
        catch (ConfigException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.FAILED_TO_PARSE_CONTEXT_ELEMENT);
        }

        return context;
    }

    public static String getContextForExport(final EnumSet<Depth> depths,
                                             final Map<Depth, Level> contextMap)
        throws ConfigException
    {
        try
        {
            List<String> labels = new ArrayList<>();
            for (Depth depth : depths)
            {
                Level l = contextMap.get(depth);
                labels.add(null == l ? "*" : l.getName());
            }

            return Utils.join(labels, ";");
        }
        catch (ConfigException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.FAILED_TO_PARSE_CONTEXT_ELEMENT);
        }
    }

    public static Collection<Level> parseAndCreate(String ctx,
                                                   final Repository repository,
                                                   final Store store,
                                                   final UserAccount user,
                                                   final Date date)
    {
        return parseAndCreate(ctx, repository, store, user, date, false);
    }

    public static Collection<Level> parseAndCreate(String ctx,
                                                   final Repository repository,
                                                   final Store store,
                                                   final UserAccount user,
                                                   final Date date,
                                                   final boolean ignoreNonExisting)
            throws ConfigException
    {
        Collection<Level> context = new HashSet<>();

        if (Utils.anyNull(ctx, repository, store) || Utils.isBlank(ctx))
            return context;

        try
        {
            String[] tokens = ctx.split(";");
            if (tokens.length == 0)
                return context;
            
            for (String token : tokens)
            {
                String[] pair = token.split(":");
                Depth depth = Depth.byScore(Integer.valueOf(pair[0].trim()));

                String lNames[] = pair[1].split(",");
                for (String lName : lNames)
                {
                    Level level = store.getLevel(lName.trim(), depth, repository, date);
                    if (null == level && !ignoreNonExisting)
                        level = store.createLevel(lName.trim(), depth, user, repository);

                    if (null != level)
                        context.add(level);
                }
            }
        }
        catch (ConfigException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.FAILED_TO_PARSE_CONTEXT_ELEMENT);
        }

        return context;
    }

    public static Collection<Level> contextFromApi(String ctx,
                                                   final Repository repository,
                                                   final Store store,
                                                   final Date date)
            throws ConfigException
    {
        Collection<Level> context = new HashSet<>();

        if (Utils.anyNull(repository, store))
            return context;

        if (Utils.isBlank(ctx))
            throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

        try
        {
            String[] ciNames = ctx.split(";");
            EnumSet<Depth> depths = Depth.getByIndex(ciNames.length).getDepths();
            if (null == ciNames || null == depths || depths.isEmpty())
                throw new ConfigException(Error.Code.PARTIAL_CONTEXT);

            int i = 0;
            for (Depth depth : depths)
            {
                Level level = store.getLevel(ciNames[i].trim(), depth, repository, date);
                if (null == level)
                    level = store.createNonPersistedLevel(ciNames[i].trim(), depth, repository);

                context.add(level);
                i++;
            }
        }
        catch (ConfigException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.warn(e.getMessage());
        }

        return context;

    }
}
