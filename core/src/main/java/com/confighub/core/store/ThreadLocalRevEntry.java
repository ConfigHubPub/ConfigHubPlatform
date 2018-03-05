/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store;

/**
 * Transport for information shared between Store and RevisionManager.
 */
public class ThreadLocalRevEntry
{
    public static final ThreadLocal<RevisionEntityContext> userThreadLocal = new ThreadLocal<>();

    public static void set(RevisionEntityContext revisionEntityContext)
    {
        userThreadLocal.set(revisionEntityContext);
    }

    public static void unset()
    {
        userThreadLocal.remove();
    }

    public static RevisionEntityContext get()
    {
        return userThreadLocal.get();
    }
}