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

package com.confighub.core.resolver;

import com.confighub.core.repository.Depth;
import com.confighub.core.repository.CtxLevel;
import com.confighub.core.repository.LevelCtx;
import com.confighub.core.store.Store;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;

public abstract class AResolver
{
    protected final Store store;

    protected AResolver(final Store store)
    {
        this.store = store;
    }

    private static final Logger log = LogManager.getLogger(AResolver.class);

    public static boolean containsAny(final Map<String, LevelCtx> propertyContext,
                                      final Map<Depth, Collection<CtxLevel>> requestContext)
    {
        if (null == propertyContext || null == requestContext)
            return false;

        for (Depth depth : requestContext.keySet())
        {
            LevelCtx lc = propertyContext.get(String.valueOf(depth.getPlacement()));
            if (null == lc) continue;

            for ( CtxLevel l : requestContext.get( depth))
                if (l.getName().equalsIgnoreCase(lc.name))
                    return true;
        }

        return false;
    }

    public static boolean containsAll(final Map<String, LevelCtx> propertyContext,
                                      final Map<Depth, Collection<CtxLevel>> requestContext)
    {
        if (null == propertyContext && null == requestContext)
            return true;

        if (propertyContext.size() != requestContext.size())
            return false;

        for (Depth depth : requestContext.keySet())
        {
            LevelCtx lc = propertyContext.get(String.valueOf(depth.getPlacement()));
            if (null == lc)
                return false;

            for ( CtxLevel l : requestContext.get( depth))
                if (!l.getName().equalsIgnoreCase(lc.name))
                    return false;
        }

        return true;
    }


    /**
     * Use by AccessRule
     */
    public static boolean isContextualMatch(final Map<String, LevelCtx> propertyContext,
                                            final Map<Depth, Collection<CtxLevel>> requestContext)
    {
        if (null == propertyContext || null == requestContext)
            return true;

        for (Depth depth : Depth.values())
        {
            String placement = String.valueOf(depth.getPlacement());

            if (null != requestContext.get(depth) &&
                propertyContext.size() > 0 &&
                propertyContext.containsKey(placement) &&
                !(containsLevelAtDepth(requestContext, depth, propertyContext.get(placement).name)))
                return false;
        }

        return true;
    }

    private static boolean containsLevelAtDepth(final Map<Depth, Collection<CtxLevel>> requestContext,
                                                Depth d,
                                                String levelName)
    {
        Collection<CtxLevel> dls = requestContext.get( d);
        if (null == dls || dls.size() == 0)
            return false;

        for ( CtxLevel l : dls)
            if (l.getName().equalsIgnoreCase(levelName))
                return true;

        // ToDo: Need to consider parents as well.
        // Look at Context.containsLevelAtDepth

        return false;
    }

    /*
     * Used by UIRepositoryResolver to collect
     */
    public static boolean isContextualMatchAudit(final Map<String, LevelCtx> propertyContext,
                                                 final Context context)
    {
        if (null == propertyContext)
            return true;

        for (Depth depth : context.elements.keySet())
        {
            if (!context.elements.get(depth).isEmpty() &&
                propertyContext.size() > 0 &&
                propertyContext.containsKey(String.valueOf(depth.getPlacement())) &&
                !context.containsLevelAtDepth(depth, propertyContext.get(String.valueOf(depth.getPlacement())).name))
                return false;
        }

        return true;
    }
}
