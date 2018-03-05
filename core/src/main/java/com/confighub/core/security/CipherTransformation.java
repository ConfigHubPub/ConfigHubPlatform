/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
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
