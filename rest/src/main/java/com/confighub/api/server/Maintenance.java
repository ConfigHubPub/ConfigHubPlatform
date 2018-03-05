/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.server;

import com.confighub.api.util.ServiceConfiguration;
import com.confighub.core.store.Store;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class Maintenance
        implements ServletContextListener
{
    private static final Logger log = LogManager.getLogger(Maintenance.class);
    private static final Timer maintenanceTimer = new Timer();
    private static final Timer versionCheckTimer = new Timer();

    private static final int thirtyMin = (1000 * 60) * 30;
    private static final int oneDay = 1000 * 60 * 60 * 24;
    private static final int tenMin = 1000 * 60 * 10;

    private static JsonObject upgrade = null;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        // Database connection keep-alive.
        maintenanceTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                long propCount = 0;

                Store store = new Store();
                try
                {
                    propCount = store.getPropertyCount();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    store.close();
                }

                log.info("DB keep-alive reports " + propCount + " properties.");
            }
        }, 2000, thirtyMin);

        Runtime runtime = Runtime.getRuntime();

        log.info("Maintenance initialized");
        log.info("Max memory:" + runtime.maxMemory());
        log.info("Total memory:" + runtime.totalMemory());
        log.info("Free memory:" + runtime.freeMemory());
        log.info("Available processors:" + runtime.availableProcessors());

        versionCheckTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {

                Store store = new Store();
                try
                {
                    long userCount = store.getUserCount();
                    long propCount = store.getPropertyCount();
                    long fileCount = store.getFileCount();
                    long repoCount = store.getRepositoryCount();

                    InetAddress address = InetAddress.getLocalHost();
                    NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
                    byte[] macAddress = networkInterface.getHardwareAddress();

                    StringBuilder mac = new StringBuilder();
                    for (int byteIndex = 0; byteIndex < macAddress.length; byteIndex++)
                        mac.append(String.format("%02X%s",
                                                 macAddress[byteIndex],
                                                 (byteIndex < macAddress.length - 1) ? "-" : ""));

                    URL url = new URL("https://www.confighub.com/rest/versionCheck");
                    HttpURLConnection con = (HttpURLConnection)url.openConnection();

                    //add reuqest header
                    con.setRequestMethod("POST");
                    con.setRequestProperty("User-Agent", "ConfigHub Platform");
                    con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                    ServiceConfiguration serviceConfiguration = ServiceConfiguration.getInstance();
                    String urlParameters = String.format("v=%s&uid=%s&uc=%d&pc=%d&fc=%d&rc=%d",
                                                         serviceConfiguration.getVersion(),
                                                         mac.toString(),
                                                         userCount,
                                                         propCount,
                                                         fileCount,
                                                         repoCount);

                    // Send post request
                    con.setDoOutput(true);
                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();

                    int responseCode = con.getResponseCode();

                    if (200 == responseCode)
                    {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null)
                            response.append(inputLine);

                        Gson gson = new Gson();
                        JsonObject json = gson.fromJson(response.toString(), JsonObject.class);

                        if (json.get("hasUpgrade").getAsBoolean())
                            Maintenance.upgrade = json;
                        else
                            Maintenance.upgrade = null;

                        in.close();
                    }

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    store.close();
                }

            }
        }, tenMin, oneDay);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent)
    {
        maintenanceTimer.cancel();
        log.info("ConfigHub Shutdown");
    }

    public static boolean hasUpgrade()
    {
        return null != Maintenance.upgrade;
    }

    public static JsonObject getUpgrade()
    {
        return Maintenance.upgrade;
    }
}
