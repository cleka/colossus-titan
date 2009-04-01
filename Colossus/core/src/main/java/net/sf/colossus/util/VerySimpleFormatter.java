package net.sf.colossus.util;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;


/**
 * This is a logging formatter doing not much more than the bare minimum.
 *
 * We don't log the source class/method, we log the level only if WARNING
 * or SEVERE. Exceptions are logged if given, but otherwise it is more like
 * printing to stdout, just that it is configurable and it is possible to have
 * a more verbose log in parallel.
 */
public class VerySimpleFormatter extends Formatter // NO_UCD
{
    public static final String LINE_SEPARATOR = System
        .getProperty("line.separator");

    @Override
    public String format(LogRecord record)
    {
        StringBuilder sb = new StringBuilder();
        String message = formatMessage(record);
        if ((record.getLevel() == Level.SEVERE)
            || (record.getLevel() == Level.WARNING))
        {
            sb.append(record.getLevel().getLocalizedName());
            sb.append(": ");
        }
        sb.append(message);
        sb.append(LINE_SEPARATOR);
        if (record.getThrown() != null)
        {
            try
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            }
            catch (Exception ex)
            {
                // really should work and we don't want to write anything anywhere else
            }
        }
        return sb.toString();
    }
}
