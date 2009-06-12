package net.sf.colossus.server;


/**
 * Advances to the next phase.
 *
 * @author David Ripton
 */
public interface PhaseAdvancer
{
    void advancePhase();

    void advancePhaseInternal();

    void advanceTurn();
}
