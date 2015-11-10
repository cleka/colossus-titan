package net.sf.colossus.util;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormats
{
    public TimeFormats()
    {
        // TODO Auto-generated constructor stub
    }

    private static final SimpleDateFormat timeFormat24h = new SimpleDateFormat(
        "HH:mm:ss");

    public static String getCurrentTime24h()
    {
        return timeFormat24h.format(new Date());
    }

    private static DateFormat timeLocalizedFormat = DateFormat
        .getTimeInstance(DateFormat.MEDIUM, Locale.getDefault());

    /**
     *  Returns a String representing the current time (hours, mins, seconds),
     *  formatted according to user's locale.
     */
    public static String getCurrentTimeLocalized()
    {
        String timeString = timeLocalizedFormat.format(new Date());
        return timeString;
    }

}
