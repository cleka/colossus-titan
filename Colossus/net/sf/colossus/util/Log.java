package net.sf.colossus.util;

import java.io.*;

import net.sf.colossus.server.Server;
import net.sf.colossus.server.Constants;
import net.sf.colossus.client.Client;


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
    private static boolean toFile = true;
    private static boolean toWindow = false;
    private static boolean toRemote = false;
    private static LogWindow logWindow;
    /** Server, for remote logging. */
    private static Server server;
    /** First local client, for log window. */
    private static Client client;
    private static PrintWriter fileout;


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

    public static boolean isToFile()
    {
        return toFile;
    }

    public static void setToFile(boolean toFile)
    {
        Log.toFile = toFile;
    }

    public static boolean isToWindow()
    {
        return toWindow;
    }

    public static void setToWindow(boolean toWindow)
    {
        Log.toWindow = toWindow;
    }

    public static void setServer(Server server)
    {
        Log.server = server;
    }

    public static void setClient(Client client)
    {
        Log.client = client;
    }

    public static boolean isToRemote()
    {
        return toRemote;
    }

    public static void setToRemote(boolean toRemote)
    {
        Log.toRemote = toRemote;
    }


    public static void disposeLogWindow()
    {
        if (logWindow != null)
        {
            logWindow.dispose();
            logWindow = null;
        }
    }


    private static synchronized void out(String s)
    {
        if (toStdout)
        {
            System.out.println(s);
        }
        if (toFile)
        {
            String logPath = Constants.gameDataPath + Constants.logFileName;
            try
            {
                if (fileout == null)
                {
                    fileout = new PrintWriter(new FileOutputStream(logPath), 
                        true);
                }
                fileout.println(s);
            }
            catch (FileNotFoundException ex)
            {
                toFile = false;
                Log.error("Could not open logfile " + logPath);
                Log.error(ex.toString());
            }
        }
        if (toWindow && client != null)
        {
            if (logWindow == null)
            {
                logWindow = new LogWindow(client);
            }
            logWindow.append(s + "\n");
        }
        if (toRemote && server != null)
        {
            server.allLog(s + "\n");
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
        boolean wasToRemote = isToRemote();
        setToRemote(false);
        if (showDebug)
        {
            out("- " + s.trim());
        }
        setToWindow(wasToWindow);
        setToRemote(wasToRemote);
    }
}
