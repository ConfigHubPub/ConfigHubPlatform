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

package com.confighub.core.utils;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Depth;
import com.confighub.core.repository.CtxLevel;
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

    public static Set<CtxLevel> parseAndCreateViaApi( String ctx,
                                                      final Repository repository,
                                                      final Store store,
                                                      final String appIdentifier,
                                                      final String changeComment)
            throws ConfigException
    {
        Set<CtxLevel> context = new HashSet<>();

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

                CtxLevel ctxLevel = store.getLevel( ci, depth, repository, null);
                if ( null == ctxLevel )
                    ctxLevel = store.createLevelViaApi( ci, depth, repository, appIdentifier, changeComment);

                context.add( ctxLevel );
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
                                             final Map<Depth, CtxLevel> contextMap)
        throws ConfigException
    {
        try
        {
            List<String> labels = new ArrayList<>();
            for (Depth depth : depths)
            {
                CtxLevel l = contextMap.get( depth);
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

    public static Collection<CtxLevel> parseAndCreate( String ctx,
                                                       final Repository repository,
                                                       final Store store,
                                                       final UserAccount user,
                                                       final Date date)
    {
        return parseAndCreate(ctx, repository, store, user, date, false);
    }

    public static Collection<CtxLevel> parseAndCreate( String ctx,
                                                       final Repository repository,
                                                       final Store store,
                                                       final UserAccount user,
                                                       final Date date,
                                                       final boolean ignoreNonExisting)
            throws ConfigException
    {
        Collection<CtxLevel> context = new HashSet<>();

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
                    CtxLevel ctxLevel = store.getLevel( lName.trim(), depth, repository, date);
                    if ( null == ctxLevel && !ignoreNonExisting)
                        ctxLevel = store.createLevel( lName.trim(), depth, user, repository);

                    if ( null != ctxLevel )
                        context.add( ctxLevel );
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

    public static Collection<CtxLevel> contextFromApi( String ctx,
                                                       final Repository repository,
                                                       final Store store,
                                                       final Date date)
            throws ConfigException
    {
        Collection<CtxLevel> context = new HashSet<>();

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
                CtxLevel ctxLevel = store.getLevel( ciNames[i].trim(), depth, repository, date);
                if ( null == ctxLevel )
                    ctxLevel = store.createNonPersistedLevel( ciNames[i].trim(), depth, repository);

                context.add( ctxLevel );
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
