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
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//import org.apache.commons.codec.binary.Base64;

public class Encryption
{
    private static final Logger log = LogManager.getLogger(Encryption.class);

    public static String encrypt(final CipherTransformation ct, final String text, final String secret)
            throws ConfigException
    {
        try
        {

            switch (ct.getAlgo())
            {
                case "AES":
                case "DES":
                case "DESede":
                    return encryptShared(ct, text, secret);

                default:
                    throw new ConfigException(Error.Code.ENCRYPTION_ERROR);
            }
        }
        catch (ConfigException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.DECRYPTION_ERROR);
        }

    }

    public static String decrypt(final CipherTransformation ct, final String text, final String secret)
            throws ConfigException
    {
        try
        {
            switch (ct.getAlgo())
            {
                case "AES":
                case "DES":
                case "DESede":
                    return decryptShared(ct, text, secret);

                default:
                    throw new ConfigException(Error.Code.DECRYPTION_ERROR);
            }
        }
        catch (ConfigException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.DECRYPTION_ERROR);
        }
    }


    //-----------------------------------------------------------------------------------------------
    // DES, DESede, AES
    //-----------------------------------------------------------------------------------------------
    private static byte[] getKeyAsBytes(String key, int len)
    {
        byte[] bb = new byte[len]; // a Triple DES key is a byte[24] array

        for (int i = 0; i < key.length() && i < bb.length; i++)
            bb[i] = (byte)key.charAt(i);

        return bb;
    }

    private static byte[] getIV(int len)
    {
        byte[] iv = new byte[len];
        for (int i = 0; i < iv.length; i++)
            iv[i] = 0;
        return iv;
    }

    private static String encryptShared(CipherTransformation ct, String decrypted, String secret)
            throws ConfigException
    {
        if (null == decrypted)
            return null;

        try
        {
            Cipher cipher = Cipher.getInstance(ct.getName());
            final int blockSize = cipher.getBlockSize();

            SecretKeySpec sharedKey = new SecretKeySpec(getKeyAsBytes(secret, ct.getKeyLen()), ct.getAlgo());

            if ("CBC".equals(ct.getMode()))
                cipher.init(Cipher.ENCRYPT_MODE, sharedKey, new IvParameterSpec(getIV(blockSize)));
            else
                cipher.init(Cipher.ENCRYPT_MODE, sharedKey);

            byte[] encrypted = cipher.doFinal(Utils.isBlank(decrypted) ? new byte[0] : decrypted.getBytes("UTF8"));
            return Base64.encodeBase64String(encrypted);
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.ENCRYPTION_ERROR);
        }
    }

    private static String decryptShared(CipherTransformation ct, String encrypted, String secret)
            throws ConfigException
    {
        if (null == encrypted)
            return null;

        try
        {
            Cipher cipher = Cipher.getInstance(ct.getName());
            final int blockSize = cipher.getBlockSize();

            SecretKeySpec sharedKey = new SecretKeySpec(getKeyAsBytes(secret, ct.getKeyLen()), ct.getAlgo());

            if ("CBC".equals(ct.getMode()))
                cipher.init(Cipher.DECRYPT_MODE, sharedKey, new IvParameterSpec(getIV(blockSize)));
            else
                cipher.init(Cipher.DECRYPT_MODE, sharedKey);

            byte[] decrypted = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(decrypted, "UTF8");
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.DECRYPTION_ERROR);
        }
    }
}
