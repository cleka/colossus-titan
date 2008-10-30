package net.sf.colossus.ai;


import net.sf.colossus.client.Client;
import net.sf.colossus.server.Constants;


/**
 * Simple implementation of a Titan AI - a bit more coward the regular SimpleAI
 * @version $Id$
 * @author Romain Dolbeau
 */

public class CowardSimpleAI extends SimpleAI
{
    public CowardSimpleAI(Client client)
    {
        super(client);


        /* this is a defensive AI, not an offensive one, so use
         the proper hints section */
        hintSectionUsed[0] = Constants.sectionDefensiveAI;
    }

    /* up the ratios a little */

    @Override
    double RATIO_WIN_MINIMAL_LOSS()
    {
        return 1.45;
    }

    @Override
    double RATIO_WIN_HEAVY_LOSS()
    {
        return 1.25;
    }

    @Override
    double RATIO_DRAW()
    {
        return 0.90;
    }

    @Override
    double RATIO_LOSE_HEAVY_LOSS()
    {
        return 0.75;
    }
}
