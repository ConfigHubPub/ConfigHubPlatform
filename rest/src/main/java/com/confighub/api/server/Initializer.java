/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.server;

import com.confighub.api.util.ServiceConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Initializer
        implements ServletContextListener
{
    private static final Logger log = LogManager.getLogger("Initializer");

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        ServiceConfiguration serviceConfiguration = ServiceConfiguration.getInstance();
        log.info("ConfigHub Version: " + serviceConfiguration.getVersion());
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent)
    {
    }
}
