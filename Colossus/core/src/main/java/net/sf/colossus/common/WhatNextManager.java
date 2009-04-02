package net.sf.colossus.common;

import java.util.logging.Level;
import java.util.logging.Logger;



public class WhatNextManager
{
    private static final Logger LOGGER = Logger
        .getLogger(WhatNextManager.class.getName());

    private WhatToDoNext whatToDoNext;
    private Options startOptions;
    private int howManyGamesLeft = 0; 

    public WhatNextManager(Options startOpts)
    {
        this.startOptions = startOpts;
    }
    public WhatToDoNext getWhatToDoNext()
    {
        return whatToDoNext;
    }

    /**
     * Set the action what shall be executed next.
     * Trigger also the timer for the "Timed Quit", if requested so.
     * 
     * @param whatToDoNext
     * @param triggerQuitTimer
     */
    public void setWhatToDoNext(WhatToDoNext whatToDoNext,
        boolean triggerQuitTimer)
    {
        this.whatToDoNext = whatToDoNext;
        LOGGER.log(Level.FINEST, "Set what to do next to "
            + whatToDoNext.toString());
        if (triggerQuitTimer)
        {
            triggerTimedQuit();
        }
    }

    /**
     * 
     * @return Returns the same startOptions object that Start object uses.
     */
    public Options getStartOptions()
    {
        return startOptions;
    }

    public void setWhatToDoNext(WhatToDoNext whatToDoNext, String loadFile)
    {
        setWhatToDoNext(whatToDoNext, false);
        startOptions.setOption(Options.loadGameFileName, loadFile);
    }

    public void updateHowManyGamesLeft(int left)
    {
        howManyGamesLeft = left;
    }
    
    /**
     * Trigger a timed Quit, which will (by using a demon thread) terminate
     * the JVM after a timeout (currently 10 seconds)  
     * - unless the JVM has quit already anyway because cleanup has
     * succeeded as planned.
     */
    public void triggerTimedQuit()
    {
        LOGGER.log(Level.FINEST, "triggerTimedQuit called.");
        if (howManyGamesLeft > 0)
        {
            LOGGER.info("HowManyGamesLeft not zero yet - ignoring the "
                + "request to trigger a timed quit.");
        }
        else
        {
            new TimedJvmQuit().start();
        }
    }

    /**
     * A demon thread which is started by triggerTimedQuit.
     * It will then (currently) sleep 10 seconds, and if it is then
     * still alive, do a System.exit(1) to terminate the JVM.
     * If, however, the game shutdown proceeded successfully as planned,
     * Start.main() will already have reached it's end and there should
     * not be any other non-demon threads alive, so the JVM *should*
     * terminate by itself cleanly.
     * So, if this TimedJvmQuit strikes, it means the "clean shutdown"
     * has somehow failed. 
     */
    public static class TimedJvmQuit extends Thread
    {
        private static final Logger LOGGER = Logger.getLogger(
            WhatNextManager.TimedJvmQuit.class.getName());

        private static final String defaultName = "TimedJvmQuit thread";
        private final String name;

        private final long timeOutInSecs = 10;

        public TimedJvmQuit()
        {
            super();
            this.setDaemon(true);
            this.name = defaultName;
        }

        @Override
        public void run()
        {
            LOGGER.info(this.name + ": started... (sleeping " + timeOutInSecs
                + " seconds)");
            sleepFor(this.timeOutInSecs * 1000);
            LOGGER.warning(this.name + ": JVM still alive? "
                + "Ok, it's time to do System.exit()...");
            System.exit(1);
        }

        public static void sleepFor(long millis)
        {
            try
            {
                Thread.sleep(millis);
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.FINEST,
                    "InterruptException caught... ignoring it...");
            }
        }
    }

    
    /**
     * The various constants for activities what the Start class should do
     * as next thing, typically when a dialog is closed or a games ended.
     */
    public static enum WhatToDoNext
    {
        START_GAME("Start Game"), START_NET_CLIENT("Start network client"), START_WEB_CLIENT(
            "Start Web Client"), LOAD_GAME("Load Game"), GET_PLAYERS_DIALOG(
            "GetPlayers dialog"), NET_CLIENT_DIALOG("Network Client dialog"), QUIT_ALL(
            "Quit All");

        private final String activity;

        private WhatToDoNext(String act)
        {
            this.activity = act;
        }

        /**
         * Returns a non-localized UI string for the "whatToDoNext" activity.
         */
        @Override
        public String toString()
        {
            return activity;
        }
    }

    
    
    
}
