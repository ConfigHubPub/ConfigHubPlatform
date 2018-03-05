/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.mail;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Validator;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileReader;

public abstract class SendMail
{
    private static final Logger log = LogManager.getLogger(SendMail.class);
    private static final String SUPPORT = "support@confighub.com";

    public static void contactSupport(UserAccount user, String name, String email, String subject, String message)
    {
        try
        {
            File catalinaBase = new File(System.getProperty("catalina.base")).getAbsoluteFile();
            File verifyTemplate = new File(catalinaBase, "templates/contactSupport.html");

            int len;
            char[] chr = new char[4096];
            final StringBuffer buffer = new StringBuffer();
            final FileReader reader = new FileReader(verifyTemplate);
            try {
                while ((len = reader.read(chr)) > 0) {
                    buffer.append(chr, 0, len);
                }
            } finally {
                reader.close();
            }

            String html = buffer.toString()
                                .replace("$REGISTERED", (null == user ? "Not registered" : user.toString()))
                                .replace("$NAME", name)
                                .replace("$EMAIL", email)
                                .replace("$MESSAGE",message);

            MultivaluedMapImpl formData = new MultivaluedMapImpl();
            formData.add("from", "ConfigHub Support <support@confighub.com>");
            formData.add("to", SUPPORT);
            formData.add("subject", "Support: " + subject);
            formData.add("text", "HTML not supported");
            formData.add("html", html);

            ClientResponse cr = send(formData);

            log.info("Hosted Solution Request: Message send status: " + cr.getStatus());

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendPasswordReset(final String to, final String userToken)
            throws ConfigException
    {
        if (!Validator.validEmail(to))
            throw new ConfigException(Error.Code.USER_BAD_EMAIL);

        try
        {
            File catalinaBase = new File(System.getProperty("catalina.base")).getAbsoluteFile();
            File verifyTemplate = new File(catalinaBase, "templates/passwordResetEmail.html");

            int len;
            char[] chr = new char[4096];
            final StringBuffer buffer = new StringBuffer();
            final FileReader reader = new FileReader(verifyTemplate);
            try {
                while ((len = reader.read(chr)) > 0) {
                    buffer.append(chr, 0, len);
                }
            } finally {
                reader.close();
            }

            String email = buffer.toString().replace("$EMAIL_LINK",
                                                     "https://www.confighub.com/passwordReset?t=" + userToken);

            log.info("reset: https://www.confighub.com/passwordReset?t=" + userToken);

            MultivaluedMapImpl formData = new MultivaluedMapImpl();
            formData.add("from", "ConfigHub <support@confighub.com>");
            formData.add("to", to);
            formData.add("subject", "ConfigHub password reset request");
            formData.add("text", "HTML not supported");
            formData.add("html", email);

            ClientResponse cr = send(formData);
            log.info("Password reset message send status: " + cr.getStatus());

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param to
     * @param userToken
     */
    public static void validateUserRegistration(final String to, final String userToken)
            throws ConfigException
    {
        if (!Validator.validEmail(to))
            throw new ConfigException(Error.Code.USER_BAD_EMAIL);

        try
        {
            File catalinaBase = new File(System.getProperty("catalina.base")).getAbsoluteFile();
            File verifyTemplate = new File(catalinaBase, "templates/verifyEmail.html");

            int len;
            char[] chr = new char[4096];
            final StringBuffer buffer = new StringBuffer();
            final FileReader reader = new FileReader(verifyTemplate);
            try {
                while ((len = reader.read(chr)) > 0) {
                    buffer.append(chr, 0, len);
                }
            } finally {
                reader.close();
            }

            String email = buffer.toString().replace("$EMAIL_LINK",
                                                     "https://www.confighub.com/email-verification?t=" + userToken);

            MultivaluedMapImpl formData = new MultivaluedMapImpl();
            formData.add("from", "ConfigHub <support@confighub.com>");
            formData.add("to", to);
            formData.add("subject", "Welcome to ConfigHub.  Please validate your email.");
            formData.add("text", "HTML not supported");
            formData.add("html", email);

            ClientResponse cr = send(formData);
            log.info("Message send status: " + cr.getStatus());

        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private static ClientResponse send(MultivaluedMapImpl formData)
    {
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("api", "key-b222ea3ed49d2a8e4c2fb6319e445a1d"));
        WebResource webResource = client.resource(
                "https://api.mailgun.net/v3/sandbox6c2738c722634be091b8eaa0b6169fa8.mailgun.org/messages");
        ClientResponse cr = webResource.type(MediaType.APPLICATION_FORM_URLENCODED).
                post(ClientResponse.class, formData);

        return cr;
    }
}
