import java.util.*;
import java.net.*;
import javax.swing.*;
import java.io.*;
import java.awt.event.*;
import java.awt.*;

/**
 *  Class Client lives on the client side and handles all communication
 *  with the server.  It talks to the client classes locally, and to
 *  Server via the network protocol.  There is one client per player.
 *  @version $Id$
 *  @author David Ripton
 */

import net.sf.colossus.*;

public final class Client
{
    /** This will eventually be a network interface rather than a
     *  direct reference.  So don't share this reference. */
    private Server server;

    private MasterBoard board;
    private StatusScreen statusScreen;
    private CreatureCollectionView caretakerDisplay;
    private SummonAngel summonAngel;
    private MovementDie movementDie;
    private BattleMap map;
    private BattleDice battleDice;

    private ArrayList battleChits = new ArrayList();

    // Moved here from Player.
    /** Stack of legion marker ids, to allow multiple levels of undo for
     *  splits, moves, and recruits. */
    private static LinkedList undoStack = new LinkedList();

    private String moverId;

    /** The end of the list is on top in the z-order. */
    private ArrayList markers = new ArrayList();

    private ArrayList recruitChits = new ArrayList();

    // Per-client and per-player options should be kept here instead
    // of in Game.  (For now we can move all options from Game/Player
    // to client.  The few server options can be moved back to Game or
    // Server later, and have their GUI manipulators removed or restricted.)
    private Properties options = new Properties();

    /** Player who owns this client. */
    String playerName;


    /** Temporary constructor. */
    public Client(Server server, String playerName)
    {
        this.server = server;
        this.playerName = playerName;
    }

    /** Null constructor for testing only. */
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
        java.util.List splitCreatureNames)
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
        clearRecruitChits();
        return server.mulligan(playerName);
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
    void engage(String land)
    {
        server.engage(land);
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
    boolean concede()
    {
        return server.tryToConcede(playerName);
    }

    /** Legion does not concede. */
    boolean dontConcede(String legion)
    {
        return false;
    }

    /** Offer to negotiate engagement in land.  If legion or unitsLeft
     *  is null or empty then propose a mutual. If this matches one
     *  of the opponent's proposals, then accept it. */
    boolean negotiate(String land, String legion, java.util.List unitsLeft)
    {
        return false;
    }

    // XXX temp
    void negotiate(NegotiationResults results)
    {
        server.negotiate(playerName, results);
    }

    /** Cease negotiations and fight a battle in land. */
    void fight(String land)
    {
        server.fight(land);
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
        // XXX Right now the game breaks if a player quits outside his
        // own turn.  But we need to support this, or players will
        // just drop connections.
        if (playerName != server.getActivePlayerName())
        {
            return false;
        }
        server.withdrawFromGame(playerName);
        return true;
    }


    // TODO Add requests for info.


    public void repaintAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.repaint();
        }
        if (caretakerDisplay != null)
        {
            caretakerDisplay.repaint();
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
        if (movementDie == null || roll != movementDie.getLastRoll())
        {
            initMovementDie(roll);
            if (board != null)
            {
                board.repaint();
            }
        }
    }


    private void initMovementDie(int roll)
    {
        if (board != null)
        {
            movementDie = new MovementDie(4 * Scale.get(), 
                MovementDie.getDieImageName(roll), board);
        }
    }

    private void disposeMovementDie()
    {
        movementDie = null;
    }

    public MovementDie getMovementDie()
    {
        return movementDie;
    }


    public boolean getOption(String name)
    {
        // If autoplay is set, then return true for all other auto* options.
        if (name.startsWith("Auto") && !name.equals(Options.autoPlay))
        {
            if (getOption(Options.autoPlay))
            {
                return true;
            }
        }
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
        else if (name.equals(Options.logDebug))
        {
            Log.setShowDebug(value);
        }
        else if (name.equals(Options.showCaretaker))
        {
            updateCaretakerDisplay();
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
            Log.debug("Couldn't read player options from " + optionsFile);
            return;
        }
        syncCheckboxes();
    }

    public void clearAllOptions()
    {
        options.clear();
    }

    /** Ensure that Player menu checkboxes reflect the correct state. */
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
                statusScreen = new StatusScreen(board.getFrame(), this, 
                    server.getGame());
            }
        }
        else
        {
            if (board != null)
            {
                board.twiddleOption(Options.showStatusScreen, false);
            }
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
            this.statusScreen = null;
        }
    }


    public void updateCaretakerDisplay()
    {
        if (getOption(Options.showCaretaker))
        {
            if (caretakerDisplay == null)
            {
                IImageUtility oImageUtility = new ChitImageUtility();
                ICreatureCollection oCaretakerCollection =
                    server.getGame().getCaretaker().getCollectionInterface();
                caretakerDisplay = new CreatureCollectionView(
                    board.getFrame(), oCaretakerCollection, oImageUtility);
                caretakerDisplay.addWindowListener(new WindowAdapter()
                {
                    public void windowClosing(WindowEvent e)
                    {
                        setOption(Options.showCaretaker, false);
                    }
                });
            }
            else
            {
                caretakerDisplay.update();
            }
        }
        else
        {
            if (board != null)
            {
                board.twiddleOption(Options.showCaretaker, false);
            }
            if (caretakerDisplay != null)
            {
                caretakerDisplay.dispose();
                caretakerDisplay = null;
            }
        }
    }


    public void initBattleDice()
    {
        battleDice = new BattleDice();
        map.getFrame().getContentPane().add(battleDice, BorderLayout.SOUTH);
    }

    public void disposeBattleDice()
    {
        if (map != null && battleDice != null)
        {
            map.getFrame().remove(battleDice);
        }
        battleDice = null;
    }

    public void disposeMasterBoard()
    {
        if (board != null)
        {
            board.dispose();
            board = null;
        }
    }

    public void disposeStatusScreen()
    {
        if (statusScreen != null)
        {
            statusScreen.dispose();
            statusScreen = null;
        }
    }


    public void dispose()
    {
        disposeBattleMap();
        disposeMovementDie();
        disposeStatusScreen();
        disposeMasterBoard();
    }


    /** Called from server to clear carry cursor on client. */
    public void clearCarries()
    {
        if (map != null)
        {
            map.clearCarries();
        }
    }


    /** Called from BattleMap to leave carry mode. */
    public void leaveCarryMode()
    {
        server.leaveCarryMode();
    }


    public void doneWithBattleMoves()
    {
        server.doneWithBattleMoves();
    }

    public boolean anyOffboardCreatures()
    {
        return server.anyOffboardCreatures();
    }

    /** Returns true if okay, or false if forced strikes remain. */
    public boolean doneWithStrikes()
    {
        return server.doneWithStrikes();
    }


    public void makeForcedStrikes(boolean rangestrike)
    {
        server.makeForcedStrikes(rangestrike);
    }


    public java.util.List getMarkers()
    {
        return markers;
    }

    /** Get the first marker with this id. */
    public Marker getMarker(String id)
    {
        Iterator it = markers.iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            if (marker.getId() == id)
            {
                return marker;
            }
        }
        return null;
    }

    /** Create a new marker and add it to the end of the list. */
    public void addMarker(String markerId)
    {
        if (board != null)
        {
            Marker marker = new Marker(3 * Scale.get(), markerId,
                board.getFrame(), this);
            setMarker(markerId, marker);
        }
    }


    // TODO Cache legion heights on client side?
    public int getLegionHeight(String markerId)
    {
        Legion legion = server.getGame().getLegionByMarkerId(markerId);
        if (legion != null)
        {
            return legion.getHeight();
        }
        return 0;
    }

    /** Add the marker to the end of the list.  If it's already
     *  in the list, remove the earlier entry. */
    public void setMarker(String id, Marker marker)
    {
        removeMarker(id);
        markers.add(marker);
    }

    /** Remove the first marker with this id from the list. Return
     *  the removed marker. */
    public Marker removeMarker(String id)
    {
        Iterator it = markers.iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            if (marker.getId() == id)
            {
                it.remove();
                return marker;
            }
        }
        return null;
    }


    public ArrayList getBattleChits()
    {
        return battleChits;
    }

    /** Get the BattleChit with this tag. */
    public BattleChit getBattleChit(int tag)
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.getTag() == tag)
            {
                return chit;
            }
        }
        return null;
    }

    /** Create a new BattleChit and add it to the end of the list. */
    public void addBattleChit(String imageName, int tag)
    {
        BattleChit chit = new BattleChit(4 * Scale.get(), imageName,
            map, tag);
        battleChits.add(chit);
    }


    /** Remove the first BattleChit with this tag from the list. */
    public void removeBattleChit(int tag)
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.getTag() == tag)
            {
                it.remove();
            }
        }
    }

    public void placeNewChit(String imageName, int tag, String hexLabel)
    {
        if (map != null)
        {
            map.placeNewChit(imageName, tag, hexLabel);
        }
    }

    public void setBattleChitDead(int tag)
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.getTag() == tag)
            {
                chit.setDead(true);
                return;
            }
        }
    }

    public void setBattleChitHits(int tag, int hits)
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.getTag() == tag)
            {
                chit.setHits(hits);
                return;
            }
        }
    }


    public ArrayList getRecruitChits()
    {
        return recruitChits;
    }

    public void addRecruitChit(String imageName, String hexLabel)
    {
        int scale = 2 * Scale.get();
        GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
        Chit chit = new Chit(scale, imageName, board);
        Point startingPoint = hex.getOffCenter();
        Point point = new Point(startingPoint);
        point.x -= scale / 2;
        point.y -= scale / 2;
        chit.setLocation(point);
        chit.setBorder(true);
        recruitChits.add(chit);
    }

    public void clearRecruitChits()
    {
        recruitChits.clear();
    }


    public static void clearUndoStack()
    {
        undoStack.clear();
    }

    public static Object topUndoStack()
    {
        return undoStack.getFirst();
    }

    public static Object popUndoStack()
    {
        Object ob = undoStack.removeFirst();
        return ob;
    }

    public static void pushUndoStack(Object object)
    {
        undoStack.addFirst(object);
    }

    public static boolean isUndoStackEmpty()
    {
        return undoStack.isEmpty();
    }


    public String getMoverId()
    {
        return moverId;
    }

    public void setMoverId(String moverId)
    {
        this.moverId = moverId;
    }


    public MasterBoard getBoard()
    {
        return board;
    }

    public void initBoard()
    {
        // Do not show boards for AI players.
        if (!getOption(Options.autoPlay))
        {
            disposeMasterBoard();

            String buf = getStringOption(Options.scale);
            if (buf != null)
            {
                int scale = Integer.parseInt(buf);
                if (scale > 0)
                {
                    Scale.set(scale);
                }
            }

            board = new MasterBoard(this);
            board.requestFocus();
        }
    }


    public BattleMap getBattleMap()
    {
        return map;
    }

    public void setBattleMap(BattleMap map)
    {
        this.map = map;
    }

    /** Don't use this. */
    public Game getGame()
    {
        return server.getGame();
    }


    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }


    public SummonAngel getSummonAngel()
    {
        return summonAngel;
    }

    public void createSummonAngel(Legion legion)
    {
        board.deiconify();
        summonAngel = SummonAngel.summonAngel(this, legion);
    }

    public void doSummon(Legion legion, Legion donor, Creature angel)
    {
        server.getGame().doSummon(legion, donor, angel);

        if (legion != null)
        {
            String hexLabel = legion.getCurrentHexLabel();
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            hex.repaint();
        }
        if (donor != null)
        {
            String hexLabel = donor.getCurrentHexLabel();
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            hex.repaint();
        }

        summonAngel = null;
        board.highlightEngagements();
    }


    /** Called from server. */
    public String pickRecruit(ArrayList recruits, java.util.List imageNames,
        String hexDescription, String markerId)
    {
        Creature recruit = PickRecruit.pickRecruit(board.getFrame(), recruits,
            imageNames, hexDescription, markerId, this);
        if (recruit != null)
        {
            return recruit.toString();
        }
        return null;
    }

    public String pickRecruiter(ArrayList recruiters, 
        java.util.List imageNames, String hexDescription, String markerId)
    {
        Creature recruiter = PickRecruiter.pickRecruiter(board.getFrame(),
            recruiters, imageNames, hexDescription, markerId, this);
        if (recruiter != null)
        {
            return recruiter.toString();
        }
        return null;
    }


    public String splitLegion(Legion legion, String selectedMarkerId)
    {
        return SplitLegion.splitLegion(this, legion, selectedMarkerId);
    }



    public String acquireAngel(ArrayList recruits)
    {
        board.deiconify();
        return AcquireAngel.acquireAngel(board.getFrame(), playerName,
            recruits);
    }


    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    public boolean chooseWhetherToTeleport()
    {
        String [] options = new String[2];
        options[0] = "Teleport";
        options[1] = "Move Normally";
        int answer = JOptionPane.showOptionDialog(board, "Teleport?",
            "Teleport?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

        // If Teleport, then leave teleported set.
        return (answer == JOptionPane.YES_OPTION);
    }


    /** Allow the player to choose whether to take a penalty (fewer dice
     *  or higher strike number) in order to be allowed to carry.  Return
     *  true if the penalty is taken. */
    public boolean chooseStrikePenalty(String prompt)
    {
        String [] options = new String[2];
        options[0] = "Take Penalty";
        options[1] = "Do Not Take Penalty";
        int answer = JOptionPane.showOptionDialog(map, prompt,
            "Take Strike Penalty?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return (answer == JOptionPane.YES_OPTION);
    }


    public void showMessageDialog(String message)
    {
        // Don't bother showing messages to AI players.  Perhaps we
        // should log them.
        if (getOption(Options.autoPlay))
        {
            return;
        }
        JFrame frame = null;
        if (map != null)
        {
            frame = map.getFrame();
        }
        else if (board != null)
        {
            frame = board.getFrame();
        }
        if (frame != null)
        {
            JOptionPane.showMessageDialog(frame, message);
        }
    }


    public int pickEntrySide(String hexLabel, Legion legion)
    {
        return PickEntrySide.pickEntrySide(board.getFrame(), hexLabel, legion);
    }


    /** Assumes this is not an AI client and board is not null. */
    public String pickLord(Legion legion)
    {
        Creature lord = PickLord.pickLord(board.getFrame(), legion);
        return lord.toString();
    }


    public void repaintMasterHex(String hexLabel)
    {
        if (board != null)
        {
            board.getGUIHexByLabel(hexLabel).repaint();
        }
    }


    public void repaintBattleHex(String hexLabel)
    {
        if (map != null)
        {
            map.getGUIHexByLabel(hexLabel).repaint();
        }
    }


    public void doFight(String hexLabel)
    {
        if (summonAngel != null)
        {
            Player player = server.getGame().getPlayer(playerName);
            Legion donor = server.getGame().getFirstFriendlyLegion(
                hexLabel, player);
            if (donor != null)
            {
                player.setDonor(donor);
                summonAngel.updateChits();
                summonAngel.repaint();
                getMarker(donor.getMarkerId()).repaint();
            }
        }
        else
        {
            engage(hexLabel);
        }
    }


    public boolean askFlee(Legion defender, Legion attacker)
    {
        return Concede.flee(this, board.getFrame(), defender, attacker);
    }

    public boolean askConcede(Legion ally, Legion enemy)
    {
        return Concede.concede(this, board.getFrame(), ally, enemy);
    }

    public void askNegotiate(Legion attacker, Legion defender)
    {
        NegotiationResults results = null;
        // AI players just fight for now anyway, and the AI reference is
        // on the server side, so make this static rather than jumping
        // through hoops.
        if (getOption(Options.autoNegotiate))
        {
            results = SimpleAI.negotiate();
        }
        else
        {
            results = Negotiate.negotiate(this, attacker, defender);
        }

        if (results.isFight())
        {
            fight(attacker.getCurrentHexLabel());
        }
        else
        {
            negotiate(results);
        }
    }

    public void setBattleDiceValues(String attackerName, String defenderName,
        String attackerHexId, String defenderHexId, char terrain,
        int strikeNumber, int damage, int carryDamage, int [] rolls)
    {
        if (battleDice != null)
        {
            battleDice.setValues(attackerName, defenderName, attackerHexId,
                defenderHexId, terrain, strikeNumber, damage, carryDamage,
                rolls);
            battleDice.showRoll();
        }
    }

    public void setBattleDiceCarries(int carries)
    {
        if (battleDice != null)
        {
            battleDice.setCarries(carries);
            battleDice.showRoll();
        }
    }


    public void initBattleMap(String masterHexLabel)
    {
        // Do not show map for AI players.
        if (!getOption(Options.autoPlay))
        {
            map = new BattleMap(this, masterHexLabel);
            showBattleMap();
            initBattleDice();
        }
    }

    public void showBattleMap()
    {
        if (map != null)
        {
            map.getFrame().toFront();
            map.requestFocus();
        }
    }

    public void disposeBattleMap()
    {
        if (map != null)
        {
            map.dispose();
            map = null;
        }
        // XXX Should these be in a separate method?
        battleChits.clear();
        disposeBattleDice();
    }


    public void highlightEngagements()
    {
        if (board != null)
        {
            board.highlightEngagements();
        }
    }


    public void setBattleWaitCursor()
    {
        if (map != null)
        {
            map.setWaitCursor();
        }
    }

    public void setBattleDefaultCursor()
    {
        if (map != null)
        {
            map.setDefaultCursor();
        }
    }

    /** Used for human players only, not the AI. */
    void doMuster(String markerId)
    {
        server.doMuster(markerId);
    }


    public void setupSplitMenu()
    {
        if (board != null)
        {
            board.setupSplitMenu();
        }
    }

    public void setupMoveMenu()
    {
        if (board != null)
        {
            board.setupMoveMenu();
        }
    }

    public void setupFightMenu()
    {
        if (board != null)
        {
            board.setupFightMenu();
        }
    }

    public void setupMusterMenu()
    {
        if (board != null)
        {
            board.setupMusterMenu();
        }
    }

    public void alignLegions(String hexLabel)
    {
        if (board != null)
        {
            board.alignLegions(hexLabel);
        }
    }

    public void alignLegions(Set hexLabels)
    {
        if (board != null)
        {
            board.alignLegions(hexLabels);
        }
    }

    public void deiconifyBoard()
    {
        if (board != null)
        {
            board.deiconify();
        }
    }

    public void unselectHexByLabel(String hexLabel)
    {
        if (board != null)
        {
            board.unselectHexByLabel(hexLabel);
        }
    }

    public void unselectAllHexes()
    {
        if (board != null)
        {
            board.unselectAllHexes();
        }
    }


    public void highlightCarries()
    {
        if (map != null)
        {
            map.highlightCarries();
        }
    }

    public void setupBattleSummonMenu()
    {
        if (map != null)
        {
            map.setupSummonMenu();
        }
    }

    public void setupBattleRecruitMenu()
    {
        if (map != null)
        {
            map.setupRecruitMenu();
        }
    }

    public void setupBattleMoveMenu()
    {
        if (map != null)
        {
            map.setupMoveMenu();
        }
    }

    public void setupBattleFightMenu()
    {
        if (map != null)
        {
            map.setupFightMenu();
        }
    }

    public void unselectBattleHexByLabel(String hexLabel)
    {
        if (map != null)
        {
            map.unselectHexByLabel(hexLabel);
        }
    }

    public void unselectAllBattleHexes()
    {
        if (map != null)
        {
            map.unselectAllHexes();
        }
    }

    public void alignBattleChits(String hexLabel)
    {
        if (map != null)
        {
            map.alignChits(hexLabel);
        }
    }

    public void alignBattleChits(Set hexLabels)
    {
        if (map != null)
        {
            map.alignChits(hexLabels);
        }
    }


    public void loadInitialMarkerImages()
    {
        if (board != null)
        {
            board.loadInitialMarkerImages();
        }
    }

    public void setupPlayerLabel()
    {
        if (board != null)
        {
            board.setupPlayerLabel();
        }
    }

    public String getColor()
    {
        Player player = server.getGame().getPlayer(playerName);
        return player.getColor();
    }


    public String getBattleActivePlayerName()
    {
        return server.getGame().getBattle().getActivePlayerName();
    }


    public int getBattlePhase()
    {
        return server.getGame().getBattle().getPhase();
    }


    public int getBattleTurnNumber()
    {
        return server.getGame().getBattle().getTurnNumber();
    }


    /** Returns true if the move was legal, or false if it
     *  was not allowed. */
    public boolean doBattleMove(int tag, String hexLabel)
    {
        return server.doBattleMove(tag, hexLabel);
    }


    /** Attempt to have critter tag strike the critter in hexLabel. */
    public void strike(int tag, String hexLabel)
    {
        server.strike(tag, hexLabel);
    }


    /** Attempt to apply carries to the critter in hexLabel. */
    public void applyCarries(String hexLabel)
    {
        server.applyCarries(hexLabel);
    }


    public int getCarryDamage()
    {
        return server.getCarryDamage();
    }

    public Set getCarryTargets()
    {
        return server.getCarryTargets();
    }


    public void undoLastBattleMove()
    {
        server.undoLastBattleMove();
    }

    public void undoAllBattleMoves()
    {
        server.undoAllBattleMoves();
    }


    public int [] getCritterTags(String hexLabel)
    {
        return server.getCritterTags(hexLabel);
    }

    /** Return a set of hexLabels. */
    public Set findMobileCritters()
    {
        return server.findMobileCritters();
    }

    /** Return a set of hexLabels. */
    public Set showBattleMoves(int tag)
    {
        return server.showBattleMoves(tag);
    }

    /** Return a set of hexLabels. */
    public Set findStrikes(int tag)
    {
        return server.findStrikes(tag);
    }

    /** Return a set of hexLabels. */
    public Set findCrittersWithTargets()
    {
        return server.findCrittersWithTargets();
    }


    public String getPlayerNameByTag(int tag)
    {
        return server.getPlayerNameByTag(tag);
    }



    public String getActivePlayerName()
    {
        return server.getActivePlayerName();
    }

    public int getPhase()
    {
        return server.getGame().getPhase();
    }

    public int getTurnNumber()
    {
        return server.getGame().getTurnNumber();
    }

    public void doSplit(String markerId)
    {
        server.getGame().doSplit(markerId);
    }

    public boolean doMove(String markerId, String hexLabel)
    {
        return server.getGame().doMove(markerId, hexLabel);
    }

    /** Return a list of Creatures. */
    public ArrayList findEligibleRecruits(String markerId, String hexLabel)
    {
        return server.getGame().findEligibleRecruits(markerId, hexLabel);
    }

    /** Return a set of hexLabels. */
    public Set findAllEligibleRecruitHexes()
    {
        return server.getGame().findAllEligibleRecruitHexes();
    }

    /** Return a set of hexLabels. */
    public Set findSummonableAngels(String markerId)
    {
        return server.getGame().findSummonableAngels(markerId);
    }

    /** Return a set of hexLabels. */
    public Set listMoves(String markerId)
    {
        return server.getGame().listMoves(markerId);
    }

    public java.util.List getAllLegionIds()
    {
        return server.getGame().getAllLegionIds();
    }

    /** XXX temp */
    public Legion getLegionByMarkerId(String markerId)
    {
        return server.getGame().getLegionByMarkerId(markerId);
    }

    /** XXX temp */
    public Player getActivePlayer()
    {
        return server.getGame().getActivePlayer();
    }

    /** XXX temp */
    public Caretaker getCaretaker()
    {
        return server.getGame().getCaretaker();
    }


    public ArrayList getLegionMarkerIds(String hexLabel)
    {
        return server.getGame().getLegionMarkerIds(hexLabel);
    }

    public Set findAllUnmovedLegionHexes()
    {
        return server.getGame().findAllUnmovedLegionHexes();
    }

    public Set findTallLegionHexes()
    {
        return server.getGame().findTallLegionHexes();
    }

    public Set findEngagements()
    {
        return server.getGame().findEngagements();
    }

    public void newGame()
    {
        server.getGame().newGame();
    }

    public void loadGame(String filename)
    {
        server.getGame().loadGame(filename);
    }

    public void saveGame()
    {
        server.getGame().saveGame();
    }

    public void saveGame(String filename)
    {
        server.getGame().saveGame(filename);
    }

    public String getLongMarkerName(String markerId)
    {
        return server.getLongMarkerName(markerId);
    }

    /** Return a list of Strings. */
    public java.util.List getLegionImageNames(String markerId)
    {
        return server.getLegionImageNames(markerId, playerName);
    }

    public void undoLastSplit()
    {
        server.undoLastSplit(playerName);
    }

    public void undoLastMove()
    {
        server.undoLastMove(playerName);
    }

    public void undoLastRecruit()
    {
        server.undoLastRecruit(playerName);
    }

    public void undoAllSplits()
    {
        server.undoAllSplits(playerName);
    }

    public void undoAllMoves()
    {
        server.undoAllMoves(playerName);
    }

    public void undoAllRecruits()
    {
        server.undoAllRecruits(playerName);
    }


    public void doneWithSplits()
    {
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        if (!server.doneWithSplits(playerName))
        {
            showMessageDialog("Must split");
        }
    }

    public void doneWithMoves()
    {
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        String error = server.doneWithMoves(playerName);
        if (!error.equals(""))
        {
            showMessageDialog(error);
            clearRecruitChits();
            board.highlightUnmovedLegions();
        }
    }

    public void doneWithEngagements()
    {
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        if (!server.doneWithEngagements(playerName))
        {
            showMessageDialog("Must resolve engagements");
        }
    }

    public void doneWithRecruits()
    {
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        server.doneWithRecruits(playerName);
    }


    public boolean isMyLegion(String markerId)
    {
        return (playerName.equals(server.getPlayerNameByMarkerId(markerId)));
    }


    public static void main(String [] args)
    {
        System.out.println("Not implemented yet");
    }
}
