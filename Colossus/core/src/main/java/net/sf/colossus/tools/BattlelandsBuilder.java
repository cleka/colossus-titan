package net.sf.colossus.tools;

import net.sf.colossus.client.GUIHex;
import net.sf.colossus.server.VariantSupport;

/**
 * Class BattlelandsBuilder
 * @version $Id$
 * @author Romain Dolbeau
 */
public class BattlelandsBuilder
{

    public static void main(String[] arg)
    {
         VariantSupport.loadVariantByName("Default",
            true);

        GUIHex.setOverlay(true);

        new ShowBuilderHexMap();
    }
}
