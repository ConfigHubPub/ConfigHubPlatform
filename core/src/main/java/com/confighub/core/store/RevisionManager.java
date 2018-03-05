/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store;

import org.hibernate.envers.RevisionListener;

/**
 * Manage information that is to be persisted in the revisionEntry
 */
public class RevisionManager
    implements RevisionListener
{
    @Override
    public void newRevision(Object revisionEntity)
    {
        RevisionEntry revEntity = (RevisionEntry) revisionEntity;

        RevisionEntityContext revisionEntityContext = ThreadLocalRevEntry.get();
        if (null == revisionEntityContext)
            throw new IllegalStateException("ThreadLocalRevEntry cannot be found");

        revEntity.set(revisionEntityContext);
        ThreadLocalRevEntry.unset();
    }
}
