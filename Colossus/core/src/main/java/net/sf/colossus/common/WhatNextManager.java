package net.sf.colossus.common;


import java.util.logging.Level;
import java.util.logging.Logger;


public class WhatNextManager
{
    private static final Logger LOGGER = Logger
        .getLogger(WhatNextManager.class.getName());

    private final Options startOptions;
    private boolean interactive;
    private WhatToDoNext whatToDoNext;
    private int howManyGamesLeft;

    public WhatNextManager(Options startOpts)
    {
        this.startOptions = startOpts;
        this.interactive = false;
        this.howManyGamesLeft = Options.getHowManyStresstestRoundsProperty();
    }

    public WhatToDoNext getWhatToDoNext()
    {
        return whatToDoNext;
    }

    /**
     * Returns true if this action was caused by interactive means.
     * If so, it makes sense to display a error message dialog box if
     * something went wrong.
     * @return action to do is marked as "was triggered interactively"
     */
    public boolean isInteractive()
    {
        return interactive;
    }

    /**
     * Set the action what shall be executed next.
     * Trigger also the timer for the "Timed Quit", if requested so.
     *
     * @param whatToDoNext
     * @param triggerQuitTimer
     * @param interactive
     */
    public void setWhatToDoNext(WhatToDoNext whatToDoNext,
        boolean triggerQuitTimer, boolean interactive)
    {
        this.whatToDoNext = whatToDoNext;
        this.interactive = interactive;
        LOGGER.log(Level.INFO,
            "Set what to do next to " + whatToDoNext.toString());
        if (triggerQuitTimer)
        {
            triggerTimedQuit();
        }
    }

    /**
     * A convenient shortcut to the 3-argument-form,
     * for the many calls where interactive is to be set to false.
     * @param whatToDoNext
     * @param triggerQuitTimer
     */
    public void setWhatToDoNext(WhatToDoNext whatToDoNext,
        boolean triggerQuitTimer)
    {
        setWhatToDoNext(whatToDoNext, triggerQuitTimer, false);
    }

    /**
     *
     * @return Returns the same startOptions object that Start object uses.
     */
    public Options getStartOptions()
    {
        return startOptions;
    }

    public void setWhatToDoNext(WhatToDoNext whatToDoNext, String loadFile,
        boolean interactive)
    {
        setWhatToDoNext(whatToDoNext, false, interactive);
        startOptions.setOption(Options.loadGameFileName, loadFile);
    }

    public int getHowManyGamesLeft()
    {
        return howManyGamesLeft;
    }

    public int decrementHowManyGamesLeft()
    {
        howManyGamesLeft--;
        LOGGER.log(Level.INFO, "howManyGamesLeft now " + howManyGamesLeft);
        return howManyGamesLeft;
    }

    /**
     * Trigger a timed Quit, which will (by using a demon thread) terminate
     * the JVM after a timeout (currently 10 (120) seconds)
     * - unless the JVM has quit already anyway because cleanup has
     * succeeded as planned.
     */
    public void triggerTimedQuit()
    {
        LOGGER.log(Level.FINEST, "triggerTimedQuit called.");
        if (Options.isFunctionalTest())
        {
            LOGGER.info("Functional test ongoing - ignoring the "
                + "request to trigger a timed quit.");
        }
        else if (Options.isStresstest() && howManyGamesLeft > 0)
        {
            LOGGER.info("HowManyGamesLeft now " + howManyGamesLeft
                + " not zero yet - ignoring the "
                + "request to trigger a timed quit.");
        }
        else
        {
            new TimedJvmQuit().start();
        }
    }

    /**
     * A demon thread which is started by triggerTimedQuit.
     * It will then (currently) sleep 10 (120) seconds, and if it is then
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
        private static final Logger LOGGER = Logger
            .getLogger(WhatNextManager.TimedJvmQuit.class.getName());

        private static final String defaultName = "TimedJvmQuit thread";
        private final String name;

        // For now, on the web server, 120, because there were cases where
        // in case of a draw Clients did not catch up.
        // I suspect they are too busy processing all the legion cleanup
        // (and related updateCreatureCount) messages.
        // So give them more time for a while.
        // try 30, reconnect causes server to hang (not cleanly exit itself) at the moment
        // 17.10.2013: 120 again, quite many games on CPGS
        // the QUIT hit before catch up processing completed...
        private final long timeOutInSecs = 120;

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
