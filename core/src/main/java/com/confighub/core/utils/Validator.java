/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.utils;

import org.apache.logging.log4j.util.Strings;

import java.util.List;

/**
 * General validation utility
 */
public class Validator
{
    /**
     * Checks to see if the email is properly formatted
     */
    public static boolean validEmail(String email)
    {
        return validEmail(email, null);
    }

    public static boolean validEmail(String email, List<String> errors)
    {
        // shortest possible email a@b.cc is 6 chars long

        if (email == null || email.length() < 6)
        {
            if (errors != null)
            { errors.add("Email address '" + email + "' is too short."); }
            return false;
        }

        int at = email.indexOf("@");
        if (at == -1)
        {
            if (errors != null)
            { errors.add("Email address '" + email + "' contains no '@'."); }
            return false;
        }

        String username = email.substring(0, at);
        if (Strings.isBlank(username))
        {
            if (errors != null)
            { errors.add("Email address '" + email + "' contains no username portion."); }
            return false;
        }

        String domain = email.substring(at);
        if (domain == null ||
                domain.length() < 4 ||
                domain.indexOf(".") < 2 ||
                domain.indexOf(".") > domain.length() - 3 ||
                domain.contains("..") ||
                !domain.matches("@[0-9A-Za-z\\.-]+"))
        {
            if (errors != null)
            { errors.add("Email address '" + email + "' does not have a valid domain."); }
            return false;
        }

        return true;
    }
}
