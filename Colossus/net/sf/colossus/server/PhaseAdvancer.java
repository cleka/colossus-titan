package net.sf.colossus.server;


import javax.swing.*;
import java.awt.event.*;


/**
 * Advances to the next phase.
 * @version $Id$
 * @author David Ripton
 */

public abstract class PhaseAdvancer
{
    private javax.swing.Timer timer;
    private Server server;
    private boolean isHuman;


    private class AdvancePhaseListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            advancePhaseInternal();
        }
    }

    void startTimer(int delay)
    {
        timer = new javax.swing.Timer(delay, new AdvancePhaseListener());
        timer.setRepeats(false);
        timer.start();
    }

    int getDelay(Server server, boolean isHuman)
    {
        int delay = Constants.MIN_DELAY;
        if (server != null)
        {
            delay = server.getClientIntOption(Options.aiDelay);
        }
        if (isHuman || delay < Constants.MIN_DELAY)
        {
            delay = Constants.MIN_DELAY;
        }
        if (delay > Constants.MAX_DELAY)
        {
            delay = Constants.MAX_DELAY;
        }
        return delay;
    }

    abstract void advancePhase();

    abstract void advancePhaseInternal();

    abstract void advanceTurn();
}
