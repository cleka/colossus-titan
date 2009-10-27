package net.sf.colossus.tools;


import net.sf.colossus.gui.GUIHex;
import net.sf.colossus.server.VariantSupport;


/**
 * Class BattlelandsBuilder
 *
 * @author Romain Dolbeau
 */
public class BattlelandsBuilder
{

    public static void main(String[] arg)
    {
        /* must load "Random" here so that the Randomize menu work. */
        VariantSupport.loadVariantByName("Random", true);

        GUIHex.setOverlay(true);

        new ShowBuilderHexMap();
    }
}
