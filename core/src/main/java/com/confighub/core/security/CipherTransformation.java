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

package com.confighub.core.security;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.utils.Utils;

/**
 *
 */
public enum CipherTransformation
{
    AES_CBC_PKCS5Padding ("AES/CBC/PKCS5Padding", 8, 128, 16), // good
    AES_ECB_PKCS5Padding ("AES/ECB/PKCS5Padding", 8 ,128, 16), // good
    DESede_CBC_PKCS5Padding ("DESede/CBC/PKCS5Padding", 8, 168, 24), // good
    DESede_ECB_PKCS5Padding ("DESede/ECB/PKCS5Padding", 8, 168, 24), // good
    DES_CBC_PKCS5Padding ("DES/CBC/PKCS5Padding", 8, 56, 8), // good
    DES_ECB_PKCS5Padding ("DES/ECB/PKCS5Padding", 8, 56, 8); // good

    private final String name;
    private final int min;
    private final int max;
    private final String algo;
    private final String mode;
    private final int keyLen;

    CipherTransformation(String name, int min, int max, int keyLen)
    {
        this.name = name;
        this.min = min;
        this.max = max;
        this.keyLen = keyLen;

        String[] ns = name.split("/");
        this.algo = ns[0];
        this.mode = ns[1];
    }

    /**
     * Get CipherTransformation from String
     *
     * @param cipher
     * @return
     */
    public static CipherTransformation get(String cipher)
        throws ConfigException
    {
        if (Utils.isBlank(cipher))
            throw new ConfigException(Error.Code.INVALID_CIPHER);

        for (CipherTransformation ct : CipherTransformation.values())
            if (ct.name.equals(cipher)) return ct;

        throw new ConfigException(Error.Code.INVALID_CIPHER);
    }

    public String getName()
    {
        return this.name;
    }

    public int getMin()
    {
        return this.min;
    }

    public int getMax()
    {
        return this.max;
    }

    public String getAlgo() { return this.algo; }

    public String getMode() { return this.mode; }

    public int getKeyLen()
    {
        return keyLen;
    }

}
