package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.ai.AI;
import net.sf.colossus.ai.SimpleAI;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.OptionObjectProvider;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Engagement;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.MovementClientSide;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.game.actions.Recruitment;
import net.sf.colossus.game.actions.Summoning;
import net.sf.colossus.gui.ClientGUI;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.GameServerSide;
import net.sf.colossus.server.IServer;
import net.sf.colossus.server.Server;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariant;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Lives on the client side and handles all communication
 *  with the server.  It talks to the Server via the network protocol
 *  and to Client side classes locally, but to all GUI related classes
 *  it should only communicate via ClientGUI class.
 *  There is one client per player.
 *
 *  TODO Handle GUI related issues purely via ClientGUI
 *
 *  TODO All GUI classes should talk to the server purely through
 *  ClientGUI which handles it via the Client.
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
 *  @author David Ripton
 *  @author Romain Dolbeau
 */
@SuppressWarnings("serial")
public final class Client implements IClient, IOracle, IVariant,
    OptionObjectProvider
{
    private static final Logger LOGGER = Logger.getLogger(Client.class
        .getName());

    /**
     *  This "server" is the access to the connector object which actually
     *  acts for us as server.
     *
     *  Right now this is always a SocketClientThread as deputy (relay)
     *  which forwards everything that we do/tell, to the Server.
     *  Perhaps one day this could either be a SocketConnection or e.g.
     *  a Queue type of connection for local Clients...
     */
    private IServer server;

    /** The object that actually handles the physical server communication for
     *  this client. Issues related to set up and tear down of the connection
     *  are handled via this access to the (right now) SocketClientThread.
     */
    private IServerConnection connection;

    /**
     * InactivityWatchdog needs this, to retrigger an event to make
     * the AI kick in
     */
    private EventExecutor eventExecutor;

    /**
     * A first start to get rid of the static-access-everywhere to
     * ResourceLoader.
     * ResourceLoader is used to "load" images, variant files, readme files
     * physically (from disk, or from remote file server thread).
     */
    private final ResourceLoader resourceLoader;

    /** Client constructor sets this to true if something goes wrong with the
     *  SocketClientThread initialization. I wanted to avoid have the Client
     *  constructor throw an exception, because that caused problems in
     *  Java 1.4 with a "created but not run thread" which was then never
     *  cleaned up and thus JVM did not exit by itself.
     *  TODO perhaps that is now fixed in Java 1.5 ?
     *  I plan to change the whole "when/how SCT is created" soon anyway...
     */
    private final boolean failed = false;

    /** Replay during load of a saved game is ongoing. Client must NOT react
     *  (not even redraw) on any of those messages, they are mostly sent to
     *  rebuild the predict split data.
     */
    private boolean replayOngoing = false;

    /**
     * Redo of the events since last commit phase is ongoing.
     * Needed right now only for "if redo ends, set flag to prevent the
     * setupXxxxxPhase methods to clear the undo stack.
     */
    private boolean redoOngoing = false;

    /** This can be an actual ClientGUI, or a NullClientGUI (which does simply
     *  nothing, so that we don't need to check for null everywhere).
     */
    private final IClientGUI gui;

    // Per-client and per-player options.
    private final Options options;

    /** At first time we get "all player info", they are created; at all
     *  later calls just update them. So this flag here tells us whether
     *  it's the first time (=true) or not any more (=false).
     */
    private boolean playersNotInitialized = true;

    /**
     * Player who owns this client.
     *
     * TODO should be final but can't be until the constructor gets all the data
     * needed
     */
    private PlayerClientSide owningPlayer;
    private boolean playerAlive = true;

    private boolean clockIsTicking = false;

    private final Autoplay autoplay;

    /**
     * The game in progress.
     */
    private final GameClientSide game;

    /**
     * Starting marker color of player who owns this client.
     *
     * TODO most likely redundant with owningPlayer.getColor()
     */
    // private PlayerColor color;

    // This ai is either the actual ai player for an AI player, but is also
    // used by human clients for the autoXXX actions.
    private final AI ai;

    // TODO: could go into owningPlayer, BUT tricky right now as long as
    // owningPlayer is created twice (once fake and later for real)...

    private MovementClientSide movement;
    private BattleMovement battleMovement;

    private final Server localServer;

    // This client is a spectator
    private final boolean spectator;

    /**
     *  This client is the very special internal spectator with the name
     * as defined in Constants.
     * The idea of this internal spectator is: it can run in standby in
     * any game, e.g. also on the public server, and detect discrepancies
     * between local state and updateCreatureCount or playerInfo from
     * Server. This is part of the work, to replace "all details need to
     * be broadcasted all the time" with "Client side does the bookkeeping
     * autonomously; so for quite a while it would do bookkeeping and the
     * updates are still sent but just for checking, and any discrepancy
     * detected can/should be fixed.
     */
    private final boolean internalSpectator;

    /**
     * Constants modelling the party who closed this client.
     */
    private enum ClosedByConstant
    {
        NOT_CLOSED, CLOSED_BY_SERVER, CLOSED_BY_CLIENT
    }

    private ClosedByConstant closedBy = ClosedByConstant.NOT_CLOSED;

    private final static int MAX_RECONNECT_ATTEMPTS = 6;
    private final static int RECONNECT_RETRY_INTERVAL = 10;

    // XXX temporary until things are synchronized
    private boolean tookMulligan;

    private int numSplitsThisTurn;

    private int delay = -1;

    /** For battle AI. */
    private List<CritterMove> bestMoveOrder = null;
    private List<CritterMove> failedBattleMoves = null;

    private final Hashtable<CreatureType, Integer> recruitReservations = new Hashtable<CreatureType, Integer>();

    /**
     * Once we got dispose from server (or user initiated it himself),
     * we'll ignore it if we we get it from server again
     * - it's then up to the user to do some "disposing" action.
     */
    private boolean gotDisposeAlready = false;

    private boolean disposeInProgress = false;

    /**
     * Everytime we request server to sync data (typically after reconnect),
     * we pass with a request counter, so that we can distinct the
     * syncCompleted responses.
     */
    private int syncRequestCounter = 0;

    /**
     * Create a Client object and other related objects
     *
     * @param host The host to which SocketClientThread shall connect
     * @param port The port to which SocketClientThread shall connect
     * @param playerName Name of the player (might still be one of the
     *                   <byXXX> templates
     * @param playerType Type of player, e.g. Human, Network, or some
     *                   concrete AI type (but not "AnyAI").
     *                   Given type must include the package name.
     * @param whatNextMgr The main controller over which to handle what to do
     *                    next when this game is over and exiting
     * @param theServer The Server object, if this is a local client
     * @param byWebClient If true, this was instantiated by a WebClient
     * @param noOptionsFile E.g. AIs should not read/save any options file
     * @param createGUI Whether to create a GUI (AI's usually not, but server
     *                  might override that e.g. in stresstest)
     * @param spectator true to join as spectator, false as real player
     *
     */

    public static synchronized Client createClient(String host, int port,
        String playerName, String playerType, WhatNextManager whatNextMgr,
        Server theServer, boolean byWebClient, boolean noOptionsFile,
        boolean createGUI, boolean spectator) throws ConnectionInitException
    {
        /* TODO Clients on same machine could share the instance
         * (proper synchronization needed, of course).
         */
        ResourceLoader loader;
        boolean remote;
        Variant variant;

        if (theServer == null)
        {
            loader = new ResourceLoader(host, port + 1);
            remote = true;
        }
        else
        {
            loader = new ResourceLoader(null, 0);
            remote = false;
        }

        IServerConnection conn = SocketClientThread.createConnection(host,
            port, playerName, remote, spectator);

        // TODO For now, loading the variant is needed only if remote client.
        // ( => theServer is null; theServer != null is the server object in
        // same JVM).
        // One day in future, every client should perhaps do this to get his
        // own instance of the variant (instead of static access).
        // Or local clients get the Variant object passed in to here...

        if (theServer == null)
        {
            String variantName = conn.getVariantNameForInit();
            // enforce loading of variant in any case, so that we get XML
            // files and README from remote server - this way at least if
            // changes were only to xml files, server and client see
            // the same data.
            // Pictures are currently loaded from own jar files in any
            // case; but if a pic is missing, it's merely an empty chit,
            // not data inconsistency which might cause mysterious errors.
            LOGGER.info("Oh, we are a remote client"
                + " - let's make sure we really get text files from server.");

            VariantSupport.unloadVariant();
            variant = VariantSupport.loadVariantByName(variantName, true);
        }
        else
        {
            variant = theServer.getGame().getVariant();
        }

        return new Client(playerName, playerType, whatNextMgr, theServer,
            byWebClient, noOptionsFile, createGUI, loader, conn, variant,
            spectator);
    }

    /**
     * Client is the main hub for info exchange on client side.
     * @param playerName Name of the player (might still be one of the
     *                   <byXXX> templates
     * @param playerType Type of player, e.g. Human, Network, or some
     *                   concrete AI type (but not "AnyAI").
     *                   Given type must include the package name.
     * @param whatNextMgr The main controller over which to handle what to do
     *                    next when this game is over and exiting
     * @param theServer The Server object, if this is a local client
     * @param byWebClient If true, this was instantiated by a WebClient
     * @param noOptionsFile E.g. AIs should not read/save any options file
     * @param createGUI Whether to create a GUI (AI's usually not, but server
     *                  might override that e.g. in stresstest)
     * @param resLoader The ResourceLoader object that gives us access to
     *                  load images, files etc (from disk or from server)
     * @param conn      The connection to server (so far, SocketClientThread)
     * @param variant The variant instance
     * @param spectator true to join as spectator, false as real player
     */

    /* TODO Now Client creates the Game (GameClientSide) instance.
     *      So far it creates it mostly with dummy info; should do better.
     *      - for example, create first SocketClientThread, and as first
     *      answer to connect gets the Variant name, and use that
     *      for game creation. So when Client constructor is completed
     *      also Game and Variant are proper.
     *      (problem would still be ... player count and names...)
     *
     * TODO try to make the Client class agnostic of the network or not question by
     *      having the SCT outside and behaving like a normal server -- that way it
     *      would be easier to run the local clients without the detour across the
     *      network and the serialization/deserialization of all objects
     *
     * TODO Make player type typesafe
     */
    public Client(String playerName, String playerType,
        WhatNextManager whatNextMgr, Server theServer, boolean byWebClient,
        boolean noOptionsFile, boolean createGUI, ResourceLoader resLoader,
        IServerConnection conn, Variant variant, boolean spectator)
    {
        assert playerName != null;
        this.spectator = spectator;
        this.internalSpectator = (playerName
            .equals(Constants.INTERNAL_DUMMY_CLIENT_NAME));

        Collection<String> playerNamesList = conn.getPreliminaryPlayerNames();

        String[] playerNames = new String[playerNamesList.size()];
        int i = 0;
        for (String name : playerNamesList)
        {
            playerNames[i++] = name;
        }

        // TODO can we change all Game constructors to List or Collection?
        // Those Array[]s are annoying...

        // TODO playerNames is still a dummy argument
        this.game = new GameClientSide(variant, playerNames);

        // TODO give it to constructor right away? Not changing it right now,
        // first do the "create SCT and Variant (and Game??) outside Client
        // and pass them in" and see then whether it's better to create the
        // Game outside ( = then we can't give Client to Game constructor)
        // or create Game inside Client (then we can pass in the Client).
        game.setClient(this);

        conn.startThread();

        this.connection = conn;
        connection.setClient(this);

        this.resourceLoader = resLoader;
        LOGGER.finest("Got ResourceLoader: " + resourceLoader.toString());

        // TODO this is currently not set properly straight away, it is fixed
        // in updatePlayerInfo(..) when the PlayerInfos are initialized.
        // Should really happen here, but doesn't yet since we don't have
        // all players (not even as names) yet
        this.owningPlayer = new PlayerClientSide(getGame(), playerName, 0);

        // type setting is needed because we ask owningPlayer.isAI() below
        // TODO set type in constructor
        // (the whole player info setup needs fixing...)
        this.owningPlayer.setType(playerType);

        this.ai = createAI(playerType);

        ViableEntityManager.register(this, "Client " + playerName);
        InstanceTracker.register(this, "Client " + playerName);

        options = new Options(playerName, noOptionsFile);

        if (createGUI)
        {
            this.gui = new ClientGUI(this, options, whatNextMgr);
        }
        else
        {
            this.gui = new NullClientGUI(this, options, whatNextMgr);
        }

        // Need to load options early so they don't overwrite server options.
        options.loadOptions();

        autoplay = new Autoplay(options);

        // This is needed because we do not do syncAutoPlay any more,
        // and many places rely on "if (Options.getOption(... .autoXXXX)
        // returning true because autoPlay was set for AI type players.
        // And it needs to be in place already when the pickColor and
        // pickMarker requests come from server:
        options.setOption(Options.autoPlay, this.owningPlayer.isAI());

        this.server = connection.getIServer();
        if (!spectator)
        {
            server.joinGame(playerName);
        }
        else
        {
            server.watchGame();
        }

        TerrainRecruitLoader.setCaretaker(getGame().getCaretaker());
        CustomRecruitBase.addCaretakerClientSide(getGame().getCaretaker());
        this.localServer = theServer;
        gui.setStartedByWebClient(byWebClient);
    }

    /**
     * Create the AI for this Client. If type is some (concrete) AI type,
     * create that type of AI (then this is an AI player).
     * Otherwise, create a SimpleAI as default (used by Human or Remote
     * clients for the autoplay functionality).
     *
     * @param playerType Type of player for which to create an AI
     * @return Some AI object, according to the situation
     */
    // TODO move (partly) to AI package, e.g. as static method
    private AI createAI(String playerType)
    {
        LOGGER.log(Level.FINEST, "Creating the AI player type " + playerType);

        AI createdAI = null;

        String createType = playerType;
        if (createType.endsWith(Constants.anyAI))
        {
            LOGGER.severe("Invalid player type " + createType
                + " on client side - server is supposed to select the "
                + "actual AI type for random choosing.");
        }

        // Non-AI (= human and remote) players use some AI for autoplay:
        if (!createType.endsWith("AI"))
        {
            createType = Constants.aiPackage + Constants.autoplayAI;
        }

        // TODO Can it still happen that we get a unqualified AI class name?
        // Or does server nowadays always send proper fully qualified name?
        if (!createType.startsWith(Constants.aiPackage))
        {
            String name = getOwningPlayer().getName();
            LOGGER.warning("Needed to add package for AI type? Type="
                + createType + ", Player name=" + name);
            createType = Constants.aiPackage + createType;
        }

        try
        {
            // TODO these seem to be classes of either AI or Client, there
            // should be a common ancestor
            // NOPE . The one is the "first argument of the constructor
            //        is of type Client. The other is the Client object passed
            //        to that constructor.
            // TODO Anyway, this could be done better, perhaps moved to
            // AI package, using typesafe enums that do the mapping from name
            // to class explicitly?
            Class<?>[] classArray = new Class<?>[1];
            classArray[0] = Class.forName("net.sf.colossus.client.Client");
            Object[] objArray = new Object[1];
            objArray[0] = this;
            createdAI = (AI)Class.forName(createType)
                .getDeclaredConstructor(classArray).newInstance(objArray);
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, "Failed to create for client "
                + owningPlayer.getName() + " the to " + createType, ex);
        }

        if (createdAI == null)
        {
            createdAI = new SimpleAI(this);
        }

        return createdAI;
    }

    public void appendToConnectionLog(String s)
    {
        // in rare cases with scratch-reconnects we might not have a gui or
        // connection log window yet.
        if (isReplayOngoing() || gui == null)
        {
            return;
        }
        gui.appendToConnectionLog(s);
    }

    public boolean isSctAlreadyDown()
    {
        return connection.isAlreadyDown();
    }

    public boolean isRemote()
    {
        return (localServer == null);
    }

    public boolean isSpectator()
    {
        return spectator;
    }

    /*
     * Following ones are used for the client side timeout (if player
     * has not done anything for certain time ((left keyboard??)), then
     * something is done in his behalf so that game is not stuck/lost
     * for other players.
     */
    public boolean needsWatchdog()
    {
        // For Debugging/Development only this particular one
        // has watchdog, other's not, so that "some other" player
        // can be active (and thus this one here does not need to be)
        if (getOwningPlayer().getName().equals("watchdogtest")
            || getOwningPlayer().getName().equals("localwatchdogtest"))
        {
            return true;
        }

        if (getOwningPlayer().isAI())
        {
            return false;
        }

        return options.getOption(Options.inactivityTimeout);
    }

    public boolean hasWatchdog()
    {
        return gui.hasWatchdog();
    }

    public void setClockTicking(boolean val)
    {
        clockIsTicking = val;
    }

    public boolean isClockTicking()
    {
        return clockIsTicking;
    }

    public boolean isTheInternalSpectator()
    {
        return internalSpectator;
    }

    public boolean isAutoplayActive()
    {
        return autoplay.isAutoplayActive();
    }

    public Autoplay getAutoplay()
    {
        return autoplay;
    }

    public boolean getAutoSplit()
    {
        return autoplay.autoSplit();
    }

    // TODO can this be replaced with "!owningPlayer.isDead()" ?
    // Only critical issue AFAIS is that owningPlayer is not properly
    // initialized right away from the start (re-assigned later).
    public boolean isAlive()
    {
        assert owningPlayer != null : "owningPlayer is null, "
            + "can't ask whether alive or not!";
        assert owningPlayer.isDead() != playerAlive : "playerAlive gives "
            + "different result than owningPlayer.isDead()!";
        return playerAlive;
    }

    private boolean paused = false;

    public boolean isPaused()
    {
        return paused;
    }

    private String currentLegionMarkerId = null;

    public void setCurrentLegionMarkerId(String MarkerId)
    {
        currentLegionMarkerId = MarkerId;
    }

    public String getCurrentLegionMarkerId()
    {
        return currentLegionMarkerId;
    }

    public void setPauseState(boolean newState)
    {
        if (isRemote())
        {
            LOGGER.warning("setPauseState should not be possible "
                + "in remote client!");
        }
        else
        {
            paused = newState;
            localServer.setPauseState(paused);
        }
    }

    public void enforcedDisconnect()
    {
        connection.enforcedConnectionException();
    }

    public boolean ensureThatConnected()
    {
        if (isConnected())
        {
            return true;
        }
        else
        {
            notifyThatNotConnected();
            return false;
        }
    }

    public void notifyThatNotConnected()
    {
        String waitText = "";
        if (isConnectRoundOngoing())
        {
            waitText = "\n\nPlease wait until Reconnect is completed.";
        }
        showMessageDialog("NOTE: You are currently not connected "
            + " to the server!" + waitText);
    }

    // for debugging/development purposes
    public void enforcedDisconnectByServer()
    {
        localServer.enforcedDisconnectClient(owningPlayer.getName());
        // localServer.enforcedDisconnectClient("remote");
    }

    /**
     * is != -1 only from the point on when client abandons the connection,
     * until sync is completed. When sync is completed, it's re-set back
     * to -1.
     */
    private int lastMsgNr = -1;
    private IServerConnection previousConn;

    public boolean isConnected()
    {
        return previousConn == null;
    }

    public void abandonCurrentConnection()
    {
        if (lastMsgNr == -1)
        {
            previousConn = connection;
            int got = previousConn.abandonAndGetMessageCounter();
            if (got == -2)
            {
                LOGGER.warning("Connection was abandoned already "
                    + "before, got msgCounter -2 ???");
            }
            else
            {
                lastMsgNr = got;
            }
        }
    }

    /**
     *
     * @param automatic true if was triggered automatically e.g. by a Socket Exception,
     * false if triggered manually (e.g. MasterBoard File menu).
     */
    public void tryReconnect(boolean automatic)
    {
        String cause = automatic ? "automatically" : "manually";
        LOGGER.info("Trying reconnect (" + cause + " triggered)");
        appendToConnectionLog("Trying reconnect (" + cause + " triggered)");

        int attempt = 1;
        IServerConnection conn = null;
        do
        {
            try
            {
                conn = SocketClientThread.recreateConnection(previousConn);
            }
            catch (ConnectionInitException e)
            {
                LOGGER.warning("Reconnect #" + attempt + " attempted ("
                    + cause + ") but got ConnectionInitException " + e);
                if (attempt < MAX_RECONNECT_ATTEMPTS)
                {
                    appendToConnectionLog("PROBLEM: Reconnect attempt #"
                        + attempt + " failed; will retry after "
                        + RECONNECT_RETRY_INTERVAL + " seconds ("
                        + (MAX_RECONNECT_ATTEMPTS - attempt)
                        + " attempts left)");
                    WhatNextManager.sleepFor(RECONNECT_RETRY_INTERVAL * 1000);
                    attempt++;
                }
                else
                {
                    break;
                }
            }
        }
        while (conn == null);

        // Okay: Reconnect succeeded!
        if (conn != null)
        {
            appendToConnectionLog("Connection succeeded, "
                + "initiating synchronization...");
            gotDisposeAlready = false;
            this.connection = conn;
            this.server = connection.getIServer();
            connection.setClient(this);
            connection.startThread();

            int oldMsgsLeft;
            int i = 0;
            while ((oldMsgsLeft = previousConn.getDisposedQueueLen()) > 0
                && i < 1)
            {
                i++;
                appendToConnectionLog("WARNING: Previous connection has still "
                    + oldMsgsLeft
                    + " items to process (handling that is not implemented yet...)!");
                // WhatNextManager.sleepFor(1000);
            }

            connection.requestSyncDelta(lastMsgNr, ++syncRequestCounter);
            LOGGER.info("Connection re-established. "
                + "Waiting for synchronization to complete.");
        }
        else
        {
            String message = "Trying to reconnect ("
                + cause
                + " triggered) failed "
                + attempt
                + " times - giving up."
                // Add the rest only for automatically triggered reconnects.
                + (automatic ? "\n\n"
                    + "You may try it again (from File Menu) after a few seconds."
                    + "\nBut if that does not succeed, well, bad luck:-("
                    : "");
            gui.showMessageDialogAndWait(message);
        }
    }

    public void guiTriggeredTryReconnect()
    {
        LOGGER.info("Menu-triggered (= manual) reconnect!");
        fireOneReconnectRunnable(false);
    }

    private final Object oneConnectAttemptsRoundMutex = new Object();

    private Runnable oneConnectAttemptsRound = null;

    private void setConnectAttemptsRoundCompleted()
    {
        synchronized (oneConnectAttemptsRoundMutex)
        {
            oneConnectAttemptsRound = null;
        }
    }

    public boolean isConnectRoundOngoing()
    {
        synchronized (oneConnectAttemptsRoundMutex)
        {
            return (oneConnectAttemptsRound != null);
        }
    }

    /**
     * Creates a runnable that executes one reconnect round (several attempts)
     */
    private void fireOneReconnectRunnable(final boolean automatic)
    {
        synchronized (oneConnectAttemptsRoundMutex)
        {
            if (oneConnectAttemptsRound != null)
            {
                appendToConnectionLog("One reconnect-attempt round already "
                    + "ongoing - starting a new one omitted.");
                return;
            }
            oneConnectAttemptsRound = new Runnable()
            {
                public void run()
                {
                    abandonCurrentConnection();
                    tryReconnect(automatic);
                    setConnectAttemptsRoundCompleted();
                }
            };
        }
        new Thread(oneConnectAttemptsRound).start();
    }

    public void tellSyncCompleted(int syncRequestNumber)
    {
        LOGGER.info("Synchronization #" + syncRequestNumber + " completed!");
        lastMsgNr = -1;
        previousConn = null;
        if (!isSpectator())
        {
            gui.actOnReconnectCompleted();
        }
        if (isSpectator() && syncRequestNumber == 0)
        {
            // this is the initial sync during connect for watching,
            // skip displaying anything
        }
        else
        {
            gui.appendToConnectionLog("Synchronization #" + syncRequestNumber
                + " completed!");
        }
    }

    /** Take a mulligan. */
    public void mulligan()
    {
        gui.undoAllMoves(); // XXX Maybe move entirely to server

        tookMulligan = true;

        server.mulligan();
    }

    public void requestExtraRoll()
    {
        server.requestExtraRoll();
    }

    // XXX temp
    public boolean tookMulligan()
    {
        return tookMulligan;
    }

    public void requestExtraRollApproval(String requestorName, int requestId)
    {
        LOGGER.finest("Client " + getOwningPlayer().getName()
            + " is asked to approve extra roll request... - answering true");

        if (isAutoplayActive())
        {
            sendExtraRollRequestResponse(true, requestId);
        }
        else if (requestorName.equals(getOwningPlayer().getName()))
        {
            /*
             * If server asks from the requestor itself, it means that no
             * other client is able to can approve.
             * User should now negotiate in chat and answer respond to
             * server accordingly.
             */
            LOGGER.finest("Server asks me, the requestor, for approval "
                + " - that means no other client can approve.");
            gui.askExtraRollApproval(requestorName, true, requestId);
        }
        else
        {
            /* Ask the player/user for approval; it will send the answer back
             * asynchronously
             */
            gui.askExtraRollApproval(requestorName, false, requestId);
        }
    }

    /**
     * Player or AI has answered, send the response to server
     * @param approved
     * @param requestId
     */
    public void sendExtraRollRequestResponse(boolean approved, int requestId)
    {
        server.extraRollResponse(approved, requestId);
    }

    public void askSuspendConfirmation(String requestorName, int timeout)
    {
        LOGGER.fine("User " + requestorName
            + " requests to suspend the game, timeout=" + timeout);
        if (getOwningPlayer().isAI() || autoplay.isAutoplayActive())
        {
            LOGGER.finest("Server asks us for suspend approval. Auto-yes.");
            suspendResponse(true);
        }
        else if (requestorName.equals(getOwningPlayer().getName()))
        {
            // If server asks from the requestor itself, it means that no
            // other client is able to can approve.
            LOGGER.finest("Server asks me, the requestor, for approval "
                + " - that means no other client can approve.");
            suspendResponse(true);
        }
        else
        {
            gui.askSuspendConfirmation(requestorName, timeout);
        }
    }

    public void suspendResponse(boolean approved)
    {
        if (game.isSuspended())
        {
            LOGGER.finest("Game already suspended, probably called during "
                + "dispose()? Not sending response.");
        }
        else if (server == null)
        {
            LOGGER.warning("In client " + getOwningPlayer().getName()
                + ": in suspendResponse(), server NULL ?");
        }
        else
        {
            LOGGER.finest("Client " + getOwningPlayer().getName()
                + ": in suspendResponse: sending response " + approved);
            server.suspendResponse(approved);
        }
    }

    public void doCheckServerConnection()
    {
        server.checkServerConnection();
    }

    public void doCheckAllConnections(String requestingClientName)
    {
        server.checkAllConnections(requestingClientName);
    }

    public void relayedPeerRequest(String requestingClientName)
    {
        LOGGER.finest("In client " + this.owningPlayer.getName()
            + " we received, that peer " + requestingClientName
            + " requests a reply.");
        server.peerRequestProcessed(requestingClientName);
    }

    /** Upon request with checkServerConnection, server sends a confirmation.
     *  This method here processes the confirmation.
     */
    public synchronized void serverConfirmsConnection()
    {
        gui.serverConfirmsConnection();
    }

    public void peerRequestReceivedBy(String respondingPlayerName, int queueLen)
    {
        LOGGER.info("Got RECEIVED  confirmation from " + respondingPlayerName
            + ", queueLen=" + queueLen);
    }

    public void peerRequestProcessedBy(String respondingPlayerName)
    {
        LOGGER.info("Got PROCESSED confirmation from " + respondingPlayerName);
    }

    public void locallyInitiateSaveGame(String filename)
    {
        localServer.initiateSaveGame(filename);
    }

    public void initiateSuspend(boolean save)
    {
        server.requestToSuspendGame(save);
    }

    public boolean getFailed()
    {
        return failed;
    }

    /** Resolve engagement in land. */
    public void engage(MasterHex hex)
    {
        server.engage(hex);
    }

    public Legion getMyEngagedLegion()
    {
        if (isMyLegion(getAttacker()))
        {
            return getAttacker();
        }
        else if (isMyLegion(getDefender()))
        {
            return getDefender();
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
    private void fight(MasterHex hex)
    {
        server.fight(hex);
    }

    boolean engagementStartupOngoing = false;

    public void setEngagementStartupOngoing(boolean val)
    {
        engagementStartupOngoing = val;
    }

    public void tellEngagement(MasterHex hex, Legion attacker, Legion defender)
    {
        if (isMyLegion(attacker) || isMyLegion(defender))
        {
            setEngagementStartupOngoing(true);
        }
        game.createEngagement(hex, attacker, defender);
        gui.tellEngagement(attacker, defender, getTurnNumber());
    }

    public void tellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        setEngagementStartupOngoing(false);
        gui.actOnTellEngagementResults(winner, method, points, turns);
        game.clearEngagementData();
        gui.actOnEngagementCompleted();
    }

    /** This player quits the whole game. The server needs to always honor
     *  this request, because if it doesn't players will just drop
     *  connections when they want to quit in a hurry. */
    public void withdrawFromGame()
    {
        if (!game.isGameOver() && !owningPlayer.isDead())
        {
            server.withdrawFromGame();
        }
    }

    public void tellMovementRoll(int roll, String reason)
    {
        game.setMovementRoll(roll);
        gui.actOnTellMovementRoll(roll, reason);

        // kickMoves() now called by kickPhase(), because the kickXXXX
        // are now done separately, not implied in the setupXXXXX
        // (or in this case, tellMovementRoll()) any more.
        // kickMoves();
    }

    public void tellWhatsHappening(String message)
    {
        gui.tellWhatsHappening(message);
    }

    public void kickPhase() // NOT: kickFace!  ;-)
    {
        if (game.isPhase(Phase.SPLIT))
        {
            kickSplit();
        }
        else if (game.isPhase(Phase.MOVE))
        {
            kickMoves();
        }

        else if (game.isPhase(Phase.FIGHT))
        {
            kickFight();
        }

        else if (game.isPhase(Phase.MUSTER))
        {
            kickMuster();
        }
    }

    private void kickMoves()
    {
        if (isMyTurn() && autoplay.autoMasterMove() && !game.isGameOver()
            && !replayOngoing)
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

    /** Server sends Client some option setting (e.g. AI type,
     *  autoPlay for stresstest (also AIs (????), ...)
     */
    public void syncOption(String optname, String value)
    {
        options.setOption(optname, value);
    }

    // public for IOracle
    public int getNumPlayers()
    {
        return game.getNumPlayers();
    }

    public void updatePlayerInfo(List<String> infoStrings)
    {
        if (playersNotInitialized)
        {
            String searchName = this.owningPlayer.getName();
            PlayerClientSide foundPlayer = game.initPlayerInfo(infoStrings,
                searchName);
            if (!spectator)
            {
                // returns null if not found, prevent the dummy player
                // initialized to it to be overwritten with null
                this.owningPlayer = foundPlayer;
            }
            playersNotInitialized = false;
        }
        game.updatePlayerInfo(infoStrings);
        gui.updateStatusScreen();
    }

    public boolean canHandleChangedValuesOnlyStyle()
    {
        // Dummy, only to satisfy the interface
        return true;
    }

    public void updateChangedPlayerValues(String valuesString, String reason)
    {
        LOGGER.finest("Updating values: " + valuesString + "(reason: "
            + reason + ")");
        game.updatePlayerValues(valuesString);
        gui.updateStatusScreen();
    }

    public PlayerClientSide getOwningPlayer()
    {
        return owningPlayer;
    }

    // Called by server during load, or by ai or gui when they choose it
    public void setColor(PlayerColor color)
    {
        // this.color = color;
    }

    public void updateCreatureCount(CreatureType type, int count, int deadCount)
    {
        getGame().getCaretaker().setAvailableCount(type, count);
        getGame().getCaretaker().setDeadCount(type, deadCount);
        gui.updateCreatureCountDisplay();
    }

    void setClosedByServer()
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

        if (connection != null && !connection.isAlreadyDown())
        {
            // send withdraw, if relevant (not game over or dead already)
            // TODO Not in use right now, Server has not enough time to
            // handle it before Exception strikes? Need further testing...
            // In practice, handling the withDraw would involve writing
            // to all clients, and that leads to BrokenPipeException.
            // In contrast, disconnect does not send anything to the
            // to-be-gone client.
            // Solution approach perhaps: pass in disconnect message
            // a mode like: withdraw / shortterm  (will disconnect soon)
            // or longterm (=> play by email) or (CH to sever) "unknown"
            // withdrawFromGame();

            // SCT will then end the loop and do the dispose.
            // So nothing else to do any more here in EDT.
            connection.stopSocketClientThread(true);
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
    public synchronized void disposeClient()
    {
        if (gotDisposeAlready)
        {
            return;
        }
        gotDisposeAlready = true;
        disposeWholeClient();
    }

    // Clean up everything related to _this_ client:

    private void disposeWholeClient()
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
                LOGGER.log(Level.SEVERE,
                    "During close in client " + owningPlayer.getName()
                        + ": got Exception!!!" + e.toString(), e);
            }
            ViableEntityManager.unregister(this);
        }
    }

    private boolean decideWhetherClose()
    {
        boolean close = true;

        // Defensive: if called too early (owningPlayer not set yet),
        // assume it's a human to prevent auto.close.
        boolean isAI = (owningPlayer != null && owningPlayer.isAI());

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
                if (gui.hasBoard())
                {
                    gui.showConnectionClosedMessage();
                    if (game.isSuspended())
                    {
                        close = true;
                    }
                    else
                    {
                        close = false;
                    }
                }
                else
                {
                    gui.actOnGameStartingFailed();
                    // No board - probably startup failed. Simply silently
                    close = true;
                }
            }
            else
            {
                // NOT remote, forced closed: just closing without asking
            }
        }
        else if (closedBy == ClosedByConstant.CLOSED_BY_CLIENT)
        {
            // ok, explicitly initiated by user.
        }
        else
        {
            LOGGER.warning("Client " + getOwningPlayer()
                + ": network connection was unexpectedly closed. "
                + "Trying to reconnect after 1 second ...");
            appendToConnectionLog("NOTE: Ooops? Connection with server side "
                + "was unexpectedly closed? "
                + "Will try to reconnect after 1 second ...");

            // give it some time...
            WhatNextManager.sleepFor(1000);
            LOGGER.info("Initiating automatic reconnect!");
            fireOneReconnectRunnable(true);
            close = false;
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

        connection = null;
        server = null;

        gui.doCleanupGUI();
    }

    /* This was earlier done at end of cleanupGUI inside client.
     * Now that is moved to GUI class, its called from there after all
     * other cleanup has completed (inside the invokeAndWait call).
     */
    public void doAdditionalCleanup()
    {
        this.battleMovement = null;
        playersNotInitialized = true;

        CustomRecruitBase.resetAllInstances();
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
        gui.actOnDoneWithBattleMoves();
        server.doneWithBattleMoves();
    }

    // TODO move to Game or Battle
    public List<BattleUnit> getActiveBattleUnits()
    {
        return getBattleCS().getActiveBattleUnits();
    }

    private boolean sansLordAutoBattleApplies()
    {
        if (!game.isBattleOngoing())
        {
            // Odd: if GUI client tries this when there hasn't been any
            // battle, seems EDT hangs and never finishes.
            // So, to prevent that do this here only if there is really
            // a battle.
            return false;
        }
        if (options.getOption(Options.sansLordAutoBattle))
        {
            // the option is on.  now check for lordly presence...
            Legion legion = game.getBattleActiveLegion();
            for (Creature creature : legion.getCreatures())
            {
                CreatureType creatureType = creature.getType();
                if (creatureType.isLord())
                {
                    // if any creature in legion is a lord, no auto battle
                    return false;
                }
            }
            // not returned yet?  then no lords.  auto battle for this legion
            return true;
        }
        // the option itself is off.  sansLordAutoBattles never applies.
        return false;
    }

    // TODO move to Game or Battle
    public List<BattleUnit> getInactiveBattleUnits()
    {
        return getBattleCS().getInactiveBattleUnits();
    }

    public void aiDoneWithStrikes()
    {
        aiPause();
        doneWithStrikes(true);
    }

    public void doneWithStrikes(boolean auto)
    {
        gui.indicateStrikesDone(auto);
        server.doneWithStrikes();
    }

    /** Return true if any strikes were taken. */
    private boolean makeForcedStrikes()
    {
        if (isMyBattlePhase() && autoplay.autoForcedStrike())
        {
            return strikeMakeForcedStrikes(autoplay.autoRangeSingle());
        }
        return false;
    }

    private boolean strikeMakeForcedStrikes(boolean autoRangeSingle)
    {
        if (getBattlePhase() == null)
        {
            LOGGER.log(Level.SEVERE,
                "Called Strike.makeForcedStrikes() when there is no battle");
            return false;
        }
        else if (!getBattlePhase().isFightPhase() && !isMyBattlePhase())
        {
            LOGGER.log(Level.SEVERE,
                "Called Strike.makeForcedStrikes() in wrong phase");
            return false;
        }

        for (BattleCritter battleUnit : getActiveBattleUnits())
        {
            if (!battleUnit.hasStruck())
            {
                Set<BattleHex> set = getBattleCS().findTargets(battleUnit,
                    autoRangeSingle);
                if (set.size() == 1)
                {
                    BattleHex hex = set.iterator().next();
                    strike(battleUnit.getTag(), hex);
                    return true;
                }
            }
        }
        return false;
    }

    /** Handle both forced strikes and AI strikes. */
    private void doAutoStrikes()
    {
        if (isMyBattlePhase())
        {
            if (isAutoplayActive() || sansLordAutoBattleApplies())
            {
                aiPause();
                boolean struck = makeForcedStrikes();
                if (!struck)
                {
                    struck = ai.strike(game.getBattleActiveLegion());
                }
                if (!struck)
                {
                    aiDoneWithStrikes();
                }
            }
            else
            {
                boolean struck = makeForcedStrikes();
                gui.highlightCrittersWithTargets();
                // If there are no strikes (or strikeback) to do, be done with
                // striking automatically.
                // NOTE! This handles also both strike phases of turn 1
                // of a battle! So for long delay (roundtrip time) client is
                // for a short while "theoretically" in "is supposed to strike"
                // status.
                if (!struck && findCrittersWithTargets().isEmpty())
                {
                    aiDoneWithStrikes();
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
        PlayerClientSide player = (PlayerClientSide)game
            .getPlayerByMarkerId(markerId);
        LegionClientSide legion = player.getLegionByMarkerId(markerId);
        // Added this logging only for the purpose that one gets a clue
        // when during the game this happened - the assertion appears only
        // on stderr. Now it's also in the log, so one sees what was logged
        // just before and after it.
        if (legion == null)
        {
            LOGGER.log(Level.SEVERE, "No legion with markerId '" + markerId
                + "'" + " (for player " + player + "), turn = "
                + getTurnNumber() + " in client " + getOwningPlayer());
        }
        assert legion != null : "No legion with markerId '" + markerId + "'"
            + " (for player " + player + "), turn = " + getTurnNumber()
            + " in client " + getOwningPlayer();
        return legion;
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
        boolean teleported, EntrySide entrySide, CreatureType lastRecruit)
    {
        legion.setMoved(moved);
        legion.setTeleported(teleported);
        legion.setEntrySide(entrySide);
        legion.setRecruit(lastRecruit);
    }

    // TODO make all who use this use directly from game
    public List<String> getLegionImageNames(Legion legion)
    {
        return game.getLegionImageNames(legion);
    }

    // TODO make all who use this use directly from game
    public List<Boolean> getLegionCreatureCertainties(Legion legion)
    {
        return game.getLegionCreatureCertainties(legion);
    }

    /**
     * Add a new creature to this legion.
     */
    public void addCreature(Legion legion, CreatureType creature, String reason)
    {
        legion.addCreature(creature);

        gui.actOnAddCreature(legion, creature, reason);

    }

    public void removeCreature(Legion legion, CreatureType creature,
        String reason)
    {
        if (legion == null || creature == null)
        {
            return;
        }

        gui.actOnRemoveCreature(legion, creature, reason);

        int height = legion.getHeight();
        legion.removeCreature(creature);
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

    public void revealCreatures(Legion legion,
        final List<CreatureType> creatures, String reason)
    {
        gui.eventViewerRevealCreatures(legion, creatures, reason);

        ((LegionClientSide)legion).revealCreatures(creatures);
    }

    /* pass revealed info to SplitPrediction and the GUI
     */
    public void revealEngagedCreatures(Legion legion,
        final List<CreatureType> names, boolean isAttacker, String reason)
    {
        revealCreatures(legion, names, reason);

        gui.revealEngagedCreatures(legion, names, isAttacker, reason);
    }

    public void removeDeadBattleChits()
    {
        for (BattleUnit battleUnit : getBattleCS().getBattleUnits())
        {
            if (battleUnit.isDead())
            {
                // Moved it.remove() to a 2nd loop that is then done inside
                // Battle, otherwise unsupportedOperationException because
                // we get an unmodifiable list from Battle.
                // it.remove();

                gui.removeBattleChit(battleUnit);

                // Also remove it from legion.
                battleUnit.getLegion().removeCreature(battleUnit.getType());

                // And generate EventViewer event:
                // Note that have to do the legion.removeCreature before this
                // gui / eventviewer call so that eventViewer sees already new
                // height.
                gui.eventViewerSetCreatureDead(battleUnit);
            }
        }

        getBattleCS().removeDeadBattleChits();

        gui.repaintBattleBoard();
    }

    /** Create a new BattleUnit and (if GUI) a new GUIBattleChit with
     *  the given parameters. Place them in given hex,
     *  and add them to the lists of BattleUnits (in Battle[ClientSide])
     *  and GUIBattleChits (in GUI)
     */
    public void placeNewChit(String bareImageName, boolean inverted, int tag,
        BattleHex hex)
    {
        Legion legion = inverted ? getDefender() : getAttacker();

        String imageName = bareImageName;
        if (imageName.equals(Constants.titan))
        {
            imageName = legion.getPlayer().getTitanBasename();
        }
        else if (imageName.equals(Constants.angel))
        {
            imageName = legion.getPlayer().getAngelBasename();
        }

        CreatureType type = getGame().getVariant().getCreatureByName(
            bareImageName);

        BattleUnit battleUnit = getBattleCS().createBattleUnit(imageName,
            inverted, tag, hex, type, legion);

        gui.actOnPlaceNewChit(imageName, battleUnit, hex);
    }

    public CreatureType chooseBestPotentialRecruit(LegionClientSide legion,
        MasterHex hex, List<CreatureType> recruits)
    {
        CreatureType recruit = ai.getVariantRecruitHint(legion, hex, recruits);
        return recruit;
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

    public void tellRedo(boolean val)
    {
        redoOngoing = val;
        gui.actOnTellRedoChange();
    }

    public boolean isRedoOngoing()
    {
        return redoOngoing;
    }

    public boolean isReplayBeforeRedo()
    {
        return replayOngoing && !redoOngoing;
    }

    public void confirmWhenCaughtUp()
    {
        server.clientConfirmedCatchup();
    }

    public void confirmRelayedPeerRequest(String requestingClientName)
    {
        server.peerRequestProcessed(requestingClientName);
    }

    public void initBoard()
    {
        // SyncOptions is now done, so now we can create movement and
        // battleMovement which need the options.
        /* NOTE/WARNING:
         * using option listeners here would have to be done the *right way*,
         * because changes from server appear here as string-based,
         * thus a listener based on the boolean option would not work.
         * For now, we just make sure we create BattleMovement after all
         * server-to-client-option-sync'ing is completed.
         */
        this.battleMovement = new BattleMovement(game, options);
        this.movement = new MovementClientSide(game, options);

        LOGGER.finest(getOwningPlayer().getName() + " Client.initBoard()");
        ai.setVariant(VariantSupport.getCurrentVariant());
        gui.initBoard();
    }

    public void setEventExecutor(EventExecutor eventExecutor)
    {
        this.eventExecutor = eventExecutor;
    }

    public EventExecutor getEventExecutor()
    {
        return this.eventExecutor;
    }

    public void setPlayerName(String playerName)
    {
        this.owningPlayer.setName(playerName);

        InstanceTracker.setId(this, "Client " + playerName);
        InstanceTracker.setId(ai, "AI: " + playerName);

        connection.updatePlayerName(playerName);
    }

    public void createSummonAngel(Legion legion)
    {
        SummonInfo summonInfo = new SummonInfo();

        List<Legion> possibleDonors = game.findLegionsWithSummonables(legion);
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
            doSummon(summonInfo);
        }
        else
        {
            if (autoplay.autoSummonAngels())
            {
                summonInfo = ai.summonAngel(legion, possibleDonors);
                doSummon(summonInfo);
            }
            else
            {
                gui.doPickSummonAngel(legion, possibleDonors);
                // GUI does a callback (sends it to server itself).
            }
        }
    }

    /**
     * recruits is the list of acquirables that can be chosen from
     * for a certain point value reached. E.g. for getting 180 points,
     * going from 380 + 180 = 560,
     * game would first call this for 400: recruits = [Angel]
     * and then call it once more for 500: recruits = [Angel, Archangel]
     */
    public void askAcquireAngel(Legion legion, List<CreatureType> recruits)
    {
        if (autoplay.autoAcquireAngels())
        {
            acquireAngelCallback(legion, ai.acquireAngel(legion, recruits));
        }
        else
        {
            gui.doAcquireAngel(legion, recruits);
        }
    }

    public void acquireAngelCallback(Legion legion, CreatureType angelType)
    {
        server.acquireAngel(legion, angelType);
    }

    /** Present a dialog allowing the player to enter via land or teleport.
     *  Return true if the player chooses to teleport. */
    private boolean chooseWhetherToTeleport(MasterHex hex)
    {
        if (autoplay.autoMasterMove())
        {
            return false;
        }

        // No point in teleporting if entry side is moot.
        // (Note that the "what if it's own legion there?" exception as
        // in pick-entry-side logic is not needed here - can't teleport
        // to same hex.
        if (!game.isOccupied(hex))
        {
            return false;
        }

        return gui.chooseWhetherToTeleport();
    }

    /** Allow the player to choose whether to take a penalty (fewer dice
     *  or higher strike number) in order to be allowed to carry. */
    public void askChooseStrikePenalty(List<String> choices)
    {
        if (isAutoplayActive() || sansLordAutoBattleApplies())
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

    public void tellGameOver(String message, boolean disposeFollows, boolean suspended)
    {
        LOGGER.info("Client " + getOwningPlayer()
            + " received from server game over message: " + message);
        game.setGameOver(true, message);
        game.setSuspended(suspended);

        gui.actOnTellGameOver(message, disposeFollows, suspended);
    }

    public void doFight(MasterHex hex)
    {
        if (!isMyTurn())
        {
            return;
        }
        engage(hex);
    }

    // TODO: handle better (Mock, extend Client class?)
    public boolean testCaseAutoDontFlee = false;

    public void askConcede(Legion ally, Legion enemy)
    {
        if (testCaseAutoDontFlee)
        {
            LOGGER.fine("askConcede: test case auto-answers 'doNotConcede'");
            server.doNotConcede(ally);
        }
        else if (autoplay.autoConcede())
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
        if (testCaseAutoDontFlee)
        {
            LOGGER.fine("askFlee: test case auto-answers 'doNotFlee'");
            server.doNotFlee(ally);
        }
        else if (autoplay.autoFlee())
        {
            if (eventExecutor.getRetriggeredEventOngoing())
            {
                boolean reply = ai.flee(ally, enemy);

                inactivityAutoFleeOrConcede(reply);
            }
            else
            {
                answerFlee(ally, ai.flee(ally, enemy));
            }
        }
        else
        {
            gui.showFlee(this, ally, enemy);
        }
    }

    /**
     * Make the concede dialog reply wit the answer the AI has provided,
     * as if the user would have selected something; so that the dialog
     * is disposed cleanly.
     * @param reply Whether to fleed/conced, or not.
     */
    public void inactivityAutoFleeOrConcede(boolean reply)
    {
        gui.inactivityAutoFleeOrConcede(reply);
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

    // TODO: handle better (Mock, extend Client class?)
    public boolean testCaseAutoDenyNegotiate = false;

    public void askNegotiate(Legion attacker, Legion defender)
    {
        if (getAttacker() != attacker)
        {
            LOGGER
                .severe("Attacker in game differs from attacker given in askNegotiate!");
        }
        if (getDefender() != defender)
        {
            LOGGER
                .severe("Defender in game differs from defender given in askNegotiate!");
        }

        if (autoplay.autoNegotiate())
        {
            // XXX AI players just fight for now.
            Proposal proposal = new Proposal(getAttacker(), getDefender(),
                true, false, null, null);

            makeProposal(proposal);
        }
        else if (testCaseAutoDenyNegotiate)
        {
            fight(attacker.getCurrentHex());
        }
        else
        {
            gui.showNegotiate(getAttacker(), getDefender());
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
            fight(getAttacker().getCurrentHex());
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

    public void tellSlowResults(int targetTag, int slowValue)
    {
        BattleCritter targetCritter = getBattleCS().getBattleUnit(targetTag);
        if (targetCritter != null)
        {
            if (slowValue != 0)
            {
                targetCritter.addSlowed(slowValue);
            }
        }
    }

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions)
    {
        BattleCritter battleUnit = getBattleCS().getBattleUnit(strikerTag);
        if (battleUnit != null)
        {
            battleUnit.setStruck(true);
        }

        gui.disposePickCarryDialog();

        BattleUnit targetUnit = getBattleCS().getBattleUnit(targetTag);
        BattleCritter targetCritter = getBattleCS().getBattleUnit(targetTag);

        gui.actOnTellStrikeResults(wasCarry, strikeNumber, rolls, battleUnit,
            targetUnit);

        if (targetCritter != null)
        {
            if (killed)
            {
                targetCritter.setDead(true);
            }
            else
            {
                if (damage != 0) // Can be negative if creature is being healed
                {
                    targetCritter.setHits(targetUnit.getHits() + damage);
                    gui.actOnHitsSet(targetUnit);
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
        if (reason.startsWith("doSplit"))
        {
            LOGGER.warning("NAK: " + reason);
            String available = Glob.glob(",",
                owningPlayer.getMarkersAvailable());
            String notAvail = Glob.glob(",", owningPlayer.getMarkersUsed());

            LOGGER.warning("Available: " + available + ": not available "
                + notAvail);
        }
        else
        {
            LOGGER.warning("other NAK,reason: " + reason);
        }
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
            if (isMyTurn() && autoplay.autoSplit() && !game.isGameOver())
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
            gui.actOnMoveNak();
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
            handleFailedBattleMove(errmsg);
        }
        else if (reason.equals(Constants.doneWithBattleMoves))
        {
            // TODO why can we ignore this?
            /*
             * Clemens' guess: This can not really happen based on "user did
             * something wrong"; when this happened, it was because user
             * clicked twice (due to delayed response from server)
             * => the illegal nak was because it was already different phase.
             * So, no point to bother user with that.
             * With recent (12.10.2015) changes (where user gets visual
             * feedback that Done was clicked) this situation "should"
             * not happen any more...
             */
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
            gui.highlightCrittersWithTargets();
            gui.revertDoneIndicator();
        }
        else if (reason.equals(Constants.doneWithEngagements))
        {
            showMessageDialog(errmsg);
        }
        else if (reason.equals(Constants.doRecruit))
        {
            // TODO: earlier here was nothing, but a TODO "why can we ignore
            //       Did/does the adding showMessageDialog cause problems?"
            showMessageDialog(errmsg);
        }
        else if (reason.equals(Constants.doneWithRecruits))
        {
            // TODO why can we ignore this?
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
            && autoplay.autoCarrySingle())
        {
            Iterator<String> it = carryTargetDescriptions.iterator();
            String desc = it.next();
            String targetHexLabel = desc.substring(desc.length() - 2);
            BattleHex targetHex = game.getBattleSite().getTerrain()
                .getHexByLabel(targetHexLabel);
            applyCarries(targetHex);
        }
        else
        {
            if (isAutoplayActive() || sansLordAutoBattleApplies())
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
        Player battleActivePlayer, BattlePhase battlePhase, Legion attacker,
        Legion defender)
    {
        gui.cleanupNegotiationDialogs();

        game.initBattle(hex, battleTurnNumber, battleActivePlayer,
            battlePhase, attacker, defender);

        if (isAutoplayActive() || sansLordAutoBattleApplies())
        {
            ai.initBattle();
        }

        gui.actOnInitBattle();
    }

    public void messageFromServer(String message)
    {
        gui.showMessageDialogAndWait(message);
    }

    public void showMessageDialog(String message)
    {
        gui.showMessageDialogAndWait(message);
    }

    public void cleanupBattle()
    {
        LOGGER.log(Level.FINEST, owningPlayer.getName()
            + " Client.cleanupBattle()");

        gui.actOnCleanupBattle();

        if (isAutoplayActive() || sansLordAutoBattleApplies())
        {
            ai.cleanupBattle();
        }

        game.cleanupBattle();
    }

    public boolean canRecruit(Legion legion)
    {
        return legion.hasMoved() && legion.getHeight() < 7
            && !legion.hasRecruited() && !legion.getPlayer().isDead()
            && !findEligibleRecruits(legion, legion.getCurrentHex()).isEmpty();
    }

    /** Used for human players only.  */
    public void doRecruit(Legion legion)
    {
        if (legion == null || !isMyTurn() || !isMyLegion(legion))
        {
            // TODO is it good to return quietly here? It seems the method should
            // not have been called in the first place
            return;
        }

        if (legion.hasRecruited() || legion.getSkipThisTime())
        {
            gui.undoRecruit(legion);
            return;
        }

        if (!canRecruit(legion))
        {
            // TODO is it good to return quietly here? It seems the method should
            // not have been called in the first place
            return;
        }

        String hexDescription = legion.getCurrentHex().getDescription();

        CreatureType recruit = gui.doPickRecruit(legion, hexDescription);
        if (legion.getSkipThisTime())
        {
            // TODO handle better, see ClientGUI.markLegionAsSkipRecruit
            return;
        }

        if (recruit == null)
        {
            return;
        }

        String recruiterName = findRecruiterName(legion, recruit,
            hexDescription);
        if (recruiterName == null)
        {
            return;
        }

        doRecruit(legion, recruit.getName(), recruiterName);
    }

    // TODO use CreatureType instead of String
    public void doRecruit(Legion legion, String recruitName,
        String recruiterName)
    {
        CreatureType recruited = (recruitName == null) ? null : game
            .getVariant().getCreatureByName(recruitName);

        // TODO solve this better?
        if ("none".equals(recruiterName))
        {
            recruiterName = null;
        }
        CreatureType recruiter = (recruiterName == null) ? null : game
            .getVariant().getCreatureByName(recruiterName);
        // Call server even if some arguments are null, to get past
        // reinforcement.
        server.doRecruit(new Recruitment(legion, recruited, recruiter));
    }

    /** Always needs to call server.doRecruit(), even if no recruit is
     *  wanted, to get past the reinforcing phase. */
    public void doReinforce(Legion legion)
    {
        if (autoplay.autoReinforce())
        {
            ai.reinforce(legion);
        }
        else
        {
            String hexDescription = legion.getCurrentHex().getDescription();

            CreatureType recruit = gui.doPickRecruit(legion, hexDescription);

            String recruiterName = null;
            if (recruit != null)
            {
                recruiterName = findRecruiterName(legion, recruit,
                    hexDescription);
            }
            doRecruit(legion, (recruit == null) ? null : recruit.getName(),
                recruiterName);
        }
    }

    public void didRecruit(Legion legion, CreatureType recruit,
        CreatureType recruiter, int numRecruiters)
    {
        List<CreatureType> recruiters = new ArrayList<CreatureType>();
        if (numRecruiters >= 1 && recruiter != null)
        {
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiters.add(recruiter);
            }
            revealCreatures(legion, recruiters, Constants.reasonRecruiter);
        }
        String reason = (getBattleSite() != null ? Constants.reasonReinforced
            : Constants.reasonRecruited);

        addCreature(legion, recruit, reason);
        legion.setRecruit(recruit);

        if (redoOngoing || !replayOngoing)
        {
            gui.actOnDidRecruit(legion, recruit, recruiters, reason);
        }
    }

    public void undoRecruit(Legion legion)
    {
        server.undoRecruit(legion);
    }

    public void undidRecruit(Legion legion, CreatureType recruit)
    {
        boolean wasReinforcement;
        if (game.isBattleOngoing())
        {
            wasReinforcement = true;
            gui.eventViewerCancelReinforcement(recruit, getTurnNumber());
        }
        else
        {
            // normal undoRecruit
            wasReinforcement = false;
            legion.removeCreature(recruit);
        }

        legion.setRecruit(null);
        if (!isReplayOngoing() || isRedoOngoing())
        {
            gui.actOnUndidRecruitPart(legion, wasReinforcement,
                getTurnNumber());
        }
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

    /** null means cancel.  "none" means no recruiter (tower creature). */
    private String findRecruiterName(Legion legion, CreatureType recruit,
        String hexDescription)
    {
        String recruiterName = null;

        List<String> recruiters = findEligibleRecruiters(legion, recruit);

        int numEligibleRecruiters = recruiters.size();
        if (numEligibleRecruiters == 0)
        {
            // A warm body recruits in a tower.
            recruiterName = "none";
        }
        else if (autoplay.autoPickRecruiter() || numEligibleRecruiters == 1)
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

    // TODO move to GameClientSide
    private void resetLegionMovesAndRecruitData()
    {
        for (Player player : game.getPlayers())
        {
            for (Legion legion : player.getLegions())
            {
                legion.setMoved(false);
                legion.setTeleported(false);
                legion.setRecruit(null);
            }
        }
    }

    public void setBoardActive(boolean val)
    {
        gui.setBoardActive(val);
    }

    /**
     * Called by server when activePlayer changes
     */
    public void setupTurnState(Player activePlayer, int turnNumber)
    {
        // "turn state is first time initialized" means also the game setup
        // is completed. GUI might now e.g. enable game saving menu actions.
        if (game.isTurnStateStillUninitialized())
        {
            gui.actOnGameStarting();
        }
        game.setActivePlayer(activePlayer);
        game.setTurnNumber(turnNumber);
        gui.actOnTurnOrPlayerChange(this, turnNumber, game.getActivePlayer());
    }

    /* Quick way to disable a code block; for simple "if (false)"
     * Eclipse gives dead code warnings :-(
     */
    private boolean isTrue(boolean val)
    {
        return val;
    }
    public void prn(String text)
    {
        if (isTrue(false))
        {
            return;
        }
        System.out.println(text);
    }

    public void setupSplit(Player activePlayer, int turnNumber)
    {
        resetLegionMovesAndRecruitData();

        // Now the actual setup split stuff
        game.setPhase(Phase.SPLIT);
        numSplitsThisTurn = 0;

        gui.actOnSetupSplit();

        // kickSplit() now called by kickPhase(), because the kickXXXX
        // are now done separately, not implied in the setupXXXXX any more.
        // kickSplit();
    }

    private void kickSplit()
    {
        if (isMyTurn() && autoplay.autoSplit() && !game.isGameOver())
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
        game.setPhase(Phase.MOVE);
        gui.actOnSetupMove();
    }

    public void setupFight()
    {
        game.setPhase(Phase.FIGHT);
        gui.actOnSetupFight();

        // kickFight() now called by kickPhase(), because the kickXXXX
        // are now done separately, not implied in the setupXXXXX any more.
        // kickFight();
    }

    // TODO very similar to nextEngagement, even called redundantly...
    private void kickFight()
    {
        // TODO
        // In practice we should not get the kickXXXX from server any more
        // if it's not our turn... but this below is how it was before

        // Should not be needed, there comes a nextEngagement additionally anyway?
        /*
        if (isMyTurn())
        {
            gui.defaultCursor();
            if (autoplay.autoPickEngagements())
            {
                aiPause();
                ai.pickEngagement();
            }
            else
            {
                if (game.findEngagements().isEmpty())
                {
                    doneWithEngagements();
                }
            }
        }
        else
        {
            LOGGER
                .warning("Got called kickFight but it's not our phase? Client "
                    + getOwningPlayer().getName()
                    + ", active player "
                    + getActivePlayer().getName());
        }
        */
    }

    // TODO check overlap with kickFight()
    public void nextEngagement()
    {
        gui.highlightEngagements();
        if (isMyTurn())
        {
            // TODO search eng. first, decide then based on autoXXX
            if (autoplay.autoPickEngagements())
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
        }
    }

    /* TODO the whole Done with Engagements is nowadays probably,
     * obsolete, server does advancePhase automatically.
     * Check also GUI classes accordingly!
     */
    public void doneWithEngagements()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();
        server.doneWithEngagements();
    }

    public void setupMuster()
    {
        game.setPhase(Phase.MUSTER);
        gui.actOnSetupMuster();

        // Not here any more...
        // kickMuster();
    }

    private void kickMuster()
    {
        if (game.isPhase(Phase.MUSTER) && isMyTurn() && isAlive()
            && !isReplayOngoing())
        {
            if (noRecruitActionPossible())
            {
                doneWithRecruits();
            }
            else if (autoplay.autoRecruit())
            {
                // Note that this fires all doRecruit calls in one row,
                // i.e. does NOT wait for callback from server.
                ai.muster();

                // For autoRecruit alone, do not automatically say we are done.
                // Allow humans to override. But full autoPlay be done.
                if (isAutoplayActive())
                //|| sansLordAutoBattleApplies())
                {
                    doneWithRecruits();
                }
            }
        }
    }

    public void setupBattleSummon(Player battleActivePlayer,
        int battleTurnNumber)
    {
        getBattleCS().setupPhase(BattlePhase.SUMMON, battleActivePlayer,
            battleTurnNumber);
        gui.actOnSetupBattleSummon();
    }

    public void setupBattleRecruit(Player battleActivePlayer,
        int battleTurnNumber)
    {
        getBattleCS().setupPhase(BattlePhase.RECRUIT, battleActivePlayer,
            battleTurnNumber);
        gui.actOnSetupBattleRecruit();
    }

    public void setupBattleMove(Player battleActivePlayer, int battleTurnNumber)
    {
        // TODO clean up order of stuff here

        getBattleCS().setBattleActivePlayer(battleActivePlayer);
        getBattleCS().setBattleTurnNumber(battleTurnNumber);

        // Just in case the other player started the battle
        // really quickly.
        gui.cleanupNegotiationDialogs();
        getBattleCS().resetAllBattleMoves();
        getBattleCS().setBattlePhase(BattlePhase.MOVE);

        gui.actOnSetupBattleMove();

        if (isMyBattlePhase()
            && (isAutoplayActive() || sansLordAutoBattleApplies()))
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
        BattleCritter critter = cm.getCritter();
        BattleHex hex = cm.getEndingHex();
        doBattleMove(critter.getTag(), hex);
        aiPause();
    }

    private void retryFailedBattleMoves()
    {
        bestMoveOrder = failedBattleMoves;
        failedBattleMoves = null;
        ai.retryFailedBattleMoves(bestMoveOrder);
        kickBattleMove();
    }

    public BattleClientSide getBattleCS()
    {
        return game.getBattleCS();
    }

    /** Used for both strike and strikeback. */
    public void setupBattleFight(BattlePhase battlePhase,
        Player battleActivePlayer)
    {
        getBattleCS().setupBattleFight(battlePhase, battleActivePlayer);
        gui.actOnSetupBattleFight();
        doAutoStrikes();
    }

    /** Create marker if necessary, and place it in hexLabel. */
    public void tellLegionLocation(Legion legion, MasterHex hex)
    {
        legion.setCurrentHex(hex);
        gui.actOnTellLegionLocation(legion, hex);
    }

    public PlayerColor getColor()
    {
        return getOwningPlayer().getColor();
    }

    public String getShortColor()
    {
        return getColor().getShortName();
    }

    public Player getBattleActivePlayer()
    {
        return game.getBattleActivePlayer();
    }

    public Engagement getEngagement()
    {
        return game.getEngagement();
    }

    public boolean isEngagementStartupOngoing()
    {
        return engagementStartupOngoing;
    }

    // public for IOracle
    // TODO placeholder, move at some point fully to Game ?
    public Legion getDefender()
    {
        return game.getEngagement().getDefendingLegion();
    }

    // public for IOracle
    // TODO placeholder, move at some point fully to Game ?
    public Legion getAttacker()
    {
        return game.getEngagement().getAttackingLegion();
    }

    // public for IOracle
    public MasterHex getBattleSite()
    {
        return game.getBattleSite();
    }

    public BattlePhase getBattlePhase()
    {
        return game.getBattlePhase();
    }

    // public for IOracle and BattleBoard
    public int getBattleTurnNumber()
    {
        return game.getBattleTurnNumber();
    }

    public void doBattleMove(int tag, BattleHex hex)
    {
        server.doBattleMove(tag, hex);
    }

    public void undoBattleMove(BattleHex hex)
    {
        server.undoBattleMove(hex);
    }

    private void markBattleMoveSuccessful(int tag, BattleHex endingHex)
    {
        if (bestMoveOrder != null)
        {
            Iterator<CritterMove> it = bestMoveOrder.iterator();
            while (it.hasNext())
            {
                CritterMove cm = it.next();
                if (tag == cm.getTag() && endingHex.equals(cm.getEndingHex()))
                {
                    // Remove this CritterMove from the list to show
                    // that it doesn't need to be retried.
                    it.remove();
                }
            }
        }
        kickBattleMove();
    }

    private void handleFailedBattleMove(String errmsg)
    {
        LOGGER.log(Level.FINEST, owningPlayer.getName()
            + "handleFailedBattleMove");
        if (isAutoplayActive() || sansLordAutoBattleApplies())
        {
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
        else
        {
            gui.showMessageDialogAndWait(errmsg);
        }
        gui.actOnPendingBattleMoveOver();
    }

    public void tellBattleMove(int tag, BattleHex startingHex,
        BattleHex endingHex, boolean undo)
    {
        boolean rememberForUndo = false;

        boolean isMyCritter = owningPlayer.equals(game.getPlayerByTag(tag));
        if (isMyCritter && !undo)
        {
            rememberForUndo = true;
            if (isAutoplayActive() || sansLordAutoBattleApplies())
            {
                markBattleMoveSuccessful(tag, endingHex);
            }
        }
        BattleCritter battleUnit = getBattleCS().getBattleUnit(tag);
        if (battleUnit != null)
        {
            battleUnit.setCurrentHex(endingHex);
            battleUnit.setMoved(!undo);
        }

        gui.actOnTellBattleMove(startingHex, endingHex, rememberForUndo);
    }

    /** Attempt to have critter tag strike the critter in hex. */
    public void strike(int tag, BattleHex hex)
    {
        gui.resetStrikeNumbers();
        server.strike(tag, hex);
    }

    /** Attempt to apply carries to the critter in hex. */
    public void applyCarries(BattleHex hex)
    {
        server.applyCarries(hex);
        gui.actOnApplyCarries(hex);
    }

    public boolean isInContact(BattleCritter critter, boolean countDead)
    {
        return game.getBattleCS().isInContact(critter, countDead);
    }

    // TODO move to Battle
    /** Return a set of hexLabels. */
    public Set<BattleHex> findMobileCritterHexes()
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (BattleCritter critter : getActiveBattleUnits())
        {
            if (!critter.hasMoved() && !isInContact(critter, false))
            {
                set.add(critter.getCurrentHex());
            }
        }
        return set;
    }

    // TODO move to Battle
    /** Return a set of BattleUnits. */
    public Set<BattleUnit> findMobileBattleUnits()
    {
        Set<BattleUnit> set = new HashSet<BattleUnit>();
        for (BattleUnit battleUnit : getActiveBattleUnits())
        {
            if (!battleUnit.hasMoved() && !isInContact(battleUnit, false))
            {
                set.add(battleUnit);
            }
        }
        return set;
    }

    public Set<BattleHex> showBattleMoves(BattleCritter battleCritter)
    {
        return battleMovement.showMoves(battleCritter);
    }

    // TODO move to Battle
    public Set<BattleHex> findCrittersWithTargets()
    {
        return getBattleCS().findCrittersWithTargets(this);
    }

    // TODO move to Battle
    public Set<BattleHex> findStrikes(int tag)
    {
        return getBattleCS().findTargets(tag);
    }

    // Mostly for SocketClientThread
    public Player getPlayerByName(String name)
    {
        return game.getPlayerByName(name);
    }

    // TODO active or not would probably work better as state in PlayerState
    // NOTTODO ... I'd say it's not about "is one active or not" but which one
    // is the one... ?

    public Player getActivePlayer()
    {
        return game.getActivePlayer();
    }

    public Phase getPhase()
    {
        return game.getPhase();
    }

    // public for IOracle
    public int getTurnNumber()
    {
        return game.getTurnNumber();
    }

    public int getNumSplitsThisTurn()
    {
        return numSplitsThisTurn;
    }

    public void askPickColor(List<PlayerColor> colorsLeft)
    {
        if (autoplay.autoPickColor())
        {
            // Convert favorite colors from a comma-separated string to a list.
            String favorites = options.getStringOption(Options.favoriteColors);
            List<PlayerColor> favoriteColors = null;
            if (favorites != null)
            {
                favoriteColors = PlayerColor.getByName(Split.split(',',
                    favorites));
            }
            else
            {
                favoriteColors = new ArrayList<PlayerColor>();
            }
            PlayerColor color = ai.pickColor(colorsLeft, favoriteColors);
            answerPickColor(color);
        }
        else
        {
            // calls answerPickColor
            gui.doPickColor(owningPlayer.getName(), colorsLeft);
        }
    }

    public void answerPickColor(PlayerColor color)
    {
        setColor(color);
        server.assignColor(color);
    }

    public void askPickFirstMarker()
    {
        Set<String> markersAvailable = getOwningPlayer().getMarkersAvailable();
        if (autoplay.autoPickMarker())
        {
            String markerId = ai.pickMarker(markersAvailable,
                getOwningPlayer().getShortColor());
            assignFirstMarker(markerId);
        }
        else
        {
            gui.doPickInitialMarker(markersAvailable);
        }
    }

    public void assignFirstMarker(String markerId)
    {
        server.assignFirstMarker(markerId);
    }

    public void log(String message)
    {
        LOGGER.log(Level.INFO, message);
    }

    /**
     *
     * Called by MasterBoard.actOnLegion() when human user clicked on a
     * legion to split it. This method here then:
     * Verifies that splitting is legal and possible at all;
     * Then get a child marker selected (either by dialog, or if
     * autoPickMarker set, ask AI to pick one);
     * If childMarkerId selection was not canceled (returned non-null),
     * bring up the split dialog (which creatures go into which legion);
     * and if that returns a list (not null) then call doSplit(...,...,...)
     * which sends the request to server.
     *
     * @param parent The legion selected to split
     */
    public void doSplit(Legion parent)
    {
        LOGGER.log(Level.FINER,
            "Client.doSplit, marker=" + parent.getMarkerId());

        if (!isMyTurn())
        {
            LOGGER.log(Level.SEVERE, "Not my turn!");
            // TODO I think this is useless here
            kickSplit();
            return;
        }
        // Can't split other players' legions.
        if (!isMyLegion(parent))
        {
            LOGGER.log(Level.SEVERE, "Not my legion!");
            // TODO is this needed here?
            kickSplit();
            return;
        }
        Set<String> markersAvailable = getOwningPlayer().getMarkersAvailable();
        // Need a legion marker to split.
        if (markersAvailable.size() < 1)
        {
            LOGGER.finer("no legion markers");
            gui.showMessageDialogAndWait("No legion markers");
            // TODO is this useful here?
            kickSplit();
            return;
        }
        LOGGER.finest("Legion markers: " + markersAvailable.size());

        if (parent.getSplitRequestSent())
        {
            LOGGER.finer("doSplit(): split request pending");
            gui.showMessageDialogAndWait("Split request still pending\n(waiting for server response)");
            // TODO is this useful here?
            kickSplit();
            return;
        }

        if (parent.getUndoSplitRequestSent())
        {
            LOGGER.finer("doSplit(): undo split request pending");
            gui.showMessageDialogAndWait("Undo-split request still pending\n(waiting for server response)");
            // TODO is this useful here?
            kickSplit();
            return;
        }

        // Legion must be tall enough to split.
        if (parent.getHeight() < 4)
        {
            gui.showMessageDialogAndWait("Legion is too short to split");
            kickSplit();
            return;
        }

        // Enforce only one split on turn 1.
        if (getTurnNumber() == 1 && numSplitsThisTurn > 0)
        {
            gui.showMessageDialogAndWait("Can only split once on the first turn");
            kickSplit();
            return;
        }

        String childId = null;

        if (autoplay.autoPickMarker())
        {
            childId = ai.pickMarker(markersAvailable, getOwningPlayer()
                .getShortColor());
            doTheSplitting(parent, childId);
        }
        else
        {
            gui.doPickSplitMarker(parent, markersAvailable);
        }
    }

    public void doTheSplitting(Legion parent, String childId)
    {
        if (childId != null)
        {
            List<CreatureType> crestures = gui.doPickSplitLegion(parent,
                childId);

            if (crestures != null)
            {
                sendDoSplitToServer(parent, childId, crestures);
                gui.actOnSplitRelatedRequestSent();
            }
        }
    }

    /** Called by AI and by doSplit() */
    public void sendDoSplitToServer(Legion parent, String childMarkerId,
        List<CreatureType> creatures)
    {
        LOGGER.log(Level.FINER,
            "Client.doSplit: parent='" + parent.getMarkerId() + "', child='"
                + childMarkerId + "', splitoffs=" + Glob.glob(",", creatures));
        parent.setSplitRequestSent(true);
        server.doSplit(parent, childMarkerId, creatures);
    }

    public Set<MasterHex> findPendingSplitHexes()
    {
        Set<MasterHex> hexes = new HashSet<MasterHex>();
        for (Legion l : getOwningPlayer().getPendingSplitLegions())
        {
            hexes.add(l.getCurrentHex());
        }
        return hexes;
    }

    public Set<MasterHex> findPendingUndoSplitHexes()
    {
        Set<MasterHex> hexes = new HashSet<MasterHex>();
        HashSet<Legion> legions = getOwningPlayer()
            .getPendingUndoSplitLegions();
        for (Legion l : legions)
        {
            hexes.add(l.getCurrentHex());
        }
        return hexes;
    }

    /**
     * Callback from server after any successful split.
     *
     * TODO childHeight is probably redundant now that we pass the legion object
     */
    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<CreatureType> splitoffs, int turn)
    {
        LOGGER.log(Level.FINEST, "Client.didSplit " + hex + " " + parent + " "
            + child + " " + childHeight + " " + turn);

        ((LegionClientSide)parent).split(childHeight, child, turn);
        child.setCurrentHex(hex);

        if (isMyLegion(child))
        {
            parent.setSplitRequestSent(false);
            numSplitsThisTurn++;
            getOwningPlayer().removeMarkerAvailable(child.getMarkerId());
        }

        gui.actOnDidSplit(turn, parent, child, hex);

        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet.
        if (isMyTurn() && game.isPhase(Phase.SPLIT) && !replayOngoing
            && autoplay.autoSplit() && !game.isGameOver())
        {
            boolean done = ai.splitCallback(parent, child);
            if (done)
            {
                doneWithSplits();
            }
        }
    }

    // because of synchronization issues we need to
    // be able to pass an undo split request to the server even if it is not
    // yet in the client UndoStack
    public void undoSplit(Legion splitoff)
    {
        server.undoSplit(splitoff);
        splitoff.setUndoSplitRequestSent(true);
        LOGGER.log(Level.FINEST, "called server.undoSplit");
    }

    public void undidSplit(Legion splitoff, Legion survivor, int turn)
    {
        ((LegionClientSide)survivor).merge(splitoff);
        removeLegion(splitoff);
        survivor.getPlayer().addMarkerAvailable(splitoff.getMarkerId());

        // do the eventViewer stuff before the board, so we are sure to get
        // a repaint.

        if (!replayOngoing || redoOngoing)
        {
            gui.eventViewerUndoEvent(splitoff, survivor, turn);
        }

        if (isMyTurn())
        {
            numSplitsThisTurn--;
            survivor.setUndoSplitRequestSent(false);
        }

        gui.actOnUndidSplit(survivor, turn);

        if (isMyTurn() && game.isPhase(Phase.SPLIT) && !replayOngoing
            && autoplay.autoSplit() && !game.isGameOver())
        {
            boolean done = ai.splitCallback(null, null);
            if (done)
            {
                doneWithSplits();
            }
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

    private CreatureType figureTeleportingLord(Legion legion, MasterHex hex)
    {
        List<CreatureType> lords = listTeleportingLords(legion, hex);
        switch (lords.size())
        {
            case 0:
                assert false : "We should have at least one teleporting lord";
                return null;

            case 1:
                return lords.get(0);

            default:
                if (autoplay.autoPickLord())
                {
                    return lords.get(0);
                }
                else
                {
                    return gui.doPickLord(lords);
                }
        }
    }

    /**
     * List the lords eligible to teleport this legion to hexLabel.
     */
    private List<CreatureType> listTeleportingLords(Legion legion,
        MasterHex hex)
    {
        // Needs to be a List not a Set so that it can be passed as
        // an imageList.
        List<CreatureType> lords = new ArrayList<CreatureType>();

        // Titan teleport
        List<Legion> legions = getGame().getLegionsByHex(hex);
        if (!legions.isEmpty())
        {
            Legion legion0 = legions.get(0);
            if (legion0 != null && !isMyLegion(legion0))
            {
                for (Creature creature : legion.getCreatures())
                {
                    if (creature.getType().isTitan())
                    {
                        lords.add(creature.getType());
                    }
                }
            }
        }

        // Tower teleport
        else
        {
            for (Creature creature : legion.getCreatures())
            {
                CreatureType creatureType = creature.getType();
                if (creatureType != null && creatureType.isLord()
                    && !lords.contains(creatureType))
                {
                    lords.add(creatureType);
                }
            }
        }
        return lords;
    }

    /** If the move looks legal, forward it to server and return true;
     *  otherwise returns false.
     *  Also let user or AI pick teleporting Lord and/or entry side,
     *  if relevant.
     */
    public boolean doMove(Legion mover, MasterHex hex)
    {
        if (mover == null)
        {
            return false;
        }

        // This check was earlier after picking entry side. Logically
        // it makes more sense to check it first, instead of first let user
        // pick something and reject move then still.
        // However technically it's irrelevant, since the "pick target hex"
        // logic does not even offer same hex if there is another friendly
        // legion (= after a split)
        // Doing the check here still anyway, since in Infinite e.g. for some
        // hexes rolling a 6 allows in theory two entrysides, and to catch
        // that (prevent it from asking user to pick entry side), need the
        // number of friendly legions value already early.

        // if this hex is already occupied, return false
        int friendlyLegions = game.getFriendlyLegions(hex, getActivePlayer())
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

        EntrySide entrySide = null;

        Set<EntrySide> entrySides = movement.listPossibleEntrySides(mover,
            hex, teleport);
        if (entrySides.isEmpty())
        {
            LOGGER.warning("Attempted move to " + hex
                + " but entrySides is empty?");
            return false;
        }
        else if (!game.isOccupied(hex))
        {
            // If unoccupied it does not really matter, just take one.
            entrySide = entrySides.iterator().next();
        }
        else if (friendlyLegions == 1 && hex.equals(mover.getCurrentHex()))
        {
            // it's only we-self there, e.g. on a six in infinite returning
            // to same hex on two possible ways.
            // dito: entry side does not really matter, just take one.
            entrySide = entrySides.iterator().next();
        }
        else if (autoplay.autoPickEntrySide())
        {
            entrySide = ai.pickEntrySide(hex, mover, entrySides);
        }
        else
        {
            entrySide = gui.doPickEntrySide(hex, entrySides);
            if (entrySide == null)
            {
                return false;
            }
        }

        CreatureType teleportingLord = null;
        if (teleport)
        {
            teleportingLord = figureTeleportingLord(mover, hex);
            if (teleportingLord == null)
            {
                return false;
            }
        }

        // if disconnected, prevent this one, since it would mess up things
        // (e.g. pending move tracking)
        if (ensureThatConnected())
        {
            gui.setMovePending(mover, mover.getCurrentHex(), hex);
            server.doMove(mover, hex, entrySide, teleport, teleportingLord);
        }
        return true;
    }

    public void didMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, EntrySide entrySide, boolean teleport,
        CreatureType teleportingLord, boolean splitLegionHasForcedMove)
    {
        legion.setCurrentHex(currentHex);
        legion.setMoved(true);
        legion.setEntrySide(entrySide);
        legion.setTeleported(teleport);

        if (isReplayBeforeRedo())
        {
            return;
        }

        gui.actOnDidMove(legion, startingHex, currentHex, teleport,
            teleportingLord, splitLegionHasForcedMove);

        kickMoves();
    }

    public void undoMove(Legion legion)
    {
        server.undoMove(legion);
    }

    public void undidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove)
    {
        legion.setRecruit(null);
        legion.setCurrentHex(currentHex);
        legion.setMoved(false);
        boolean didTeleport = legion.hasTeleported();
        legion.setTeleported(false);

        gui.actOnUndidMove(legion, formerHex, currentHex,
            splitLegionHasForcedMove, didTeleport);

    }

    public void doneWithMoves()
    {
        if (!isMyTurn())
        {
            return;
        }
        aiPause();

        gui.actOnDoneWithMoves();
        server.doneWithMoves();
    }

    public void relocateLegion(Legion legion, MasterHex destination)
    {
        localServer.getGame().editModeRelocateLegion(legion.getMarkerId(),
            destination.getLabel());
    }

    /** Legion target summons unit from Legion donor.
     *  @param summonInfo A SummonInfo object that contains the values
     *                    for target, donor and unit.
     */
    public void doSummon(SummonInfo summonInfo)
    {
        assert summonInfo != null : "SummonInfo object must not be null!";

        if (summonInfo.noSummoningWanted())
        {
            // could also use getXXX from object...
            server.doSummon(null);
        }
        else
        {
            Summoning event = new Summoning(summonInfo.getTarget(),
                summonInfo.getDonor(), summonInfo.getUnit());
            server.doSummon(event);
        }
        // Highlight engagements and repaint
        gui.actOnDoSummon();
    }

    public void didSummon(Legion summoner, Legion donor, CreatureType summon)
    {
        // Create summon event
        gui.didSummon(summoner, donor, summon);
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

    /**
     * Return a list of Creatures (ignore reservations).
     */
    public List<CreatureType> findEligibleRecruits(Legion legion, MasterHex hex)
    {
        return findEligibleRecruits(legion, hex, false);
    }

    /**
     * Return a list of Creatures and consider reservations if wanted.
     *
     * @param legion The legion to recruit with.
     * @param hex The hex in which to recruit (not necessarily the same as the legion's position). Not null.
     * @param considerReservations Flag to determine if reservations should be considered.
     * @return A list of possible recruits for the legion in the hex.
     */
    public List<CreatureType> findEligibleRecruits(Legion legion,
        MasterHex hex, boolean considerReservations)
    {
        // TODO why not: assert legion != null;
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

        for (CreatureType creature : tempRecruits)
        {
            if (getTurnNumber() != 1
                || !options.getOption(Options.noFirstTurnWarlockRecruit)
                || !creature.getName().equals("Warlock"))
            {
                for (CreatureType lesser : recruiters)
                {
                    if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                        creature, terrain, hex) <= ((LegionClientSide)legion)
                        .numCreature(lesser))
                        && (recruits.indexOf(creature) == -1))
                    {
                        recruits.add(creature);
                    }
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
        CreatureType recruit)
    {
        if (recruit == null)
        {
            return new ArrayList<String>();
        }

        Set<CreatureType> recruiters;
        MasterHex hex = legion.getCurrentHex();
        MasterBoardTerrain terrain = hex.getTerrain();

        recruiters = new HashSet<CreatureType>(
            TerrainRecruitLoader.getPossibleRecruiters(terrain, hex));
        Iterator<CreatureType> it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType possibleRecruiter = it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain, hex);
            if (needed < 1 || needed > legion.numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }

        List<String> strings = new ArrayList<String>();
        for (CreatureType creature : recruiters)
        {
            strings.add(creature.getName());
        }
        return strings;
    }

    /**
     * Return a set of hexes with legions that can (still) muster anything
     * and are not marked as skip.
     */
    public Set<MasterHex> getPossibleRecruitHexes()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();

        for (Legion legion : game.getActivePlayer().getLegions())
        {
            if (canRecruit(legion) && !legion.getSkipThisTime())
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /** Return a set of hexLabels with legions that could do a recruit
     *  or undo recruit. Used for "if there is nothing to do in this recruit
     *  phase, muster phase can immediately be "doneWithRecruit".
     */
    private Set<MasterHex> getPossibleRecruitActionHexes()
    {
        Set<MasterHex> result = new HashSet<MasterHex>();

        for (Legion legion : game.getActivePlayer().getLegions())
        {
            if (canRecruit(legion) || legion.hasRecruited())
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /**
     * Check whether any legion has possibility to recruit at all,
     * no matter whether it could or has already.
     * If there is none, autoDone can automatically be done with recruit
     * phase; but if there is something (e.g. autoRecruit has recruited
     * something, allow human to override/force him to really confirm "Done".
     *
     * @return Whether there is any legion that could recruit or undoRecruit
     */
    public boolean noRecruitActionPossible()
    {
        return getPossibleRecruitActionHexes().isEmpty();
    }

    public MovementClientSide getMovement()
    {
        return movement;
    }

    /** Return a set of hexLabels. */
    public Set<MasterHex> listTeleportMoves(Legion legion)
    {
        MasterHex hex = legion.getCurrentHex();
        return movement.listTeleportMoves(legion, hex, game.getMovementRoll());
    }

    /** Return a set of hexLabels. */
    public Set<MasterHex> listNormalMoves(Legion legion)
    {
        return movement.listNormalMoves(legion, legion.getCurrentHex(),
            game.getMovementRoll());
    }

    /**
     * Returns status of client's legions
     *
     * @param legionStatus  an array of integers with various status
     * states to be set. Array should be initialized to all zeroes
     *
     * Current array contents:
     *   [Constants.legionStatusCount] == count of legions
     *   [Constants.legionStatusMoved] == legions that have moved
     *   [Constants.legionStatusBlocked] == unmoved legions with no legal move
     *   [Constants.legionStatusNotVisitedSkippedBlocked] == legions that have not been moved,
     *            are not blocked and have not been skipped
     */
    public void legionsNotMoved(int legionStatus[], boolean have_roll)
    {
        for (Legion legion : game.getActivePlayer().getLegions())
        {
            legionStatus[Constants.legionStatusCount]++;
            // If don't have roll, can't have moved or been marked skipped
            // Can't tell if blocked
            if (have_roll == true)
            {
                if (legion.hasMoved())
                {
                    legionStatus[Constants.legionStatusMoved]++;
                }
                else
                {
                    Set<MasterHex> teleport = listTeleportMoves(legion);
                    Set<MasterHex> normal = listNormalMoves(legion);
                    if (teleport.isEmpty() && normal.isEmpty())
                    {
                        legionStatus[Constants.legionStatusBlocked]++;
                    }
                    else
                    {
                        if (!legion.getVisitedThisPhase()
                            && !legion.getSkipThisTime())
                        {
                            legionStatus[Constants.legionStatusNotVisitedSkippedBlocked]++;
                        }
                    }
                }
            }
        }
    }

    public Set<MasterHex> findUnmovedLegionHexes(
        boolean considerSkippedAsMoved, HashSet<Legion> pendingLegions)
    {
        Set<MasterHex> result = new HashSet<MasterHex>();
        for (Legion legion : game.getActivePlayer().getLegions())
        {
            if (!legion.hasMoved()
                && !(considerSkippedAsMoved && legion.getSkipThisTime())
                && !pendingLegions.contains(legion))
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    /**
     * Return a set of hexLabels for the active player's legions with
     * 7 or more creatures, and which are not marked as skip this turn.
     */
    public Set<MasterHex> findTallLegionHexes()
    {
        return findTallLegionHexes(7, false);
    }

    /**
     * Return a set of hexLabels for the active player's legions with
     * minHeight or more creatures.
     *
     * @param ignoreSkipFlag Set to true, legion will be considered even if
     * it was marked as "skip this time".
     */
    public Set<MasterHex> findTallLegionHexes(int minHeight,
        boolean ignoreSkipFlag)
    {
        Set<MasterHex> result = new HashSet<MasterHex>();

        for (Legion legion : game.getActivePlayer().getLegions())
        {
            if (legion.getHeight() >= minHeight
                && (ignoreSkipFlag || !legion.getSkipThisTime()))
            {
                result.add(legion.getCurrentHex());
            }
        }
        return result;
    }

    public void notifyServer()
    {
        if (!isRemote())
        {
            localServer.setPauseState(false);
            /* Calling it locally is "safer". If the stopGame is sent via the
             * socket connection, the call of disposeClientOriginated below
             * might be handled so fast, that it's connection closed exception
             * on server side causes the server to handle a withdrawal for it.
             * And if there is only two players AND autoQuit is set, this might
             * terminate the application despite the fact that one wanted to do
             * e.g. New Game or Load Game.
             */
            localServer.initiateQuitGame();
        }
        else
        {
            // If remote clients do New Game etc, this does not directly cause
            // the server to do anything as above, so: stopGame commented out.

            // If after this remote client gone there is only one player left,
            // i.e. game is then over, server side will act accordingly.

            //server.stopGame();
        }
        disposeClientOriginated();
    }

    public boolean isMyLegion(Legion legion)
    {
        return !spectator && owningPlayer.equals(legion.getPlayer());
    }

    public boolean isMyTurn()
    {
        return !spectator && owningPlayer.equals(getActivePlayer());
    }

    public boolean isFightPhase()
    {
        if (game != null)
        {
            return game.isPhase(Phase.FIGHT);
        }
        return false;
    }

    public boolean isMyBattlePhase()
    {
        // check also for phase, because delayed callbacks could come
        // after our phase is over but activePlayerName not updated yet
        return isAlive() && owningPlayer.equals(getBattleActivePlayer())
            && game.isPhase(Phase.FIGHT);
    }

    public void pingRequest(long requestTime)
    {
        // Dummy, SocketClientThread handles this already.
    }

    public void logMsgToServer(String severity, String message)
    {
        server.logMsgToServer(severity, message);
    }

    public boolean testBattleMove(BattleCritter battleUnit, BattleHex hex)
    {
        if (showBattleMoves(battleUnit).contains(hex))
        {
            battleUnit.setCurrentHex(hex);
            return true;
        }
        return false;
    }


    /** Wait for aiDelay. */
    private void aiPause()
    {
        // TODO why is this not set up once, when Client is created?
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
        // If not in autoPlay mode, set it to minimum, because then it is a
        // human player who just uses some autoXXX functionality,
        // and we don't want a human to have to wait after certain activities
        // the AI does for him.
        if (!isAutoplayActive() || delay < Constants.MIN_AI_DELAY)
        {
            delay = Constants.MIN_AI_DELAY;
        }
        else if (delay > Constants.MAX_AI_DELAY)
        {
            delay = Constants.MAX_AI_DELAY;
        }
    }

    public Game getGame()
    {
        return game;
    }

    public GameClientSide getGameClientSide()
    {
        return game;
    }

    public Options getOptions()
    {
        return options;
    }

    public String getClientName()
    {
        if (getOwningPlayer() != null)
        {
            return getOwningPlayer().getName();
        }
        else
        {
            return "<ownplayernotset>";
        }
    }

    /** TODO get from Variant instead of static TerrainRecruitLoader access
     *  Just forwarding the query, to get at least the GUI classes get rid of
     *  dependency to static TerrainRecruitLoader access.
     *
     * {@link TerrainRecruitLoader#getPossibleRecruits(MasterBoardTerrain, MasterHex)}
     */
    public List<CreatureType> getPossibleRecruits(MasterBoardTerrain terrain,
        MasterHex hex)
    {
        return TerrainRecruitLoader.getPossibleRecruits(terrain, hex);

    }

    /** TODO get from Variant instead of static TerrainRecruitLoader access
     *  Just forwarding the query, to get at least the GUI classes get rid of
     *  dependency to static TerrainRecruitLoader access.
     *
     * {@link TerrainRecruitLoader#numberOfRecruiterNeeded(CreatureType,
        CreatureType, MasterBoardTerrain, MasterHex)}
     */
    public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterBoardTerrain terrain, MasterHex hex)
    {
        return TerrainRecruitLoader.numberOfRecruiterNeeded(recruiter,
            recruit, terrain, hex);
    }

    /**
     * Return a collection of all possible terrains.
     *
     * @return A collection containing all instances of {@link MasterBoardTerrain}.
     */
    public Collection<MasterBoardTerrain> getTerrains()
    {
        return game.getVariant().getTerrains();
    }

    public static class ConnectionInitException extends Exception
    {
        public ConnectionInitException(String reason)
        {
            super(reason);
        }
    }

    public void setPreferencesCheckBoxValue(String name, boolean value)
    {
        gui.setPreferencesCheckBoxValue(name, value);
    }

    public void setPreferencesRadioButtonValue(String name, boolean value)
    {
        gui.setPreferencesRadioButtonValue(name, value);
    }

    public void editAddCreature(String markerId, String creatureType)
    {
        localServer.getGame().editModeAddCreature(markerId, creatureType);
    }

    public void editRemoveCreature(String markerId, String creatureType)
    {
        localServer.getGame().editModeRemoveCreature(markerId, creatureType);
    }

    public void editRelocateLegion(String markerId, String hexLabel)
    {
        localServer.getGame().editModeRelocateLegion(markerId, hexLabel);
    }

    public void destroyLegion(Legion legion)
    {
        server.cheatModeDestroyLegion(legion);
    }

}
