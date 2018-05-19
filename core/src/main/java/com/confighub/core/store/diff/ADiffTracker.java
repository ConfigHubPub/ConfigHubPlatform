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

package com.confighub.core.store.diff;

import com.confighub.core.store.APersisted;
import com.confighub.core.store.RevisionEntityContext;
import com.confighub.core.store.ThreadLocalRevEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class ADiffTracker
{
    private static final Logger log = LogManager.getLogger(ADiffTracker.class);

    public static ThreadLocal<Map<Long, OriginalAPersistent>> edits = new ThreadLocal<>();
    public static void track()
    {
        edits.set(new HashMap<>());
    }

    public boolean isTracked()
    {
        if (null == edits) return false;

        Map<Long, OriginalAPersistent> data = edits.get();
        return null != data;
    }

    public OriginalAPersistent getIfRecorded(APersisted obj)
    {
        if (!isTracked())
            return null;

        Map<Long, OriginalAPersistent> data = edits.get();
        return data.get(obj.getId());
    }

    public static abstract class OriginalAPersistent {}

    void markForNotification()
    {
        RevisionEntityContext revContext = ThreadLocalRevEntry.get();
        if (null == revContext)
            revContext = new RevisionEntityContext();

        revContext.setNotify(true);
    }

    void setSearchKey(String key)
    {
        RevisionEntityContext revContext = ThreadLocalRevEntry.get();
        if (null == revContext)
            revContext = new RevisionEntityContext();

        revContext.setSearchKey(key);
    }

    void setContextResize()
    {
        RevisionEntityContext revContext = ThreadLocalRevEntry.get();
        if (null == revContext)
            revContext = new RevisionEntityContext();

        revContext.setContextResize(true);
    }
}
