package net.sf.colossus.client;


import java.util.logging.Logger;


/**
 * Class InactivityWatchdog follows whether there is any GUI activity by the
 * user. If it's the player's turn and he's inactive for too long, it shows a
 * notification dialogs after each interval of "inactivityWarningInterval"
 * seconds.
 * After INITIALLY_TOLERATED_INTERVALS times, it triggers the AI to finish the
 * turn. In following player's turn, it waits one interval fewer, following
 * again one fewer.
 * Once user becomes active again, number of intervals it waits is reset to
 * INITIALLY_TOLERATED_INTERVALS.
 *
 * @author Clemens Katzer
 */

public class InactivityWatchdog extends Thread
{
    private static final Logger LOGGER = Logger
        .getLogger(InactivityWatchdog.class.getName());

    private static final int INACTIVITY_CHECK_INTERVAL = 1;

    private static final boolean IA_DEBUG = false;

    private final Client client;

    private final int inactivityWarningInterval;

    // First turn 3, next time 2, then 1, then only few secs
    private static int INITIALLY_TOLERATED_INTERVALS = 3;
    private int currentlyStillToleratedIntervals;
    private int currentInterval;

    private int inactiveSeconds = 0;

    private boolean clockIsTicking = false;

    private boolean wasTicking = false;

    private boolean somethingHappened = false;

    private boolean done;

    public InactivityWatchdog(Client client, int inactivityWarningInterval)
    {
        this.client = client;

        this.inactivityWarningInterval = inactivityWarningInterval;
        done = false;

        LOGGER.fine("\n\nInactivityWatchdog instantiated");
    }

    public void setDone(boolean value)
    {
        done = value;
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
                if (IA_DEBUG)
                {
                    System.out.print("0");
                }
                inactiveSeconds = 0;
                currentInterval = 0;
                currentlyStillToleratedIntervals = INITIALLY_TOLERATED_INTERVALS;
                somethingHappened = false;
            }
            else
            {
                if (IA_DEBUG)
                {
                    if (isClockTicking())
                    {
                        System.out.print(":");
                    }
                    else
                    {
                        System.out.print(".");
                    }
                }
            }

            if (isClockTicking())
            {
                if (!wasTicking)
                {
                    LOGGER
                        .fine("Noticed now that Clock has started ticking...");
                    wasTicking = true;
                }
                checkInactivityStatus();
            }
            else
            {
                if (wasTicking)
                {
                    LOGGER
                        .fine("Noticed now that Clock has stopped ticking...");
                    wasTicking = false;
                }
                else
                {
                    LOGGER.finest("clock is not ticking...");
                }
                inactiveSeconds = 0;
                currentInterval = 0;
            }
        }
        LOGGER.info("Done flag set, watchdog ends now...");
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
                LOGGER
                    .fine("Watchdog sleep was interrupted and done is true; that's fine.");
            }
            else
            {
                LOGGER
                    .warning("watchdog: interruptedException but done not set??");
            }
        }
    }

    private void checkInactivityStatus()
    {
        inactiveSeconds++;
        if (inactiveSeconds >= inactivityWarningInterval)
        {
            inactiveSeconds = 0;
            currentInterval++;
            int totalInactiveSeconds = currentInterval
                * inactivityWarningInterval;

            if (currentInterval >= currentlyStillToleratedIntervals)
            {
                LOGGER.fine("TIMEOUT! User was " + totalInactiveSeconds
                    + " seconds idle! Notifying client.");
                client.inactivityTimeoutReached();
                currentlyStillToleratedIntervals--;
            }
            else
            {
                LOGGER.fine("User was already " + currentInterval
                    + " intervals ("
                    + (currentInterval * inactivityWarningInterval)
                    + " seconds) idle!");
                client
                    .inactivityWarning(
                        totalInactiveSeconds,
                        (currentlyStillToleratedIntervals * inactivityWarningInterval));
            }
        }
    }

    public void markThatSomethingHappened()
    {
        somethingHappened = true;
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

    public void setClockTicking()
    {
        synchronized (this)
        {
            clockIsTicking = true;
            markThatSomethingHappened();
        }
    }

    public void stopClockTicking()
    {
        synchronized (this)
        {
            clockIsTicking = false;
        }
    }

    public boolean isClockTicking()
    {
        synchronized (this)
        {
            return clockIsTicking;
        }

    }
}
