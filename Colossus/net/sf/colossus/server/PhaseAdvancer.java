package net.sf.colossus.server;


/**
 * Advances to the next phase.
 * @version $Id$
 * @author David Ripton
 */

public interface PhaseAdvancer
{
    void advancePhase();

    void advancePhaseInternal();

    void advanceTurn();
}
