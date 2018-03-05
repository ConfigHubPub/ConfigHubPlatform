/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.repository;

public class LevelCtx
{
    public final String name;
    public final Level.LevelType type;

    protected LevelCtx(String name, Level.LevelType type)
    {
        this.name = name;
        this.type = type;
    }
}
