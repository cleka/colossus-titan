package net.sf.colossus.server;


import java.util.*;

import net.sf.colossus.util.Options;


/**
 * Advances to the next phase.
 * @version $Id$
 * @author David Ripton
 */

public abstract class PhaseAdvancer
{
    private Timer timer;
    private boolean isHuman;


    void startTimer(int delay)
    {
        timer = new Timer();
        timer.schedule(new AdvancePhaseListener(), delay);
    }

    int getDelay(Server server, boolean isHuman)
    {
        int delay = Constants.MIN_AI_DELAY;
        if (server != null)
        {
            delay = server.getIntOption(Options.aiDelay);
        }
        if (isHuman || delay < Constants.MIN_AI_DELAY)
        {
            delay = Constants.MIN_AI_DELAY;
        }
        if (delay > Constants.MAX_AI_DELAY)
        {
            delay = Constants.MAX_AI_DELAY;
        }

        return delay;
    }

    abstract void advancePhase();

    abstract void advancePhaseInternal();

    abstract void advanceTurn();


    private class AdvancePhaseListener extends TimerTask
    {
        public void run()
        {
            advancePhaseInternal();
        }
    }
}
