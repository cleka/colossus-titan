package net.sf.colossus.client;


import java.util.*;
import java.net.*;
import javax.swing.*;
import java.io.*;
import java.awt.event.*;
import java.awt.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.server.Server;
import net.sf.colossus.server.Options;
import net.sf.colossus.server.Player;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.AI;
import net.sf.colossus.server.SimpleAI;
import net.sf.colossus.server.Constants;


/**
 *  Lives on the client side and handles all communication
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

    private MasterBoard board;
    private StatusScreen statusScreen;
    private CreatureCollectionView caretakerDisplay;
    private SummonAngel summonAngel;
    private MovementDie movementDie;
    private BattleMap map;
    private BattleDice battleDice;
    // XXX Will need more than one of each.
    private Negotiate negotiate;
    private ReplyToProposal replyToProposal;

    private java.util.List battleChits = new ArrayList();

    /** Stack of legion marker ids, to allow multiple levels of undo for
     *  splits, moves, and recruits. */
    private LinkedList undoStack = new LinkedList();

    // Information on the current moving legion.
    private String moverId;
    private int entrySide;
    private boolean teleport;

    /** The end of the list is on top in the z-order. */
    private java.util.List markers = new ArrayList();

    private java.util.List recruitChits = new ArrayList();

    // Per-client and per-player options. 
    private Properties options = new Properties();

    /** Player who owns this client. */
    private String playerName;

    /** Marker color of player who owns this client. */
    private String color;

    /** Last movement roll for any player. */
    private int movementRoll = -1;

    /** Sorted set of available legion markers for this player. */
    private TreeSet markersAvailable = new TreeSet();
    private String parentId;
    private int numSplitsThisTurn;

    /** Gradually moving AI to client side. */
    private AI ai = new SimpleAI();

    private Set possibleRecruitHexes;

    /** Map of markerId to hexLabel */
    private Map legionToHex = new HashMap();

    /** Map of creature name to Integer count.  As in Caretaker, if an entry
     *  is missing then we assume it is set to the maximum. */
    private Map creatureCounts = new HashMap();

    /** Map of markerId to Integer height */
    private Map legionToHeight = new HashMap();

    /** Map of markerId to List of known creature names in legion. */
    private Map legionContents = new HashMap();


    int turnNumber = -1;
    String activePlayerName = "none";
    int phase = -1;
    int battleTurnNumber = -1;
    String battleActivePlayerName = "none";
    int battlePhase = -1;

    /** If the game is over, then quitting does not require confirmation. */
    private boolean gameOver;

    /** The primary client controls some game options.  If all players
     *  are AIs, then the primary client gets a board and map. */
    private boolean primary;



    public Client(Server server, String playerName, boolean primary)
    {
        this.server = server;
        this.playerName = playerName;
        this.primary = primary;
    }


    boolean isPrimary()
    {
        return primary;
    }

    /** Take a mulligan. */
    void mulligan()
    {
        // TODO This should be enforced on the server side.
        undoAllMoves();

        // XXX These are redundant, but will be needed again when we 
        // move the undo.
        clearUndoStack();
        clearRecruitChits();

        server.mulligan(playerName);
    }


    /** Resolve engagement in land. */
    void engage(String land)
    {
        server.engage(land);
    }

    /** Legion concedes. */
    void concede()
    {
        server.tryToConcede(playerName);
    }

    /** Cease negotiations and fight a battle in land. */
    void fight(String land)
    {
        server.fight(land);
    }


    /** Legion summoner summons unit from legion donor. */
    void doSummon(String summoner, String donor, String unit)
    {
        server.doSummon(summoner, donor, unit);
        board.repaint();
        summonAngel = null;
        // TODO Make this consistent.
        board.highlightEngagements();

        repaintHexByMarkerId(summoner);
        repaintHexByMarkerId(donor);
    }


    private void repaintHexByMarkerId(String markerId)
    {
        if (board == null)
        {
            return;
        }
        String hexLabel = getHexForLegion(markerId);
        if (hexLabel != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            if (hex != null)
            {
                hex.repaint();
            }
        }
    }



    /** This player quits the whole game. The server needs to always honor
     *  this request, because if it doesn't players will just drop
     *  connections when they want to quit in a hurry. */
    void withdrawFromGame()
    {
        // XXX Right now the game breaks if a player quits outside his
        // own turn.  But we need to support this, or players will
        // just drop connections.
        server.withdrawFromGame(playerName);
    }


    private void repaintAllWindows()
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

    void rescaleAllWindows()
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
        if (caretakerDisplay != null)
        {
            caretakerDisplay.rescale();
        }
        repaintAllWindows();
    }

    public void tellMovementRoll(int roll)
    {
        movementRoll = roll;
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
        movementRoll = roll;
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

    MovementDie getMovementDie()
    {
        return movementDie;
    }

    // XXX All the public option methods need to be non-public.
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
        String value = options.getProperty(optname);
        return value;
    }

    public void setOption(String name, boolean value)
    {
        options.setProperty(name, String.valueOf(value));

        // Side effects
        if (name.equals(Options.antialias))
        {
            Hex.setAntialias(value);
            repaintAllWindows();
        }
        else if (name.equals(Options.useOverlay))
        {
            Hex.setOverlay(value);
            repaintAllWindows();
        }
        else if (name.equals(Options.logDebug))
        {
            Log.setShowDebug(value);
        }
        else if (name.equals(Options.showCaretaker))
        {
            updateCreatureCountDisplay();
        }
        else if (name.equals(Options.showLogWindow))
        {
            Log.setToWindow(value);
            if (value)
            {
                // Force log window to appear.
                Log.event("");
            }
            else
            {
                Log.disposeLogWindow();
            }
        }
    }

    void setStringOption(String optname, String value)
    {
        options.setProperty(optname, String.valueOf(value));
        // TODO Add some triggers so that if autoPlay or autoSplit is set
        // during this player's split phase, the appropriate action
        // is called.
    }

    void setIntOption(String optname, int value)
    {
        options.setProperty(optname, String.valueOf(value));
    }

    /** Return -1 if the option's value has not been set. */
    public int getIntOption(String optname)
    {
        String buf = options.getProperty(optname);
        int value = -1;
        try
        {
            value = Integer.parseInt(buf);
        }
        catch (Exception ex)
        {
            value = -1;
        }
        return value;
    }


    /** Save player options to a file.  The current format is standard
     *  java.util.Properties keyword=value. */
    void saveOptions()
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
    void loadOptions(String optionsFile)
    {
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
        setBooleanOptions();
    }


    /** Load player options from a file. The current format is standard
     *  java.util.Properties keyword=value */
    private void loadOptions()
    {
        final String optionsFile = Options.optionsPath + Options.optionsSep +
            playerName + Options.optionsExtension;
        loadOptions(optionsFile);
    }


    /** Ensure that Player menu checkboxes reflect the correct state. */
    private void syncCheckboxes()
    {
        if (board == null)
        {
            return;
        }
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            boolean value = getOption(name);
            board.twiddleOption(name, value);
        }
    }

    /** Trigger all option-setting side effects by setting all
     *  options to their just-loaded values. */
    private void setBooleanOptions()
    {
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            String value = options.getProperty(name);
            if (value.equals("true") || value.equals("false"))
            {
                setOption(name, Boolean.valueOf(value).booleanValue());
            }
        }
    }


    // TODO Update the status screen model regardless of whether the
    // dialog is visible.  Rename to something less GUI-centric.
    public void updateStatusScreen(String [] playerInfo)
    {
        if (getOption(Options.showStatusScreen))
        {
            if (statusScreen != null)
            {
                statusScreen.updateStatusScreen(playerInfo);
            }
            else
            {
                if (board != null)
                {
                    statusScreen = new StatusScreen(board.getFrame(), this,
                        playerInfo);
                }
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

        // Side effects
        setupPlayerLabel();
    }


    public void setColor(String color)
    {
        this.color = color;
    }


    public void updateCreatureCount(String creatureName, int count)
    {
        if (creatureName != null)
        {
            creatureCounts.put(creatureName, new Integer(count));
        }
        updateCreatureCountDisplay();
    }

    private void updateCreatureCountDisplay()
    {
        if (getOption(Options.showCaretaker))
        {
            if (caretakerDisplay == null)
            {
                if (board != null)
                {
                    caretakerDisplay = new CreatureCollectionView(
                        board.getFrame(), this);
                    caretakerDisplay.addWindowListener(new WindowAdapter()
                    {
                        public void windowClosing(WindowEvent e)
                        {
                            setOption(Options.showCaretaker, false);
                        }
                    });
                }
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


    private void disposeMasterBoard()
    {
        if (board != null)
        {
            board.dispose();
            board = null;
        }
    }

    void disposeStatusScreen()
    {
        if (statusScreen != null)
        {
            statusScreen.dispose();
            statusScreen = null;
        }
    }


    public void dispose()
    {
        cleanupBattle();
        disposeMovementDie();
        disposeStatusScreen();
        disposeMasterBoard();
    }


    /** Called from BattleMap to leave carry mode. */
    void leaveCarryMode()
    {
        server.leaveCarryMode();
    }


    void doneWithBattleMoves()
    {
        clearUndoStack();
        server.doneWithBattleMoves();
    }

    boolean anyOffboardCreatures()
    {
        return server.anyOffboardCreatures();
    }


    void doneWithStrikes()
    {
        server.doneWithStrikes(playerName);
    }

    private void makeForcedStrikes()
    {
        if (playerName.equals(getBattleActivePlayerName()) &&
            getOption(Options.autoForcedStrike))
        {
            server.makeForcedStrikes(playerName, getOption(
                Options.autoRangeSingle));
        }
    }


    java.util.List getMarkers()
    {
        return Collections.unmodifiableList(markers);
    }

    /** Get the first marker with this id. */
    Marker getMarker(String id)
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

    /** Add the marker to the end of the list.  If it's already
     *  in the list, remove the earlier entry. */
    void setMarker(String id, Marker marker)
    {
        markers.remove(marker);
        markers.add(marker);
    }

    /** Remove the first marker with this id from the list.
     *  Also remove any recruitChits for this marker's hex. */
    public void removeMarker(String id)
    {
        Iterator it = markers.iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            if (marker.getId().equals(id))
            {
                String hexLabel = getHexForLegion(id);
                it.remove();
                legionToHex.remove(id);
                if (board != null)
                {
                    board.alignLegions(hexLabel);
                }
                // XXX Not perfect, but since we don't track recruitChits
                // by legion this is as good as we can do for now.
                removeRecruitChit(hexLabel);
                return;
            }
        }
    }


    // TODO Extract legion info class.

    int getLegionHeight(String markerId)
    {
        Integer integer = (Integer)legionToHeight.get(markerId);
        if (integer != null)
        {
            return integer.intValue();
        }
        else
        {
            return 0;
        }
    }

    public void setLegionHeight(String markerId, int height)
    {
        legionToHeight.put(markerId, new Integer(height));
        Marker marker = getMarker(markerId);
        if (marker != null)
        {
            marker.repaint();
        }
    }

    void incLegionHeight(String markerId)
    {
        int height = getLegionHeight(markerId);
        height++;
        setLegionHeight(markerId, height);
    }

    void decLegionHeight(String markerId)
    {
        int height = getLegionHeight(markerId);
        height--;
        setLegionHeight(markerId, height);
    }

    /** Return an immutable copy of the legion's contents. */
    java.util.List getLegionContents(String markerId)
    {
        java.util.List contents = (java.util.List)legionContents.get(markerId);
        if (contents == null)
        {
            contents = new ArrayList();
        }
        return Collections.unmodifiableList(contents);
    }

    // TODO Colorized, powered titans.  See Critter.getImageName()
    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    java.util.List getLegionImageNames(String markerId)
    {
        java.util.List names = new ArrayList();
        names.addAll(getLegionContents(markerId));
        int numUnknowns = getLegionHeight(markerId) - names.size();
        for (int i = 0; i < numUnknowns; i++)
        {
            names.add("Unknown");
        }
        return names;
    }

    /** Replace the existing contents for this legion with these. */
    public void setLegionContents(String markerId, java.util.List names)
    {
        legionContents.put(markerId, names);
    }

    /** Remove all contents for this legion. */
    public void clearLegionContents(String markerId)
    {
        legionContents.remove(markerId);
    }

    /** Add a new creature to this legion. */
    public void addCreature(String markerId, String name)
    {
        incLegionHeight(markerId);
        java.util.List names = new ArrayList();
        names.addAll(getLegionContents(markerId));
        names.add(name);
        setLegionContents(markerId, names);
    }

    public void removeCreature(String markerId, String name)
    {
        decLegionHeight(markerId);
        java.util.List names = getLegionContents(markerId);
        if (names == null)
        {
            return;   // Nothing to remove
        }
        java.util.List newNames = new ArrayList();
        newNames.addAll(names);
        newNames.remove(name);
        setLegionContents(markerId, newNames);
    }

    /** Reveal creatures in this legion, some of which already may be known. */
    public void revealCreatures(String markerId, final java.util.List names)
    {
        java.util.List newNames = new ArrayList();
        java.util.List oldNames = getLegionContents(markerId);
        if (oldNames == null || oldNames.isEmpty())
        {
            newNames.addAll(names);
        }
        else
        {
            newNames.addAll(oldNames);

            java.util.List oldScratch = new ArrayList();  // Can't just clone
            oldScratch.addAll(oldNames);

            Iterator it = names.iterator();
            while (it.hasNext())
            {
                String name = (String)it.next();
                // If it's already there, don't add it, but remove it from
                // the list in case we have multiples of this creature.
                if (!oldScratch.remove(name))
                {
                    newNames.add(name);
                }
            }
        }
        setLegionContents(markerId, newNames);
    }





    java.util.List getBattleChits()
    {
        return battleChits;
    }

    /** Get the BattleChit with this tag. */
    BattleChit getBattleChit(int tag)
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
    void addBattleChit(String imageName, boolean inverted, int tag)
    {
        BattleChit chit = new BattleChit(4 * Scale.get(), imageName,
            map, inverted, tag);
        battleChits.add(chit);
    }


    public void removeDeadBattleChits()
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.isDead())
            {
                it.remove();
                // TODO Also remove it from other places.
            }
        }
        if (map != null)
        {
            map.repaint();
        }
    }

    // Rename
    public void placeNewChit(String imageName, boolean inverted, int tag, 
        String hexLabel)
    {
        if (map != null)
        {
            map.placeNewChit(imageName, inverted, tag, hexLabel);
        }
    }


    java.util.List getRecruitChits()
    {
        return Collections.unmodifiableList(recruitChits);
    }

    void addRecruitChit(String imageName, String hexLabel)
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

    void removeRecruitChit(String hexLabel)
    {
        Iterator it = recruitChits.iterator();
        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            if (hex != null && hex.contains(chit.getCenter()))
            {
                it.remove();
                return;
            }
        }
    }

    void clearRecruitChits()
    {
        recruitChits.clear();
        // TODO Only repaint needed hexes.
        if (board != null)
        {
            board.repaint();
        }
    }


    private void clearUndoStack()
    {
        undoStack.clear();
    }

    private Object popUndoStack()
    {
        Object ob = undoStack.removeFirst();
        return ob;
    }

    private void pushUndoStack(Object object)
    {
        undoStack.addFirst(object);
    }

    private boolean isUndoStackEmpty()
    {
        return undoStack.isEmpty();
    }


    void setMoverId(String moverId)
    {
        this.moverId = moverId;
    }


    MasterBoard getBoard()
    {
        return board;
    }

    // TODO Should take board data from variant file, or stream, as argument.
    public void initBoard()
    {
        // Do not show boards for AI players, except primary client.
        if (!getOption(Options.autoPlay) || primary)
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

        // Now that the board is ready we can load options and
        // sync the option checkboxes.
        loadOptions();
    }


    BattleMap getBattleMap()
    {
        return map;
    }

    void setBattleMap(BattleMap map)
    {
        this.map = map;
    }


    // TODO Make this non-public.  Have Server track client IDs itself.
    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }


    SummonAngel getSummonAngel()
    {
        return summonAngel;
    }

    // Only called for human players.
    public void createSummonAngel(String markerId, String longMarkerName)
    {
        if (board == null)
        {
            Log.error("Called createSummonAngel() with null board");
            return;
        }
        board.deiconify();
        board.getFrame().toFront();
        summonAngel = SummonAngel.summonAngel(this, markerId, longMarkerName);
    }

    String getDonorId()
    {
        return server.getDonorId(playerName);
    }

    boolean donorHas(String name)
    {
        return server.donorHas(playerName, name);
    }


    public void askAcquireAngel(String markerId, java.util.List recruits)
    {
Log.debug("called Client.askAcquireAngel()");
        if (getOption(Options.autoAcquireAngels))
        {
            acquireAngelCallback(markerId, ai.acquireAngel( markerId,
                recruits));
        }
        else
        {
            board.deiconify();
            new AcquireAngel(board.getFrame(), this, markerId, recruits);
        }
    }

    void acquireAngelCallback(String markerId, String angelType)
    {
Log.debug("called Client.acquireAngelCallback()");
        server.acquireAngel(markerId, angelType);
        if (board != null)
        {
            String hexLabel = getHexForLegion(markerId);
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            hex.repaint();
        }
    }


    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    private boolean chooseWhetherToTeleport()
    {
        String [] options = new String[2];
        options[0] = "Teleport";
        options[1] = "Move Normally";
        int answer = JOptionPane.showOptionDialog(board, "Teleport?",
            "Teleport?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

        return (answer == JOptionPane.YES_OPTION);
    }



    /** Allow the player to choose whether to take a penalty (fewer dice
     *  or higher strike number) in order to be allowed to carry.
     *  Used by human players only. */
    public void askChooseStrikePenalty(java.util.List choices)
    {
        if (choices == null || choices.isEmpty())
        {
            Log.error("Called Client.askChooseStrikePenalty with no prompts");
            return;
        }
        if (map == null)
        {
            Log.error("Called Client.askChooseStrikePenalty with null map");
            return;
        }
        new PickStrikePenalty(map.getFrame(), this, choices);
    }

    void assignStrikePenalty(String prompt)
    {
        server.assignStrikePenalty(playerName, prompt);
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


    public void tellGameOver(String message)
    {
        gameOver = true;
        showMessageDialog(message);
    }

    boolean isGameOver()
    {
        return gameOver;
    }



    void doFight(String hexLabel)
    {
        if (summonAngel != null)
        {
            server.setDonor(hexLabel);
            summonAngel.updateChits();
            summonAngel.repaint();
            getMarker(server.getDonorId(playerName)).repaint();
        }
        else
        {
            engage(hexLabel);
        }
    }


    public void askConcede(String longMarkerName, String hexDescription,
        String allyMarkerId, java.util.List allyImageNames, String
        enemyMarkerId, java.util.List enemyImageNames)
    {
        Concede.concede(this, board.getFrame(), longMarkerName,
            hexDescription, allyMarkerId, allyImageNames,
            enemyMarkerId, enemyImageNames);
    }

    public void askFlee(String longMarkerName, String hexDescription,
        String allyMarkerId, java.util.List allyImageNames, String
        enemyMarkerId, java.util.List enemyImageNames)
    {
        Concede.flee(this, board.getFrame(), longMarkerName,
            hexDescription, allyMarkerId, allyImageNames,
            enemyMarkerId, enemyImageNames);
    }

    void answerFlee(String markerId, boolean answer)
    {
        if (answer)
        {
            server.flee(markerId);
        }
        else
        {
            server.doNotFlee(markerId);
        }
    }

    void answerConcede(String markerId, boolean answer)
    {
        if (answer)
        {
            server.concede(markerId);
        }
        else
        {
            server.doNotConcede(markerId);
        }
    }


    public void askNegotiate(String attackerLongMarkerName, 
        String defenderLongMarkerName, String attackerId, String defenderId, 
        java.util.List attackerImageNames, java.util.List defenderImageNames, 
        String hexLabel)
    {
        Proposal proposal = null;
        if (getOption(Options.autoNegotiate))
        {
            // TODO AI players just fight for now.
            proposal = new Proposal(attackerId, defenderId, true, false, 
                null, null, hexLabel);
            makeProposal(proposal);
        }
        else
        {
        /* TODO Finish
            negotiate = new Negotiate(this, attackerLongMarkerName, 
                defenderLongMarkerName, attackerId, defenderId,
                attackerImageNames, defenderImageNames, hexLabel);
        */
        }
    }

    void negotiateCallback(Proposal proposal)
    {
        if (proposal == null)
        {
            // Just drop it.
        }
        else if (proposal.isFight())
        {
            fight(proposal.getHexLabel());
        }
        else
        {
            makeProposal(proposal);
        }
    }


    private void makeProposal(Proposal proposal)
    {
        // TODO Stringify the proposal.
        server.makeProposal(playerName, proposal);
    }


    /** Inform this player about the other player's proposal. */
    public void tellProposal(Proposal proposal)
    {
        new ReplyToProposal(this, proposal);
    }


    public void tellStrikeResults(String strikerDesc, int strikerTag,
        String targetDesc, int targetTag, int strikeNumber, int [] rolls, 
        int damage, boolean killed, boolean wasCarry, int carryDamageLeft,
        Set carryTargetDescriptions)
    {
        if (battleDice != null)
        {
            battleDice.setValues(strikerDesc, targetDesc, strikeNumber, 
                damage, rolls);
            battleDice.showRoll();
        }
        if (map != null)
        {
            map.unselectAllHexes();
        }

        if (carryDamageLeft >= 1 && !carryTargetDescriptions.isEmpty())
        {
            pickCarries(carryDamageLeft, carryTargetDescriptions);
        }
        else
        {
            // TODO Reduce round trips by doing this on the server?
            makeForcedStrikes();

            if (map != null)
            {
                map.highlightCrittersWithTargets();
            }
        }

        BattleChit targetChit = getBattleChit(targetTag);
        if (targetChit != null)
        {
            if (killed)
            {
                targetChit.setDead(true);
            }
            else
            {
                if (damage > 0)
                {
                    targetChit.setHits(targetChit.getHits() + damage);
                }
            }
        }
    }

    private void pickCarries(int carryDamage, Set carryTargetDescriptions)
    {
        if (!playerName.equals(getBattleActivePlayerName()))
        {
            return;
        }
        if (getOption(Options.autoStrike))
        {
            // AI carries are handled on server side.
            return;
        }

        if (carryDamage < 1 || carryTargetDescriptions.isEmpty())
        {
            leaveCarryMode();
        }
        else if (carryTargetDescriptions.size() == 1 &&
            getOption(Options.autoCarrySingle))
        {
            Iterator it = carryTargetDescriptions.iterator();
            String desc = (String)it.next();
            String targetHex = desc.substring(desc.length() - 2);
            applyCarries(targetHex);
        }
        else
        {
            new PickCarry(map.getFrame(), this, carryDamage, 
                carryTargetDescriptions);
        }
    }

    private void setBattleChitDead(int tag)
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

    private void setBattleChitHits(int tag, int hits)
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


    void cleanupNegotiationDialogs()
    {
        if (negotiate != null)
        {
            negotiate.dispose();
            negotiate = null;
        }
        if (replyToProposal != null)
        {
            replyToProposal.dispose();
            replyToProposal = null;
        }
    }


    public void initBattle(String masterHexLabel, int battleTurnNumber,
        String battleActivePlayerName, int battlePhase)
    {
        cleanupNegotiationDialogs();

        this.battleTurnNumber = battleTurnNumber;
        setBattleActivePlayerName(battleActivePlayerName);
        this.battlePhase = battlePhase;

        // Do not show map for AI players, except primary client.
        if (!getOption(Options.autoPlay) || primary)
        {
            map = new BattleMap(this, masterHexLabel);
            JFrame frame = map.getFrame();
            battleDice = new BattleDice();
            frame.getContentPane().add(battleDice, BorderLayout.SOUTH);
            frame.pack();
            frame.setVisible(true);
            map.requestFocus();
            map.getFrame().toFront();
        }
    }


    public void cleanupBattle()
    {
        if (map != null)
        {
            map.dispose();
            map = null;
        }
        battleChits.clear();
    }

    // TODO Change to inform and let client control highlighting.
    public void highlightEngagements(Set hexLabels)
    {
        if (board != null)
        {
            board.unselectAllHexes();
            board.selectHexesByLabels(hexLabels);
        }
    }

    // TODO This should be controlled by the client.
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

    /** Currently used for human players only. */
    void doRecruit(String markerId)
    {
        // TODO Cache this on client
        if (!server.canRecruit(markerId))
        {
            return;
        }
        String hexLabel = getHexForLegion(markerId);

        java.util.List recruits = server.findEligibleRecruits(markerId, 
            hexLabel);
        java.util.List imageNames = getLegionImageNames(markerId);
        String hexDescription =
            MasterBoard.getHexByLabel(hexLabel).getDescription();

        String recruitName = PickRecruit.pickRecruit(board.getFrame(), 
            recruits, imageNames, hexDescription, markerId, this);

        if (recruitName == null)
        {
            return;
        }

        String recruiterName = findRecruiterName(hexLabel, markerId,
            recruitName, imageNames, hexDescription);
        if (recruiterName == null)
        {
            return;
        }

        server.doRecruit(markerId, recruitName, recruiterName);
    }

    /** Currently used for human players only.  Always needs to call
     *  server.doRecruit(), even if no recruit is wanted, to get past
     *  the reinforcing phase. */
    public void doReinforce(String markerId)
    {
        String hexLabel = getHexForLegion(markerId);

        java.util.List recruits = server.findEligibleRecruits(markerId, 
            hexLabel);
        java.util.List imageNames = getLegionImageNames(markerId);
        String hexDescription =
            MasterBoard.getHexByLabel(hexLabel).getDescription();

        String recruitName = PickRecruit.pickRecruit(board.getFrame(), 
            recruits, imageNames, hexDescription, markerId, this);

        String recruiterName = null;
        if (recruitName != null)
        {
            recruiterName = findRecruiterName(hexLabel, markerId, recruitName,
                imageNames, hexDescription);
        }

        server.doRecruit(markerId, recruitName, recruiterName);
    }

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters)
    {
        String hexLabel = getHexForLegion(markerId);
        possibleRecruitHexes.remove(hexLabel);
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }

        if (numRecruiters >= 1 && recruiterName != null)
        {
            java.util.List recruiters = new ArrayList();
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiters.add(recruiterName);
            }
            revealCreatures(markerId, recruiters);
        }
        addCreature(markerId, recruitName);

        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            addRecruitChit(recruitName, hexLabel);
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    public void undidRecruit(String markerId, String recruitName)
    {
        String hexLabel = getHexForLegion(markerId);
        possibleRecruitHexes.add(hexLabel);
        removeCreature(markerId, recruitName);
        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            removeRecruitChit(hexLabel);
            hex.repaint();
            board.highlightPossibleRecruits();
        }
    }

    /** null means cancel.  "none" means no recruiter (tower creature). */
    private String findRecruiterName(String hexLabel, String markerId, String
        recruitName, java.util.List imageNames, String hexDescription)
    {
        String recruiterName = null;

        java.util.List recruiters = server.findEligibleRecruiters(
            markerId, recruitName);

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiterName = "none";
        }
        else if (getOption(Options.autoPickRecruiter) || 
            numEligibleRecruiters == 1)
        {
            // If there's only one possible recruiter, or if
            // the user has chosen the autoPickRecruiter option,
            // then just reveal the first possible recruiter.
            recruiterName = (String)recruiters.get(0);
        }
        else
        {
            recruiterName = PickRecruiter.pickRecruiter(board.getFrame(),
                recruiters, imageNames, hexDescription, markerId, this);
        }
        return recruiterName;
    }

    // TODO Update markersAvailable more often.
    public void setupSplit(Set markersAvailable, String activePlayerName,
        int turnNumber)
    {
        this.activePlayerName = activePlayerName;
        this.turnNumber = turnNumber;
        this.phase = Constants.SPLIT;

        this.markersAvailable.clear();
        this.markersAvailable.addAll(markersAvailable);

        numSplitsThisTurn = 0;

        if (board != null)
        {
            board.setupSplitMenu();
            if (playerName.equals(getActivePlayerName()))
            {
                board.getFrame().toFront();
            }
        }
    }

    public void setupMove()
    {
        this.phase = Constants.MOVE;
        clearUndoStack();
        if (board != null)
        {
            board.setupMoveMenu();
        }
    }

    public void setupFight()
    {
        this.phase = Constants.FIGHT;
        if (board != null)
        {
            board.setupFightMenu();
        }
    }

    public void setupMuster(Set possibleRecruitHexes)
    {
        this.phase = Constants.MUSTER;
        this.possibleRecruitHexes = new HashSet(possibleRecruitHexes);

        if (board != null)
        {
            board.setupMusterMenu();
        }
    }


    // TODO  Should be handled fully on client side.
    public void alignLegions(Set hexLabels)
    {
        if (board != null)
        {
            board.alignLegions(hexLabels);
        }
    }


    public void setupBattleSummon(String battleActivePlayerName,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.SUMMON;
        setBattleActivePlayerName(battleActivePlayerName);
        this.battleTurnNumber = battleTurnNumber;

        if (map != null)
        {
            if (playerName.equals(getBattleActivePlayerName()))
            {
                map.getFrame().toFront();
            }
            map.setupSummonMenu();
        }
    }

    public void setupBattleRecruit(String battleActivePlayerName,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.RECRUIT;
        setBattleActivePlayerName(battleActivePlayerName);
        this.battleTurnNumber = battleTurnNumber;

        if (map != null)
        {
            if (playerName.equals(getBattleActivePlayerName()))
            {
                map.getFrame().toFront();
            }
            map.setupRecruitMenu();
        }
    }

    public void setupBattleMove()
    {
        // Just in case the other player started the battle
        // really quickly.
        cleanupNegotiationDialogs();

        this.battlePhase = Constants.MOVE;

        if (map != null)
        {
            map.setupMoveMenu();
        }
    }

    /** Used for both strike and strikeback. */
    public void setupBattleFight(int battlePhase,  
        String battleActivePlayerName)
    {
        this.battlePhase = battlePhase;
        setBattleActivePlayerName(battleActivePlayerName);

        if (map != null)
        {
            if (playerName.equals(getBattleActivePlayerName()))
            {
                map.getFrame().toFront();
            }
            map.setupFightMenu();
        }
    }


    // TODO Remember hex for each marker.
    public void tellBattleMove(int tag, String startingHex, String currentHex,
        boolean undo)
    {
        if (map != null)
        {
            map.alignChits(startingHex);
            map.alignChits(currentHex);
        }
    }


    /** Create a new marker and add it to the end of the list. */
    public void addMarker(String markerId, String hexLabel)
    {
        if (board != null)
        {
            Marker marker = new Marker(3 * Scale.get(), markerId,
                board.getFrame(), this);
            setMarker(markerId, marker);

            legionToHex.put(markerId, hexLabel);
            board.alignLegions(hexLabel);
        }
    }

    /** Create new markers in response to a rescale. */
    void recreateMarkers()
    {
        ListIterator it = markers.listIterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            String markerId = marker.getId();
            String hexLabel = (String)legionToHex.get(markerId);
            marker = new Marker(3 * Scale.get(), markerId,
                                board.getFrame(), this);
            it.set(marker);
            legionToHex.put(markerId, hexLabel);
            board.alignLegions(hexLabel);
        }
    }

    private void setupPlayerLabel()
    {
        if (board != null)
        {
            board.setupPlayerLabel();
        }
    }

    String getColor()
    {
        return color;
    }

    String getShortColor()
    {
        return Player.getShortColor(getColor());
    }


    String getBattleActivePlayerName()
    {
        return battleActivePlayerName;
    }

    void setBattleActivePlayerName(String name)
    {
        battleActivePlayerName = name;
    }

    int getBattlePhase()
    {
        return battlePhase;
    }

    int getBattleTurnNumber()
    {
        return battleTurnNumber;
    }


    void doBattleMove(int tag, String hexLabel)
    {
        server.doBattleMove(tag, hexLabel);
    }

    public void didBattleMove(int tag, String startingHexLabel, 
        String endingHexLabel)
    {
        if (isMyCritter(tag))
        {
            pushUndoStack(endingHexLabel);
        }
    }


    /** Attempt to have critter tag strike the critter in hexLabel. */
    void strike(int tag, String hexLabel)
    {
        server.strike(tag, hexLabel);
    }

    /** Attempt to apply carries to the critter in hexLabel. */
    void applyCarries(String hexLabel)
    {
        server.applyCarries(hexLabel);
        if (map != null)
        {
            map.unselectHexByLabel(hexLabel);
            map.repaint();
        }
    }


    void undoLastBattleMove()
    {
        if (!isUndoStackEmpty())
        {
            String hexLabel = (String)popUndoStack();
            server.undoBattleMove(hexLabel);
        }
    }

    void undoAllBattleMoves()
    {
        while (!isUndoStackEmpty())
        {
            undoLastBattleMove();
        }
    }


    int [] getCritterTags(String hexLabel)
    {
        return server.getCritterTags(hexLabel);
    }

    /** Return a set of hexLabels. */
    Set findMobileCritters()
    {
        return server.findMobileCritters();
    }

    /** Return a set of hexLabels. */
    Set showBattleMoves(int tag)
    {
        return server.showBattleMoves(tag);
    }

    /** Return a set of hexLabels. */
    Set findStrikes(int tag)
    {
        return server.findStrikes(tag);
    }

    /** Return a set of hexLabels. */
    Set findCrittersWithTargets()
    {
        return server.findCrittersWithTargets();
    }

    // TODO Cache this
    String getPlayerNameByTag(int tag)
    {
        return server.getPlayerNameByTag(tag);
    }

    boolean isMyCritter(int tag)
    {
        return (playerName.equals(getPlayerNameByTag(tag)));
    }

    String getActivePlayerName()
    {
        return activePlayerName;
    }

    int getPhase()
    {
        return phase;
    }

    int getTurnNumber()
    {
        return turnNumber;
    }


    private String figureTeleportingLord(String hexLabel)
    {
        java.util.List lords = server.listTeleportingLords(moverId, hexLabel);
        switch (lords.size()) 
        {
            case 0:
                return null;
            case 1:
                return (String)lords.get(0);
            default:
                return PickLord.pickLord(board.getFrame(), lords);
        }
    }

    void doMove(String hexLabel)
    {
        if (moverId == null)
        {
            return;
        }

        boolean teleport = false;

        Set teleports = server.listTeleportMoves(moverId);
        Set normals = server.listNormalMoves(moverId);
        if (teleports.contains(hexLabel) && normals.contains(hexLabel))
        {
            teleport = chooseWhetherToTeleport();
        }
        else if (teleports.contains(hexLabel))
        {
            teleport = true;
        }
        else if (normals.contains(hexLabel))
        {
            teleport = false;
        }
        else
        {
            return;
        }

        Set entrySides = server.getPossibleEntrySides(moverId, hexLabel, 
            teleport);

        String entrySide = PickEntrySide.pickEntrySide(board.getFrame(),
            hexLabel, entrySides);

        if (!goodEntrySide(entrySide))
        {
            return;
        }

        String teleportingLord = null;
        if (teleport)
        {
            teleportingLord = figureTeleportingLord(hexLabel);
        }

        server.doMove(moverId, hexLabel, entrySide, teleport, teleportingLord);
    }

    private boolean goodEntrySide(String entrySide)
    {
        return (entrySide != null && (entrySide.equals(Constants.left) ||
            entrySide.equals(Constants.bottom) ||
            entrySide.equals(Constants.right)));
    }

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel)
    {
        removeRecruitChit(startingHexLabel);
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }
        legionToHex.put(markerId, currentHexLabel);
        if (board != null)
        {
            board.alignLegions(startingHexLabel);
            board.alignLegions(currentHexLabel);
        }
    }

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel)
    {
        removeRecruitChit(formerHexLabel);
        removeRecruitChit(currentHexLabel);
        legionToHex.put(markerId, currentHexLabel);
        if (board != null)
        {
            board.alignLegions(formerHexLabel);
            board.alignLegions(currentHexLabel);
        }
    }

    /** Return a list of Creatures. */
    java.util.List findEligibleRecruits(String markerId, String hexLabel)
    {
        return server.findEligibleRecruits(markerId, hexLabel);
    }

    /** Return a set of hexLabels. */
    Set getPossibleRecruitHexes()
    {
        return Collections.unmodifiableSet(possibleRecruitHexes);
    }

    /** Return a set of hexLabels. */
    Set findSummonableAngels(String markerId)
    {
        return server.findSummonableAngels(markerId);
    }

    /** Return a set of hexLabels. */
    Set listTeleportMoves(String markerId)
    {
        return server.listTeleportMoves(markerId);
    }

    /** Return a set of hexLabels. */
    Set listNormalMoves(String markerId)
    {
        return server.listNormalMoves(markerId);
    }


    int getCreatureCount(String creatureName)
    {
        Integer count = (Integer)creatureCounts.get(creatureName);
        if (count == null)
        {
            return Creature.getCreatureByName(creatureName).getMaxCount();
        }
        return count.intValue();
    }


    java.util.List getLegionsByHex(String hexLabel)
    {
        java.util.List markerIds = new ArrayList();
        Iterator it = legionToHex.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            if (hexLabel.equals((String)entry.getValue()))
            {
                markerIds.add((String)entry.getKey());
            }
        }
        return markerIds;
    }

    Set findAllUnmovedLegionHexes()
    {
        return server.findAllUnmovedLegionHexes();
    }

    // TODO Cache legion heights.
    Set findTallLegionHexes()
    {
        return server.findTallLegionHexes();
    }

    Set findEngagements()
    {
        return server.findEngagements();
    }

    void newGame()
    {
        clearUndoStack();
        server.newGame();
    }

    void loadGame(String filename)
    {
        clearUndoStack();
        server.loadGame(filename);
    }

    void saveGame()
    {
        server.saveGame();
    }

    void saveGame(String filename)
    {
        server.saveGame(filename);
    }


    void undoLastSplit()
    {
        if (!isUndoStackEmpty())
        {
            String splitoffId = (String)popUndoStack();
            server.undoSplit(playerName, splitoffId);
            markersAvailable.add(splitoffId);
            numSplitsThisTurn--;
        }
    }

    void undoLastMove()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            server.undoMove(playerName, markerId);
        }
    }

    public void undidSplit(String splitoffId)
    {
        String hexLabel = getHexForLegion(splitoffId);
        legionToHex.remove(splitoffId);
        if (board != null)
        {
            board.alignLegions(hexLabel);
        }
    }

    void undoLastRecruit()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            server.undoRecruit(playerName, markerId);
            String hexLabel = getHexForLegion(markerId);
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            hex.repaint();
            possibleRecruitHexes.add(hexLabel);
        }
    }

    void undoAllSplits()
    {
        while (!isUndoStackEmpty())
        {
            undoLastSplit();
        }
    }

    void undoAllMoves()
    {
        while (!isUndoStackEmpty())
        {
            undoLastMove();
        }
    }

    void undoAllRecruits()
    {
        while (!isUndoStackEmpty())
        {
            undoLastRecruit();
        }
    }


    void doneWithSplits()
    {
        if (!playerName.equals(getActivePlayerName()))
        {
            return;
        }
        server.doneWithSplits(playerName);
        clearUndoStack();
        clearRecruitChits();
    }

    void doneWithMoves()
    {
        if (!playerName.equals(getActivePlayerName()))
        {
            return;
        }
        clearRecruitChits();
        server.doneWithMoves(playerName);
        clearUndoStack();
    }

    void doneWithEngagements()
    {
        if (!playerName.equals(getActivePlayerName()))
        {
            return;
        }
        server.doneWithEngagements(playerName);
        clearUndoStack();
    }

    void doneWithRecruits()
    {
        if (!playerName.equals(getActivePlayerName()))
        {
            return;
        }
        clearUndoStack();
        server.doneWithRecruits(playerName);
    }

    void forceAdvancePhase()
    {
        server.forceAdvancePhase();
    }

    void forceAdvanceBattlePhase()
    {
        server.forceAdvanceBattlePhase();
    }


    // TODO cache this
    private String getPlayerNameByMarkerId(String markerId)
    {
        return (server.getPlayerNameByMarkerId(markerId));
    }

    boolean isMyLegion(String markerId)
    {
        return (playerName.equals(getPlayerNameByMarkerId(markerId)));
    }

    int getMovementRoll()
    {
        return movementRoll;
    }

    int getMulligansLeft()
    {
        return server.getMulligansLeft(playerName);
    }


    // XXX markersAvailable needs to be up to date before this is called.
    void doSplit(String parentId)
    {
        this.parentId = null;

        // Need a legion marker to split.
        if (markersAvailable.size() < 1)
        {
            showMessageDialog("No legion markers");
            return;
        }
        // Can't split other players' legions.
        if (!isMyLegion(parentId))
        {
            return;
        }
        // Legion must be tall enough to split.
        if (getLegionHeight(parentId) < 4)
        {
            showMessageDialog("Legion is too short to split");
            return;
        }
        // Enforce only one split on turn 1.
        if (getTurnNumber() == 1 && numSplitsThisTurn > 0)
        {
            showMessageDialog("Can only split once on the first turn");
            return;
        }

        this.parentId = parentId;

        if (getOption(Options.autoPickMarker))
        {
            String childId = ai.pickMarker(markersAvailable, getShortColor());
            pickMarkerCallback(childId);
        }
        else
        {
            new PickMarker(board.getFrame(), playerName, markersAvailable,
                this);
        }
    }

    /** Second part of doSplit, after the child marker is picked. */
    void pickMarkerCallback(String childId)
    {
        if (parentId == null || childId == null)
        {
            Log.warn("Called Client.pickMarkerCallback with null markerId");
            return;
        }

        String results = SplitLegion.splitLegion(this, parentId,
            childId, getLegionImageNames(parentId));

        if (results != null)
        {
            server.doSplit(parentId, childId, results);
        }
    }

    /** Callback from server after any successful split. */
    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight)
    {
        // If my legion, or allStacksVisible, separate calls will update
        // contents of both legions soon.

        if (isMyLegion(childId))
        {
            clearRecruitChits();
            pushUndoStack(childId);
            markersAvailable.remove(childId);
        }
        else
        {
            // TODO split prediction, saving somewhere in case split is undone
            clearLegionContents(parentId);
            clearLegionContents(childId);
        }

        numSplitsThisTurn++;

        legionToHex.put(childId, hexLabel);

        setLegionHeight(childId, childHeight);
        setLegionHeight(parentId, getLegionHeight(parentId) - childHeight);

        if (board != null)
        {
            board.alignLegions(hexLabel);
            board.highlightTallLegions();
        }
    }


    public void askPickColor(Set colorsLeft)
    {
        String color = null;
        if (getOption(Options.autoPickColor))
        {
            // Convert favorite colors from a comma-separated string to a list.
            String favorites = getStringOption(Options.favoriteColors);
            java.util.List favoriteColors = null;
            if (favorites != null)
            {
                favoriteColors = Split.split(',', favorites);
            }
            else
            {
                favoriteColors = new ArrayList();
            }
            color = ai.pickColor(colorsLeft, favoriteColors);
        }
        else do
        {
            color = PickColor.pickColor(board.getFrame(), playerName, 
                colorsLeft);
        }
        while (color == null);

        this.color = color;

        server.assignColor(playerName, color);
    }

    private String getHexForLegion(String markerId)
    {
        return (String)legionToHex.get(markerId);
    }
}

