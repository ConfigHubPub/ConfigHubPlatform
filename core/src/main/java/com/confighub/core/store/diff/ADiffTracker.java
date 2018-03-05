/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
