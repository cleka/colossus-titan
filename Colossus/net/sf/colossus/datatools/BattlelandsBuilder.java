package net.sf.colossus.datatools;

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

        ShowBuilderHexMap bhm = new ShowBuilderHexMap(file);
    }
}
