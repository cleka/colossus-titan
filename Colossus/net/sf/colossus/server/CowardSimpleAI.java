package net.sf.colossus.server;

/**
 * Simple implementation of a Titan AI - a bit more coward the regular SimpleAI
 * @version $Id$
 * @author Romain Dolbeau
 */

public class CowardSimpleAI extends SimpleAI
{

    CowardSimpleAI()
    {
        /* up the ratios a little */
        RATIO_WIN_MINIMAL_LOSS = 1.45; // 1.30;
        RATIO_WIN_HEAVY_LOSS = 1.25; // 1.15;
        RATIO_DRAW = 0.90; // 0.85;
        RATIO_LOSE_HEAVY_LOSS = 0.75; // 0.70;
    }
}
