package net.sf.colossus.tools;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.BattleHex;


/**
 * Class BattlelandsRandomizer
 * @version $Id: BattlelandsRandomizer.java 2557 2006-05-05 10:42:15Z peterbecker $
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
            System.err.println("<!-- BattlelandsRandomizer is using " + file
                + " -->");
        }
        else
        {
            System.err.println("Must supply an input file on command-line");
            System.exit(0);
        }

        BuilderHexMap bhm = new BuilderHexMap();

        List<String> directories = new ArrayList<String>();
        directories.add(".");
        directories.add("");
        InputStream inputFile = StaticResourceLoader.getInputStream(file,
            directories);

        BattleHex[][] h = bhm.getBattleHexArray();
        bhm.doRandomization(h, inputFile);

        System.out.println(bhm.dumpAsString());

        System.exit(0);
    }
}
