package net.sf.colossus.util;


/** Logging functions.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Log
{
    //    debug -- intended for developers only -- console or logfile
    //    info  -- routine info -- console or logfile or GUI scroll window
    //    warn  -- user mistake or important info -- message dialog
    //    error -- serious program error -- message dialog or stderr
    //    fatal -- fatal program error -- message dialog or stderr


    private static boolean showDebug = true;
    private static boolean toStdout = true;
    private static boolean toWindow = false;
    private static LogWindow logWindow;


    public static boolean getShowDebug()
    {
        return showDebug;
    }

    public static void setShowDebug(boolean showDebug)
    {
        Log.showDebug = showDebug;
    }

    public static boolean isToStdout()
    {
        return toStdout;
    }

    public static void setToStdout(boolean toStdout)
    {
        Log.toStdout = toStdout;
    }

    public static boolean isToWindow()
    {
        return toWindow;
    }

    public static void setToWindow(boolean toWindow)
    {
        Log.toWindow = toWindow;
    }


    public static void disposeLogWindow()
    {
        if (logWindow != null)
        {
            logWindow.dispose();
            logWindow = null;
        }
    }


    private static void out(String s)
    {
        if (toStdout)
        {
            System.out.println(s);
        }
        if (toWindow)
        {
            if (logWindow == null)
            {
                logWindow = new LogWindow();
            }
            logWindow.append(s + "\n");
        }
    }

    /** Log an event. */
    public static void event(String s)
    {
        out(s.trim());
    }

    /** Log an error. */
    public static void error(String s)
    {
        out("Error: " + s.trim());
    }

    /** Log a warning. */
    public static void warn(String s)
    {
        out("Warn: " + s.trim());
    }

    /** Log a debug message, to stdout only. */
    public static void debug(String s)
    {
        boolean wasToWindow = isToWindow();
        setToWindow(false);
        if (showDebug)
        {
            out(s.trim());
        }
        setToWindow(wasToWindow);
    }
}
