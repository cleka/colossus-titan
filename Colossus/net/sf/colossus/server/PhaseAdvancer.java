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
    private boolean isHuman;

    abstract void advancePhase();

    abstract void advancePhaseInternal();

    abstract void advanceTurn();
}
