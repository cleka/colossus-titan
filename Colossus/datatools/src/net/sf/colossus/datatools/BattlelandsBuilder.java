package net.sf.colossus.datatools;


import net.sf.colossus.client.Hex;


/**
 * Class BattlelandsBuilder
 * @version $Id$
 * @author Romain Dolbeau
 */
public class BattlelandsBuilder
{
    public static void main(String[] arg)
    {
        String file = null;

        if (arg.length > 0)
        {
            file = arg[0];
            System.out.println("Opening " + file);
        }

        Hex.setOverlay(true);

        new ShowBuilderHexMap(file);
    }
}
