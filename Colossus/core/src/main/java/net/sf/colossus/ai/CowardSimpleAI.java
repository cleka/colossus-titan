package net.sf.colossus.ai;


import java.util.Collections;

import net.sf.colossus.client.Client;
import net.sf.colossus.variant.IVariantHint;


/**
 * Simple implementation of a Titan AI - a bit more coward the regular SimpleAI
 *
 * @author Romain Dolbeau
 */

public class CowardSimpleAI extends SimpleAI // NO_UCD
{
    public CowardSimpleAI(Client client)
    {
        super(client);

        /* this is a defensive AI, not an offensive one, so use
         the proper hints section */
        hintSectionUsed = Collections
            .singletonList(IVariantHint.AIStyle.Defensive);
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
