/**
 *
 */
package net.sf.colossus.webcommon;


import java.text.SimpleDateFormat;
import java.util.Date;


public class FormatWhen
{
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";

    private final SimpleDateFormat dateFormatter;
    private final SimpleDateFormat timeFormatter;

    private String datePrev;
    private String changedDateString = null;

    public FormatWhen()
    {
        datePrev = "";
        dateFormatter = new SimpleDateFormat(DATE_FORMAT);
        timeFormatter = new SimpleDateFormat(TIME_FORMAT);

    }

    /* call this *after* timeAsString() call
     * It will return the new date, if changed, null otherwise */

    public String hasDateChanged()
    {
        return changedDateString;
    }

    public String timeAsString(long when)
    {
        Date whenDate = new Date(when);
        String timeNow = timeFormatter.format(whenDate);
        String dateNow = dateFormatter.format(whenDate);

        if (!dateNow.equals(datePrev))
        {
            changedDateString = dateNow;
        }
        else
        {
            changedDateString = null;
        }
        datePrev = dateNow;

        return timeNow;
    }

    public String timeAndDateAsString(long when)
    {
        Date whenDate = new Date(when);
        String timeNow = timeFormatter.format(whenDate);
        String dateNow = dateFormatter.format(whenDate);

        return dateNow + " " + timeNow;
    }
}