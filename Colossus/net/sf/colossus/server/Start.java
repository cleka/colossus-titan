package net.sf.colossus.server;


import java.util.*;
import javax.swing.*;

import net.sf.colossus.util.Log;


/**
 *  Class Start contains code to start a hotseat game.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Start
{
    // TODO Let the user pick a different L&F.
    private static void setLookAndFeel()
    {
        // Set look and feel to native, since Metal does not show titles
        // for popup menus.
        try
        {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {
            Log.error(e + "Could not set look and feel.");
        }
    }

    public static void main(String [] args)
    {

        /* Options we want:
              Start new game, skipping GetPlayers dialog
                  names and types for each player
                  --variant <variant name>
              Load savegame 
                  --loadgame <savegame name)
                  --latest
              --help
              If no args, we need to just start a game.
        */

        // Trying werken.opts
        /*
        com.werken.opt.Options opts = new com.werken.opt.Options();

        // TODO Add each allowed option here.

        try
        {
            opts.parse(args);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        */

        setLookAndFeel();

        if (args.length == 0)
        {
            // Start a new game.
            Game game = new Game();
            game.newGame();
        }
        else
        {
            // Load a game.
            Game game = new Game();
            game.loadGame(args[0]);
        }
    }
}
