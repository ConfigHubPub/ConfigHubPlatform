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

package com.confighub.core.repository;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;

import java.util.EnumSet;

/**
 * Specifies how many levels a configCloud has.
 */
public enum Depth
{
    // The sum of all higher levels has to be less then score of current level - at any level depth.
    D9 (10,   5120, 10),
    D8 (20,   2560, 9),
    D7 (40,   1280, 8),
    D6 (80,   640,  7),
    D5 (160,  320,  6),
    D4 (320,  160,  5), // Enterprise
    D3 (640,  80,   4), // Product
    D2 (1280, 40,   3), // Environment
    D1 (2560, 20,   2), // Application
    D0 (5120, 10,   1); // Instance

    private final int score;
    private final int inverse;
    private final int index;

    Depth(int score, int inverse, int index)
    {
        this.score = score;
        this.inverse = inverse;
        this.index = index;
    }

    public static Depth getByIndex(int index)
        throws ConfigException
    {
        for (Depth depth : Depth.values())
        {
            if (index == depth.index)
                return depth;
        }

        throw new ConfigException(Error.Code.INVALID_TYPE);
    }

    public int getIndex() { return this.index; }

    protected int getScore(boolean isCluster)
    {
        if (isCluster)
            return this.score - this.index;

        return this.score;
    }

    public static Depth byScore(int score)
            throws ConfigException
    {
        for (Depth depth : Depth.values())
        {
            if (score == depth.score)
                return depth;
        }

        throw new ConfigException(Error.Code.INVALID_TYPE);
    }

    public int getPlacement() { return this.score; }

    public static Depth getByPlacement(int placement)
        throws ConfigException
    {
        for (Depth depth : Depth.values())
        {
            if (placement == depth.score)
                return depth;
        }

        throw new ConfigException(Error.Code.INVALID_TYPE);
    }

    public int getInverse()
    {
        return inverse;
    }

    public EnumSet<Depth> getDepths()
    {
        switch (this) {
            case D9:
                return EnumSet.of(D9, D8, D7, D6, D5, D4, D3, D2, D1, D0);
            case D8:
                return EnumSet.of(D8, D7, D6, D5, D4, D3, D2, D1, D0);
            case D7:
                return EnumSet.of(D7, D6, D5, D4, D3, D2, D1, D0);
            case D6:
                return EnumSet.of(D6, D5, D4, D3, D2, D1, D0);
            case D5:
                return EnumSet.of(D5, D4, D3, D2, D1, D0);
            case D4:
                return EnumSet.of(D4, D3, D2, D1, D0);
            case D3:
                return EnumSet.of(D3, D2, D1, D0);
            case D2:
                return EnumSet.of(D2, D1, D0);
            case D1:
                return EnumSet.of(D1, D0);
            case D0:
                return EnumSet.of(D0);
        }

        return null;
    }


}
