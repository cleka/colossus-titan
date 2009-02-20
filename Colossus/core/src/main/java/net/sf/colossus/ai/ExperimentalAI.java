package net.sf.colossus.ai;

import java.util.Collection;
import java.util.List;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;


/**
 * Yet Another AI, to test some stuff.
 * @version $Id$
 * @author Romain Dolbeau
 */

public class ExperimentalAI extends SimpleAI
{
    public ExperimentalAI(Client client)
    {
        super(client);
    }

    Collection<LegionMove> findLegionMoves(final List<List<CritterMove>> allCritterMoves) {
        return new OnTheFlyLegionMove(allCritterMoves);
    }
}
