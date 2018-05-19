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
