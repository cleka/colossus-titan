package net.sf.colossus.client;


import java.util.logging.Logger;


/**
 * Class InactivityWatchdog follows whether there is any GUI activity by the
 * user. If it's the player's turn and he's inactive for too long, it shows a
 * notification dialogs after each interval of "inactivityWarningInterval"
 * seconds.
 * After INITIALLY_TOLERATED_INTERVALS times, it triggers the AI to finish the
 * turn. In following player's turn, it waits only for one intverval.
 * Once user becomes active again, number of intervals it waits is reset to
 * INITIALLY_TOLERATED_INTERVALS.
 *
 * @author Clemens Katzer
 */

public class InactivityWatchdog extends Thread
{
    private static final Logger LOGGER = Logger
        .getLogger(InactivityWatchdog.class.getName());

    private static final int INACTIVITY_CHECK_INTERVAL = 5;

    private static final boolean IA_DEBUG = false;

    public static final int DEBUG_INTERVAL = 15;

    private final Client client;

    private final Autoplay autoplay;

    private final int inactivityWarningInterval;

    private static int INITIALLY_TOLERATED_INTERVALS = 3;
    private int currentlyStillToleratedIntervals;
    private int currentInterval;

    private int inactiveSeconds = 0;

    private boolean clockIsTicking = false;

    private boolean aiWasInControl = false;

    private boolean aiIsInControl = false;

    private boolean wasTicking = false;

    private boolean somethingHappened = false;

    private boolean done;

    public InactivityWatchdog(Client client, int inactivityWarningInterval)
    {
        this.client = client;
        this.autoplay = client.getAutoplay();
        this.inactivityWarningInterval = inactivityWarningInterval;
        done = false;

        LOGGER.fine("\n\nInactivityWatchdog instantiated");
    }

    public void setDone(boolean value)
    {
        done = value;
    }

    public void setClockTicking()
    {
        synchronized (this)
        {
            clockIsTicking = true;
            // markThatSomethingHappened();
        }
        if (aiWasInControl)
        {
            printDebugValues("inactivityContinues");
            aiWasInControl = false;
            inactivityContinues();
        }
    }

    public void stopClockTicking()
    {
        synchronized (this)
        {
            clockIsTicking = false;
        }
        inactiveSeconds = 0;
        currentInterval = 0;
        // If ClientThread switched us back to clock not ticking while
        // AI is in control, it means AI has completed the turn.
        if (aiIsInControl)
        {
            aiIsInControl = false;
            autoplay.resetInactivityAutoplay();
            aiWasInControl = true;
            inactivityAiCompletedTurn();
        }
    }

    private void printDebugText(String text)
    {
        if (IA_DEBUG)
        {
            System.out.print(text);
        }
    }

    @Override
    public void run()
    {
        currentlyStillToleratedIntervals = INITIALLY_TOLERATED_INTERVALS;
        currentInterval = 0;
        while (!done)
        {
            sleepForCheckIntervalSecs();
            if (done)
            {
                continue;
            }

            if (somethingHappened)
            {
                inactiveSeconds = 0;
                currentInterval = 0;
                currentlyStillToleratedIntervals = INITIALLY_TOLERATED_INTERVALS;
                somethingHappened = false;
            }
            else
            {
                printDebugText(isClockTicking() ? ":" : ".");
            }

            if (isClockTicking())
            {
                if (!wasTicking)
                {
                    LOGGER
                        .fine("Noticed now that Clock has started ticking (= this user's turn started, or was attacked)...");
                    wasTicking = true;
                }
                if (autoplay.isInactivityAutoplayActive())
                {
                    printDebugText("watchdog: clock is ticking but AI is working anyway.\n");
                    inactiveSeconds = 0;
                }
                else
                {
                    checkInactivityStatus();
                }
            }
            else
            // not ticking
            {
                if (wasTicking)
                {
                    LOGGER
                        .fine("Noticed now that Clock has stopped ticking...");
                    wasTicking = false;
                    printDebugValues("Clock stopped ticking");
                }
                else
                {
                    LOGGER.finest("clock is still not ticking...");
                }
                inactiveSeconds = 0;
                currentInterval = 0;
            }
        }
        LOGGER.info("Done flag set, watchdog ends now...");
    }

    private void checkInactivityStatus()
    {
        printDebugValues("check start");

        if (inactiveSeconds >= inactivityWarningInterval)
        {
            inactiveSeconds -= inactivityWarningInterval;
            currentInterval++;
            int totalInactiveSeconds = currentInterval
                * inactivityWarningInterval + inactiveSeconds;

            if (currentInterval >= currentlyStillToleratedIntervals)
            {
                LOGGER.finer("ClientThread "
                    + client.getOwningPlayer().getName()
                    + ": reached inactivity timeout! Enabling Autoplay.");
                printDebugValues("TIMEOUT");
                inactivityTimeoutReached();
                aiIsInControl = true;
                currentlyStillToleratedIntervals = 1;
                inactiveSeconds = 0;
                currentInterval = 0;
                aiIsInControl = true;
                autoplay.setInactivityAutoplay();
                client.getEventExecutor().retriggerEvent();
            }
            else
            {
                LOGGER.fine("Turn #" + client.getTurnNumber()
                    + " - user was already " + currentInterval
                    + " intervals (" + totalInactiveSeconds
                    + " seconds) doing nothing (away from screen?)!");
                inactivityWarning(
                    totalInactiveSeconds,
                    (currentlyStillToleratedIntervals * inactivityWarningInterval));
            }
        }
    }

    public void inactivityWarning(int inactiveSecs, int timeoutSecs)
    {
        LOGGER.finer("ClientThread " + client.getOwningPlayer().getName()
            + ": idle for " + inactiveSeconds + "seconds!");
        final String title = "It's your turn (" + inactiveSecs + "/"
            + timeoutSecs + ")!";
        final String text = "\nHey, it's your turn #"
            + client.getTurnNumber()
            + ", and you've been doing nothing for "
            + inactiveSecs
            + " seconds!\n "
            + "\nAfter a total of "
            + timeoutSecs
            + " seconds of inactivity the AI will take over this turn for you!";
        client.getGUI().displayInactivityDialogEnsureEDT(title, text);
    }

    public void inactivityTimeoutReached()
    {
        LOGGER.finer("ClientThread " + client.getOwningPlayer().getName()
            + ": reached inactivity timeout! Enabling Autoplay.");
        final String title = "AI took over your turn #"
            + client.getTurnNumber();
        final String text = "\n"
            + "You've been inactive for too long. AI took over for you in this round\n"
            + "(turn #"
            + client.getTurnNumber()
            + ", so now it's probably somebody else's turn.\n\n"
            + "You will be back in control in your next turn (or when attacked).";
        client.getGUI().displayInactivityDialogEnsureEDT(title, text);
    }

    public void inactivityAiCompletedTurn()
    {
        final String title = "The AI finished your turn #"
            + client.getTurnNumber();
        final String text = "The AI had finished your turn #"
            + client.getTurnNumber()
            + ",\nso right now it's probably some other players turn.\n"
            + "Close this dialog and/or click something in the masterboard to signal that you are back,\n"
            + "otherwise time AI will take over again when you are next time supposed to do something\n"
            + "(your turn starts, or you are attacked), but then after a shorter waiting period.\n";
        client.getGUI().displayInactivityDialogEnsureEDT(title, text);
    }

    public void inactivityContinues()
    {
        final String title = "Your turn #" + client.getTurnNumber()
            + " has started!";
        final String text = "\n"
            + "You have not done anything since last turn where AI took over,\n"
            + "so this time AI will take over after a shorter waiting period.\n"
            + "Close this dialog and/or click something in the masterboard to\n"
            + "to signal that you are back and take back control.";
        client.getGUI().displayInactivityDialogEnsureEDT(title, text);
    }

    public boolean isClockTicking()
    {
        synchronized (this)
        {
            return clockIsTicking;
        }
    }

    public void markThatSomethingHappened()
    {
        somethingHappened = true;
    }

    private void sleepForCheckIntervalSecs()
    {
        try
        {
            Thread.sleep(INACTIVITY_CHECK_INTERVAL * 1000);
        }
        catch (InterruptedException e)
        {
            LOGGER.finest("got interrupted, done=" + done);
            if (done)
            {
                LOGGER.fine("Watchdog sleep was interrupted "
                    + "and done is true; that's fine.");
            }
            else
            {
                LOGGER.warning("watchdog: interruptedException "
                    + "but done not set??");
            }
        }
        inactiveSeconds += INACTIVITY_CHECK_INTERVAL;
    }

    private void printDebugValues(String msg)
    {
        if (IA_DEBUG)
        {
            System.out.println(msg + ": inactiveSecs: " + inactiveSeconds
                + ", currentInterval: " + currentInterval + ", tolerated: "
                + currentlyStillToleratedIntervals);
        }
    }

    public void finish()
    {
        if (done)
        {
            LOGGER.fine("watchdog finish: done is already set.");
            return;
        }
        LOGGER.fine("finnish: setting done true");
        setDone(true);
        LOGGER.fine("interrupting sleep");
        this.interrupt();
    }
}
