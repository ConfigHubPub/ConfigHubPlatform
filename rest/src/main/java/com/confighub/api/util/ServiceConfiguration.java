/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServiceConfiguration
{
    private static final Logger log = LogManager.getLogger(ServiceConfiguration.class);

    private static final Properties props = new Properties();
    private static ServiceConfiguration serviceConfiguration;

    private ServiceConfiguration()
    {
        readVersion();
    }

    public static synchronized ServiceConfiguration getInstance()
    {
        if (null == serviceConfiguration)
            serviceConfiguration = new ServiceConfiguration();

        return serviceConfiguration;
    }

    public static String getVersion()
    {
        return props.getProperty("version");
    }

    private String readVersion()
    {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream resourceStream = classLoader.getResourceAsStream("config.properties")) {
            props.load(resourceStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props.getProperty("version");
    }
}
