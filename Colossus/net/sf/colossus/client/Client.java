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

    // Per-client and per-player options should be kept here instead
    // of in Game.  (For now we can move all options from Game/Player
    // to client.  The few server options can be moved back to Game or
    // Server later, and have their GUI manipulators removed or restricted.)
    private Properties options = new Properties();

    /** Player who owns this client. */
    String playerName;

    /** Last movement roll for any player. */
    private int movementRoll = -1;

    /** Sorted set of available legion markers for this player. */
    private TreeSet markersAvailable;
    private String parentId;
    private int numSplitsThisTurn;

    /** Gradually moving AI to client side. */
    private AI ai = new SimpleAI();


    public Client(Server server, String playerName)
    {
        this.server = server;
        this.playerName = playerName;
    }


    /** Take a mulligan. Returns the roll, or -1 on error. */
    int mulligan()
    {
        clearUndoStack();
        clearRecruitChits();
        return server.mulligan(playerName);
    }


    /** Resolve engagement in land. */
    void engage(String land)
    {
        server.engage(land);
    }

    /** Legion concedes. */
    boolean concede()
    {
        return server.tryToConcede(playerName);
    }

    /** Cease negotiations and fight a battle in land. */
    void fight(String land)
    {
        clearUndoStack();
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
        // XXX Repaint just the two legions' hexes.
        board.repaint();
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

    public void showMovementRoll(int roll)
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
    // XXX Server-side options need to be tracked on the server.
    //    (Pass from controlling client at initialization.)
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
        if (name.equals(Options.showStatusScreen))
        {
            updateStatusScreen(server.getPlayerInfo());
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

    public void setStringOption(String optname, String value)
    {
        options.setProperty(optname, String.valueOf(value));
        // TODO Add some triggers so that if autoPlay or autoSplit is set
        // during this player's split phase, the appropriate action
        // is called.
    }

    public void setIntOption(String optname, int value)
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
        setBooleanOptions();
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
        setupPlayerLabel();
    }


    // TODO Rename.  Should take all caretaker counts as a parameter
    // rather than having the display call back to server.
    public void updateCaretakerDisplay()
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


    public void doneWithCarries()
    {
        if (map != null)
        {
            map.clearCarries();
        }
        makeForcedStrikes();
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

    /** Returns true if okay, or false if forced strikes remain. */
    boolean doneWithStrikes()
    {
        return server.doneWithStrikes();
    }

    private void makeForcedStrikes()
    {
        if (playerName.equals(getBattleActivePlayerName()))
        {
            server.makeForcedStrikes(playerName, false);
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

    // TODO Cache legion heights on client.
    int getLegionHeight(String markerId)
    {
        return server.getLegionHeight(markerId);
    }

    /** Add the marker to the end of the list.  If it's already
     *  in the list, remove the earlier entry. */
    void setMarker(String id, Marker marker)
    {
        removeMarker(id);
        markers.add(marker);
    }

    // TODO Rename, align legions in this hex.
    /** Remove the first marker with this id from the list. Return
     *  the removed marker. */
    public void removeMarker(String id)
    {
        Iterator it = markers.iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            if (marker.getId() == id)
            {
                it.remove();
                return;
            }
        }
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
    void addBattleChit(String imageName, int tag)
    {
        BattleChit chit = new BattleChit(4 * Scale.get(), imageName,
            map, tag);
        battleChits.add(chit);
    }


    // TODO Rename.
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
        // XXX Repaint just the chit's hex.
        if (map != null)
        {
            map.repaint();
        }
    }

    // Rename
    public void placeNewChit(String imageName, int tag, String hexLabel)
    {
        if (map != null)
        {
            map.placeNewChit(imageName, tag, hexLabel);
        }
    }


    java.util.List getRecruitChits()
    {
        return recruitChits;
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

    void clearRecruitChits()
    {
        recruitChits.clear();
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

    public void createSummonAngel(String markerId, String longMarkerName)
    {
        board.deiconify();
        summonAngel = SummonAngel.summonAngel(this, markerId, longMarkerName);
    }

    String getDonorId()
    {
        return server.getDonorId(playerName);
    }

    boolean donorHasAngel()
    {
        return server.donorHasAngel(playerName);
    }

    boolean donorHasArchangel()
    {
        return server.donorHasArchangel(playerName);
    }


    public void askAcquireAngel(String markerId, java.util.List recruits)
    {
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
        server.acquireAngel(markerId, angelType);
        // XXX repaint just the one hex.
        if (board != null)
        {
            board.repaint();
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
            negotiate = new Negotiate(this, attackerLongMarkerName, 
                defenderLongMarkerName, attackerId, defenderId,
                attackerImageNames, defenderImageNames, hexLabel);
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


    // TODO Use this method to mark chits as wounded / dead.
    public void setBattleValues(String attackerName, String defenderName,
        String attackerHexId, String defenderHexId, char terrain,
        int strikeNumber, int damage, int carryDamage, int [] rolls,
        Set carryTargets)
    {
        if (battleDice != null)
        {
            battleDice.setValues(attackerName, defenderName, attackerHexId,
                defenderHexId, terrain, strikeNumber, damage, carryDamage,
                rolls);
            battleDice.showRoll();
            if (carryDamage > 0)
            {
                map.highlightCarries(carryDamage, carryTargets);
            }
            else
            {
                makeForcedStrikes();
                map.highlightCrittersWithTargets();
                // XXX Needed?
                map.repaint();
            }
        }
    }

    public void setCarries(int carryDamage, Set carryTargets)
    {
        if (battleDice != null)
        {
            battleDice.setCarries(carryDamage);
        }
        if (map != null && carryDamage > 0)
        {
            map.highlightCarries(carryDamage, carryTargets);
        }
    }

    // TODO Handle this from setBattleValues()
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

    // TODO Handle this from setBattleValues()
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


    public void initBattle(String masterHexLabel)
    {
        cleanupNegotiationDialogs();

        // Do not show map for AI players.
        if (!getOption(Options.autoPlay))
        {
            map = new BattleMap(this, masterHexLabel);
            JFrame frame = map.getFrame();
            battleDice = new BattleDice();
            frame.getContentPane().add(battleDice, BorderLayout.SOUTH);
            frame.pack();
            map.requestFocus();
            frame.setVisible(true);
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
    public void doMuster(String markerId)
    {
        if (!server.canRecruit(markerId))
        {
            return;
        }
        // TODO Cache this on the client side.
        String hexLabel = server.getHexForLegion(markerId);

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

        server.doMuster(markerId, recruitName, recruiterName);
    }

    /** Currently used for human players only.  Always needs to call
     *  server.doMuster(), even if no recruit is wanted, to get past
     *  the reinforcing phase. */
    public void doReinforce(String markerId)
    {
Log.debug("Called Client.reinforce for " + markerId);
        // TODO Cache this on the client side.
        String hexLabel = server.getHexForLegion(markerId);

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

        server.doMuster(markerId, recruitName, recruiterName);
    }

    // TODO Remember remaining recruiting hexes.
    public void didMuster(String markerId)
    {
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }
        if (board != null)
        {
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


    public void setupSplitMenu()
    {
        // XXX Should track this better.
        initMarkersAvailable();

        numSplitsThisTurn = 0;

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


    public void alignLegions(Set hexLabels)
    {
        if (board != null)
        {
            board.alignLegions(hexLabels);
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
        // Just in case the other player started the battle
        // really quickly.
        cleanupNegotiationDialogs();

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


    // TODO Need to cache hex for each battle chit on client,
    // and notify moves instead of aligns.
    public void alignBattleChits(Set hexLabels)
    {
        if (map != null)
        {
            map.alignChits(hexLabels);
        }
    }


    // TODO This should happen based on notification of a split.
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

    /** Create new markers for all passed legions ids. */
    public void addMarkers(Collection markerIds)
    {
        if (board != null)
        {
            Iterator it = markerIds.iterator();
            while (it.hasNext())
            {
                String markerId = (String)it.next();
                addMarker(markerId);
            }
            board.alignAllLegions();
            board.getFrame().setVisible(true);
            board.repaint();
        }
    }

    /** Create new markers in response to a rescale. */
    void recreateMarkers()
    {
        Iterator it = markers.iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            String markerId = marker.getId();
            addMarker(markerId);
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
        return server.getPlayerColor(playerName);
    }

    String getShortColor()
    {
        return Player.getShortColor(getColor());
    }


    String getBattleActivePlayerName()
    {
        return server.getBattleActivePlayerName();
    }

    int getBattlePhase()
    {
        return server.getBattlePhase();
    }

    int getBattleTurnNumber()
    {
        return server.getBattleTurnNumber();
    }


    /** Returns true if the move was legal, or false if it was not allowed. */
    boolean doBattleMove(int tag, String hexLabel)
    {
        boolean moved = server.doBattleMove(tag, hexLabel);
        if (moved)
        {
            pushUndoStack(hexLabel);
        }
        return moved;
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
    }


    int getCarryDamage()
    {
        return server.getCarryDamage();
    }

    Set getCarryTargets()
    {
        return server.getCarryTargets();
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

    String getPlayerNameByTag(int tag)
    {
        return server.getPlayerNameByTag(tag);
    }

    String getActivePlayerName()
    {
        return server.getActivePlayerName();
    }

    int getPhase()
    {
        return server.getPhase();
    }

    int getTurnNumber()
    {
        return server.getTurnNumber();
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

    boolean goodEntrySide(String entrySide)
    {
        return (entrySide != null && (entrySide.equals("Left") || 
            entrySide.equals("Bottom") || entrySide.equals("Right")));
    }

    public void didMove(String markerId)
    {
        if (isMyLegion(markerId))
        {
            pushUndoStack(markerId);
        }
    }

    /** Return a list of Creatures. */
    java.util.List findEligibleRecruits(String markerId, String hexLabel)
    {
        return server.findEligibleRecruits(markerId, hexLabel);
    }

    /** Return a set of hexLabels. */
    Set findAllEligibleRecruitHexes()
    {
        return server.findAllEligibleRecruitHexes();
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

    int getActivePlayerNum()
    {
        return server.getActivePlayerNum();
    }

    int getCreatureCount(String creatureName)
    {
        return server.getCreatureCount(creatureName);
    }

    // TODO cache this
    java.util.List getLegionMarkerIds(String hexLabel)
    {
        return server.getLegionMarkerIds(hexLabel);
    }

    Set findAllUnmovedLegionHexes()
    {
        return server.findAllUnmovedLegionHexes();
    }

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

    String getLongMarkerName(String markerId)
    {
        return server.getLongMarkerName(markerId);
    }

    /** Return a list of Strings. */
    java.util.List getLegionImageNames(String markerId)
    {
        return server.getLegionImageNames(markerId, playerName);
    }

    void undoLastSplit()
    {
        if (!isUndoStackEmpty())
        {
            String splitoffId = (String)popUndoStack();
            server.undoSplit(playerName, splitoffId);
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

    void undoLastRecruit()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            server.undoRecruit(playerName, markerId);
            // XXX Repaint just markerId's hex
            board.repaint();
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
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        if (!server.doneWithSplits(playerName))
        {
            showMessageDialog("Must split");
            return;
        }
        clearUndoStack();
    }

    void doneWithMoves()
    {
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        clearRecruitChits();
        String error = server.doneWithMoves(playerName);
        if (error.equals(""))
        {
            clearUndoStack();
        }
        else
        {
            showMessageDialog(error);
            board.highlightUnmovedLegions();
        }
    }

    void doneWithEngagements()
    {
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        if (!server.doneWithEngagements(playerName))
        {
            showMessageDialog("Must resolve engagements");
        }
        clearUndoStack();
    }

    void doneWithRecruits()
    {
        if (!playerName.equals(server.getActivePlayerName()))
        {
            return;
        }
        clearUndoStack();
        server.doneWithRecruits(playerName);
    }


    boolean isMyLegion(String markerId)
    {
        return (playerName.equals(server.getPlayerNameByMarkerId(markerId)));
    }

    int getMovementRoll()
    {
        return movementRoll;
    }

    int getMulligansLeft()
    {
        return server.getMulligansLeft(playerName);
    }

    // XXX Stringify
    /** Initialize this player's available legion markers from the server. */
    void initMarkersAvailable()
    {
        markersAvailable = new TreeSet(
            MarkerComparator.getMarkerComparator(getShortColor()));
        markersAvailable.addAll(server.getMarkersAvailable(playerName));
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
            String childId = ai.pickMarker(markersAvailable);
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
            getLongMarkerName(parentId), childId,
            getLegionImageNames(parentId));

        if (results != null)
        {
            server.doSplit(parentId, childId, results);
        }
    }

    /** Callback from server after any successful split. */
    public void didSplit(String childId)
    {
        if (isMyLegion(childId))
        {
            pushUndoStack(childId);
            markersAvailable.remove(childId);
        }
        numSplitsThisTurn++;

        if (board != null)
        {
            // XXX Align only the split hex.
            board.alignAllLegions();
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

        server.assignColor(playerName, color);
    }
}

