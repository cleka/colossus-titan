package net.sf.colossus.client;

/**
 * Simple implementation of a Titan AI - a bit more coward the regular SimpleAI
 * @version $Id$
 * @author Romain Dolbeau
 */

public class CowardSimpleAI extends SimpleAI
{
    CowardSimpleAI(Client client)
    {
        super(client);

        /* up the ratios a little */
        RATIO_WIN_MINIMAL_LOSS = 1.45; // 1.30;
        RATIO_WIN_HEAVY_LOSS = 1.25; // 1.15;
        RATIO_DRAW = 0.90; // 0.85;
        RATIO_LOSE_HEAVY_LOSS = 0.75; // 0.70;

        /* this is a defensive AI, not an offensive one, so use
           the proper hints section */
        hintSectionUsed[0] = 
            net.sf.colossus.parser.AIHintLoader.sectionDefensiveAI;
    }
}
