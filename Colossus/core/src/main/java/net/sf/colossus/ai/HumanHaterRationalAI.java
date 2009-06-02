package net.sf.colossus.ai;


import java.util.Collections;

import net.sf.colossus.client.Client;
import net.sf.colossus.variant.IVariantHint.AIStyle;


/**
 * Simple implementation of a Titan AI - a bit more coward the regular SimpleAI
 *
 * @author Romain Dolbeau
 */

public class HumanHaterRationalAI extends RationalAI // NO_UCD
{
    public HumanHaterRationalAI(Client client)
    {
        super(client);

        I_HATE_HUMANS = true;

        /* this is a defensive AI, not an offensive one, so use
         the proper hints section */
        hintSectionUsed = Collections.singletonList(AIStyle.Defensive);
    }

    /** Return true if we need to run this method again after the server
     *  updates the client with the results of a move or mulligan. */
    @Override
    public boolean masterMove()
    {
        if (this.I_HATE_HUMANS)
        {
            // check that this is still the case
            if (this.client.getGameClientSide().onlyAIsRemain())
            {
                // only computer players remain.
                this.I_HATE_HUMANS = false;
            }
        }

        // call the (overridden) parent method.
        return super.masterMove();
    }
}
