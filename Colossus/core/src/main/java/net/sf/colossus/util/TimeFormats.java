package net.sf.colossus.util;


import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeFormats
{
    public TimeFormats()
    {
        // TODO Auto-generated constructor stub
    }

    // TODO: duplicate with SwingDocumentLogHandler,move to util ?
    private static final SimpleDateFormat timeFormat24h = new SimpleDateFormat(
        "HH:mm:ss");

    public static String getCurrentTime24h()
    {
        return timeFormat24h.format(new Date());
    }
}
