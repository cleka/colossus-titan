package net.sf.colossus.server;


import java.util.*;
import javax.swing.*;

import com.werken.opt.Option;
import com.werken.opt.CommandLine;
import net.sf.colossus.util.Log;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.StartClient;

// This class uses both com.werken.opt.Options and 
// net.sf.colossus.util.Options, but only the latter is imported.
import net.sf.colossus.util.Options;



/**
 *  Class Start contains code to start a hotseat game.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Start
{
    private static void usage(com.werken.opt.Options opts)
    {
        Log.event("Usage: java -jar Colossus.jar [options]");
        Iterator it = opts.getOptions().iterator();
        while (it.hasNext())
        {
            Option opt = (Option)it.next();
            Log.event(opt.toString());
        }
    }


    // TODO Detect contradictory options.
    private static void startClient(CommandLine cl)
    {
        String playerName = Constants.username;
        String hostname = Constants.localhost;
        int port = Constants.defaultPort;

        if (cl.optIsSet('m'))
        {
            playerName = cl.getOptValue('m');
        }
        if (cl.optIsSet('s'))
        {
            hostname = cl.getOptValue('s');
        }
        if (cl.optIsSet('p'))
        {
            port = Integer.parseInt(cl.getOptValue('p'));
        }

        if (cl.optIsSet('g'))
        {
            StartClient.connect(playerName, hostname, port);
        }
        else
        {
            new StartClient(playerName, hostname, port);
        }
    }

    private static void clearNonPersistentOptions(
        net.sf.colossus.util.Options options)
    {
        options.setOption(Constants.runClient, false);
        options.removeOption(Constants.loadGame);
    }

    static void startupDialog(Game game, CommandLine cl)
    {
        net.sf.colossus.util.Options options = game.getOptions();
        options.loadOptions();
        clearNonPersistentOptions(options);
        setupOptionsFromCommandLine(cl, game);

        new GetPlayers(new JFrame(), options);
        String loadFilename = options.getStringOption(Constants.loadGame);

        if (options.isEmpty())
        {
             // Bad input, or user selected Quit.
             game.dispose();
        }

        // See if user hit the Load game button, and we should
        // load a game instead of starting a new one.
        else if (loadFilename != null && loadFilename.length() > 0)
        {
            options.clearPlayerInfo();
            game.loadGame(loadFilename);
        }

        // See if user hit the Run client button, and we should abort
        // the server and run the client.
        else if (options.getOption(Constants.runClient))
        {
            startClient(cl);
        }

        else
        {
            game.newGame();
        }
    }


    /** Modify options from command-line args if possible.  Clear
     *  options to abort if something is wrong. */
    private static void setupOptionsFromCommandLine(CommandLine cl, Game game)
    {
        if (cl == null)
        {
            return;
        }
        net.sf.colossus.util.Options options = game.getOptions();

        int numHumans = 0;
        int numAIs = 0;
        int numNetworks = 0;

        if (cl.optIsSet('v'))
        {
            String variantName = cl.getOptValue('v');
            // XXX Check that this variant is in the list.
            options.setOption(Options.variant, variantName);
        }
        if (cl.optIsSet('q'))
        {
            options.setOption(Options.autoQuit, true);
        }
        if (cl.optIsSet('u'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('u');
            numHumans = Integer.parseInt(buf);
        }
        if (cl.optIsSet('i'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('i');
            numAIs = Integer.parseInt(buf);
        }
        if (cl.optIsSet('n'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('n');
            numNetworks = Integer.parseInt(buf);
        }
        if (cl.optIsSet('p'))
        {
            String buf = cl.getOptValue('p');
            int port = Integer.parseInt(buf);
            game.setPort(port);
        }
        if (cl.optIsSet('d'))
        {
            String buf = cl.getOptValue('d');
            int delay = Integer.parseInt(buf);
            options.setOption(Options.aiDelay, delay);
        }
        if (cl.optIsSet('t'))
        {
            String buf = cl.getOptValue('t');
            int limit = Integer.parseInt(buf);
            options.setOption(Options.aiTimeLimit, limit);
        }
        // Quit if values are bogus.
        if (numHumans < 0 || numAIs < 0 || numNetworks < 0 ||
            numHumans + numAIs + numNetworks > Constants.MAX_MAX_PLAYERS)
        {
            Log.error("Illegal number of players");
            options.clear();
            return;
        }

        for (int i = 0; i < numHumans; i++)
        {
            String name = null;
            if (i == 0)
            {
                name = Constants.username;
            }
            else
            {
                name = Constants.byColor + i;
            }
            options.setOption(Options.playerName + i, name);
            options.setOption(Options.playerType + i, Constants.human);
        }
        for (int j = numHumans; j < numNetworks + numHumans; j++)
        {
            String name = Constants.byClient + j;
            options.setOption(Options.playerName + j, name);
            options.setOption(Options.playerType + j, Constants.network);
        }
        for (int k = numHumans + numNetworks; 
            k < numAIs + numHumans + numNetworks; k++)
        {
            String name = Constants.byColor + k;
            options.setOption(Options.playerName + k, name);
            options.setOption(Options.playerType + k, Constants.defaultAI);
        }
    }



    public static void main(String [] args)
    {
        Log.event("Start for Colossus version " + Client.getVersion() + 
            " at " + new Date().getTime());

        com.werken.opt.Options opts = new com.werken.opt.Options();
        CommandLine cl = null;

        try
        {
            opts.addOption('h', "help", false, "Show options help");
            opts.addOption('l', "load", true, "Load savegame");
            opts.addOption('z', "latest", false, "Load latest savegame");
            opts.addOption('g', "go", false, "Skip startup dialogs");
            opts.addOption('v', "variant", true, "Set variant");
            opts.addOption('u', "nhuman", true, "Number of humans");
            opts.addOption('i', "nai", true, "Number of SimpleAIs");
            opts.addOption('n', "nnetwork", true, "Number of network slots");
            opts.addOption('q', "quit", false, "Quit JVM when game ends");
            opts.addOption('p', "port", true, "Server port number");
            opts.addOption('d', "delay", true, "AI delay in ms");
            opts.addOption('t', "timelimit", true, "AI time limit in s");
            opts.addOption('c', "client", false, "Run network client instead");
            opts.addOption('s', "server", true, "Server name or IP");
            opts.addOption('m', "myname", true, "My player name");

            cl = opts.parse(args);
        }
        catch (Exception ex)
        {
            // TODO Clean up the output.
            ex.printStackTrace();
            return;
        }

        Game game = new Game();

        if (cl.optIsSet('h'))
        {
            usage(opts);
        }

        else if (cl.optIsSet('c'))
        {
            startClient(cl);
        }

        else if (cl.optIsSet('l') || cl.optIsSet('z'))
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
            game.getOptions().loadOptions();
            game.loadGame(filename);
        }
        else if (cl.optIsSet('g'))
        {
            game.getOptions().loadOptions();
            setupOptionsFromCommandLine(cl, game);
            game.newGame(); 
        }
        else
        {
            startupDialog(game, cl);
        }
    }
}
