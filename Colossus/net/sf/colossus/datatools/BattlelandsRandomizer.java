package net.sf.colossus.datatools;

import net.sf.colossus.client.BattleHex;

/**
 * Class BattlelandsRandomizer
 * @version $Id$
 * @author Romain Dolbeau
 */
public class BattlelandsRandomizer
{
    private static final boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };

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

        java.util.List directories = new java.util.ArrayList();
        directories.add(".");
        directories.add("");
        java.io.InputStream inputFile = net.sf.colossus.util.ResourceLoader.getInputStream(file, directories);
        net.sf.colossus.parser.BattlelandRandomizerLoader parser = 
            new net.sf.colossus.parser.BattlelandRandomizerLoader(inputFile);

        BattleHex[][] h = bhm.getBattleHexArray();
        try {
            while (parser.oneArea(h) >= 0) {}
        } catch (Exception e) { System.err.println(e); }

        System.out.println(bhm.dumpAsString());

        System.exit(0);
    }

}
