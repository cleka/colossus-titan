package net.sf.colossus.util;

import java.util.logging.Level;
import java.util.logging.Logger;


/** 
 *  Logging functions. Wraps java.util.logging.
 * 
 *  This class should get deprecated and proper java.util.logging Loggers created where
 *  needed.
 *
 *  @version $Id$
 *  @author David Ripton
 *  @author Barrie Treloar
 */

public final class Log
{
    public static final Logger LOGGER = Logger.getLogger(Log.class.getName());

    public static void event(String s)
    {
        LOGGER.log(Level.INFO, s);
    }

    public static void error(String s)
    {
        error(s, null);
    }

    public static void warn(String s)
    {
        LOGGER.log(Level.WARNING, s);
    }

    public static void debug(String s)
    {
        LOGGER.log(Level.FINEST, s);
    }

    public static void error(String message, Throwable ex) {
        LOGGER.log(Level.SEVERE, message, ex);
    }

    public static void error(Throwable ex) {
        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
    }
}
