import java.util.*;
import javax.swing.*;

/**
 *  Class Start contains code to start a hotseat game.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Start
{

    public static void main(String [] args)
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

        if (args.length == 0)
        {
            // Start a new game.
            new Game(null);
        }
        else
        {
            // Load a game.
            new Game(null, args[0]);
        }
    }
}
