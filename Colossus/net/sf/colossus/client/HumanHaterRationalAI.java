package net.sf.colossus.client;


import net.sf.colossus.server.Constants;


/**
 * Simple implementation of a Titan AI - a bit more coward the regular SimpleAI
 * @version $Id$
 * @author Romain Dolbeau
 */

public class HumanHaterRationalAI extends RationalAI
{
    HumanHaterRationalAI(Client client)
    {
        super(client);

        I_HATE_HUMANS = true;

        /* this is a defensive AI, not an offensive one, so use
         the proper hints section */
        hintSectionUsed[0] = Constants.sectionDefensiveAI;
    }
}
