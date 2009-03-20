package net.sf.colossus.client;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.ai.AI;
import net.sf.colossus.ai.SimpleAI;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.gui.BattleChit;
import net.sf.colossus.gui.ClientGUI;
import net.sf.colossus.gui.GUIMasterHex;
import net.sf.colossus.gui.IClientGUI;
import net.sf.colossus.gui.Marker;
import net.sf.colossus.gui.MasterBoard;
import net.sf.colossus.gui.NullClientGUI;
import net.sf.colossus.gui.Scale;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.GameServerSide;
import net.sf.colossus.server.IServer;
import net.sf.colossus.server.Server;
import net.sf.colossus.server.Start;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.server.Start.WhatToDoNext;
import net.sf.colossus.util.ChildThreadManager;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.webcommon.InstanceTracker;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Lives on the client side and handles all communication
 *  with the server.  It talks to the Server via the network protocol  
 *  and to Client side classes locally, but to all GUI related classes
 *  it should only communicate via ClientGUI class.
 *  There is one client per player.
 *  
 *  TODO Handle GUI related issues purely via ClientGUI
 *       All GUI classes should talk to the server purely through
 *       ClientGUI which handles it via the Client.
 *
 *  TODO the logic for the battles could probably be separated from the
 *  rest of this code. At the moment the battle logic seems to bounce
 *  back and forth between BattleBoard (which is really a GUI class) and
 *  this class.
 *
 *  TODO this class also has the functionality of a GameClientSide class,
 *  which should be separated and ideally moved up into the {@link Game}
 *  class. The whole {@link IOracle} interface is part of that.
 *  One approach would be moving code from {@link GameServerSide}
 *  up into {@link Game} and then reuse it here in the matching methods,
 *  then inlining it into the calling code. Another one would be creating
 *  the GameClientSide for now and relocating code there.
 *  ==> Clemens march 2009: I started the GameClientSide approach :)
 *   
 *  TODO there are a few places where an Iterator is used to remove all elements
 *  of a list -- an enhanced for loop with a Collection.clear() would probably
 *  look better and be more efficient (not that the latter would be significant
 *  in any of the cases)
 *
 *  @version $Id$
 *  @author David Ripton
 *  @author Romain Dolbeau
 */
@SuppressWarnings("serial")
public final class Client implements IClient, IOracle
{
    private static final Logger LOGGER = Logger.getLogger(Client.class
        .getName());

    /** This will eventually be a network interface rather than a
     *  direct reference.  So don't share this reference. */
    private IServer server;
    private ChildThreadManager threadMgr;

    public boolean failed = false;

    // TODO keep in sync with GUI
    private boolean replayOngoing = false;

    private IClientGUI gui;

    // TODO TODO TODO : CHECK !!
    // this is possibly updated (at least reset to null) only properly if GUI is there!!!
    private Legion attacker;
    private Legion defender;
    private MasterHex battleSite;

    private final List<BattleChit> battleChits = new ArrayList<BattleChit>();

    // Information on the current moving legion.
    private LegionClientSide mover;

    // Per-client and per-player options.
    private final Options options;

    // TODO move to Constants?
    // private static String propNameForceViewBoard = "net.sf.colossus.forceViewBoard";

    /**
     * Player who owns this client.
     *
     * TODO should be final but can't be until the constructor gets all the data
     * needed
     */
    private PlayerClientSide owningPlayer;
    private boolean playerAlive = true;

    /**
     * The game in progress.
     */
    private final Game game;

    /**
     * The "Start" object which created this Client
     */
    private final Start startObject;
    /**
     * Starting marker color of player who owns this client.
     *
     * TODO most likely redundant with owningPlayer.getColor()
     */
    private Constants.PlayerColor color;

    /** Last movement roll for any player. */
    private int movementRoll = -1;

    private Legion parent;

    // This ai is either the actual ai player for an AI player, but is also
    // used by human clients for the autoXXX actions.
    private AI ai;

    /**
     * This is used as a placeholder for activePlayer and battleActivePlayer since they
     * are sometimes accessed when they are not available.
     *
     * TODO this is a hack. Those members should just not be accessed at times where they
     * are not available. It seems to happen during startup (the not yet set case) and in
     * some GUI parts after battles, when battleActivePlayer has been reset already.
     */
    private final PlayerClientSide noone;

    private int turnNumber = -1;
    private PlayerClientSide activePlayer;
    private Constants.Phase phase;

    private int battleTurnNumber = -1;
    private Player battleActivePlayer;
    private Constants.BattlePhase battlePhase;

    /** If the game is over, then quitting does not require confirmation. */
    private boolean gameOver;

    /** One per player. */
    private PlayerClientSide[] players;

    private int numPlayers;

    private Movement movement;
    private BattleMovement battleMovement;
    private Strike strike;

    private Server localServer;
    private SocketClientThread sct;

    /**
     * Constants modeling the party who closed this client.
     *
     * TODO the CLOSED_BY_WEBCLIENT seems unused
     */
    private enum ClosedByConstant
    {
        NOT_CLOSED, CLOSED_BY_SERVER, CLOSED_BY_CLIENT, CLOSED_BY_WEBCLIENT
    }

    public ClosedByConstant closedBy = ClosedByConstant.NOT_CLOSED;

    // XXX temporary until things are synched
    private boolean tookMulligan;

    private int numSplitsThisTurn;

    private int delay = -1;

    /** For battle AI. */
    private List<CritterMove> bestMoveOrder = null;
    private List<CritterMove> failedBattleMoves = null;

    private final Hashtable<CreatureType, Integer> recruitReservations = new Hashtable<CreatureType, Integer>();

    // Once we got dispose from server (or user initiated it himself),
    // we'll ignore it if we we get it from server again
    // - it's then up to the user to do some "disposing" action.
    private boolean gotDisposeAlready = false;

    private boolean disposeInProgress = false;

    /**
     * TODO Now Client creates the Game (GameClientSide) instance.
     *      So far it creates it mostly with dummy info; should do better.
     *      - for example, create first SocketClientThread, and as first
     *      answer to connect gets the Variant name, and use that
     *      for game creation. So when Client constructor is completed
     *      also Game and Variant are proper.
     *      (problem would still be ... player cound and names...) 
     *
     * TODO try to make the Client class agnostic of the network or not question by
     *      having the SCT outside and behaving like a normal server -- that way it
     *      would be easier to run the local clients without the detour across the
     *      network and the serialization/deserialization of all objects
     */
    public Client(String host, int port, String playerName, Start startObj,
        Server theServer, boolean byWebClient, boolean noOptionsFile,
        boolean createGUI)
    {
        this(startObj, playerName, noOptionsFile, createGUI);

        this.localServer = theServer;

        gui.setStartedByWebClient(byWebClient);

        sct = new SocketClientThread(this, host, port);

        String reasonFail = sct.getReasonFail();
        if (reasonFail != null)
        {
            // If this failed here, it is usually a "could not connect"-problem
            // (wrong host or port or server not yet up).
            // In this case we just do cleanup and end.

            // start needs to be run, otherwise thread won't be GC'd.
            sct.start();
            sct = null;

            LOGGER.warning("Client startup failed: " + reasonFail);
            if (!Options.isStresstest())
            {
                String title = "Socket initialialization failed!";
                gui.showErrorMessage(reasonFail, title);
            }

            failed = true;
            ViableEntityManager.unregister(this);
        }
        else
        {
            this.server = sct;
            if (isRemote())
            {
                ResourceLoader.setDataServer(host, port + 1);
            }
            else
            {
                ResourceLoader.setDataServer(null, 0);
            }

            sct.start();

            TerrainRecruitLoader.setCaretaker(getGame().getCaretaker());
            CustomRecruitBase.addCaretakerClientSide(getGame().getCaretaker());
            failed = false;
        }
    }

    private Client(Start startObj, String playerName, boolean noOptionsFile,
        boolean createGUI)
    {
        assert playerName != null;

        // TODO still dummy arguments
        this.game = new GameClientSide(null, new String[0]);
        this.startObject = startObj;

        ((GameClientSide)game).setClient(this);

        // TODO this is currently not set properly straight away, it is fixed in
        // updatePlayerInfo(..) when the PlayerInfos are initialized. Should really
        // happen here, but doesn't yet since we don't have all players (not even as
        // names) yet
        this.owningPlayer = new PlayerClientSide(getGame(), playerName, 0);

        this.noone = new PlayerClientSide(getGame(), "", 0);
        this.activePlayer = noone;
        this.battleActivePlayer = noone;

        this.threadMgr = new ChildThreadManager("Client " + playerName);

        this.ai = new SimpleAI(this);

        this.movement = new Movement(this);
        this.battleMovement = new BattleMovement(this);
        this.strike = new Strike(this);

        ViableEntityManager.register(this, "Client " + playerName);
        InstanceTracker.register(this, "Client " + playerName);

        options = new Options(playerName, noOptionsFile);

        /*
                // Intended for stresstest, to see whats happening, and that graphics
                // stuff is there done, too.
                // This here works only if name setting is done "by-type", so that
                // at least one AI gets a name ending with "1".
                boolean forceViewBoard = false;
                String propViewBoard = System.getProperty(propNameForceViewBoard);
                if (propViewBoard != null && propViewBoard.equalsIgnoreCase("yes"))
                {
                    forceViewBoard = true;
                    options.setOption(Options.showEventViewer, "true");
                    options.setOption(Options.showStatusScreen, "true");
                }

                if (!options.getOption(Options.autoPlay)
                    || (forceViewBoard && (getOwningPlayer().getName().endsWith("1")
                        || options.getStringOption(Options.playerType).endsWith(
                            "Human") || options.getStringOption(Options.playerType)
                        .endsWith("Network"))))
                {
                    createGUI = true;
                }
        */

        if (createGUI)
        {
            this.gui = new ClientGUI(this, options);
        }
        else
        {
            this.gui = new NullClientGUI(this, options);
        }

        setupOptionListeners();
        // Need to load options early so they don't overwrite server options.
        loadOptions();
    }

    public ChildThreadManager getThreadMgr()
    {
        return threadMgr;
    }

    public boolean isRemote()
    {
        return (localServer == null);
    }

    public boolean isAlive()
    {
        return playerAlive;
    }

    public void doCheckServerConnection()
    {
        server.checkServerConnection();
    }

    /** Upon request with checkServerConnection, server sends a confirmation.
     *  This method here processes the confirmation.
     */
    public synchronized void serverConfirmsConnection()
    {
        gui.serverConfirmsConnection();
    }

    public void locallyInitiateSaveGame(String filename)
    {
        localServer.initiateSaveGame(filename);
    }

    public boolean getFailed()
    {
        return failed;
    }

    // because of synchronization issues we need to
    // be able to pass an undo split request to the server even if it is not
    // yet in the client UndoStack
    public void undoSplit(Legion splitoff)
    {
        getServer().undoSplit(splitoff);
        getOwningPlayer().addMarkerAvailable(splitoff.getMarkerId());

        numSplitsThisTurn++;

        if (getTurnNumber() == 1 && numSplitsThisTurn == 0)
        {
            gui.informSplitRequiredFirstRound();
        }
        LOGGER.log(Level.FINEST, "called server.undoSplit");
    }

    /** Take a mulligan. */
    public void mulligan()
    {
        gui.undoAllMoves(); // XXX Maybe move entirely to server
        gui.clearUndoStack();

        tookMulligan = true;

        server.mulligan();
    }

    // XXX temp
    public boolean tookMulligan()
    {
        return tookMulligan;
    }

    /** Resolve engagement in land. */
    public void engage(MasterHex hex)
    {
        server.engage(hex);
    }

    public Legion getMyEngagedLegion()
    {
        if (isMyLegion(attacker))
        {
            return attacker;
        }
        else if (isMyLegion(defender))
        {
            return defender;
        }
        return null;
    }

    public void concede()
    {
        concede(getMyEngagedLegion());
    }

    private void concede(Legion legion)
    {
        if (legion != null)
        {
            server.concede(legion);
        }
    }

    private void doNotConcede(Legion legion)
    {
        server.doNotConcede(legion);
    }

    /** Cease negotiations and fight a battle in land. */
    void fight(MasterHex hex)
    {
        server.fight(hex);
    }

    public void tellEngagement(MasterHex hex, Legion attacker, Legion defender)
    {
        this.battleSite = hex;
        this.attacker = attacker;
        this.defender = defender;

        gui.tellEngagement(attacker, defender, turnNumber);
    }

    public void tellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        gui.actOnTellEngagementResults(winner, method, points, turns);
        this.battleSite = null;
        this.attacker = null;
        this.defender = null;
    }

    /**
     * Displays the recruit tree of the hex if possible.
     */
    public void showHexRecruitTree(GUIMasterHex hex)
    {
        gui.showHexRecruitTree(hex);
    }

    /** Legion target summons unit from legion donor. */
    void doSummon(SummonInfo summonInfo)
    {
        assert summonInfo != null : "SummonInfo object must not be null!";

        if (summonInfo.noSummoningWanted())
        {
            // could also use getXXX from object...
            server.doSummon(null, null, null);
        }
        else
        {
            server.doSummon(summonInfo.getTarget(), summonInfo.getDonor(),
                summonInfo.getUnit());
        }
        gui.actOnDoSummon();
    }

    public void didSummon(Legion summoner, Legion donor, String summon)
    {
        gui.didSummon(summoner, donor, summon);
    }

    /** This player quits the whole game. The server needs to always honor
     *  this request, because if it doesn't players will just drop
     *  connections when they want to quit in a hurry. */
    public void withdrawFromGame()
    {
        if (!isGameOver())
        {
            server.withdrawFromGame();
        }
    }

    public void tellMovementRoll(int roll)
    {
        movementRoll = roll;
        gui.tellMovementRoll(roll);
        kickMoves();
    }

    public void tellWhatsHappening(String message)
    {
        gui.tellWhatsHappening(message);
    }

    private void kickMoves()
    {
        if (isMyTurn() && options.getOption(Options.autoMasterMove)
            && !isGameOver() && !replayOngoing)
        {
            doAutoMoves();
        }
    }

    private void doAutoMoves()
    {
        boolean again = ai.masterMove();
        aiPause();
        if (!again)
        {
            doneWithMoves();
        }
    }

    /** public so that server can set autoPlay for AIs.
     *
     * TODO This it totally confusing: this method is declared
     * to fulfill the IClient interface, but it is never actually
     * used, since the SocketClientThread directly deals with
     * the actual Options object itself...
     */
    public void setOption(String optname, String value)
    {
        options.setOption(optname, value);
        gui.syncCheckboxes();
        options.saveOptions();
    }

    /**
     * Trigger side effects after changing an option value.
     *
     *  TODO now that there are listeners, many of the other classes could listen to the
     *  options relevant to them instead of dispatching it all through the Client class.
     */
    private void setupOptionListeners()
    {
        gui.setupGUIOptionListeners();

        options.addListener(Options.playerType, new IOptions.Listener()
        {
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                setType(newValue);
            }
        });
    }

    /** Load player options from a file. */
    private void loadOptions()
    {
        options.loadOptions();
        gui.syncCheckboxes();
    }

    // public for IOracle
    public int getNumPlayers()
    {
        return numPlayers;
    }

    public int getNumLivingPlayers()
    {
        int total = 0;
        for (PlayerClientSide info : players)
        {
            if (!info.isDead())
            {
                total++;
            }
        }
        return total;
    }

    public void updatePlayerInfo(List<String> infoStrings)
    {
        numPlayers = infoStrings.size();
        if (players == null)
        {
            // first time we get the player infos, store them locally and set our
            // own, too -- which has been a fake until now
            players = new PlayerClientSide[numPlayers];
            for (int i = 0; i < numPlayers; i++)
            {
                List<String> data = Split.split(":", infoStrings.get(i));
                String playerName = data.get(1);
                PlayerClientSide info = new PlayerClientSide(getGame(),
                    playerName, i);
                players[i] = info;
                if (playerName.equals(this.owningPlayer.getName()))
                {
                    this.owningPlayer = info;
                }
            }
        }
        for (int i = 0; i < numPlayers; i++)
        {
            players[i].update(infoStrings.get(i));
        }
        gui.updateStatusScreen();
    }

    // TODO fix this mess with lots of different methods for retrieving Player[Info]s
    public PlayerClientSide getPlayer(int playerNum)
    {
        return players[playerNum];
    }

    public PlayerClientSide getPlayerInfo(String playerName)
    {
        for (PlayerClientSide info : players)
        {
            if (info.getName().equals(playerName))
            {
                return info;
            }
        }
        throw new IllegalArgumentException("No player info found for player '"
            + playerName + "'");
    }

    public PlayerClientSide getOwningPlayer()
    {
        return owningPlayer;
    }

    public List<PlayerClientSide> getPlayers()
    {
        return Collections.unmodifiableList(Arrays.asList(players));
    }

    /** Return the average point value of all legions in the game. */
    public int getAverageLegionPointValue()
    {
        int totalValue = 0;
        int totalLegions = 0;

        for (Player player : players)
        {
            totalLegions += player.getLegions().size();
            totalValue += player.getTotalPointValue();
        }
        return (int)(Math.round((double)totalValue / totalLegions));
    }

    // TODO probably unnecessary?
    public void setColor(Constants.PlayerColor color)
    {
        this.color = color;
    }

    public void updateCreatureCount(CreatureType type, int count, int deadCount)
    {
        getGame().getCaretaker().setAvailableCount(type, count);
        getGame().getCaretaker().setDeadCount(type, deadCount);
        gui.updateCreatureCountDisplay();
    }

    public void setClosedByServer()
    {
        closedBy = ClosedByConstant.CLOSED_BY_SERVER;
    }

    public void disposeClientOriginated()
    {
        if (disposeInProgress)
        {
            return;
        }
        closedBy = ClosedByConstant.CLOSED_BY_CLIENT;

        if (sct != null && !sct.isAlreadyDown())
        {
            {
                // SCT will then end the loop and do the dispose.
                // So nothing else to do any more here in EDT.
                sct.stopSocketClientThread();
            }
        }
        else
        {
            // SCT already closed and requested to dispose client,
            // but user declined. Now, when user manually wants to
            // close the board, have to do it directly.
            disposeWholeClient();
        }
    }

    // used from server, when game is over and server closes all sockets
    public synchronized void dispose()
    {
        if (gotDisposeAlready)
        {
            return;
        }
        gotDisposeAlready = true;
        disposeWholeClient();
    }

    // Clean up everything related to _this_ client:

    public void disposeWholeClient()
    {
        gui.handleWebClientRestore();

        // -----------------------------------------------
        // Now a long decision making, whether to actually close
        // everything or not... - depending on the situation.
        boolean close = true;

        try
        {
            close = decideWhetherClose();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Exception " + e.toString()
                + " while deciding whether to close", e);
        }

        if (close)
        {
            try
            {
                disposeInProgress = true;
                disposeAll();

                gui.setClientInWebClientNull();
            }
            // just in case, so we are sure to get the unregistering done
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE, "During close in client "
                    + owningPlayer.getName() + ": got Exception!!!"
                    + e.toString(), e);
            }
            ViableEntityManager.unregister(this);
        }
    }

    private boolean decideWhetherClose()
    {
        boolean close = true;

        // I don't use "getPlayerInfo().isAI() here, because if done
        // so very early, getPlayerInfo delivers null.
        boolean isAI = true;
        String pType = options.getStringOption(Options.playerType);
        if (pType != null
            && (pType.endsWith("Human") || pType.endsWith("Network")))
        {
            isAI = false;
        }

        // AIs in general, and any (local or remote) client during
        // stresstesting should close without asking...
        if (isAI || Options.isStresstest())
        {
            close = true;
        }
        else if (closedBy == ClosedByConstant.CLOSED_BY_SERVER)
        {
            if (isRemote())
            {
                gui.closePerhapsWithMessage();

                close = false;
            }
            else
            {
                // NOT remote, forced closed: just closing without asking
            }
        }
        return close;
    }

    /* Dispose all windows, and clean up lot of references,
     * so that GC can do it's job
     * - in case we keep JVM open to play another one...
     */
    private void disposeAll()
    {
        disposeInProgress = true;

        sct = null;
        server = null;

        threadMgr.cleanup();
        threadMgr = null;

        gui.doCleanupGUI();

    }

    /* This was earlier done at end of cleanupGUI inside client.
     * Now that is moved to GUI class, its called from there after all
     * other cleanup has completed (inside the invokeAndWait call).
     */
    public void doAdditionalCleanup()
    {
        movement.dispose();
        this.movement = null;
        this.battleMovement = null;
        this.strike = null;
        this.players = null;

        net.sf.colossus.server.CustomRecruitBase.reset();
    }

    /** Called from BattleBoard to leave carry mode. */
    public void leaveCarryMode()
    {
        gui.disposePickCarryDialog();
        server.leaveCarryMode();
        doAutoStrikes();
    }

    public void doneWithBattleMoves()
    {
        aiPause();
        gui.clearUndoStack();
        server.doneWithBattleMoves();
    }

    public boolean anyOffboardCreatures()
    {
        for (BattleChit chit : getActiveBattleChits())
        {
            if (chit.getCurrentHexLabel().startsWith("X"))
            {
                return true;
            }
        }
        return false;
    }

    public List<BattleChit> getActiveBattleChits()
    {
        return CollectionHelper.selectAsList(battleChits,
            new Predicate<BattleChit>()
            {
                public boolean matches(BattleChit chit)
                {
                    return getBattleActivePlayer().equals(
                        getPlayerByTag(chit.getTag()));
                }
            });
    }

    public List<BattleChit> getInactiveBattleChits()
    {
        return CollectionHelper.selectAsList(battleChits,
            new Predicate<BattleChit>()
            {
                public boolean matches(BattleChit chit)
                {
                    return !getBattleActivePlayer().equals(
                        getPlayerByTag(chit.getTag()));
                }
            });
    }

    private void markOffboardCreaturesDead()
    {
        for (BattleChit chit : getActiveBattleChits())
        {
            if (chit.getCurrentHexLabel().startsWith("X"))
            {
                chit.setDead(true);
                chit.repaint();
            }
        }
    }

    public void doneWithStrikes()
    {
        aiPause();
        server.doneWithStrikes();
    }

    /** Return true if any strikes were taken. */
    boolean makeForcedStrikes()
    {
        if (isMyBattlePhase() && options.getOption(Options.autoForcedStrike))
        {
            return strike.makeForcedStrikes(options
                .getOption(Options.autoRangeSingle));
        }
        return false;
    }

    /** Handle both forced strikes and AI strikes. */
    void doAutoStrikes()
    {
        if (isMyBattlePhase())
        {
            if (options.getOption(Options.autoPlay))
            {
                aiPause();
                boolean struck = makeForcedStrikes();
                if (!struck)
                {
                    struck = ai.strike(getBattleActiveLegion());
                }
                if (!struck)
                {
                    doneWithStrikes();
                }
            }
            else
            {
                boolean struck = makeForcedStrikes();
                gui.highlightCrittersWithTargets();
                if (!struck && findCrittersWithTargets().isEmpty())
                {
                    doneWithStrikes();
                }
            }
        }
    }

    /**
     * Get this legion's info or create if necessary.
     *
     * TODO move legion creation into a factory on {@link Player}
     */
    public LegionClientSide getLegion(String markerId)
    {
        PlayerClientSide player = getPlayerByMarkerId(markerId);
        LegionClientSide legion = player.getLegionByMarkerId(markerId);
        // Added this logging only for the purpose that one gets a clue
        // when during the game this happened - the assertion appears only
        // on stderr. Now it's also in the log, so one sees what was logged
        // just before and after it.
        if (legion == null)
        {
            LOGGER.log(Level.SEVERE, "No legion with markerId '" + markerId
                + "'" + " (for player " + player + "), turn = " + turnNumber
                + " in client " + getOwningPlayer());
        }
        assert legion != null : "No legion with markerId '" + markerId + "'"
            + " (for player " + player + "), turn = " + turnNumber
            + " in client " + getOwningPlayer();
        return legion;
    }

    public boolean legionExists(String markerId)
    {
        Player player = getPlayerByMarkerId(markerId);
        return player.hasLegion(markerId);
    }

    public void setMarkerForLegion(Legion legion, Marker marker)
    {
        // TODO next line seems redundant since setMarker(..) does set the marker on the legion
        ((LegionClientSide)legion).setMarker(marker);
    }

    /** Remove this eliminated legion, and clean up related stuff. */
    public void removeLegion(Legion legion)
    {
        gui.actOnRemoveLegion(legion);

        // TODO Do for all players
        if (isMyLegion(legion))
        {
            getOwningPlayer().addMarkerAvailable(legion.getMarkerId());
        }

        legion.getPlayer().removeLegion(legion);

        gui.alignLegionsMaybe(legion);
    }

    public int getLegionHeight(String markerId)
    {
        Legion legionInfo = getLegion(markerId);
        if (legionInfo == null)
        {
            return 0; //no legion, no height
        }
        return legionInfo.getHeight();
    }

    /** Needed when loading a game outside split phase. */
    public void setLegionStatus(Legion legion, boolean moved,
        boolean teleported, Constants.EntrySide entrySide, String lastRecruit)
    {
        legion.setMoved(moved);
        legion.setTeleported(teleported);
        ((LegionClientSide)legion).setEntrySide(entrySide);
        ((LegionClientSide)legion).setRecruitName(lastRecruit);
    }

    /** Return the full basename for a titan in legion,
     *  first finding that legion's player, player color, and titan size.
     */
    String getTitanBasename(Legion legion)
    {
        return ((LegionClientSide)legion).getTitanBasename();
    }

    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    // public for IOracle
    public List<String> getLegionImageNames(Legion legion)
    {
        LegionClientSide info = (LegionClientSide)legion;
        if (info != null)
        {
            return info.getImageNames();
        }
        return new ArrayList<String>();
    }

    /** Return a list of Booleans */
    // public for IOracle
    public List<Boolean> getLegionCreatureCertainties(Legion legion)
    {
        LegionClientSide info = (LegionClientSide)legion;
        if (info != null)
        {
            return info.getCertainties();
        }
        else
        {
            // TODO: is this the right thing?
            List<Boolean> l = new ArrayList<Boolean>(10); // just longer then max
            for (int idx = 0; idx < 10; idx++)
            {
                l.add(Boolean.valueOf(true)); // all true
            }
            return l;
        }
    }

    /** Add a new creature to this legion. */
    public void addCreature(Legion legion, String name, String reason)
    {
        ((LegionClientSide)legion).addCreature(name);

        gui.actOnAddCreature(legion, name, reason);

    }

    public void removeCreature(Legion legion, String name, String reason)
    {
        if (legion == null || name == null)
        {
            return;
        }

        gui.actOnRemoveCreature(legion, name, reason);

        int height = legion.getHeight();
        ((LegionClientSide)legion).removeCreature(name);
        if (height <= 1)
        {
            // do not remove this, sever will give explicit order to remove it
            // removeLegion(markerId);
        }
        if (height <= 1 && getTurnNumber() == -1)
        {
            // hack to remove legions correctly during load
            removeLegion(legion);
        }

        gui.actOnRemoveCreaturePart2(legion);
    }

    /** Reveal creatures in this legion, some of which already may be known.
     *  - this "reveal" is related to data coming from server being
     *  revealed to the split prediction
     * */

    public void revealCreatures(Legion legion, final List<String> names,
        String reason)
    {
        gui.eventViewerRevealCreatures(legion, names, reason);

        ((LegionClientSide)legion).revealCreatures(names);
    }

    /* pass revealed info to SplitPrediction and the GUI
     */
    public void revealEngagedCreatures(Legion legion,
        final List<String> names, boolean isAttacker, String reason)
    {
        revealCreatures(legion, names, reason);

        gui.revealEngagedCreatures(legion, names, isAttacker, reason);
    }

    public List<BattleChit> getBattleChits()
    {
        return Collections.unmodifiableList(battleChits);
    }

    public List<BattleChit> getBattleChits(final String hexLabel)
    {
        return CollectionHelper.selectAsList(battleChits,
            new Predicate<BattleChit>()
            {
                public boolean matches(BattleChit chit)
                {
                    return hexLabel.equals(chit.getCurrentHexLabel());
                }
            });
    }

    // TODO make typesafe
    public BattleChit getBattleChit(String hexLabel)
    {
        List<BattleChit> chits = getBattleChits(hexLabel);
        if (chits.isEmpty())
        {
            return null;
        }
        return chits.get(0);
    }

    /** Get the BattleChit with this tag. */
    BattleChit getBattleChit(int tag)
    {
        for (BattleChit chit : battleChits)
        {
            if (chit.getTag() == tag)
            {
                return chit;
            }
        }
        return null;
    }

    public void removeDeadBattleChits()
    {
        Iterator<BattleChit> it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit chit = it.next();
            if (chit.isDead())
            {
                it.remove();

                // Also remove it from LegionInfo.
                String name = chit.getId();
                if (chit.isInverted())
                {
                    Legion legion = defender;
                    ((LegionClientSide)legion).removeCreature(name);
                    gui.eventViewerDefenderSetCreatureDead(name, legion
                        .getHeight());

                }
                else
                {
                    Legion info = attacker;
                    ((LegionClientSide)info).removeCreature(name);

                    gui.eventViewerAttackerSetCreatureDead(name, info
                        .getHeight());

                }
            }
        }

        gui.repaintBattleBoard();
    }

    // TODO move to GUI ?
    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel)
    {
        addBattleChit(imageName, inverted, tag, hexLabel);
        gui.actOnPlaceNewChit(hexLabel);
    }

    // TODO move to GUI ?
    /** Create a new BattleChit and add it to the end of the list. */
    private void addBattleChit(final String bareImageName, boolean inverted,
        int tag, String hexLabel)
    {
        String imageName = bareImageName;
        if (imageName.equals(Constants.titan))
        {
            if (inverted)
            {
                imageName = getTitanBasename(defender);
            }
            else
            {
                imageName = getTitanBasename(attacker);
            }
        }
        Constants.PlayerColor playerColor;
        if (inverted)
        {
            Player player = defender.getPlayer();
            playerColor = player.getColor();
        }
        else
        {
            Player player = attacker.getPlayer();
            playerColor = player.getColor();
        }
        BattleChit chit = new BattleChit(5 * Scale.get(), imageName, inverted,
            tag, hexLabel, playerColor, this);
        battleChits.add(chit);
    }

    public CreatureType chooseBestPotentialRecruit(LegionClientSide legion,
        MasterHex hex, List<CreatureType> recruits)
    {
        CreatureType recruit = ai.getVariantRecruitHint(legion, hex, recruits);
        return recruit;
    }

    public Legion getMover()
    {
        return mover;
    }

    public void setMover(LegionClientSide legion)
    {
        this.mover = legion;
    }

    // TODO several gui components still need to ask the client for access
    //      to the board via the gui class ... - can this cleaned up?
    public MasterBoard getBoard()
    {
        return gui.getBoard();
    }

    public IClientGUI getGUI()
    {
        return gui;
    }

    public void tellReplay(boolean val, int maxTurn)
    {
        replayOngoing = val;
        gui.actOnTellReplay(maxTurn);
    }

    public boolean isReplayOngoing()
    {
        return replayOngoing;
    }

    public void confirmWhenCaughtUp()
    {
        sct.clientConfirmedCatchup();
    }

    public void initBoard()
    {
        LOGGER.finest(getOwningPlayer().getName() + " Client.initBoard()");
        if (isRemote())
        {
            VariantSupport.loadVariantByName(options
                .getStringOption(Options.variant), false);
        }

        gui.initBoard();
    }

    public void setupClientGUI()
    {
        gui.setupClientGUI();
    }

    public void setPlayerName(String playerName)
    {
        this.owningPlayer.setName(playerName);

        InstanceTracker.setId(this, "Client " + playerName);
        InstanceTracker.setId(ai, "AI: " + playerName);

        if (sct != null)
        {
            sct.fixName(playerName);
        }
    }

    public void createSummonAngel(Legion legion)
    {
        SummonInfo summonInfo = new SummonInfo();

        SortedSet<Legion> possibleDonors = findLegionsWithSummonables(legion);
        if (possibleDonors.size() < 1)
        {
            // Should not happen any more since I fixed it on server side.
            // But, who knows. Better check earlier than somehwere inside
            // the GUI.
            LOGGER.warning("Server requested us to createSummonAngel but "
                + "there are no legions with summonable Angels!");
            // still, do the summon with the default created summonInfo,
            // Server might wait for an answer (so, NOT just return without
            // doing anything).
        }
        else
        {
            if (options.getOption(Options.autoSummonAngels))
            {
                summonInfo = ai.summonAngel(legion, possibleDonors);
            }
            else
            {
                summonInfo = gui.doPickSummonAngel(legion, possibleDonors);

            }
        }

        doSummon(summonInfo);
    }

    /**
     * recruits is the list of acquirables that can be chosen from
     * for a certain point value reached. E.g. for getting 180 points,
     * going from 380 + 180 = 560,
     * game would first call this for 400: recruits = [Angel]
     * and then call it once more for 500: recruits = [Angel, Archangel]
     */
    public void askAcquireAngel(Legion legion, List<String> recruits)
    {
        if (options.getOption(Options.autoAcquireAngels))
        {
            acquireAngelCallback(legion, ai.acquireAngel(legion, recruits));
        }
        else
        {
            gui.doAcquireAngel(legion, recruits);
        }
    }

    public void acquireAngelCallback(Legion legion, String angelType)
    {
        server.acquireAngel(legion, angelType);
    }

    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    private boolean chooseWhetherToTeleport(MasterHex hex)
    {
        if (options.getOption(Options.autoMasterMove))
        {
            return false;
        }

        // No point in teleporting if entry side is moot.
        if (!isOccupied(hex))
        {
            return false;
        }

        return gui.chooseWhetherToTeleport();
    }

    /** Allow the player to choose whether to take a penalty (fewer dice
     *  or higher strike number) in order to be allowed to carry. */
    public void askChooseStrikePenalty(List<String> choices)
    {
        if (options.getOption(Options.autoPlay))
        {
            String choice = ai.pickStrikePenalty(choices);
            assignStrikePenalty(choice);
        }
        else
        {
            gui.doPickStrikePenalty(this, choices);
        }
    }

    public void assignStrikePenalty(String prompt)
    {
        gui.highlightCrittersWithTargets();
        server.assignStrikePenalty(prompt);
    }

    // TODO Move legion markers to slayer on client side.
    // TODO parameters should be PlayerState
    public void tellPlayerElim(Player deadPlayer, Player slayer)
    {
        assert deadPlayer != null;
        LOGGER.log(Level.FINEST, this.owningPlayer.getName()
            + " tellPlayerElim(" + deadPlayer + ", " + slayer + ")");

        // TODO Merge these
        // TODO should this be rather calling Player.die()?
        deadPlayer.setDead(true);
        ((PlayerClientSide)deadPlayer).removeAllLegions();
        // otherwise called too early, e.g. someone quitted
        // already during game start...

        if (this.owningPlayer.equals(deadPlayer))
        {
            playerAlive = false;
        }
    }

    public void tellGameOver(String message, boolean disposeFollows)
    {
        LOGGER.info("Client " + getOwningPlayer()
            + " received from server game over message: " + message);
        gameOver = true;

        gui.actOnTellGameOver(message, disposeFollows);
    }

    public boolean isGameOver()
    {
        return gameOver;
    }

    public void doFight(MasterHex hex)
    {
        if (!isMyTurn())
        {
            return;
        }
        engage(hex);
    }

    public void askConcede(Legion ally, Legion enemy)
    {
        if (options.getOption(Options.autoConcede))
        {
            answerConcede(ally, ai.concede(ally, enemy));
        }
        else
        {
            gui.showConcede(this, ally, enemy);
        }
    }

    public void askFlee(Legion ally, Legion enemy)
    {
        if (options.getOption(Options.autoFlee))
        {
            answerFlee(ally, ai.flee(ally, enemy));
        }
        else
        {
            gui.showFlee(this, ally, enemy);
        }
    }

    public void answerFlee(Legion ally, boolean answer)
    {
        if (answer)
        {
            server.flee(ally);
        }
        else
        {
            server.doNotFlee(ally);
        }
    }

    public void answerConcede(Legion legion, boolean answer)
    {
        if (answer)
        {
            concede(legion);
        }
        else
        {
            doNotConcede(legion);
        }
    }

    public void askNegotiate(Legion attacker, Legion defender)
    {
        this.attacker = attacker;
        this.defender = defender;

        if (options.getOption(Options.autoNegotiate))
        {
            // XXX AI players just fight for now.
            Proposal proposal = new Proposal(attacker, defender, true, false,
                null, null);

            makeProposal(proposal);
        }
        else
        {
            gui.showNegotiate(attacker, defender);
        }
    }

    /** Inform this player about the other player's proposal. */
    public void tellProposal(String proposalString)
    {
        gui.tellProposal(proposalString);
    }

    /** Called from both Negotiate and ReplyToProposal. */
    public void negotiateCallback(Proposal proposal, boolean respawn)
    {
        if (proposal != null && proposal.isFight())
        {
            fight(attacker.getCurrentHex());
            return;
        }
        else if (proposal != null)
        {
            makeProposal(proposal);
        }

        if (respawn)
        {
            gui.respawnNegotiate();
        }

    }

    private void makeProposal(Proposal proposal)
    {
        server.makeProposal(proposal.toString());
    }

    public void doTellProposal(String proposalString)
    {
        gui.tellProposal(proposalString);
    }

    public BattleHex getBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleSite().getTerrain(), chit
            .getCurrentHexLabel());
    }

    BattleHex getStartingBattleHex(BattleChit chit)
    {
        return HexMap.getHexByLabel(getBattleSite().getTerrain(), chit
            .getStartingHexLabel());
    }

    public boolean isOccupied(BattleHex hex)
    {
        return !getBattleChits(hex.getLabel()).isEmpty();
    }

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions)
    {
        BattleChit chit = getBattleChit(strikerTag);
        if (chit != null)
        {
            chit.setStruck(true);
        }

        gui.disposePickCarryDialog();

        BattleChit targetChit = getBattleChit(targetTag);

        gui.actOnTellStrikeResults(wasCarry, strikeNumber, rolls, chit,
            targetChit);

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

        if (strikerTag == Constants.HEX_DAMAGE)
        {
            // Do not trigger auto strikes in parallel with setupBattleFight()
        }
        else if (carryDamageLeft >= 1 && !carryTargetDescriptions.isEmpty())
        {
            pickCarries(carryDamageLeft, carryTargetDescriptions);
        }
        else
        {
            doAutoStrikes();
        }
    }

    public void nak(String reason, String errmsg)
    {
        LOGGER.log(Level.WARNING, owningPlayer.getName() + " got nak for "
            + reason + " " + errmsg);
        recoverFromNak(reason, errmsg);
    }

    private void recoverFromNak(String reason, String errmsg)
    {
        LOGGER.log(Level.FINEST, owningPlayer.getName() + " recoverFromNak "
            + reason + " " + errmsg);
        if (reason == null)
        {
            LOGGER.log(Level.SEVERE, "recoverFromNak with null reason!");
        }
        else if (reason.equals(Constants.doSplit))
        {
            showMessageDialog(errmsg);

            // if AI just got NAK for split, there's no point for
            // kickSplit again. Instead, let's just be DoneWithSplits.
            if (isMyTurn() && options.getOption(Options.autoSplit)
                && !isGameOver())
            {
                // XXX This may cause advancePhance illegally messages,
                // if e.g. SimpleAI fires two splits, both gets rejected,
                // and it responds with dineWithSplits two times.
                // But this whole situation should normally never happen, did
                // happen now only because of server/client data regarding
                // available markers was out of sync and then as reaction
                // to nak for didSplit do kickSplit() did just end in an
                // endless loop. Now perhaps 2 error messages, but no hang.
                doneWithSplits();
            }
        }
        else if (reason.equals(Constants.doneWithSplits))
        {
            showMessageDialog(errmsg);
            kickSplit();
        }
        else if (reason.equals(Constants.doMove))
        {
            showMessageDialog(errmsg);
            kickMoves();
        }
        else if (reason.equals(Constants.doneWithMoves))
        {
            showMessageDialog(errmsg);
            kickMoves();
        }
        else if (reason.equals(Constants.doBattleMove))
        {
            handleFailedBattleMove();
        }
        else if (reason.equals(Constants.doneWithBattleMoves))
        {
            // @todo: why can we ignore this?
        }
        else if (reason.equals(Constants.assignStrikePenalty))
        {
            doAutoStrikes();
        }
        else if (reason.equals(Constants.strike))
        {
            doAutoStrikes();
        }
        else if (reason.equals(Constants.doneWithStrikes))
        {
            showMessageDialog(errmsg);
        }
        else if (reason.equals(Constants.doneWithEngagements))
        {
            showMessageDialog(errmsg);
        }
        else if (reason.equals(Constants.doRecruit))
        {
            // @todo: why can we ignore this?
        }
        else if (reason.equals(Constants.doneWithRecruits))
        {
            // @todo: why can we ignore this?
        }
        else
        {
            LOGGER.log(Level.WARNING, owningPlayer.getName()
                + " unexpected nak " + reason + " " + errmsg);
        }
    }

    private void pickCarries(int carryDamage,
        Set<String> carryTargetDescriptions)
    {
        if (!isMyBattlePhase())
        {
            return;
        }

        if (carryDamage < 1 || carryTargetDescriptions.isEmpty())
        {
            leaveCarryMode();
        }
        else if (carryTargetDescriptions.size() == 1
            && options.getOption(Options.autoCarrySingle))
        {
            Iterator<String> it = carryTargetDescriptions.iterator();
            String desc = it.next();
            String targetHex = desc.substring(desc.length() - 2);
            applyCarries(targetHex);
        }
        else
        {
            if (options.getOption(Options.autoPlay))
            {
                aiPause();
                ai.handleCarries(carryDamage, carryTargetDescriptions);
            }
            else
            {
                gui.doPickCarries(this, carryDamage, carryTargetDescriptions);
            }
        }
    }

    public void initBattle(MasterHex hex, int battleTurnNumber,
        Player battleActivePlayer, Constants.BattlePhase battlePhase,
        Legion attacker, Legion defender)
    {
        gui.cleanupNegotiationDialogs();

        this.battleTurnNumber = battleTurnNumber;
        setBattleActivePlayer(battleActivePlayer);
        this.battlePhase = battlePhase;
        // TODO the following three are probably not needed,
        //      they are already set during tellEngagement()
        this.attacker = attacker;
        this.defender = defender;
        this.battleSite = hex;

        this.defender.setEntrySide(this.attacker.getEntrySide()
            .getOpposingSide());

        gui.actOnInitBattle();
    }

    public void showMessageDialog(String message)
    {
        gui.showMessageDialog(message);
    }

    public void cleanupBattle()
    {
        LOGGER.log(Level.FINEST, owningPlayer.getName()
            + " Client.cleanupBattle()");

        gui.actOnCleanupBattle();

        battleChits.clear();
        battlePhase = null;
        battleTurnNumber = -1;
        battleActivePlayer = noone;
    }

    public int[] getReinforcementTurns()
    {
        int[] reinforcementTurns = { 4 };
        return reinforcementTurns;
    }

    public int getMaxBattleTurns()
    {
        return 7;
    }

    public void nextEngagement()
    {
        gui.highlightEngagements();
        if (isMyTurn())
        {
            if (options.getOption(Options.autoPickEngagements))
            {
                aiPause();
                MasterHex hex = ai.pickEngagement();
                if (hex != null)
                {
                    engage(hex);
                }
                else
                {
                    doneWithEngagements();
                }
            }
            else
            {
                gui.defaultCursor();
            }

            gui.actOnNextEngagement();
        }
    }

    /** Used for human players only.  */
    public void doRecruit(Legion legion)
    {
        if (isMyTurn() && isMyLegion(legion)
            && ((LegionClientSide)legion).hasRecruited())
        {
            gui.undoRecruit(legion);
            return;
        }

        if (legion == null || !((LegionClientSide)legion).canRecruit()
            || !isMyTurn() || !isMyLegion(legion))
        {
            return;
        }

        String hexDescription = legion.getCurrentHex().getDescription();

        String recruitName = gui.doPickRecruit(legion, hexDescription);

        if (recruitName == null)
        {
            return;
        }

        String recruiterName = findRecruiterName(legion, recruitName,
            hexDescription);
        if (recruiterName == null)
        {
            return;
        }

        doRecruit(legion, recruitName, recruiterName);
    }

    // TODO use CreatureType instead of String
    public void doRecruit(Legion legion, String recruitName,
        String recruiterName)
    {
        // Call server even if some arguments are null, to get past
        // reinforcement.
        server.doRecruit(legion, recruitName, recruiterName);
    }

    /** Always needs to call server.doRecruit(), even if no recruit is
     *  wanted, to get past the reinforcing phase. */
    public void doReinforce(Legion legion)
    {
        if (options.getOption(Options.autoReinforce))
        {
            ai.reinforce(legion);
        }
        else
        {
            String hexDescription = legion.getCurrentHex().getDescription();

            String recruitName = gui.doPickRecruit(legion, hexDescription);

            String recruiterName = null;
            if (recruitName != null)
            {
                recruiterName = findRecruiterName(legion, recruitName,
                    hexDescription);
            }
            doRecruit(legion, recruitName, recruiterName);
        }
    }

    public void didRecruit(Legion legion, String recruitName,
        String recruiterName, int numRecruiters)
    {
        if (isMyLegion(legion))
        {
            gui.pushUndoStack(legion.getMarkerId());
        }

        List<String> recruiters = new ArrayList<String>();
        if (numRecruiters >= 1 && recruiterName != null)
        {
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiters.add(recruiterName);
            }
            revealCreatures(legion, recruiters, Constants.reasonRecruiter);
        }
        String reason = (battleSite != null ? Constants.reasonReinforced
            : Constants.reasonRecruited);

        addCreature(legion, recruitName, reason);
        ((LegionClientSide)legion).setRecruitName(recruitName);

        gui.actOnDidRecruit(legion, recruitName, recruiters, reason);

    }

    public void undidRecruit(Legion legion, String recruitName)
    {
        boolean wasReinforcement;
        if (battlePhase != null)
        {
            wasReinforcement = true;
            gui.eventViewerCancelReinforcement(recruitName, getTurnNumber());
        }
        else
        {
            // normal undoRecruit
            wasReinforcement = false;
            ((LegionClientSide)legion).removeCreature(recruitName);
        }

        ((LegionClientSide)legion).setRecruitName(null);
        gui.actOnUndidRecruitPart2(legion, wasReinforcement, turnNumber);
    }

    /** null means cancel.  "none" means no recruiter (tower creature). */
    private String findRecruiterName(Legion legion, String recruitName,
        String hexDescription)
    {
        String recruiterName = null;

        List<String> recruiters = findEligibleRecruiters(legion, recruitName);

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiterName = "none";
        }
        else if (options.getOption(Options.autoPickRecruiter)
            || numEligibleRecruiters == 1)
        {
            // If there's only one possible recruiter, or if
            // the user has chosen the autoPickRecruiter option,
            // then just reveal the first possible recruiter.
            recruiterName = recruiters.get(0);
        }
        else
        {
            recruiterName = gui.doPickRecruiter(recruiters, hexDescription,
                legion);
        }
        return recruiterName;
    }

    /** Needed if we load a game outside the split phase, where
     *  active player and turn are usually set. */
    public void setupTurnState(Player activePlayer, int turnNumber)
    {
        this.activePlayer = (PlayerClientSide)activePlayer;
        this.turnNumber = turnNumber;

        gui
            .turnOrPlayerChange(this, turnNumber, this.activePlayer
                .getNumber());

    }

    private void resetAllMoves()
    {
        for (PlayerClientSide player : players)
        {
            for (LegionClientSide legion : player.getLegions())
            {
                legion.setMoved(false);
                legion.setTeleported(false);
                legion.setRecruitName(null);
            }
        }
    }

    public void setBoardActive(boolean val)
    {
        gui.setBoardActive(val);
    }

    public void setupSplit(Player activePlayer, int turnNumber)
    {
        this.activePlayer = (PlayerClientSide)activePlayer;
        this.turnNumber = turnNumber;

        gui.actOnSetupSplit(this, turnNumber, this.activePlayer.getNumber());

        numSplitsThisTurn = 0;
        this.phase = Constants.Phase.SPLIT;

        resetAllMoves();

        gui.actOnSetupSplitPart2();

        kickSplit();
    }

    public boolean onlyAIsRemain()
    {
        for (Player p : players)
        {
            if (!p.isAI() && !p.isDead())
            {
                return false;
            }
        }
        return true;
    }

    private void kickSplit()
    {
        if (isMyTurn() && options.getOption(Options.autoSplit)
            && !isGameOver())
        {
            boolean done = ai.split();
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    public void setupMove()
    {
        this.phase = Constants.Phase.MOVE;

        gui.actOnSetupMove();
    }

    public void setupFight()
    {
        this.phase = Constants.Phase.FIGHT;

        gui.actOnSetupFight();

        if (isMyTurn())
        {
            gui.defaultCursor();
            if (options.getOption(Options.autoPickEngagements))
            {
                aiPause();
                ai.pickEngagement();
            }
            else
            {
                if (findEngagements().isEmpty())
                {
                    doneWithEngagements();
                }
            }
        }
    }

    public void setupMuster()
    {
        this.phase = Constants.Phase.MUSTER;

        gui.actOnSetupMuster();

        // I changed the "&& !isGameOver()" to "&& I am not dead";
        // before, this makes autorecruit stop working also for human
        // when they won against all others and continue playing
        // (just for growing bigger creatures ;-)
        if (options.getOption(Options.autoRecruit) && playerAlive
            && isMyTurn() && this.phase == Constants.Phase.MUSTER)
        {
            ai.muster();
            // Do not automatically say we are done.
            // Allow humans to override.
            if (options.getOption(Options.autoPlay))
            {
                doneWithRecruits();
            }
        }
    }

    public void setupBattleSummon(Player battleActivePlayer,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.BattlePhase.SUMMON;
        setBattleActivePlayer(battleActivePlayer);
        this.battleTurnNumber = battleTurnNumber;

        gui.actOnSetupBattleSummon();

    }

    public void setupBattleRecruit(Player battleActivePlayer,
        int battleTurnNumber)
    {
        this.battlePhase = Constants.BattlePhase.RECRUIT;
        setBattleActivePlayer(battleActivePlayer);
        this.battleTurnNumber = battleTurnNumber;

        gui.actOnSetupBattleRecruit();

    }

    private void resetAllBattleMoves()
    {
        for (BattleChit chit : battleChits)
        {
            chit.setMoved(false);
            chit.setStruck(false);
        }
    }

    public void setupBattleMove(Player battleActivePlayer, int battleTurnNumber)
    {
        setBattleActivePlayer(battleActivePlayer);
        this.battleTurnNumber = battleTurnNumber;

        // Just in case the other player started the battle
        // really quickly.
        gui.cleanupNegotiationDialogs();
        resetAllBattleMoves();
        this.battlePhase = Constants.BattlePhase.MOVE;

        gui.actOnSetupBattleMove();

        if (isMyBattlePhase() && options.getOption(Options.autoPlay))
        {
            bestMoveOrder = ai.battleMove();
            failedBattleMoves = new ArrayList<CritterMove>();
            kickBattleMove();
        }
    }

    private void kickBattleMove()
    {
        if (bestMoveOrder == null || bestMoveOrder.isEmpty())
        {
            if (failedBattleMoves == null || failedBattleMoves.isEmpty())
            {
                doneWithBattleMoves();
            }
            else
            {
                retryFailedBattleMoves();
            }
        }
        else
        {
            CritterMove cm = bestMoveOrder.get(0);
            tryBattleMove(cm);
        }
    }

    public void tryBattleMove(CritterMove cm)
    {
        BattleChit critter = cm.getCritter();
        String hexLabel = cm.getEndingHexLabel();
        doBattleMove(critter.getTag(), hexLabel);
        aiPause();
    }

    private void retryFailedBattleMoves()
    {
        bestMoveOrder = failedBattleMoves;
        failedBattleMoves = null;
        ai.retryFailedBattleMoves(bestMoveOrder);
        kickBattleMove();
    }

    /** Used for both strike and strikeback. */
    public void setupBattleFight(Constants.BattlePhase battlePhase,
        Player battleActivePlayer)
    {
        this.battlePhase = battlePhase;
        setBattleActivePlayer(battleActivePlayer);
        if (battlePhase == Constants.BattlePhase.FIGHT)
        {
            markOffboardCreaturesDead();
        }

        gui.actOnSetupBattleFight(battlePhase, battleTurnNumber);

        doAutoStrikes();
    }

    /** Create marker if necessary, and place it in hexLabel. */
    public void tellLegionLocation(Legion legion, MasterHex hex)
    {
        legion.setCurrentHex(hex);

        gui.actOnTellLegionLocation(legion, hex);
    }

    public Constants.PlayerColor getColor()
    {
        return color;
    }

    public String getShortColor()
    {
        return color.getShortName();
    }

    // public for RevealEvent
    public String getShortColor(int playerNum)
    {
        PlayerClientSide player = getPlayer(playerNum);
        return player.getShortColor();
    }

    // TODO this would probably work better as state in PlayerState
    // TODO Comment Clemens I think this would be better a variable
    //      in GameClientSide...
    public Player getBattleActivePlayer()
    {
        return battleActivePlayer;
    }

    void setBattleActivePlayer(Player player)
    {
        this.battleActivePlayer = player;
    }

    Legion getBattleActiveLegion()
    {
        if (battleActivePlayer.equals(defender.getPlayer()))
        {
            return defender;
        }
        else
        {
            return attacker;
        }
    }

    Legion getBattleInactiveLegion()
    {
        if (battleActivePlayer.equals(defender.getPlayer()))
        {
            return attacker;
        }
        else
        {
            return defender;
        }
    }

    // public for IOracle
    public Legion getDefender()
    {
        return defender;
    }

    // public for IOracle
    public Legion getAttacker()
    {
        return attacker;
    }

    public Constants.BattlePhase getBattlePhase()
    {
        return battlePhase;
    }

    // public for IOracle
    public String getBattlePhaseName()
    {
        if (phase == Constants.Phase.FIGHT)
        {
            if (battlePhase != null)
            {
                return battlePhase.toString();
            }
        }
        return "";
    }

    // public for IOracle and BattleBoard
    public int getBattleTurnNumber()
    {
        return battleTurnNumber;
    }

    public void doBattleMove(int tag, String hexLabel)
    {
        server.doBattleMove(tag, hexLabel);
    }

    private void markBattleMoveSuccessful(int tag, String endingHexLabel)
    {
        if (bestMoveOrder != null)
        {
            Iterator<CritterMove> it = bestMoveOrder.iterator();
            while (it.hasNext())
            {
                CritterMove cm = it.next();
                if (tag == cm.getTag()
                    && endingHexLabel.equals(cm.getEndingHexLabel()))
                {
                    // Remove this CritterMove from the list to show
                    // that it doesn't need to be retried.
                    it.remove();
                }
            }
        }
        kickBattleMove();
    }

    private void handleFailedBattleMove()
    {
        LOGGER.log(Level.FINEST, owningPlayer.getName()
            + "handleFailedBattleMove");
        if (bestMoveOrder != null)
        {
            Iterator<CritterMove> it = bestMoveOrder.iterator();
            if (it.hasNext())
            {
                CritterMove cm = it.next();
                it.remove();
                if (failedBattleMoves != null)
                {
                    failedBattleMoves.add(cm);
                }
            }
        }
        kickBattleMove();
    }

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo)
    {
        if (isMyCritter(tag) && !undo)
        {
            gui.pushUndoStack(endingHexLabel);
            if (options.getOption(Options.autoPlay))
            {
                markBattleMoveSuccessful(tag, endingHexLabel);
            }
        }
        BattleChit chit = getBattleChit(tag);
        if (chit != null)
        {
            chit.setHexLabel(endingHexLabel);
            chit.setMoved(!undo);
        }

        gui.actOnTellBattleMove(startingHexLabel, endingHexLabel);

    }

    /** Attempt to have critter tag strike the critter in hexLabel. */
    public void strike(int tag, String hexLabel)
    {
        resetStrikeNumbers();
        server.strike(tag, hexLabel);
    }

    /** Attempt to apply carries to the critter in hexLabel. */
    public void applyCarries(String hexLabel)
    {
        server.applyCarries(hexLabel);
        gui.actOnAppliyCarries(hexLabel);
    }

    // public for IOracle
    public MasterHex getBattleSite()
    {
        return battleSite;
    }

    /** Return true if there are any enemies adjacent to this chit.
     *  Dead critters count as being in contact only if countDead is true. */
    public boolean isInContact(BattleChit chit, boolean countDead)
    {
        BattleHex hex = getBattleHex(chit);

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
                    if (other != null
                        && (other.isInverted() != chit.isInverted())
                        && (countDead || !other.isDead()))
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
        return battleActivePlayer.equals(getPlayerByTag(chit.getTag()));
    }

    /** Return a set of hexLabels. */
    public Set<String> findMobileCritterHexes()
    {
        Set<String> set = new HashSet<String>();
        for (BattleChit chit : getActiveBattleChits())
        {
            if (!chit.hasMoved() && !isInContact(chit, false))
            {
                set.add(chit.getCurrentHexLabel());
            }
        }
        return set;
    }

    /** Return a set of BattleChits. */
    public Set<BattleChit> findMobileBattleChits()
    {
        Set<BattleChit> set = new HashSet<BattleChit>();
        for (BattleChit chit : getActiveBattleChits())
        {
            if (!chit.hasMoved() && !isInContact(chit, false))
            {
                set.add(chit);
            }
        }
        return set;
    }

    /** Return a set of hexLabels. */
    public Set<String> showBattleMoves(int tag)
    {
        return battleMovement.showMoves(tag);
    }

    /** Return a set of hexLabels. */
    public Set<String> findCrittersWithTargets()
    {
        return strike.findCrittersWithTargets();
    }

    /** Return a set of hexLabels. */
    public Set<String> findStrikes(int tag)
    {
        return strike.findStrikes(tag);
    }

    // TODO GUI stuff?
    public void setStrikeNumbers(int tag, Set<String> targetHexLabels)
    {
        BattleChit chit = getBattleChit(tag);
        for (String targetHexLabel : targetHexLabels)
        {
            BattleChit target = getBattleChit(targetHexLabel);
            target.setStrikeNumber(strike.getStrikeNumber(chit, target));
            // TODO this whole block of code was written under the assumption
            //      that the Strike.getDice(BattleChit,BattleChit,int) method
            //      (now deleted) would return a new baseDice value through the
            //      third parameter. Java's parameters are CallByValue and do
            //      not allow OUT parameters, thus the whole code does nothing
            //      at all.
            //
            // CreatureType striker = game.getVariant().getCreatureByName(
            //     chit.getCreatureName());
            // int dice;
            // if (striker.isTitan())
            // {
            //     dice = chit.getTitanPower();
            // }
            // else
            // {
            //     dice = striker.getPower();
            // }
            // int baseDice = 0;
            // int strikeDice = strike.getDice(chit, target, baseDice);
            // if (baseDice == dice
            //     || options.getOption(Options.showDiceAjustmentsRange))
            // {
            //     target.setStrikeDice(strikeDice - dice);
            // }
        }
    }

    /** reset all strike numbers on chits */
    public void resetStrikeNumbers()
    {
        for (BattleChit chit : battleChits)
        {
            chit.setStrikeNumber(0);
            chit.setStrikeDice(0);
        }
    }

    public Player getPlayerByTag(int tag)
    {
        BattleChit chit = getBattleChit(tag);
        assert chit != null : "Illegal value for tag parameter";

        if (chit.isInverted())
        {
            return defender.getPlayer();
        }
        else
        {
            return attacker.getPlayer();
        }
    }

    boolean isMyCritter(int tag)
    {
        return (owningPlayer.equals(getPlayerByTag(tag)));
    }

    // TODO active or not would probably work better as state in PlayerState
    public PlayerClientSide getActivePlayer()
    {
        return activePlayer;
    }

    public Constants.Phase getPhase()
    {
        return phase;
    }

    // public for IOracle
    public String getPhaseName()
    {
        if (phase != null)
        {
            return phase.toString();
        }
        return "";
    }

    // public for IOracle
    public int getTurnNumber()
    {
        return turnNumber;
    }

    private String figureTeleportingLord(LegionClientSide legion, MasterHex hex)
    {
        List<String> lords = listTeleportingLords(legion, hex);
        String lordName = null;
        switch (lords.size())
        {
            case 0:
                return null;

            case 1:
                lordName = lords.get(0);
                if (lordName.startsWith(Constants.titan))
                {
                    lordName = Constants.titan;
                }
                return lordName;

            default:
                if (options.getOption(Options.autoPickLord))
                {
                    lordName = lords.get(0);
                    if (lordName.startsWith(Constants.titan))
                    {
                        lordName = Constants.titan;
                    }
                    return lordName;
                }
                else
                {
                    return gui.doPickLord(lords);
                }
        }
    }

    /** List the lords eligible to teleport this legion to hexLabel,
     *  as strings.
     *
     *  TODO return value should be List<Creature> or List<CreatureType>
     */
    private List<String> listTeleportingLords(LegionClientSide legion,
        MasterHex hex)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List<String> lords = new ArrayList<String>();

        // Titan teleport
        List<LegionClientSide> legions = getLegionsByHex(hex);
        if (!legions.isEmpty())
        {
            Legion legion0 = legions.get(0);
            if (legion0 != null && !isMyLegion(legion0) && legion.hasTitan())
            {
                lords.add(legion.getTitanBasename());
            }
        }

        // Tower teleport
        else
        {
            for (String name : legion.getContents())
            {
                CreatureType creature = game.getVariant().getCreatureByName(
                    name);
                if (creature != null && creature.isLord()
                    && !lords.contains(name))
                {
                    if (creature.isTitan())
                    {
                        lords.add(legion.getTitanBasename());
                    }
                    else
                    {
                        lords.add(name);
                    }
                }
            }
        }
        return lords;
    }

    public boolean doMove(MasterHex hex)
    {
        return doMove(mover, hex);
    }

    /** Return true if the move looks legal. */
    public boolean doMove(LegionClientSide mover, MasterHex hex)
    {
        if (mover == null)
        {
            return false;
        }

        boolean teleport = false;

        Set<MasterHex> teleports = listTeleportMoves(mover);
        Set<MasterHex> normals = listNormalMoves(mover);
        if (teleports.contains(hex) && normals.contains(hex))
        {
            teleport = chooseWhetherToTeleport(hex);
        }
        else if (teleports.contains(hex))
        {
            teleport = true;
        }
        else if (normals.contains(hex))
        {
            teleport = false;
        }
        else
        {
            return false;
        }

        Set<Constants.EntrySide> entrySides = listPossibleEntrySides(mover,
            hex, teleport);

        Constants.EntrySide entrySide = null;
        if (options.getOption(Options.autoPickEntrySide))
        {
            entrySide = ai.pickEntrySide(hex, mover, entrySides);
        }
        else
        {
            entrySide = gui.doPickEntrySide(hex, entrySides);
        }

        String teleportingLord = null;
        if (teleport)
        {
            teleportingLord = figureTeleportingLord(mover, hex);
        }

        // if this hex is already occupied, return false
        int friendlyLegions = getFriendlyLegions(hex, getActivePlayer())
            .size();
        if (hex.equals(mover.getCurrentHex()))
        {
            // same hex as starting hex, but it might be occupied by
            // multiple legions after split
            if (friendlyLegions > 1)
            {
                return false;
            }
        }
        else
        {
            if (friendlyLegions > 0)
            {
                return false;
            }
        }

        server.doMove(mover, hex, entrySide, teleport, teleportingLord);
        return true;
    }

    public void didMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, Constants.EntrySide entrySide, boolean teleport,
        String teleportingLord, boolean splitLegionHasForcedMove)
    {
        if (isMyLegion(legion))
        {
            gui.pushUndoStack(legion.getMarkerId());
        }
        legion.setCurrentHex(currentHex);
        legion.setMoved(true);
        legion.setEntrySide(entrySide);

        if (teleport)
        {
            legion.setTeleported(true);
        }

        gui.actOnDidMove(legion, startingHex, currentHex, teleport,
            teleportingLord, splitLegionHasForcedMove);

        kickMoves();
    }

    public void undidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove)
    {
        ((LegionClientSide)legion).setRecruitName(null);
        legion.setCurrentHex(currentHex);
        legion.setMoved(false);
        boolean didTeleport = legion.hasTeleported();
        legion.setTeleported(false);

        gui.actOnUndidMove(legion, formerHex, currentHex,
            splitLegionHasForcedMove, didTeleport);

    }

    /*
     * Reset the cached reservations.
     * Should be called at begin of each recruit turn, if
     * reserveRecruit and getReservedCount() are going to be used.
     *
     */
    public void resetRecruitReservations()
    {
        recruitReservations.clear();
    }

    /*
     * Reserve one. Expects that getReservedCount() had been called in this
     * turn for same creature before called reserveRecruit (= to cache the
     * caretakers stack value).
     * Returns whether creature can still be recruited (=is available according
     * to caretakers stack plus reservations)
     */
    public boolean reserveRecruit(CreatureType recruitType)
    {
        boolean ok = false;
        int remain;

        Integer count = recruitReservations.get(recruitType);
        if (count != null)
        {
            remain = count.intValue();
            recruitReservations.remove(recruitType);
        }
        else
        {
            LOGGER.log(Level.WARNING, owningPlayer.getName()
                + " reserveRecruit creature " + recruitType
                + " not fround from hash, should have been created"
                + " during getReservedCount!");
            remain = getGame().getCaretaker().getAvailableCount(recruitType);
        }

        if (remain > 0)
        {
            remain--;
            ok = true;
        }

        recruitReservations.put(recruitType, Integer.valueOf(remain));
        return ok;
    }

    /*
     * On first call (during a turn), cache remaining count from recruiter,
     * decrement on each further reserve for this creature.
     * This way we are independent of when the changes which are triggered by
     * didRecruit influence the caretaker Stack.
     * Returns how many creatures can still be recruited (=according
     * to caretaker's stack plus reservations)
     */
    public int getReservedRemain(CreatureType recruitType)
    {
        assert recruitType != null : "Can not reserve recruit for null";
        int remain;

        Integer count = recruitReservations.get(recruitType);
        if (count == null)
        {
            remain = getGame().getCaretaker().getAvailableCount(recruitType);
        }
        else
        {
            remain = count.intValue();
            recruitReservations.remove(recruitType);
        }

        // in case someone called getReservedRemain with bypassing the
        // reset or reserve methods, to be sure double check against the
        // real remaining value.
        int realCount = getGame().getCaretaker()
            .getAvailableCount(recruitType);
        if (realCount < remain)
        {
            remain = realCount;
        }
        recruitReservations.put(recruitType, Integer.valueOf(remain));

        return remain;
    }

    /** Return a list of Creatures (ignore reservations).
     *
     * TODO the extra hexLabel parameter is probably not needed anymore
     */
    public List<CreatureType> findEligibleRecruits(Legion legion, MasterHex hex)
    {
        return findEligibleRecruits(legion, hex, false);
    }

    /** Return a list of Creatures. Consider reservations if wanted
     *
     * TODO the extra hexLabel parameter is probably not needed anymore
     */
    public List<CreatureType> findEligibleRecruits(Legion legion,
        MasterHex hex, boolean considerReservations)
    {
        assert hex != null : "Null hex given to find recruits in";

        List<CreatureType> recruits = new ArrayList<CreatureType>();

        if (legion == null)
        {
            return recruits;
        }

        MasterBoardTerrain terrain = hex.getTerrain();

        List<CreatureType> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, hex);
        List<CreatureType> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hex);

        Iterator<CreatureType> lit = tempRecruits.iterator();
        while (lit.hasNext())
        {
            CreatureType creature = lit.next();
            Iterator<CreatureType> liter = recruiters.iterator();
            while (liter.hasNext())
            {
                CreatureType lesser = liter.next();
                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                    creature, terrain, hex) <= ((LegionClientSide)legion)
                    .numCreature(lesser))
                    && (recruits.indexOf(creature) == -1))
                {
                    recruits.add(creature);
                }
            }
        }

        // Make sure that the potential recruits are available.
        Iterator<CreatureType> it = recruits.iterator();
        while (it.hasNext())
        {
            CreatureType recruit = it.next();
            int remaining = getGame().getCaretaker()
                .getAvailableCount(recruit);

            if (remaining > 0 && considerReservations)
            {
                remaining = getReservedRemain(recruit);
            }
            if (remaining < 1)
            {
                it.remove();
            }
        }

        return recruits;
    }

    /**
     * Return a list of creature name strings.
     *
     * TODO return List<CreatureType>
     */
    public List<String> findEligibleRecruiters(Legion legion,
        String recruitName)
    {
        Set<CreatureType> recruiters;
        CreatureType recruit = game.getVariant()
            .getCreatureByName(recruitName);
        if (recruit == null)
        {
            return new ArrayList<String>();
        }

        MasterHex hex = legion.getCurrentHex();
        MasterBoardTerrain terrain = hex.getTerrain();

        recruiters = new HashSet<CreatureType>(TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hex));
        Iterator<CreatureType> it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType possibleRecruiter = it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain, hex);
            if (needed < 1
                || needed > ((LegionClientSide)legion)
                    .numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }

        List<String> strings = new ArrayList<String>();
        it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType creature = it.next();
            strings.add(creature.getName());
        }
        return strings;
    }

    /** Return a set of hexLabels. */
    public Set<MasterHex> getPossibleRecruitHexes()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();

        for (Legion legion : activePlayer.getLegions())
        {
            if (((LegionClientSide)legion).canRecruit())
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /** Return a set of all other unengaged legions of the legion's player
     *  that have summonables.
     */
    public SortedSet<Legion> findLegionsWithSummonables(Legion summoner)
    {
        SortedSet<Legion> result = new TreeSet<Legion>();
        Player player = summoner.getPlayer();
        for (Legion legion : player.getLegions())
        {
            if (!legion.equals(summoner))
            {
                if (legion.hasSummonable()
                    && !(((LegionClientSide)legion).isEngaged()))
                {
                    result.add(legion);
                }
            }
        }
        return result;
    }

    public Movement getMovement()
    {
        return movement;
    }

    public Strike getStrike()
    {
        return strike;
    }

    /** Return a set of hexLabels. */
    public Set<MasterHex> listTeleportMoves(Legion legion)
    {
        MasterHex hex = legion.getCurrentHex();
        return movement.listTeleportMoves(legion, hex, movementRoll);
    }

    /** Return a set of hexLabels. */
    public Set<MasterHex> listNormalMoves(LegionClientSide legion)
    {
        return movement.listNormalMoves(legion, legion.getCurrentHex(),
            movementRoll);
    }

    public Set<Constants.EntrySide> listPossibleEntrySides(
        LegionClientSide mover, MasterHex hex, boolean teleport)
    {
        return movement.listPossibleEntrySides(mover, hex, teleport);
    }

    public List<LegionClientSide> getLegionsByHex(MasterHex hex)
    {
        assert hex != null : "No hex given to find legions on.";
        List<LegionClientSide> legions = new ArrayList<LegionClientSide>();
        for (PlayerClientSide player : players)
        {
            for (LegionClientSide legion : player.getLegions())
            {
                if (hex.equals(legion.getCurrentHex()))
                {
                    legions.add(legion);
                }
            }
        }
        return legions;
    }

    public Set<MasterHex> findUnmovedLegionHexes()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();
        for (Legion legion : activePlayer.getLegions())
        {
            if (!legion.hasMoved())
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /** Return a set of hexLabels for the active player's legions with
     *  7 or more creatures. */
    public Set<MasterHex> findTallLegionHexes()
    {
        return findTallLegionHexes(7);
    }

    /** Return a set of hexLabels for the active player's legions with
     *  minHeight or more creatures. */
    public Set<MasterHex> findTallLegionHexes(int minHeight)
    {
        Set<MasterHex> result = new HashSet<MasterHex>();

        for (Legion legion : activePlayer.getLegions())
        {
            if (legion.getHeight() >= minHeight)
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /**
     * Return a set of all hexes with engagements.
     *
     * TODO if we can be sure that the activePlayer is set properly, we could
     *      just create a set of all hexes he is on and then check if someone
     *      else occupies any of the same
     */
    public Set<MasterHex> findEngagements()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();
        Map<MasterHex, Player> playersOnHex = new HashMap<MasterHex, Player>();
        for (Player player : players)
        {
            for (Legion legion : player.getLegions())
            {
                MasterHex hex = legion.getCurrentHex();
                if (playersOnHex.get(hex) == null)
                {
                    // no player on that hex found yet, set this one
                    playersOnHex.put(hex, player);
                }
                else
                {
                    if (!playersOnHex.get(hex).equals(player))
                    {
                        // someone else already on the hex -> engagement
                        result.add(hex);
                    }
                }
            }
        }
        return result;
    }

    boolean isOccupied(MasterHex hex)
    {
        return !getLegionsByHex(hex).isEmpty();
    }

    boolean isEngagement(MasterHex hex)
    {
        List<LegionClientSide> legions = getLegionsByHex(hex);
        if (legions.size() == 2)
        {
            Legion info0 = legions.get(0);
            Player player0 = info0.getPlayer();

            Legion info1 = legions.get(1);
            Player player1 = info1.getPlayer();

            return !player0.equals(player1);
        }
        return false;
    }

    public List<Legion> getEnemyLegions(final Player player)
    {
        List<Legion> result = new ArrayList<Legion>();
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                result.addAll(otherPlayer.getLegions());
            }
        }
        return result;
    }

    public List<Legion> getEnemyLegions(final MasterHex hex,
        final Player player)
    {
        List<Legion> result = new ArrayList<Legion>();
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                for (Legion legion : otherPlayer.getLegions())
                {
                    if (legion.getCurrentHex().equals(hex))
                    {
                        result.add(legion);
                    }
                }
            }
        }
        return result;
    }

    public Legion getFirstEnemyLegion(MasterHex hex, Player player)
    {
        for (Player otherPlayer : players)
        {
            if (!otherPlayer.equals(player))
            {
                for (Legion legion : otherPlayer.getLegions())
                {
                    if (legion.getCurrentHex().equals(hex))
                    {
                        return legion;
                    }
                }
            }
        }
        return null;
    }

    public List<LegionClientSide> getFriendlyLegions(final MasterHex hex,
        final PlayerClientSide player)
    {
        return CollectionHelper.selectAsList(player.getLegions(),
            new Predicate<LegionClientSide>()
            {
                public boolean matches(LegionClientSide legion)
                {
                    return legion.getCurrentHex().equals(hex);
                }
            });
    }

    public Legion getFirstFriendlyLegion(final MasterHex hex, Player player)
    {
        return CollectionHelper.selectFirst(player.getLegions(),
            new Predicate<Legion>()
            {
                public boolean matches(Legion legion)
                {
                    return legion.getCurrentHex().equals(hex);
                }
            });
    }

    public void notifyServer()
    {
        gui.clearUndoStack();
        if (!isRemote())
        {
            server.stopGame();
        }
        disposeClientOriginated();
    }

    public boolean isSctAlreadyDown()
    {
        return sct.isAlreadyDown();
    }

    public void undidSplit(Legion splitoff, Legion survivor, int turn)
    {
        ((LegionClientSide)survivor).merge(splitoff);
        removeLegion(splitoff);

        // do the eventViewer stuff before the board, so we are sure to get
        // a repaint.

        if (!replayOngoing)
        {
            gui.eventViewerUndoEvent(splitoff, survivor, turn);
        }

        gui.boardActOnUndidSplit(survivor, turn);

        if (isMyTurn() && this.phase == Constants.Phase.SPLIT
            && !replayOngoing && options.getOption(Options.autoSplit)
            && !isGameOver())
        {
            boolean done = ai.splitCallback(null, null);
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    // TODO HACK to reduce work...

    public IServer getServer()
    {
        return server;
    }

    /**
     * Finishes the current phase.
     *
     * Depending on the current phase this method dispatches to
     * the different done methods.
     *
     * @see Client#doneWithSplits()
     * @see Client#doneWithMoves()
     * @see Client#doneWithEngagements()()
     * @see Client#doneWithRecruits()()
     */
    public void doneWithPhase()
    {
        if (phase == Constants.Phase.SPLIT)
        {
            doneWithSplits();
        }
        else if (phase == Constants.Phase.MOVE)
        {
            doneWithMoves();
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            doneWithEngagements();
        }
        else if (phase == Constants.Phase.MUSTER)
        {
            doneWithRecruits();
        }
        else
        {
            throw new IllegalStateException("Client has unknown phase value");
        }
    }

    public void doneWithSplits()
    {
        if (!isMyTurn())
        {
            return;
        }
        server.doneWithSplits();

        gui.actOnDoneWithSplits();

    }

    void doneWithMoves()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();

        gui.actOnDoneWithMoves();
        server.doneWithMoves();
    }

    void doneWithEngagements()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();
        server.doneWithEngagements();
    }

    public void doneWithRecruits()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();
        server.doneWithRecruits();
    }

    public PlayerClientSide getPlayerByMarkerId(String markerId)
    {
        assert markerId != null : "Parameter must not be null";

        String shortColor = markerId.substring(0, 2);
        return getPlayerUsingColor(shortColor);
    }

    private PlayerClientSide getPlayerUsingColor(String shortColor)
    {
        assert this.players != null : "Client not yet initialized";
        assert shortColor != null : "Parameter must not be null";

        // Stage 1: See if the player who started with this color is alive.
        for (PlayerClientSide info : players)
        {
            if (shortColor.equals(info.getShortColor()) && !info.isDead())
            {
                return info;
            }
        }

        // Stage 2: He's dead.  Find who killed him and see if he's alive.
        for (PlayerClientSide info : players)
        {
            if (info.getPlayersElim().indexOf(shortColor) != -1)
            {
                // We have the killer.
                if (!info.isDead())
                {
                    return info;
                }
                else
                {
                    return getPlayerUsingColor(info.getShortColor());
                }
            }
        }
        return null;
    }

    public boolean isMyLegion(String markerId)
    {
        return owningPlayer.equals(getPlayerByMarkerId(markerId));
    }

    public boolean isMyLegion(Legion legion)
    {
        return owningPlayer.equals(legion.getPlayer());
    }

    public boolean isMyTurn()
    {
        return owningPlayer.equals(getActivePlayer());
    }

    public boolean isMyBattlePhase()
    {
        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet
        return playerAlive && owningPlayer.equals(getBattleActivePlayer())
            && this.phase == Constants.Phase.FIGHT;
    }

    public int getMovementRoll()
    {
        return movementRoll;
    }

    // TODO inline
    public int getMulligansLeft()
    {
        return owningPlayer.getMulligansLeft();
    }

    public void doSplit(Legion legion)
    {
        LOGGER.log(Level.FINER, "Client.doSplit " + legion);
        this.parent = null;

        if (!isMyTurn())
        {
            LOGGER.log(Level.SEVERE, "Not my turn!");
            kickSplit();
            return;
        }
        // Can't split other players' legions.
        if (!isMyLegion(legion))
        {
            LOGGER.log(Level.SEVERE, "Not my legion!");
            kickSplit();
            return;
        }
        Set<String> markersAvailable = getOwningPlayer().getMarkersAvailable();
        // Need a legion marker to split.
        if (markersAvailable.size() < 1)
        {
            gui.showMessageDialog("No legion markers");
            kickSplit();
            return;
        }
        // Legion must be tall enough to split.
        if (legion.getHeight() < 4)
        {
            gui.showMessageDialog("Legion is too short to split");
            kickSplit();
            return;
        }

        // TODO Questionable: why does this depend on numSplitsThisTurn?
        //      is that modified at all when there is no GUI ?
        // Enforce only one split on turn 1.
        if (getTurnNumber() == 1 && numSplitsThisTurn > 0)
        {
            gui.showMessageDialog("Can only split once on the first turn");
            kickSplit();
            return;
        }

        this.parent = legion;
        String childId = null;

        if (options.getOption(Options.autoPickMarker))
        {
            childId = ai.pickMarker(markersAvailable, getShortColor());
        }
        else
        {
            childId = gui.doPickMarker(markersAvailable);
        }
        pickMarkerCallback(childId);
    }

    // TODO whole method to gui ?

    /** Called after a marker is picked, either first marker or split. */
    void pickMarkerCallback(String childMarker)
    {
        if (childMarker == null)
        {
            return;
        }
        if (parent == null)
        {
            // Picking first marker.
            server.assignFirstMarker(childMarker);
            return;
        }
        String results = gui.doPickSplitLegion(parent, childMarker);

        if (results != null)
        {
            doSplit(parent, childMarker, results);
        }
    }

    /** Called by AI, and by pickMarkerCallback() */
    public void doSplit(Legion parent, String childMarker, String results)
    {
        LOGGER.log(Level.FINER, "Client.doSplit " + parent + " " + childMarker
            + " " + results);
        server.doSplit(parent, childMarker, results);
    }

    /**
     * Callback from server after any successful split.
     *
     * TODO childHeight is probably redundant now that we pass the legion object
     */
    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<String> splitoffs, int turn)
    {
        LOGGER.log(Level.FINEST, "Client.didSplit " + hex + " " + parent + " "
            + child + " " + childHeight + " " + turn);

        ((LegionClientSide)parent).split(childHeight, child, turn);

        child.setCurrentHex(hex);

        gui.actOnDidSplit(turn, parent, child, hex);

        if (isMyLegion(child))
        {
            getOwningPlayer().removeMarkerAvailable(child.getMarkerId());
        }

        numSplitsThisTurn++;

        gui.actOnDidSplitPart2(hex);

        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet.
        if (isMyTurn() && this.phase == Constants.Phase.SPLIT
            && !replayOngoing && options.getOption(Options.autoSplit)
            && !isGameOver())
        {
            boolean done = ai.splitCallback(parent, child);
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    public void askPickColor(List<Constants.PlayerColor> colorsLeft)
    {
        if (options.getOption(Options.autoPickColor))
        {
            // Convert favorite colors from a comma-separated string to a list.
            String favorites = options.getStringOption(Options.favoriteColors);
            List<Constants.PlayerColor> favoriteColors = null;
            if (favorites != null)
            {
                favoriteColors = Constants.PlayerColor.getByName(Split.split(
                    ',', favorites));
            }
            else
            {
                favoriteColors = new ArrayList<Constants.PlayerColor>();
            }
            color = ai.pickColor(colorsLeft, favoriteColors);
        }
        else
        {
            color = gui.doPickColor(owningPlayer.getName(), colorsLeft);
        }
        server.assignColor(color);
    }

    public void askPickFirstMarker()
    {
        String markerId = null;

        Set<String> markersAvailable = getOwningPlayer().getMarkersAvailable();
        if (options.getOption(Options.autoPickMarker))
        {
            markerId = ai.pickMarker(markersAvailable, getShortColor());
        }
        else
        {
            markerId = gui.doPickMarkerUntilGotOne(markersAvailable);
        }
        pickMarkerCallback(markerId);
    }

    public void log(String message)
    {
        LOGGER.log(Level.INFO, message);
    }

    public static String getVersion()
    {
        try
        {
            Properties buildInfo = new Properties();
            ClassLoader cl = Client.class.getClassLoader();
            InputStream is = cl
                .getResourceAsStream("META-INF/build.properties");
            if (is == null)
            {
                LOGGER.log(Level.INFO, "No build information available.");
                return "UNKNOWN";
            }
            buildInfo.load(is);
            return buildInfo.getProperty("svn.revision.max-with-flags");
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "Problem reading build info file", ex);
        }
        return "UNKNOWN";
    }

    public boolean testBattleMove(BattleChit chit, String hexLabel)
    {
        if (showBattleMoves(chit.getTag()).contains(hexLabel))
        {
            chit.setHexLabel(hexLabel);
            return true;
        }
        return false;
    }

    private void setType(final String aType)
    {
        LOGGER.log(Level.FINEST, "Called setType for " + aType);
        String type = aType;
        if (type.endsWith(Constants.anyAI))
        {
            int whichAI = Dice.rollDie(Constants.numAITypes) - 1;
            type = Constants.aiArray[whichAI];
        }
        if (!type.startsWith(Constants.aiPackage))
        {
            if (type.startsWith(Constants.oldAiPackage))
            {
                type = type.replace(Constants.oldAiPackage,
                    Constants.aiPackage);
            }
            else
            {
                type = Constants.aiPackage + type;
            }
        }
        LOGGER.log(Level.FINEST, "final type: " + type);
        if (type.endsWith("AI"))
        {
            LOGGER.log(Level.FINEST, "new type is AI. current ai is "
                + ai.getClass().getName());
            if (!(ai.getClass().getName().equals(type)))
            {
                LOGGER.log(Level.FINEST, "need to change type");
                LOGGER.log(Level.INFO, "Changing client "
                    + owningPlayer.getName() + " from "
                    + ai.getClass().getName() + " to " + type);
                try
                {
                    // TODO these seem to be classes of either AI or Client, there
                    // should be a common ancestor
                    Class<?>[] classArray = new Class<?>[1];
                    classArray[0] = Class
                        .forName("net.sf.colossus.client.Client");
                    Object[] objArray = new Object[1];
                    objArray[0] = this;
                    ai = (AI)Class.forName(type).getDeclaredConstructor(
                        classArray).newInstance(objArray);
                }
                catch (Exception ex)
                {
                    LOGGER.log(Level.SEVERE, "Failed to change client "
                        + owningPlayer.getName() + " from "
                        + ai.getClass().getName() + " to " + type, ex);
                }
            }
        }
    }

    /** Wait for aiDelay. */
    void aiPause()
    {
        if (delay < 0)
        {
            setupDelay();
        }

        try
        {
            Thread.sleep(delay);
        }
        catch (InterruptedException ex)
        {
            LOGGER.log(Level.SEVERE, "Client.aiPause() interrupted", ex);
        }
    }

    private void setupDelay()
    {
        delay = options.getIntOption(Options.aiDelay);
        if (!options.getOption(Options.autoPlay)
            || delay < Constants.MIN_AI_DELAY)
        {
            delay = Constants.MIN_AI_DELAY;
        }
        else if (delay > Constants.MAX_AI_DELAY)
        {
            delay = Constants.MAX_AI_DELAY;
        }
    }

    class MarkerComparator implements Comparator<String>
    {
        public int compare(String s1, String s2)
        {
            String shortColor = "None"; // In case not initialized yet.
            if (color != null)
            {
                shortColor = getShortColor();
            }
            if (s1.startsWith(shortColor) && !s2.startsWith(shortColor))
            {
                return -1;
            }
            if (!s1.startsWith(shortColor) && s2.startsWith(shortColor))
            {
                return 1;
            }
            return s1.compareTo(s2);
        }
    }

    /*
     * Called by ClientGUI, to fulfill the items called in menu 
     */

    public void doSetWhatToDoNext(WhatToDoNext whatToDoNext,
        boolean triggerQuitTimer)
    {
        startObject.setWhatToDoNext(whatToDoNext, triggerQuitTimer);
    }

    public void doSetWhatToDoNext(WhatToDoNext whatToDoNext, String loadFile)
    {
        startObject.setWhatToDoNext(whatToDoNext, loadFile);
    }

    /*
     * ClientGUI needs that when bringing up a new WebClient
     */
    public Start getStartObject()
    {
        return startObject;

    }

    public Game getGame()
    {
        return game;
    }

    public GameClientSide getGameClientSide()
    {
        return (GameClientSide)game;
    }

    public Options getOptions()
    {
        return options;
    }
}
