package net.sf.colossus.server;


import java.util.*;
import javax.swing.*;

import net.sf.colossus.util.Log;
import com.werken.opt.Options;
import com.werken.opt.Option;
import com.werken.opt.CommandLine;


/**
 *  Class Start contains code to start a hotseat game.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Start
{
    public static void main(String [] args)
    {
        // This is a werken Options, not a util Options.
        Options opts = new Options();
        CommandLine cl = null;

        try
        {
            opts.addOption('h', "help", false, "Show options help");
            opts.addOption('l', "load", true, "Load savegame");
            opts.addOption('z', "latest", false, "Load latest savegame");
            opts.addOption('s', "start", false, "Start new game immediately");
            opts.addOption('v', "variant", true, "Set variant");
            opts.addOption('u', "nhuman", true, "Number of humans");
            opts.addOption('i', "nai", true, "Number of SimpleAIs");
            opts.addOption('q', "quit", false, "Quit JVM when game ends");

            cl = opts.parse(args);
        }
        catch (Exception ex)
        {
            // TODO Clean up the output.
            ex.printStackTrace();
            System.exit(1);
        }

        if (cl.optIsSet('h'))
        {
            usage(opts);
            System.exit(0);
        }

        Game game = new Game(cl);
        if (cl.optIsSet('l') || cl.optIsSet('z'))
        {
            String filename = "--latest";
            if (cl.optIsSet('l'))
            {
                filename = cl.getOptValue('l');
            }
            game.loadGame(filename);
        }
        else
        {
            game.newGame(); 
        }
    }

    private static void usage(Options opts)
    {
        System.out.println("Usage: java -jar Colossus.jar [options]");
        Iterator it = opts.getOptions().iterator();
        while (it.hasNext())
        {
            Option opt = (Option)it.next();
            System.out.println(opt.toString());
        }
    }
}
