import java.util.*;
import java.net.*;
import javax.swing.*;
import java.io.*;

/**
 *  Class Client lives on the client side and handles all communication
 *  with the server.  It talks to the client classes locally, and to
 *  Server via the network protocol.  There is one client per player.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Client
{
    /** This will eventually be a network interface rather than a
     *  direct reference.  So don't share this reference. */
    private Server server;

    // Moved here from Game.

    private MasterBoard board;
    private StatusScreen statusScreen;
    private SummonAngel summonAngel;
    private MovementDie movementDie;

    // Moved here from Battle.
    private BattleMap map;
    public static BattleDice battleDice; // XXX Only temporarily static.
    /** Stack of critters moved, to allow multiple levels of undo. */
    private LinkedList lastCrittersMoved = new LinkedList();

    // Moved here from Player.
    /** Stack of legion marker ids, to allow multiple levels of undo for
     *  splits, moves, and recruits. */
    private LinkedList undoStack = new LinkedList();
    private String moverId;

    // Per-client and per-player options should be kept here instead
    // of in Game.
    private Properties options = new Properties();

    /** Help keep straight whose client this is. */
    String playerName;
    int playerNum;


    /** Temporary constructor. */
    public Client(Server server)
    {
        this.server = server;
    }

    public Client()
    {
    }


    // Add all methods that GUI classes need to call against server classes.
    // Use boolean return type for voids so we can tell if they succeeded.

    // XXX How to distinguish things that failed because they were out
    // of sequence or the network packet was dropped, versus illegal in
    // the game?  Maybe change all boolean returns to ints and set up
    // some constants?

    // XXX Need to set up events to model requests from server to client.


    /** Pick a player name */
    boolean name(String playername)
    {
        return false;
    }

    /** Stop waiting for more players to join and start the game. */
    boolean start()
    {
        return false;
    }

    /** Pick a color */
    boolean color(String colorname)
    {
        return false;
    }

    /** Undo a split. Used during split phase and just before the end
     *  of the movement phase. */
    boolean join(String childLegionId, String parentLegionId)
    {
        return false;
    }

    /** Split some creatures off oldLegion into newLegion. */
    boolean split(String parentLegionId, String childLegionId,
        List splitCreatureNames)
    {
        return false;
    }

    /** Done with splits. Roll movement.  Returns the roll, or -1 on error. */
    int roll()
    {
        return -1;
    }

    /** Take a mulligan. Returns the roll, or -1 on error. */
    int mulligan()
    {
        return -1;
    }

    /** Normally move legion to land entering on entrySide. */
    boolean move(String legion, String land, int entrySide)
    {
        return false;
    }

    /** Lord teleports legion to land entering on entrySide. */
    boolean teleport(String legion, String land, int entrySide, String lord)
    {
        return false;
    }

    /** Legion undoes its move. */
    boolean undoMove(String legion)
    {
        return false;
    }

    /** Attempt to end the movement phase. Fails if nothing moved, or if
     *  split legions were not separated or rejoined. */
    boolean doneMoving()
    {
        return false;
    }

    /** Legion recruits recruit using the correct number of recruiter. */
    boolean recruit(String legion, String recruit, String recruiter)
    {
        return false;
    }

    /** Legion undoes its last recruit. */
    boolean undoRecruit(String legion)
    {
        return false;
    }

    /** Resolve engagement in land. */
    boolean engage(String land)
    {
        return false;
    }

    /** Legion flees. */
    boolean flee(String legion)
    {
        return false;
    }

    /** Legion does not flee. */
    boolean dontFlee(String legion)
    {
        return false;
    }

    /** Legion concedes. */
    boolean concede(String legion)
    {
        return false;
    }

    /** Legion does not concede. */
    boolean dontConcede(String legion)
    {
        return false;
    }

    /** Offer to negotiate engagement in land.  If legion or unitsLeft
     *  is null or empty then propose a mutual. If this matches one
     *  of the opponent's proposals, then accept it. */
    boolean negotiate(String land, String legion, List unitsLeft)
    {
        return false;
    }

    /** Cease negotiations and fight a battle in land. */
    boolean fight(String land)
    {
        return false;
    }

    /** Move offboard unit to hex. */
    boolean enter(String unit, String hex)
    {
        return false;
    }

    /** Maneuver unit in hex1 to hex2. */
    boolean maneuver(String hex1, String hex2)
    {
        return false;
    }

    /** Move unit in hex back to its old location. */
    boolean undoManeuver(String hex)
    {
        return false;
    }

    /** Done with battle maneuver phase. */
    boolean doneManeuvering()
    {
        return false;
    }

    /** Unit in hex1 strikes unit in hex2 with the specified number
     *  of dice and target number.  Returns number of hits or -1
     *  on error. */
    int strike(String hex1, String hex2, int dice, int target)
    {
        return -1;
    }

    /** Carry hits to unit in hex. */
    boolean carry(String hex, int hits)
    {
        return false;
    }

    /** Done with battle strike phase. */
    boolean doneStriking()
    {
        return false;
    }

    /** Legion summoner summons unit from legion donor. */
    boolean summon(String summoner, String donor, String unit)
    {
        return false;
    }

    /** Legion acquires an angel or archangel. */
    boolean acquire(String legion, String unit)
    {
        return false;
    }

    /** Finish this player's whole game turn. */
    boolean nextturn()
    {
        return false;
    }

    /** This player quits the whole game. The server needs to always honor
     *  this request, because if it doesn't players will just drop
     *  connections when they want to quit in a hurry. */
    boolean withdrawFromGame()
    {
        return false;
    }


    // TODO Add requests for info.

    public void setupDice(boolean enable)
    {
        if (enable)
        {
            initMovementDie();
            initBattleDice();
        }
        else
        {
            disposeMovementDie();
            disposeBattleDice();
            if (board != null)
            {
                board.twiddleOption(Options.showDice, false);
            }
        }
    }

    public void repaintAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.repaint();
        }
        if (movementDie != null)
        {
            movementDie.repaint();
        }
        if (board != null)
        {
            board.getFrame().repaint();
        }
        if (battleDice != null)
        {
            battleDice.repaint();
        }
        if (map != null)
        {
            map.repaint();
        }
    }

    public void rescaleAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.rescale();
        }
        if (movementDie != null)
        {
            movementDie.rescale();
        }
        if (board != null)
        {
            board.rescale();
        }
        if (battleDice != null)
        {
            battleDice.rescale();
        }
        if (map != null)
        {
            map.rescale();
        }
        repaintAllWindows();
    }

    public void showMovementRoll(int roll)
    {
        if (movementDie != null)
        {
            movementDie.showRoll(roll);
        }
    }



    private void initMovementDie()
    {
        movementDie = new MovementDie(this);
    }


    private void disposeMovementDie()
    {
        if (movementDie != null)
        {
            movementDie.dispose();
            movementDie = null;
        }
    }


    public boolean getOption(String name)
    {
        String value = options.getProperty(name);
        return (value != null && value.equals("true"));
    }

    public String getStringOption(String optname)
    {
        // If autoPlay is set, all options that start with "Auto" return true.
        if (optname.startsWith("Auto"))
        {
            String value = options.getProperty(Options.autoPlay);
            if (value != null && value.equals("true"))
            {
                return "true";
            }
        }

        String value = options.getProperty(optname);
        return value;
    }

    public void setOption(String name, boolean value)
    {
        options.setProperty(name, String.valueOf(value));

        // Side effects
        if (name.equals(Options.showStatusScreen))
        {
            updateStatusScreen();
        }
        else if (name.equals(Options.antialias))
        {
            Hex.setAntialias(value);
            repaintAllWindows();
        }
        else if (name.equals(Options.showDice))
        {
            setupDice(value);
        }
    }

    public void setStringOption(String optname, String value)
    {
        options.setProperty(optname, String.valueOf(value));
        // TODO Add some triggers so that if autoPlay or autoSplit is set
        // during this player's split phase, the appropriate action
        // is called.
    }


    /** Save player options to a file.  The current format is standard
     *  java.util.Properties keyword=value. */
    public void saveOptions()
    {
        final String optionsFile = Options.optionsPath + Options.optionsSep +
            playerName + Options.optionsExtension;
        try
        {
            FileOutputStream out = new FileOutputStream(optionsFile);
            options.store(out, Options.configVersion);
            out.close();
        }
        catch (IOException e)
        {
            Log.error("Couldn't write options to " + optionsFile);
        }
    }

    /** Load player options from a file. The current format is standard
     *  java.util.Properties keyword=value */
    public void loadOptions()
    {
        final String optionsFile = Options.optionsPath + Options.optionsSep +
            playerName + Options.optionsExtension;
        try
        {
            FileInputStream in = new FileInputStream(optionsFile);
            options.load(in);
        }
        catch (IOException e)
        {
            Log.error("Couldn't read player options from " + optionsFile);
            return;
        }
        syncCheckboxes();
    }

    public void clearAllOptions()
    {
        options.clear();
    }

    public void syncCheckboxes()
    {
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            boolean value = getOption(name);
            if (board != null)
            {
                board.twiddleOption(name, value);
            }
        }
    }


    public void updateStatusScreen()
    {
        if (getOption(Options.showStatusScreen))
        {
            if (statusScreen != null)
            {
                statusScreen.updateStatusScreen();
            }
            else
            {
                statusScreen = new StatusScreen(server.getGame()); // XXX
            }
        }
        else
        {
            board.twiddleOption(Options.showStatusScreen, false);
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
            this.statusScreen = null;
        }
    }


    public BattleDice getBattleDice()
    {
        return battleDice;
    }

    public void initBattleDice()
    {
        battleDice = new BattleDice(this);
    }

    public void disposeBattleDice()
    {
        if (battleDice != null)
        {
            battleDice.dispose();
            battleDice = null;
        }
    }



    public void dispose()
    {
        // TODO Call dispose() on every window
    }

    public void clearAllCarries()
    {
        map.unselectAllHexes();
    }


    public static void main(String [] args)
    {
        System.out.println("Not implemented yet");
    }
}
