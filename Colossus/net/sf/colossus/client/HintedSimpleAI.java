package net.sf.colossus.client;

import net.sf.colossus.server.Creature;
import java.util.*;

/**
 * Simple implementation of a Titan AI - using the hints
 * @version $Id$
 * @author Romain Dolbeau
 */

public class HintedSimpleAI extends SimpleAI
{   
    HintedSimpleAI(Client client)
    {
        super(client);

        /* up the ratios a little, as in CowardSimpleAI */
        RATIO_WIN_MINIMAL_LOSS = 1.45; // 1.30;
        RATIO_WIN_HEAVY_LOSS = 1.25; // 1.15;
        RATIO_DRAW = 0.90; // 0.85;
        RATIO_LOSE_HEAVY_LOSS = 0.75; // 0.70;
    }

    Creature chooseRecruit(LegionInfo legion, String hexLabel)
    {
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);

        List recruits = client.findEligibleRecruits(legion.getMarkerId(), 
            hexLabel);

        if (recruits.size() == 0)
        {
            return null;
        }

        return getVariantRecruitHint(legion, hex, recruits);
    }

    static List doInitialGameSplit(String label)
    {
        java.util.List hintSuggestedSplit = getInitialSplitHint(label);

        if ((hintSuggestedSplit == null) ||
            (hintSuggestedSplit.size() != 4))
        {
            return SimpleAI.doInitialGameSplit(label);
        }

        return hintSuggestedSplit;
    }
}
