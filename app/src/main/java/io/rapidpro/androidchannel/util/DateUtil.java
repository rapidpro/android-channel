/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.androidchannel.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;
    public static final long MONTH = 30 * DAY;

    /** The date format in iso. */
    public static String FORMAT_DATE_ISO="yyyy-MM-dd'T'HH:mm:ssZ";

    /**
     * Takes in an ISO date string of the following format:
     * yyyy-mm-ddThh:mm:ss.ms+HoMo
     *
     * @param isoDateString the iso date string
     * @return the date
     * @throws Exception the exception
     */
    public static Date fromISODateString(String isoDateString) {
        DateFormat f = new SimpleDateFormat(FORMAT_DATE_ISO);
        try {
            return f.parse(isoDateString);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Render date
     *
     * @param date the date obj
     * @param format - if not specified, will use FORMAT_DATE_ISO
     * @param tz - tz to set to, if not specified uses local timezone
     * @return the iso-formatted date string
     */
    public static String toISOString(Date date, String format, TimeZone tz){
        if( format == null ) format = FORMAT_DATE_ISO;
        if( tz == null ) tz = TimeZone.getDefault();
        DateFormat f = new SimpleDateFormat(format);
        f.setTimeZone(tz);
        return f.format(date);
    }

    public static String toISOString(Date date) {
        return toISOString(date,FORMAT_DATE_ISO,TimeZone.getDefault());
    }

    public static String getFuzzyTime(long ts) {

        long delta = System.currentTimeMillis() - ts;

        long seconds = delta / SECOND;
        long minutes = delta / MINUTE;
        long hours = delta / HOUR;
        long days = delta / DAY;
        long months = delta / MONTH;
        long years = delta / (MONTH * 12);

        if (delta < 0) {
            return "not yet";
        }
        if (delta < 2 * MINUTE) {
            return "moments ago";
        }
        if (delta < 45 * MINUTE) {
            return minutes + " minutes ago";
        }
        if (delta < 90 * MINUTE) {
            return "an hour ago";
        }
        if (delta < 24 * HOUR) {
            return hours + " hours ago";
        }
        if (delta < 48 * HOUR) {
            return "yesterday";
        }
        if (delta < 30 * DAY) {
            return days + " days ago";
        }
        if (delta < 12 * MONTH) {
            return months <= 1 ? "one month ago" : months + " months ago";
        }
        else {
            return years <= 1 ? "one year ago" : years + " years ago";
        }
    }
}
