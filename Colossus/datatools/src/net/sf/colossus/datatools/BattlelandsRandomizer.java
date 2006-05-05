package net.sf.colossus.datatools;


import java.io.InputStream;
import java.util.List;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.parser.BattlelandRandomizerLoader;
import net.sf.colossus.util.ResourceLoader;


/**
 * Class BattlelandsRandomizer
 * @version $Id$
 * @author Romain Dolbeau
 */
public class BattlelandsRandomizer
{
    public static void main(String[] arg)
    {
        String file = null;

        if (arg.length > 0)
        {
            file = arg[0];
            System.out.println("# BattlelandsRandomizer is using " + file);
        }
        else
        {
            System.err.println("Must supply an input file on command-line");
            System.exit(0);
        }

        BuilderHexMap bhm = new BuilderHexMap(null);

        List directories = new java.util.ArrayList();
        directories.add(".");
        directories.add("");
        InputStream inputFile = ResourceLoader.getInputStream(file, directories);
        BattlelandRandomizerLoader parser = new BattlelandRandomizerLoader(inputFile);

        BattleHex[][] h = bhm.getBattleHexArray();
        try
        {
            while (parser.oneArea(h) >= 0)
            {
            }
            parser.resolveAllHexsides(h);
        }
        catch (Exception e)
        {
            System.err.println(e);
        }

        System.out.println(bhm.dumpAsString());

        System.exit(0);
    }

}
