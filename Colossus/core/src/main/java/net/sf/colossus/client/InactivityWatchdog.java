package net.sf.colossus.client;


import java.awt.Color;
import java.util.logging.Logger;

import net.sf.colossus.util.HTMLColor;


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

    public static final int DEFAULT_INTERVAL = 30;

    public static final int DEBUG_INTERVAL = 10;

    private static final boolean IA_DEBUG = false;

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

    private boolean userRequestsControlBack = false;

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
        if (aiWasInControl || userRequestsControlBack)
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
            autoplay.switchOffInactivityAutoplay();
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
                aiIsInControl = true;
                currentlyStillToleratedIntervals = 1;
                inactiveSeconds = 0;
                currentInterval = 0;
                autoplay.switchOnInactivityAutoplay();
                inactivityTimeoutReached();
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
        final String title = "It's your turn (" + inactiveSecs
            + " seconds inactive of allowed " + timeoutSecs + " seconds)!";
        String pluralS = (currentInterval == 1 ? "" : "s");
        final String text = "\nHey, it's your turn to do something, and "
            + "you've been doing nothing for " + currentInterval + " interval"
            + pluralS + " à " + inactivityWarningInterval + " seconds.\n"
            + "After a total of " + timeoutSecs
            + " seconds of inactivity the AI will take over for you!";
        Color color = (currentInterval == 1 ? HTMLColor.lightYellow
            : HTMLColor.yellow);
        client.getGUI().displayInactivityDialogEnsureEDT(title, text, color);
    }

    public void inactivityTimeoutReached()
    {
        LOGGER.finer("ClientThread " + client.getOwningPlayer().getName()
            + ": reached inactivity timeout! Enabling Autoplay.");
        final String title = "AI took over for you!";
        final String text = "\n"
            + "You've been inactive for too long. AI took over for you in this round, "
            + "and can not be safely interrupted (might hang the game).\n\n"
            + "If you click OK now, then you will be back in control at next possible occasion.";
        client.getGUI().displayInactivityDialogEnsureEDT(title, text,
            HTMLColor.orange);
    }

    public void inactivityAiCompletedTurn()
    {
        String part2;
        Color color;
        if (userRequestsControlBack)
        {
            aiWasInControl = false;
            markThatSomethingHappened();
            return;
        }

        part2 = "You clicked OK, so you will be back in control next time when you are supposed to do "
            + " something (your turn starts, or you are attacked). You can safely close this window now.";
        color = Color.lightGray;

        part2 = "Click OK to signal that you are back, otherwise time AI will take over again "
            + "when you are next time\nsupposed to do something\n"
            + "(your turn starts, or you are attacked), but then after a shorter waiting period.\n";
        color = HTMLColor.yellow;

        final String title = "The AI finished for you!";
        final String text = "\nThe AI had finished your turn or engagement, "
            + "so right now it's probably some other players turn.\n" + part2;
        client.getGUI().displayInactivityDialogEnsureEDT(title, text, color);
    }

    public void inactivityContinues()
    {
        if (userRequestsControlBack)
        {
            userRequestsControlBack = false;
            printDebugText("\n\n OK, go ahead!\n\n");
            final String title = "Your turn #" + client.getTurnNumber()
                + " has started!";
            final String text = "\n" + "All right, go ahead!";
            client.getGUI().displayInactivityDialogEnsureEDT(title, text,
                HTMLColor.lightYellow);
        }
        else
        {
            printDebugText("inactivityContinues");
            final String title = "Your turn #" + client.getTurnNumber()
                + " has started!";
            final String text = "\n"
                + "You have not done anything since last turn where AI took over,\n"
                + "so this time AI will take over after a shorter waiting period.\n"
                + "Click OK to signal that you are back and take back control.";
            client.getGUI().displayInactivityDialogEnsureEDT(title, text,
                HTMLColor.lightSalmon);
        }
    }

    public boolean isClockTicking()
    {
        synchronized (this)
        {
            return clockIsTicking;
        }
    }

    public boolean userRequestsControlBack()
    {
        printDebugValues("userRequestsControlBack");
        boolean canCloseImmediately;
        if (aiIsInControl)
        {
            printDebugText("ai in Control - return false");
            canCloseImmediately = false;
            userRequestsControlBack = true;
        }
        else
        {
            printDebugText("ai not in Control - return true");
            canCloseImmediately = true;
            somethingHappened = true;
        }
        return canCloseImmediately;
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
