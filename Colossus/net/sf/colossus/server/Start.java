package net.sf.colossus.server;


import java.util.*;
import javax.swing.*;

import com.werken.opt.Options;
import com.werken.opt.Option;
import com.werken.opt.CommandLine;
import net.sf.colossus.util.Log;
import net.sf.colossus.client.Client;


/**
 *  Class Start contains code to start a hotseat game.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Start
{
    private static void usage(Options opts)
    {
        Log.event("Usage: java -jar Colossus.jar [options]");
        Iterator it = opts.getOptions().iterator();
        while (it.hasNext())
        {
            Option opt = (Option)it.next();
            Log.event(opt.toString());
        }
    }


    public static void main(String [] args)
    {
        Log.event("Start for Colossus version " + Client.getVersion());

        // This is a werken Options, not a util Options.
        Options opts = new Options();
        CommandLine cl = null;

        try
        {
            opts.addOption('h', "help", false, "Show options help");
            opts.addOption('l', "load", true, "Load savegame");
            opts.addOption('z', "latest", false, "Load latest savegame");
            opts.addOption('g', "go", false, "Start new game immediately");
            opts.addOption('v', "variant", true, "Set variant");
            opts.addOption('u', "nhuman", true, "Number of humans");
            opts.addOption('i', "nai", true, "Number of SimpleAIs");
            opts.addOption('n', "nnetwork", true, "Number of network slots");
            opts.addOption('q', "quit", false, "Quit JVM when game ends");
            opts.addOption('p', "port", true, "Server port number");

            cl = opts.parse(args);
        }
        catch (Exception ex)
        {
            // TODO Clean up the output.
            ex.printStackTrace();
            return;
        }

        if (cl.optIsSet('h'))
        {
            usage(opts);
            return;
        }

        Game game = new Game(cl);

        if (cl.optIsSet('l') || cl.optIsSet('z'))
        {
            String filename = null;
            if (cl.optIsSet('l'))
            {
                filename = cl.getOptValue('l');
            }
            else if (cl.optIsSet('z'))
            {
                filename = "--latest";
            }
            game.loadGame(filename);
        }
        else
        {
            game.newGame(); 
        }
    }
}
