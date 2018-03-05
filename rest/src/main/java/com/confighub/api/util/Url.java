/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.api.util;

import com.confighub.core.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class Url
{
    private static final Logger log = LogManager.getLogger(Url.class);

    /**
     * Sanitize a URL path element.  Sanitize is a one-way operation:
     * the original string cannot be reconstructed later.
     */
    public static String sanitizePath(String s) {
        // Should other unpleasant characters be removed?
        return Utils.remove(s.replaceAll(" ", "-"), '\'');
    }

    /**
     * Encode a URL path element, attempting to return a "pretty"
     * URL component.  Unlike sanitizePath, the resulting string can be decoded,
     * with the exception of underscores.  Any underscores present prior to
     * encoding will be converted to spaces upon decoding.  Hyphens will be
     * preserved if they are adjacent to spaces on both sides, or word
     * characters (\w) on both sides.
     */
    public static String encodePath(String s) {
        // Spaces become -, a-b becomes a--b, and a - b becomes a---b.

        // The order of the following replacements is significant.
        s = s.replaceAll(" - ", "---");

        // See decodePath() for an explanation concerning the apparent redundancy
        // in the following two lines.
        s = s.replaceAll("(\\w)-(\\w)", "$1--$2");
        s = s.replaceAll("(\\w)-(\\w)", "$1--$2");
        s = s.replaceAll(" ", "-"); // Just for aesthetics
        s = encodeQuery(s);
        s = s.replaceAll("%26", "&"); // Just for aesthetics
        return s;
    }

    /**
     * Decode the specified path element.  We assume that it has already been
     * URLDecoded by Tomcat.  Note that direct URL access via request.getRequestURI
     * will not have already ben URLDecoded by Tomcat.
     */
    public static String decodePath(String s) {
        if (s == null)
            return s;

        // Replace both hyphens and underscores with spaces since underscores were
        // originally used.

        // Once we're confident that all _ URLs are redirect, the _ replacement below
        // could be removed.

        // The order of the following replacements is significant.

        // Perform the following replacement twice, since a single letter
        // will be captured once.  Consider "bar-b-q": If replaceAll()
        // is called just once, the result will be "bar b-q".
        s = s.replaceAll("([^-])[-_]([^-])", "$1 $2");
        s = s.replaceAll("([^-])[-_]([^-])", "$1 $2");
        s = s.replaceAll("---", " - ");
        s = s.replaceAll("--", "-");
        return s;
    }

    /**
     * Encode the specified query component.  A query component is anything
     * appearing in the URL query string (anything after the question mark in
     * a URL).
     */
    private static String encodeQuery(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8"); // All JVMs should do UTF-8
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            return s;
        }
    }


    /**
     * Decide if we need to forward to /index.html (angular home), or, of the server
     * should let the request go through as requested.
     *
     *
     *
     *
     * @param url
     * @return
     */
    public static boolean foo(String url)
    {
        return true;
    }
}
