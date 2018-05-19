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

package com.confighub.core.utils;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Tag;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public abstract class DateTimeUtils
{
    public static final long HOUR_MS = 60 * 60 * 1000;
    public static final long DAY_MS = 24 * HOUR_MS;
    public static final long WEEK_MS = 7 * DAY_MS;

    public static final ThreadLocal<SimpleDateFormat> standardDTFormatter = toThreadSafeFormat("MM/dd/yyyy HH:mm:ss");
    public static final ThreadLocal<SimpleDateFormat> iso8601 = toThreadSafeFormat("yyyy-MM-dd'T'HH:mm'Z'");
    public static final ThreadLocal<SimpleDateFormat> date = toThreadSafeFormat("MM/dd/yyyy");

    private static ThreadLocal<SimpleDateFormat> toThreadSafeFormat(final String format)
    {
        return new ThreadLocal<SimpleDateFormat>()
        {
            @Override
            protected SimpleDateFormat initialValue()
            {

                return new SimpleDateFormat(format);
            }
        };
    }

    public static Date dateFromTsOrTag(Tag tag, Long ts, Date cutoff)
    {
        if (null == tag)
            return dateFromTs(ts, cutoff);

        return dateFromTs(tag.getTs(), cutoff);
    }


    public static Date dateFromTs(Long ts, Date cutoff)
    {
        if (null == ts)
            return null;

        try
        {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeInMillis(ts);

            if (null != cutoff && gc.before(cutoff))
                return cutoff;

            return gc.getTime();
        }
        catch (Exception ignore) {
            return null;
        }
    }


    public static Date parseISO8601Date(String dateString, Date cutoff)
            throws ConfigException
    {
        if (Utils.isBlank(dateString))
            return null;

        try
        {
            DateTimeFormatter parser = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
            DateTime dt = parser.parseDateTime(dateString);
            dt = dt.toDateTime(DateTimeZone.UTC);
            Date date = dt.toDate();

            if (null != cutoff && date.before(cutoff))
                return cutoff;

            return date;
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.DATE_API_FORMAT_ERROR);
        }
    }

    public static Date parseAPIDate(Tag tag, String dateString, Date cutoff)
            throws ConfigException
    {
        if (null == tag)
            return parseISO8601Date(dateString, cutoff);

        return dateFromTs(tag.getTs(), cutoff);
    }

    public static String toISO8601(final Date date)
        throws ConfigException
    {
        try
        {
            return DateTimeUtils.iso8601.get().format(date);
        } catch (Exception e) {
            throw new ConfigException(Error.Code.DATE_API_FORMAT_ERROR);
        }
    }

}
