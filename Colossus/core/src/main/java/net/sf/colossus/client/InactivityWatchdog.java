/**
 *
 */
package net.sf.colossus.client;


import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.JComponent;


public class InactivityWatchdog extends Thread
{
    private static final Logger LOGGER = Logger
        .getLogger(InactivityWatchdog.class
        .getName());
    private static final long CHECK_INTERVAL_SECONDS = 30;
    private static final long INACTIVITY_WARNING_INTERVAL = 60;
    private static final long INACTIVITY_TIMEOUT = 180;

    private final Client client;

    private JComponent masterBoard = null;

    private JComponent currentBattleBoardPane = null;

    private final MouseActivityDetector mbmaDetector;

    private MouseActivityDetector currentBbmaDetector;

    private boolean clockIsTicking = false;

    private boolean wasTicking = false;

    private boolean done;

    private long lastActivity;

    public InactivityWatchdog(Client client, JComponent masterBoard)
    {
        this.client = client;
        this.masterBoard = masterBoard;
        mbmaDetector = new MouseActivityDetector(masterBoard, this);
        currentBbmaDetector = null;

        this.lastActivity = new Date().getTime();
        done = false;

        LOGGER.fine("InactivityWatchdog instantiated");
    }

    public void addBattleBoardFrame(JComponent battleBoardPane)
    {
        if (this.currentBattleBoardPane != null
            && this.currentBbmaDetector != null)
        {
            LOGGER.warning("there was still a currentBattleBoard???");
            currentBattleBoardPane.removeMouseListener(currentBbmaDetector);
            this.currentBattleBoardPane = null;
            this.currentBbmaDetector = null;
        }

        this.currentBattleBoardPane = battleBoardPane;
        currentBbmaDetector = new MouseActivityDetector(
            currentBattleBoardPane,
            this);
    }

    public void removeBattleBoardFrame()
    {
        if (this.currentBattleBoardPane != null)
        {
            if (currentBbmaDetector != null)
            {
                currentBattleBoardPane
                    .removeMouseListener(currentBbmaDetector);
                this.currentBbmaDetector = null;
            }
            else
            {
                this.currentBattleBoardPane = null;
            }
        }
        else
        {
            LOGGER.warning("called to remove bbFrame but is already null?");
        }
    }

    public void removeMasterBoardFrame()
    {
        masterBoard.removeMouseListener(mbmaDetector);
    }

    public void setDone(boolean value)
    {
        done = value;
    }

    private int lastIntervals = 0;

    @Override
    public void run()
    {
        while (!done)
        {
            if (isClockTicking())
            {
                if (!wasTicking)
                {
                    LOGGER
                        .fine("Noticed now that Clock has started ticking...");
                    wasTicking = true;
                }


                long now = new Date().getTime();
                long inactiveSecs = (now - lastActivity) / 1000;
                int intervals = (int)Math.floor(inactiveSecs
                    / INACTIVITY_WARNING_INTERVAL);

                if (inactiveSecs >= INACTIVITY_TIMEOUT)
                {
                    LOGGER.fine("Timeout! You were " + inactiveSecs
                        + " seconds idle!");
                    client.inactivityTimeoutReached();
                    stopClockTicking();
                    setLastActivity();
                    lastIntervals = 0;
                }
                else if (intervals > 0)
                {
                    if (intervals != lastIntervals)
                    {
                        client.inactivityWarning(intervals);
                        LOGGER.fine("Warning: You were now already "
                            + intervals + " intervals (" + inactiveSecs
                            + " seconds) idle!");
                        lastIntervals = intervals;
                    }
                    else
                    {
                        LOGGER
                            .finest("Inactivity check: still inactive, still in same interval");
                    }
                }
                else
                {
                    LOGGER.finest("Inactivity timeout not reached, going on");
                    lastIntervals = 0;
                }
            }
            else
            {
                if (wasTicking)
                {
                    LOGGER
                        .fine("Noticed now that Clock has stopped ticking...");
                    // this.lastActivity = new Date().getTime();
                    wasTicking = false;
                }
                else
                {
                    LOGGER.finest("clock is not ticking...");
                }
            }

            try
            {
                Thread.sleep(CHECK_INTERVAL_SECONDS * 1000);
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

        LOGGER.info("Done flag set, watchdog ends now...");
    }

    public void setLastActivity()
    {
        long previous = this.lastActivity;
        this.lastActivity = new Date().getTime();
        if (this.lastActivity - previous > 1000)
        {
            LOGGER.fine("Reset counter => 0 seconds idle");
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

    public void setClockTicking()
    {
        synchronized (this)
        {
            clockIsTicking = true;
            setLastActivity();
        }
    }

    public void stopClockTicking()
    {
        synchronized (this)
        {
            clockIsTicking = false;
        }
    }

    private boolean isClockTicking()
    {
        synchronized (this)
        {
            return clockIsTicking;
        }

    }

    public class MouseActivityDetector implements MouseListener
    {
        private final JComponent someWindow;

        private final InactivityWatchdog watchdog;

        public MouseActivityDetector(JComponent someWindow,
            InactivityWatchdog watchdog)
        {
            this.watchdog = watchdog;
            this.someWindow = someWindow;
            this.someWindow.addMouseListener(this);
        }

        //where initialization occurs:
        //Register for mouse events on blankArea and the panel.

        void somethingHappened(String what)
        {
            // dummy, just to silence eclipse warnings...
            if (what.equals("bla"))
            {
                LOGGER.fine(what);
            }
            if (what.equals("dummy"))
            {
                // nothing
            }
            watchdog.setLastActivity();
        }

        public void mousePressed(MouseEvent e)
        {
            somethingHappened("mp");
        }

        public void mouseExited(MouseEvent e)
        {
            somethingHappened("dummy");
        }

        public void mouseEntered(MouseEvent e)
        {
            somethingHappened("dummy");
        }

        public void mouseClicked(MouseEvent e)
        {
            somethingHappened("mc");
        }

        public void mouseReleased(MouseEvent e)
        {
            somethingHappened("mr");
        }


    } // END class MouseActivityDetector

}
