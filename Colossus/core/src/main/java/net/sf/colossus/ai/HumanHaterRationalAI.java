package net.sf.colossus.ai;


import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;


/**
 * Simple implementation of a Titan AI - a bit more coward the regular SimpleAI
 * @version $Id$
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
        hintSectionUsed[0] = Constants.sectionDefensiveAI;
    }

    /** Return true if we need to run this method again after the server
     *  updates the client with the results of a move or mulligan. */
    @Override
    public boolean masterMove()
    {
        if (this.I_HATE_HUMANS)
        {
            // check that this is still the case
            if (this.client.onlyAIsRemain())
            {
                // only computer players remain.
                this.I_HATE_HUMANS = false;
            }
        }

        // call the (overridden) parent method.
        return super.masterMove();
    }
}
