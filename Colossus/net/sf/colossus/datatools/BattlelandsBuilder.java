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
        String name = "new";

        if (arg.length > 0)
        {
            file = arg[0];
            System.out.println("Opening " + file);
            name = file;
        }

        ShowBuilderHexMap bhm = new ShowBuilderHexMap(name, ' ', file);

        
    }
}
