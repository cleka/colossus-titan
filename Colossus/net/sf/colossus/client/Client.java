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
import net.sf.colossus.util.Options;
import net.sf.colossus.server.Player;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.AI;
import net.sf.colossus.server.SimpleAI;
import net.sf.colossus.server.Constants;
import net.sf.colossus.parser.TerrainRecruitLoader;


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

    /** hexLabel of MasterHex for current or last battle. */
    private String battleSite;
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

    /** The end of the list is on top in the z-order. */
    private java.util.List markers = new ArrayList();

    private java.util.List recruitChits = new ArrayList();

    // Per-client and per-player options.
    private Options options;
    private boolean optionChanged = false;

    /** Player who owns this client. */
    private String playerName;

    /** Starting marker color of player who owns this client. */
    private String color;

    /** Last movement roll for any player. */
    private int movementRoll = -1;

    /** Sorted set of available legion markers for this player. */
    private TreeSet markersAvailable = new TreeSet();
    private String parentId;
    private int numSplitsThisTurn;

    /** Gradually moving AI to client side. */
    private AI ai = new SimpleAI();

    /** Map of creature name to Integer count.  As in Caretaker, if an entry
     *  is missing then we assume it is set to the maximum. */
    private Map creatureCounts = new HashMap();


    private int turnNumber = -1;
    private String activePlayerName = "none";
    private int phase = -1;

    private int battleTurnNumber = -1;
    private String battleActivePlayerName = "none";
    private int battlePhase = -1;
    private String attackerMarkerId = "none";
    private String defenderMarkerId = "none";

    /** Summon angel donor legion, for this client's player only. */
    private String donorId;

    /** If the game is over, then quitting does not require confirmation. */
    private boolean gameOver;

    /** If all players are AIs, then the primary client gets a GUI. */
    private boolean primary;

    /** One per player. */
    private PlayerInfo [] playerInfo;

    /** One LegionInfo per legion, keyed by markerId.  Never null. */
    private SortedMap legionInfo = new TreeMap();

    private int numPlayers;

    private String currentLookAndFeel = null;

    private Movement movement = new Movement(this);
    private BattleMovement battleMovement = new BattleMovement(this);
    private Strike strike = new Strike(this);


    // XXX replace with socket
    public Client(Server server, String playerName, boolean primary)
    {
        this.server = server;
        this.playerName = playerName;
        this.primary = primary;
        options = new Options(playerName);
        // Need to load options early so they don't overwrite server options.
        loadOptions();
    }


    boolean isPrimary()
    {
        return primary;
    }


    /** Take a mulligan. */
    void mulligan()
    {
        undoAllMoves();   // XXX Maybe move entirely to server
        clearUndoStack();
        clearRecruitChits();

        server.mulligan(playerName);
    }


    /** Resolve engagement in land. */
    void engage(String land)
    {
        server.engage(land);
    }

    String getMyEngagedMarkerId()
    {
        String markerId = null;
        if (isMyLegion(attackerMarkerId))
        {
            markerId = attackerMarkerId;
        }
        else if (isMyLegion(defenderMarkerId))
        {
            markerId = defenderMarkerId;
        }
        return markerId;
    }

    /** Legion concedes. */
    void concede()
    {
        server.concede(getMyEngagedMarkerId());
    }

    void doNotConcede()
    {
        server.doNotConcede(getMyEngagedMarkerId());
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
        if (board != null)
        {
            board.repaint();
            summonAngel = null;

            board.highlightEngagements();
            board.repaint();
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

    boolean getOption(String optname)
    {
        // If autoplay is set, then return true for all other auto* options.
        if (optname.startsWith("Auto") && !optname.equals(Options.autoPlay))
        {
            if (options.getOption(Options.autoPlay))
            {
                return true;
            }
        }
        return options.getOption(optname);
    }

    String getStringOption(String optname)
    {
        return options.getStringOption(optname);
    }

    /** Return -1 if the option's value has not been set. */
    int getIntOption(String optname)
    {
        return options.getIntOption(optname);
    }

    /** public so that server can set autoPlay for AIs. */
    public void setOption(String optname, String value)
    {
        if (!value.equals(getStringOption(optname)))
        {
            optionChanged = true;
        }
        options.setOption(optname, value);
        optionTrigger(optname, value);
    }

    void setOption(String optname, boolean value)
    {
        setOption(optname, String.valueOf(value));
    }

    void setOption(String optname, int value)
    {
        setOption(optname, String.valueOf(value));
    }


    /** Fully sync the board state by running all option triggers. */
    private void runAllOptionTriggers()
    {
        Enumeration en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = (String)en.nextElement();
            String value = getStringOption(name);
            optionTrigger(name, value);
        }
    }

    /** Trigger side effects after changing an option value. */
    private void optionTrigger(String optname, String value)
    {
        boolean bval = Boolean.valueOf(value).booleanValue();

        if (optname.equals(Options.antialias))
        {
            Hex.setAntialias(bval);
            repaintAllWindows();
        }
        else if (optname.equals(Options.useOverlay))
        {
            Hex.setOverlay(bval);
            repaintAllWindows();
        }
        else if (optname.equals(Options.noBaseColor))
        {
            Creature.setNoBaseColor(bval);
            net.sf.colossus.util.ResourceLoader.purgeCache();
            repaintAllWindows();
        }
        else if (optname.equals(Options.logDebug))
        {
            Log.setShowDebug(bval);
        }
        else if (optname.equals(Options.showCaretaker))
        {
            updateCreatureCountDisplay();
        }
        else if (optname.equals(Options.showLogWindow))
        {
            Log.setToWindow(bval);
            if (bval)
            {
                // Force log window to appear.
                Log.event("");
            }
            else
            {
                Log.disposeLogWindow();
            }
        }
        else if (optname.equals(Options.showStatusScreen))
        {
            updateStatusScreen();
        }
        else if (optname.equals(Options.favoriteLookFeel))
        {
            setLookAndFeel(value);
        }
        syncOptions();
    }

    /** Save player options to a file.  The current format is standard
     *  java.util.Properties keyword=value. */
    void saveOptions()
    {
        options.saveOptions();
    }

    /** Load player options from a file. The current format is standard
     *  java.util.Properties keyword=value */
    private void loadOptions()
    {
        options.loadOptions();
        optionChanged = true;
        syncOptions();
    }

    /** Synchronize menu checkboxes, cfg file, and handle side effects
     *  after an option change. */
    private void syncOptions()
    {
        if (optionChanged)
        {
            syncCheckboxes();
            saveOptions();
            optionChanged = false;
            
            String lfName = getStringOption(Options.favoriteLookFeel);
            if ((lfName != null) && !lfName.equals(currentLookAndFeel))
            {
                setLookAndFeel(lfName);
            }

            int scale = getIntOption(Options.scale);
            if (scale > 0)
            {
                Scale.set(scale);
                rescaleAllWindows();
            }
        }
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


    int getNumPlayers()
    {
        return numPlayers;
    }


    public void updatePlayerInfo(String [] infoStrings)
    {
        numPlayers = infoStrings.length;

        if (playerInfo == null)
        {
            playerInfo = new PlayerInfo[numPlayers];
            for (int i = 0; i < numPlayers; i++)
            {
                playerInfo[i] = new PlayerInfo(this);
            }
        }

        for (int i = 0; i < numPlayers; i++)
        {
            playerInfo[i].update(infoStrings[i]);
        }

        updateStatusScreen();
    }

    private void updateStatusScreen()
    {
        if (getNumPlayers() < 1)
        {
            // Called too early.
            return;
        }
        if (getOption(Options.showStatusScreen))
        {
            if (statusScreen != null)
            {
                statusScreen.updateStatusScreen();
            }
            else
            {
                if (board != null)
                {
                    statusScreen = new StatusScreen(board.getFrame(), this);
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

    PlayerInfo getPlayerInfo(int playerNum)
    {
        return playerInfo[playerNum];
    }

    PlayerInfo getPlayerInfo(String name)
    {
        for (int i = 0; i < playerInfo.length; i++)
        {
            if (name.equals(playerInfo[i].getName()))
            {
                return playerInfo[i];
            }
        }
        return null;
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
        Iterator it = getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.getHexLabel().startsWith("X") &&
                playerName.equals(getPlayerNameByTag(chit.getTag())))
            {
                return true;
            }
        }
        return false;
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


    /** Get this legion's info.  Create it first if necessary.
     *  public for client-side AI -- do not call from server side. */
    public LegionInfo getLegionInfo(String markerId)
    {
        LegionInfo info = (LegionInfo)legionInfo.get(markerId);
        if (info == null)
        {
            info = new LegionInfo(markerId, this);
            legionInfo.put(markerId, info);
        }
        return info;
    }

    /** Get the marker with this id. */
    Marker getMarker(String id)
    {
        return getLegionInfo(id).getMarker();
    }

    /** Add the marker to the end of the list and to the LegionInfo.  
        If it's already in the list, remove the earlier entry. */
    void setMarker(String id, Marker marker)
    {
        markers.remove(marker);
        markers.add(marker);
        getLegionInfo(id).setMarker(marker);
    }


    /** Remove this eliminated legion, and clean up related stuff. */
    public void removeLegion(String id)
    {
        Marker marker = getMarker(id);
        markers.remove(marker);

        if (isMyLegion(id))
        {
            markersAvailable.add(id);
        }

        LegionInfo info = getLegionInfo(id);
        String hexLabel = info.getHexLabel();

        // XXX Not perfect -- Need to track recruitChits by legion.
        removeRecruitChit(hexLabel);

        legionInfo.remove(id);

        if (board != null)
        {
            board.alignLegions(hexLabel);
        }
    }


    int getLegionHeight(String markerId)
    {
        return getLegionInfo(markerId).getHeight();
    }

    public void setLegionHeight(String markerId, int height)
    {
        getLegionInfo(markerId).setHeight(height);
    }


    /** Return the full basename for a titan in legion markerId,
     *  first finding that legion's player, player color, and titan size.
     *  Default to "Titan" if the info is not there. */
    String getTitanBasename(String markerId)
    {
        return getLegionInfo(markerId).getTitanBasename();
    }

    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    java.util.List getLegionImageNames(String markerId)
    {
        return getLegionInfo(markerId).getImageNames();
    }

    /** Replace the existing contents for this legion with these. */
    public void setLegionContents(String markerId, java.util.List names)
    {
        getLegionInfo(markerId).setContents(names);
    }

    /** Remove all contents for this legion. */
    private void clearLegionContents(String markerId)
    {
        getLegionInfo(markerId).clearContents();
    }

    /** Add a new creature to this legion. */
    public void addCreature(String markerId, String name)
    {
        getLegionInfo(markerId).addCreature(name);
    }

    public void removeCreature(String markerId, String name)
    {
        getLegionInfo(markerId).removeCreature(name);
    }

    /** Reveal creatures in this legion, some of which already may be known. */
    public void revealCreatures(String markerId, final java.util.List names)
    {
        getLegionInfo(markerId).revealCreatures(names);
    }


    java.util.List getBattleChits()
    {
        return battleChits;
    }

    java.util.List getBattleChits(String hexLabel)
    {
        java.util.List chits = new ArrayList();

        Iterator it = getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (hexLabel.equals(chit.getHexLabel()))
            {
                chits.add(chit);
            }
        }
        return chits;
    }

    BattleChit getBattleChit(String hexLabel)
    {
        java.util.List chits = getBattleChits(hexLabel);
        if (chits.isEmpty())
        {
            return null;
        }
        return (BattleChit)chits.get(0);
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


    public void removeDeadBattleChits()
    {
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.isDead())
            {
                it.remove();

                // Also remove it from LegionInfo.
                String name = chit.getId();
                if (chit.isInverted())
                {
                    getLegionInfo(defenderMarkerId).removeCreature(name);
                }
                else
                {
                    getLegionInfo(attackerMarkerId).removeCreature(name);
                }
            }
        }
        if (map != null)
        {
            map.repaint();
        }
    }

    // XXX A bit too much direct server GUI control.
    public void placeNewChit(String imageName, boolean inverted, int tag, 
        String hexLabel)
    {
        if (map != null)
        {
            addBattleChit(imageName, inverted, tag, hexLabel);
            map.alignChits(hexLabel);
            // Make sure map is visible after summon or muster.
            map.toFront();
        }
    }

    /** Create a new BattleChit and add it to the end of the list. */
    private void addBattleChit(final String bareImageName, boolean inverted, 
        int tag, String hexLabel)
    {
        String imageName = bareImageName;
        if (imageName.equals("Titan"))
        {
            if (inverted)
            {
                imageName = getTitanBasename(defenderMarkerId);
            }
            else
            {
                imageName = getTitanBasename(attackerMarkerId);
            }
        }
        BattleChit chit = new BattleChit(4 * Scale.get(), imageName,
            map, inverted, tag, hexLabel);
        battleChits.add(chit);
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
    // XXX Too much direct client GUI control.
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
        runAllOptionTriggers();
    }


    BattleMap getBattleMap()
    {
        return map;
    }

    void setBattleMap(BattleMap map)
    {
        this.map = map;
    }


    String getPlayerName()
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

    // XXX Rename
    public void createSummonAngel(String markerId, String longMarkerName)
    {
        if (getOption(Options.autoSummonAngels))
        {
            String typeColonDonor = ai.summonAngel(markerId, this);
            java.util.List parts = Split.split(':', typeColonDonor);
            String unit = (String)parts.get(0);
            String donor = (String)parts.get(1);
            doSummon(markerId, donor, unit);
        }
        else
        {
            board.deiconify();
            board.getFrame().toFront();
            summonAngel = SummonAngel.summonAngel(this, markerId, 
                longMarkerName);
        }
    }

    String getDonorId()
    {
        return donorId;
    }

    boolean donorHas(String name)
    {
        if (donorId == null)
        {
            return false;
        }
        LegionInfo info = getLegionInfo(donorId);
        return info.getContents().contains(name);
    }


    public void askAcquireAngel(String markerId, java.util.List recruits)
    {
        if (getOption(Options.autoAcquireAngels))
        {
            acquireAngelCallback(markerId, ai.acquireAngel(markerId,
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


    // XXX rename
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
            java.util.List legions = getLegionsByHex(hexLabel);
            if (legions.size() != 1)
            {
                Log.error("Not exactly one legion in donor hex");
                return;
            }
            String markerId = (String)legions.get(0);
            donorId = markerId;
            server.setDonor(markerId);
            summonAngel.updateChits();
            summonAngel.repaint();
            getLegionInfo(markerId).getMarker().repaint();
        }
        else
        {
            engage(hexLabel);
        }
    }

    // XXX too many arguments
    public void askConcede(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId)
    {
        Concede.concede(this, board.getFrame(), longMarkerName,
            hexDescription, allyMarkerId, enemyMarkerId);
    }

    // XXX too many arguments
    public void askFlee(String longMarkerName, String hexDescription,
        String allyMarkerId, String enemyMarkerId)
    {
        Concede.flee(this, board.getFrame(), longMarkerName,
            hexDescription, allyMarkerId, enemyMarkerId);
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
            concede();
        }
        else
        {
            doNotConcede();
        }
    }


    // XXX too many arguments
    public void askNegotiate(String attackerLongMarkerName, 
        String defenderLongMarkerName, String attackerId, String defenderId, 
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
                hexLabel);
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
        // XXX Stringify the proposal.
        server.makeProposal(playerName, proposal);
    }


    /** Inform this player about the other player's proposal. */
    public void tellProposal(Proposal proposal)
    {
        new ReplyToProposal(this, proposal);
    }

    // XXX too many arguments
    public void tellStrikeResults(String strikerDesc, int strikerTag,
        String targetDesc, int targetTag, int strikeNumber, int [] rolls, 
        int damage, boolean killed, boolean wasCarry, int carryDamageLeft,
        Set carryTargetDescriptions)
    {
        BattleChit chit = getBattleChit(strikerTag);
        if (chit != null)
        {
            chit.setStruck(true);
        }

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
        if (getOption(Options.autoPlay))
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
        String battleActivePlayerName, int battlePhase,
        String attackerMarkerId, String defenderMarkerId)
    {
        cleanupNegotiationDialogs();

        this.battleTurnNumber = battleTurnNumber;
        setBattleActivePlayerName(battleActivePlayerName);
        this.battlePhase = battlePhase;
        this.attackerMarkerId = attackerMarkerId;
        this.defenderMarkerId = defenderMarkerId;
        this.battleSite = masterHexLabel;

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


    // XXX Too much direct GUI control
    public void cleanupBattle()
    {
        if (map != null)
        {
            map.dispose();
            map = null;
        }
        battleChits.clear();
    }


    // XXX Too much direct GUI control
    public void highlightEngagements()
    {
        if (board != null)
        {
            if (playerName.equals(getActivePlayerName()))
            {
                board.getFrame().toFront();
            }
            board.highlightEngagements();
        }
    }


    /** Currently used for human players only. */
    void doRecruit(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        if (info == null || !info.canRecruit())
        {
            return;
        }

        String hexLabel = getHexForLegion(markerId);
        java.util.List recruits = findEligibleRecruits(markerId, hexLabel);
        String hexDescription =
            MasterBoard.getHexByLabel(hexLabel).getDescription();

        String recruitName = PickRecruit.pickRecruit(board.getFrame(), 
            recruits, hexDescription, markerId, this);

        if (recruitName == null)
        {
            return;
        }

        String recruiterName = findRecruiterName(hexLabel, markerId,
            recruitName, hexDescription);
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

        java.util.List recruits = findEligibleRecruits(markerId, hexLabel);
        String hexDescription =
            MasterBoard.getHexByLabel(hexLabel).getDescription();

        String recruitName = PickRecruit.pickRecruit(board.getFrame(), 
            recruits, hexDescription, markerId, this);

        String recruiterName = null;
        if (recruitName != null)
        {
            recruiterName = findRecruiterName(hexLabel, markerId, recruitName,
                hexDescription);
        }

        server.doRecruit(markerId, recruitName, recruiterName);
    }

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters)
    {
        String hexLabel = getHexForLegion(markerId);
        if (hexLabel == null)
        {
            Log.error("Client.didRecruit() null hexLabel for " + markerId);
        }
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
        getLegionInfo(markerId).setRecruited(true);
        getLegionInfo(markerId).setLastRecruit(recruitName);

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
        removeCreature(markerId, recruitName);
        if (board != null)
        {
            GUIMasterHex hex = board.getGUIHexByLabel(hexLabel);
            removeRecruitChit(hexLabel);
            hex.repaint();
            board.highlightPossibleRecruits();
        }
        getLegionInfo(markerId).setRecruited(false);
    }

    /** null means cancel.  "none" means no recruiter (tower creature). */
    private String findRecruiterName(String hexLabel, String markerId, String
        recruitName, String hexDescription)
    {
        String recruiterName = null;

        java.util.List recruiters = findEligibleRecruiters(markerId, 
            recruitName);

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
                recruiters, hexDescription, markerId, this);
        }
        return recruiterName;
    }

    /** Needed if we load a game outside the split phase, where 
     *  active player and turn are usually set. */
    public void setupTurnState(String activePlayerName, int turnNumber)
    {
        this.activePlayerName = activePlayerName;
        this.turnNumber = turnNumber;
    }

    private void resetAllMoves()
    {
        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            info.setMoved(false);
            info.setTeleported(false);
            info.setRecruited(false);
        }
    }

    // XXX Update markersAvailable more often.
    public void setupSplit(Set markersAvailable, String activePlayerName,
        int turnNumber)
    {
        this.activePlayerName = activePlayerName;
        this.turnNumber = turnNumber;
        this.phase = Constants.SPLIT;

        this.markersAvailable.clear();
        this.markersAvailable.addAll(markersAvailable);

        numSplitsThisTurn = 0;

        resetAllMoves();

        if (board != null)
        {
            disposeMovementDie();
            board.setupSplitMenu();
            if (playerName.equals(getActivePlayerName()))
            {
                board.getFrame().toFront();
                board.fullRepaint();
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

    public void setupMuster()
    {
        this.phase = Constants.MUSTER;

        if (board != null)
        {
            board.setupMusterMenu();
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
                map.setupSummonMenu();
            }
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
                map.setupRecruitMenu();
            }
        }
    }

    private void resetAllBattleMoves()
    {
        Iterator it = getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            chit.setMoved(false);
            chit.setStruck(false);
        }
    }

    public void setupBattleMove()
    {
        // Just in case the other player started the battle
        // really quickly.
        cleanupNegotiationDialogs();

        resetAllBattleMoves();

        this.battlePhase = Constants.MOVE;

        if (map != null && playerName.equals(getBattleActivePlayerName()))
        {
            map.getFrame().toFront();
            map.setupMoveMenu();
        }
    }

    /** Used for both strike and strikeback. */
    public void setupBattleFight(int battlePhase, 
        String battleActivePlayerName)
    {
        this.battlePhase = battlePhase;
        setBattleActivePlayerName(battleActivePlayerName);

        makeForcedStrikes();

        if (map != null)
        {
            if (playerName.equals(getBattleActivePlayerName()))
            {
                map.getFrame().toFront();
                map.setupFightMenu();
            }
        }
    }


    // XXX Excessive GUI control
    /** Create a new marker and add it to the end of the list. */
    public void addMarker(String markerId, String hexLabel)
    {
        LegionInfo info = getLegionInfo(markerId);
        info.setHexLabel(hexLabel);

        if (board != null)
        {
            Marker marker = new Marker(3 * Scale.get(), markerId,
                board.getFrame(), this);
            setMarker(markerId, marker);
            info.setMarker(marker);
            board.alignLegions(hexLabel);
        }
    }

    /** Create new markers in response to a rescale. */
    void recreateMarkers()
    {
        markers.clear();

        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
            String markerId = info.getMarkerId();
            String hexLabel = info.getHexLabel();
            Marker marker = new Marker(3 * Scale.get(), markerId, 
                board.getFrame(), this);
            info.setMarker(marker);
            markers.add(marker);
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

    String getBattleActiveMarkerId()
    {
        LegionInfo info = getLegionInfo(defenderMarkerId);
        if (battleActivePlayerName.equals(info.getPlayerName()))
        {
            return defenderMarkerId;
        }
        else
        {
            return attackerMarkerId;
        }
    }

    String getDefenderMarkerId()
    {
        return defenderMarkerId;
    }

    String getAttackerMarkerId()
    {
        return defenderMarkerId;
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

    public void tellBattleMove(int tag, String startingHexLabel, 
        String endingHexLabel, boolean undo)
    {
        if (isMyCritter(tag) && !undo)
        {
            pushUndoStack(endingHexLabel);
        }
        BattleChit chit = getBattleChit(tag);
        if (chit != null)
        {
            chit.setHexLabel(endingHexLabel);
            chit.setMoved(!undo);
        }
        if (map != null)
        {
            map.alignChits(startingHexLabel);
            map.alignChits(endingHexLabel);
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

    char getBattleTerrain()
    {
        MasterHex mHex = MasterBoard.getHexByLabel(battleSite);
        return mHex.getTerrain();
    }

    /** Return true if there are any enemies adjacent to this chit.
     *  Dead critters count as being in contact only if countDead is true. */
    boolean isInContact(BattleChit chit, boolean countDead)
    {
        BattleHex hex = HexMap.getHexByLabel(getBattleTerrain(),
            chit.getHexLabel());

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return false;
        }

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    BattleChit other = getBattleChit(neighbor.getLabel());
                    if (other != null && 
                        (other.isInverted() != chit.isInverted()) &&
                        (countDead || !other.isDead()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    boolean isActive(BattleChit chit)
    {
        return battleActivePlayerName.equals(getPlayerNameByTag(
            chit.getTag()));
    }


    /** Return a set of hexLabels. */
    Set findMobileCritters()
    {
        Set set = new HashSet();
        Iterator it = getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (isActive(chit) && !chit.hasMoved() &&
                !isInContact(chit, false))
            {
                set.add(chit.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels. */
    Set showBattleMoves(int tag)
    {
        return battleMovement.showMoves(tag);
    }

    /** Return a set of hexLabels. */
    Set findCrittersWithTargets()
    {
        return strike.findCrittersWithTargets();
    }

    /** Return a set of hexLabels. */
    Set findStrikes(int tag)
    {
        return strike.findStrikes(tag);
    }


    String getPlayerNameByTag(int tag)
    {
        BattleChit chit = getBattleChit(tag);
        if (chit == null)
        {
            return "???";
        }
        if (chit.isInverted())
        {
            return getPlayerNameByMarkerId(defenderMarkerId);
        }
        else
        {
            return getPlayerNameByMarkerId(attackerMarkerId);
        }
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
        java.util.List lords = listTeleportingLords(moverId, hexLabel);
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

    /** List the lords eligible to teleport this legion to hexLabel,
     *  as strings. */
    private java.util.List listTeleportingLords(String moverId, 
        String hexLabel)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        java.util.List lords = new ArrayList();

        LegionInfo info = getLegionInfo(moverId);

        // Titan teleport
        java.util.List legions = getLegionsByHex(hexLabel);
        if (!legions.isEmpty())
        {
            LegionInfo other = getLegionInfo((String)legions.get(0));
            if (other != null && !playerName.equals(other.getPlayerName()) &&
                info.hasTitan())
            {
                lords.add("Titan");
            }
        }

        // Tower teleport
        else
        {
            Iterator it = info.getContents().iterator();
            while (it.hasNext())
            {
                String name = (String)it.next();
                Creature creature = Creature.getCreatureByName(name);
                if (creature != null && creature.isLord())
                {
                    if (!lords.contains(name))
                    {
                        lords.add(name);
                    }
                }
            }
        }

        return lords;
    }


    void doMove(String hexLabel)
    {
        if (moverId == null)
        {
            return;
        }

        boolean teleport = false;

        Set teleports = listTeleportMoves(moverId);
        Set normals = listNormalMoves(moverId);
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

        Set entrySides = listPossibleEntrySides(moverId, hexLabel, teleport);

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
        String currentHexLabel, boolean teleport)
    {
        removeRecruitChit(startingHexLabel);
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }
        getLegionInfo(markerId).setHexLabel(currentHexLabel);
        getLegionInfo(markerId).setMoved(true);
        if (teleport)
        {
            getLegionInfo(markerId).setTeleported(true);
        }
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
        getLegionInfo(markerId).setHexLabel(currentHexLabel);
        getLegionInfo(markerId).setMoved(false);
        getLegionInfo(markerId).setTeleported(false);
        if (board != null)
        {
            board.alignLegions(formerHexLabel);
            board.alignLegions(currentHexLabel);
        }
    }

    /** Return a list of Creatures. */
    java.util.List findEligibleRecruits(String markerId, String hexLabel)
    {
        java.util.List recruits = new ArrayList();

        LegionInfo info = getLegionInfo(markerId);
        if (info == null)
        {
            return recruits;
        }

        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        char terrain = hex.getTerrain();

        java.util.List tempRecruits = 
            TerrainRecruitLoader.getPossibleRecruits(terrain);
        java.util.List recruiters = 
            TerrainRecruitLoader.getPossibleRecruiters(terrain);

        Iterator lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            Creature creature = (Creature)lit.next();
            Iterator liter = recruiters.iterator();
            while (liter.hasNext())
            {
                Creature lesser = (Creature)liter.next();
                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser, 
                    creature, terrain) <= info.numCreature(lesser)) &&
                    (recruits.indexOf(creature) == -1))
                {
                    recruits.add(creature);
                }
            }
        }

        // Make sure that the potential recruits are available.
        Iterator it = recruits.iterator();
        while (it.hasNext())
        {
            Creature recruit = (Creature)it.next();
            if (getCreatureCount(recruit) < 1)
            {
                it.remove();
            }
        }

        return recruits;
    }

    /** Return a list of creature name strings. */
    java.util.List findEligibleRecruiters(String markerId, String recruitName)
    {
        java.util.List recruiters;
        Creature recruit = Creature.getCreatureByName(recruitName);
        if (recruit == null)
        {
            return new ArrayList();
        }

        LegionInfo info = getLegionInfo(markerId);
        String hexLabel = info.getHexLabel();
        MasterHex hex = MasterBoard.getHexByLabel(hexLabel);
        char terrain = hex.getTerrain();

        recruiters = TerrainRecruitLoader.getPossibleRecruiters(terrain);
        Iterator it = recruiters.iterator();
        while (it.hasNext())
        {
            Creature possibleRecruiter = (Creature)it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain);
            if (needed < 1 || needed > info.numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }

        java.util.List strings = new ArrayList();
        it = recruiters.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            strings.add(creature.getName());
        }
        return strings;
    }


    /** Return a set of hexLabels. */
    Set getPossibleRecruitHexes()
    {
        Set set = new HashSet();

        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            if (activePlayerName.equals(info.getPlayerName()) &&
                info.canRecruit())
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }


    /** Return a set of hexLabels for all other unengaged legions of 
     *  markerId's player that have summonables.
     * public for client-side AI -- do not call from server side. */
    public Set findSummonableAngelHexes(String summonerId)
    {
Log.debug("Called Client.findSummonableAngelHexes for " + summonerId);
        Set set = new HashSet();
        LegionInfo summonerInfo = getLegionInfo(summonerId);
        String playerName = summonerInfo.getPlayerName();
        Iterator it = getLegionsByPlayer(playerName).iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
Log.debug("checking " + markerId);
            if (!markerId.equals(summonerId)) 
            {
Log.debug(markerId + " not the same as summoner");
                LegionInfo info = getLegionInfo(markerId);
                if (info.hasSummonable() && !(info.isEngaged()))
                {
                    set.add(info.getHexLabel());
                }
            }
        }
Log.debug("found " + set.size() + " hexes");
        return set;
    }

    /** Return a set of hexLabels. */
    Set listTeleportMoves(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        MasterHex hex = MasterBoard.getHexByLabel(info.getHexLabel());
        return movement.listTeleportMoves(info, hex, movementRoll);
    }

    /** Return a set of hexLabels. */
    Set listNormalMoves(String markerId)
    {
        LegionInfo info = getLegionInfo(markerId);
        MasterHex hex = MasterBoard.getHexByLabel(info.getHexLabel());
        return movement.listNormalMoves(info, hex, movementRoll);
    }

    Set listPossibleEntrySides(String moverId, String hexLabel, 
        boolean teleport)
    {
        return movement.listPossibleEntrySides(moverId, hexLabel, teleport);
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

    int getCreatureCount(Creature creature)
    {
        return getCreatureCount(creature.getName());
    }


    /** Returns a list of markerIds.
     *  public for client-side AI -- do not call from server side */
    public java.util.List getLegionsByHex(String hexLabel)
    {
        java.util.List markerIds = new ArrayList();
        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
            if (info.getHexLabel() != null &&
                hexLabel.equals(info.getHexLabel()))
            {
                markerIds.add(info.getMarkerId());
            }
        }
        return markerIds;
    }

    /** Returns a list of markerIds. */
    java.util.List getLegionsByPlayer(String name)
    {
        java.util.List markerIds = new ArrayList();
        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
            if (name.equals(info.getPlayerName()))
            {
                markerIds.add(info.getMarkerId());
            }
        }
        return markerIds;
    }

    Set findUnmovedLegionHexes()
    {
        Set set = new HashSet();

        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            if (!info.hasMoved() && playerName.equals(info.getPlayerName())) 
            {
                set.add(info.getHexLabel());
            }
        }
        return set;

    }

    /** Return a set of hexLabels for the active player's legions with
     *  7 or more creatures. */
    Set findTallLegionHexes()
    {
        Set set = new HashSet();

        Iterator it = legionInfo.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            LegionInfo info = (LegionInfo)entry.getValue();
            if (info.getHeight() >= 7 && 
                activePlayerName.equals(info.getPlayerName()))
            {
                set.add(info.getHexLabel());
            }
        }
        return set;
    }

    /** Return a set of hexLabels for all hexes with engagements. */
    Set findEngagements()
    {
        Set set = new HashSet();
        Iterator it = MasterBoard.getAllHexLabels().iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            java.util.List markerIds = getLegionsByHex(hexLabel);
            if (markerIds.size() == 2)
            {
                String marker0 = (String)markerIds.get(0);
                LegionInfo info0 = getLegionInfo(marker0);
                String playerName0 = info0.getPlayerName();

                String marker1 = (String)markerIds.get(1);
                LegionInfo info1 = getLegionInfo(marker1);
                String playerName1 = info1.getPlayerName();

                if (!playerName0.equals(playerName1))
                {
                    set.add(hexLabel);
                }
            }
        }
        return set;
    }

    boolean isOccupied(String hexLabel)
    {
        return !getLegionsByHex(hexLabel).isEmpty();
    }

    boolean isEngagement(String hexLabel)
    {
        java.util.List markerIds = getLegionsByHex(hexLabel);
        if (markerIds.size() == 2)
        {
            String marker0 = (String)markerIds.get(0);
            LegionInfo info0 = getLegionInfo(marker0);
            String playerName0 = info0.getPlayerName();

            String marker1 = (String)markerIds.get(1);
            LegionInfo info1 = getLegionInfo(marker1);
            String playerName1 = info1.getPlayerName();

            return !playerName0.equals(playerName1);
        }
        return false;
    }

    java.util.List getEnemyLegions(String playerName)
    {
        java.util.List markerIds = new ArrayList();
        Iterator it = legionInfo.values().iterator();
        while (it.hasNext())
        {
            LegionInfo info = (LegionInfo)it.next();
            String markerId = info.getMarkerId();
            if (!playerName.equals(info.getPlayerName()))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    java.util.List getEnemyLegions(String hexLabel, String playerName)
    {
        java.util.List markerIds = new ArrayList();
        java.util.List legions = getLegionsByHex(hexLabel);
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            if (!playerName.equals(getPlayerNameByMarkerId(markerId)))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    int getNumEnemyLegions(String hexLabel, String playerName)
    {
        return getEnemyLegions(hexLabel, playerName).size();
    }

    java.util.List getFriendlyLegions(String hexLabel, String playerName)
    {
        java.util.List markerIds = new ArrayList();
        java.util.List legions = getLegionsByHex(hexLabel);
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            if (playerName.equals(getPlayerNameByMarkerId(markerId)))
            {
                markerIds.add(markerId);
            }
        }
        return markerIds;
    }

    int getNumFriendlyLegions(String hexLabel, String playerName)
    {
        return getFriendlyLegions(hexLabel, playerName).size();
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
        LegionInfo info = getLegionInfo(splitoffId);
        String hexLabel = info.getHexLabel();
        removeLegion(splitoffId);
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


    // XXX For debug only -- remove
    void forceAdvancePhase()
    {
        server.forceAdvancePhase();
    }

    // XXX For debug only -- remove
    void forceAdvanceBattlePhase()
    {
        server.forceAdvanceBattlePhase();
    }


    String getPlayerNameByMarkerId(String markerId)
    {
        String shortColor = markerId.substring(0, 2);
        return getPlayerNameForShortColor(shortColor);
    }

    String getPlayerNameForShortColor(String shortColor)
    {
        PlayerInfo info = null;

        // Stage 1: See if the player who started with this color is alive.
        for (int i = 0; i < playerInfo.length; i++)
        {
            info = playerInfo[i];
            if (shortColor.equals(info.getShortColor()) && !info.isDead())
            {
                return info.getName();
            }
        }

        // Stage 2: He's dead.  Find who killed him and see if he's alive.

        for (int i = 0; i < playerInfo.length; i++)
        {
            info = playerInfo[i];
            if (info.getPlayersElim().indexOf(shortColor) != -1)
            {
                // We have the killer.
                if (!info.isDead())
                {
                    return info.getName();
                }
                else
                {
                    return getPlayerNameForShortColor(info.getShortColor());
                }
            }
        }
        return null;
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
        PlayerInfo info = getPlayerInfo(playerName);
        return info.getMulligansLeft();
    }


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

        String results = SplitLegion.splitLegion(this, parentId, childId);

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

        LegionInfo childInfo = getLegionInfo(childId);
        childInfo.setHexLabel(hexLabel);
        childInfo.setHeight(childHeight);

        LegionInfo parentInfo = getLegionInfo(parentId);
        parentInfo.setHeight(parentInfo.getHeight() - childHeight);

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
        else 
        {
            do
            {
                color = PickColor.pickColor(board.getFrame(), playerName, 
                    colorsLeft);
            }
            while (color == null);
        }

        this.color = color;

        server.assignColor(playerName, color);
    }

    private String getHexForLegion(String markerId)
    {
        return getLegionInfo(markerId).getHexLabel();
    }

    void setLookAndFeel(String lfName)
    {
        try
        {
            UIManager.setLookAndFeel(lfName);
            updateEverything();
            Log.debug("Switched to Look & Feel: " + lfName);
            setOption(Options.favoriteLookFeel, lfName);
            currentLookAndFeel = lfName;
        }
        catch (Exception e)
        {
            Log.error("Look & Feel " + lfName + " not usable (" + e + ")");
        }
    }

    private void updateEverything()
    {
        if (board != null)
        {
            board.updateComponentTreeUI();
            board.pack();
        }
        if (statusScreen != null)
        {
            SwingUtilities.updateComponentTreeUI(statusScreen);
            statusScreen.pack();
        }
        if (caretakerDisplay != null)
        {
            SwingUtilities.updateComponentTreeUI(caretakerDisplay);
            caretakerDisplay.pack();
        }
        repaintAllWindows();
    }
}
