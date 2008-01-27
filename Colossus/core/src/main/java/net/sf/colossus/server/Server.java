package net.sf.colossus.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.IClient;
import net.sf.colossus.client.Proposal;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.ChildThreadManager;
import net.sf.colossus.util.Options;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.webcommon.InstanceTracker;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Class Server lives on the server side and handles all communcation with
 *  the clients.  It talks to the server classes locally, and to the Clients
 *  via the network protocol.
 *  @version $Id$
 *  @author David Ripton
 */
public final class Server implements IServer
{
    private static final Logger LOGGER = Logger.getLogger(Server.class
        .getName());
    private static StartupProgress startLog;

    private GameServerSide game;

    /**
     *  Maybe also save things like the originating IP, in case a
     *  connection breaks and we need to authenticate reconnects.
     *  Do not share these references. */
    private final List<IClient> clients = new ArrayList<IClient>();
    private final List<IClient> remoteClients = new ArrayList<IClient>();
    private final List<RemoteLogHandler> remoteLogHandlers = new ArrayList<RemoteLogHandler>();

    /** Map of players to their clients. */
    private final Map<Player, IClient> clientMap = new HashMap<Player, IClient>();

    /** Number of remote clients we're waiting for. */
    private int waitingForClients;

    /** Server socket port. */
    private final int port;

    // Cached strike information.
    private CreatureServerSide striker;
    private CreatureServerSide target;
    private int strikeNumber;
    private List<String> rolls;

    // Network stuff
    private ServerSocket serverSocket;
    // list of Socket that are currently active
    private final List<Socket> activeSocketList = Collections
        .synchronizedList(new ArrayList<Socket>());
    private int numClients;
    private int maxClients;

    /* static so that new instance of Server can destroy a
     * previously allocated FileServerThread */
    private static Thread fileServerThread = null;

    private final ChildThreadManager threadMgr;

    private boolean serverRunning = false;
    private boolean obsolete = false;
    private boolean shuttingDown = false;

    // Earlier I have locked on an Boolean object itself, 
    // which I modify... and when this is done too often,
    // e.g. in ClientSocketThread every read, it caused
    // StockOverflowException... :-/
    private final Object disposeAllClientsDoneMutex = new Object();
    private boolean disposeAllClientsDone = false;

    Server(GameServerSide game, int port)
    {
        this.game = game;
        this.port = port;

        if (startLog != null)
        {
            startLog.dispose();
            startLog = null;
        }

        if (game.getNotifyWebServer().isActive())
        {
            // If started by WebServer, do not log to StartupProgressLog.
        }
        else
        {
            startLog = new StartupProgress(this);
        }

        threadMgr = new ChildThreadManager("Server");

        waitingForClients = game.getNumLivingPlayers();
        InstanceTracker.register(this, "only one");
    }

    public ChildThreadManager getThreadMgr()
    {
        return threadMgr;
    }

    void initSocketServer()
    {
        numClients = 0;
        maxClients = game.getNumLivingPlayers();
        LOGGER
            .log(Level.FINEST, "initSocketServer maxClients = " + maxClients);
        LOGGER.log(Level.FINEST, "About to create server socket on port "
            + port);
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
                serverSocket = null;
            }
            serverSocket = new ServerSocket(port, Constants.MAX_MAX_PLAYERS);
            serverSocket.setReuseAddress(true);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE,
                "Could not create socket. Configure networking in OS, "
                    + "or check that no previous Colossus instance got stuck "
                    + "and is blocking the socket.", ex);
            System.exit(1);
        }
        createLocalClients();
    }

    /** Returns true if all clients came in, false if startup was aborted
     *  before that. */
    boolean waitForClients()
    {
        logToStartLog("\nStarting up, waiting for " + waitingForClients
            + " clients at port " + port + "\n");
        serverRunning = true;
        while (numClients < maxClients && serverRunning && !shuttingDown)
        {
            waitForConnection();
        }

        return (numClients >= maxClients);
    }

    void initFileServer()
    {
        stopFileServer();

        if (game.getNumRemoteRemaining() > 0)
        {
            fileServerThread = new FileServerThread(activeSocketList,
                port + 1, threadMgr);
            fileServerThread.start();
        }
        else
        {
            LOGGER.log(Level.FINEST,
                "No alive remote client, not launching the file server.");
        }
    }

    public void waitUntilGameFinishes()
    {
        LOGGER.log(Level.FINEST, "Server.waitUntilGameFinishes: "
            + "waitUntilAllChildThreadsGone is next");
        threadMgr.waitUntilAllChildThreadsGone();
        threadMgr.cleanup();
        LOGGER.log(Level.FINEST, "Server.waitUntilGameFinishes: "
            + "waitUntilAllChildThreadsGone completed.");
    }

    private void waitForConnection()
    {
        Socket clientSocket = null;
        try
        {
            clientSocket = serverSocket.accept();
            LOGGER.log(Level.FINE, "Got client connection from "
                + clientSocket.getInetAddress().toString());
            logToStartLog("Got client connection from IP: "
                + clientSocket.getInetAddress().toString());

            synchronized (activeSocketList)
            {
                activeSocketList.add(clientSocket);
            }
            numClients++;
        }
        catch (SocketException ex)
        {
            if (shuttingDown)
            {
                LOGGER.log(Level.FINE,
                    "Server.waitForConnection SocketException - but "
                        + "that's ok, it seems shutdown is in progress.");
            }
            return;

        }
        catch (IOException ex)
        {
            if (shuttingDown)
            {
                LOGGER.log(Level.FINE,
                    "Server.waitForConnection SocketException - but "
                        + "that's ok, it seems shutdown is in progress.");
            }
            else
            {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            }
            return;
        }

        new SocketServerThread(this, clientSocket).start();
    }

    /*
     * Stops the file server (closes FileServerSocket), 
     * disposes all clients, and closes ServerSockets
     */
    public void stopServerRunning()
    {
        serverRunning = false;
        if (!game.isOver())
        {
            game.setGameOver(true);
        }

        stopFileServer();

        // particularly to remove the loggers
        if (!clients.isEmpty())
        {
            disposeAllClients();
        }
    }

    public boolean isServerRunning()
    {
        return serverRunning;
    }

    // last SocketClientThread going down calls this
    public synchronized void stopFileServer()
    {
        LOGGER.log(Level.FINEST, "About to stop file server socket on port "
            + (port + 1));

        if (fileServerThread != null)
        {
            try
            {
                LOGGER.log(Level.FINEST, "Stopping the FileServerThread ");
                ((FileServerThread)fileServerThread).stopGoingOn();
            }
            catch (Exception e)
            {
                LOGGER.log(Level.FINE,
                    "Couldn't stop FileServerThread, got Exception: " + e);
            }
            fileServerThread = null;
        }
        else
        {
            // no fileserver running
        }
    }

    public void unregisterSocket(Socket socket)
    {
        if (activeSocketList == null)
        {
            return;
        }
        synchronized (activeSocketList)
        {
            int index = activeSocketList.indexOf(socket);
            if (index == -1)
            {
                return;
            }
            activeSocketList.remove(index);

            if (!serverRunning)
            {
                return;
            }

            // no client whatsoever left => end the game and close server stuff
            // Even if socket list is empty, client list may not be empty yet,
            // and need to empty it and close all loggers.
            if (activeSocketList.isEmpty())
            {
                LOGGER.log(Level.FINEST, "Server.unregisterSocket() thread "
                    + Thread.currentThread().getName()
                    + ": activeSocketList empty - stopping server...");
                stopServerRunning();
            }

            else if (game.getOption(Options.goOnWithoutObserver))
            {
                LOGGER
                    .log(
                        Level.FINEST,
                        "\n==========\nOne socket went away, "
                            + "but we go on because goOnWithoutObserver is set...\n");
            }
            // or, if only AI player clients left as "observers", 
            // then close everything, too 
            else if (!anyNonAiSocketsLeft())
            {
                LOGGER.log(Level.FINEST, "Server.unregisterSocket() thread "
                    + Thread.currentThread().getName()
                    + ": All connections to human or network players gone "
                    + " (no point to keep AIs running if noone sees it) "
                    + "- stopping server...");
                stopServerRunning();
            }
        }
    }

    public void setBoardVisibility(Player player, boolean val)
    {
        getClient(player).setBoardActive(val);
    }

    public boolean isClientGone(Player player)
    {
        SocketServerThread sst = (SocketServerThread)getClient(player);
        if (sst == null || sst.isGone())
        {
            return true;
        }
        return false;
    }

    private boolean anyNonAiSocketsLeft()
    {
        if (clientMap.isEmpty())
        {
            return false;
        }

        Iterator<PlayerServerSide> it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            PlayerServerSide p = it.next();
            if (!p.isAI() && !isClientGone(p))
            {
                return true;
            }
        }

        // no nonAI connected any more - return false:
        return false;
    }

    // Game calls this, when a new game starts and new Server is created,
    // so that old SocketServerThreads can see from this flag
    // that they shall not do anything any more
    // - otherwise it can happen that PlayerEliminated messages from
    // dying game reach clients of the new game...

    public void setObsolete()
    {
        this.obsolete = true;
        if (startLog != null)
        {
            startLog.cleanRef();
        }
    }

    /** 
     * Each server thread's name is set to its player's name.
     * 
     * TODO store the player as member, thus avoiding all the lookups. It has
     * to be made sure that changes are explicitly made then -- at the moment
     * at least in theory someone could change the thread's name without us
     * knowing, so we have to make sure we know this type of change all the time.
     */
    String getPlayerName()
    {
        return Thread.currentThread().getName();
    }

    private PlayerServerSide getPlayer()
    {
        return game.getPlayer(getPlayerName());
    }

    /** return true if the active player is the player owning this client */
    private boolean isActivePlayer()
    {
        return getPlayer().equals(game.getActivePlayer());
    }

    private boolean isBattleActivePlayer()
    {
        return game.getBattle() != null
            && game.getBattle().getActivePlayer() != null
            && getPlayer().equals(game.getBattle().getActivePlayer());
    }

    private void createLocalClients()
    {
        for (Player player : game.getPlayers())
        {
            if (!player.isDead()
                && !player.getType().endsWith(Constants.network))
            {
                createLocalClient(player.getName());
            }
        }
    }

    private void createLocalClient(String playerName)
    {
        LOGGER.log(Level.FINEST, "Called Server.createLocalClient() for "
            + playerName);

        // a hack to pass something into the Client constructor
        // TODO needs to be constructed properly
        Game dummyGame = new Game(null, new String[0]);

        new Client("127.0.0.1", port, dummyGame, playerName, false, false);
    }

    // TODO temporary method since SocketServerThread doesn't know the player
    // nor the game, thus can't pass a Player instance directly [Peter]
    // XXX Still TODO? Replaced the two methods by one, because 
    // SocketServerThread cannot know the Player, and for finding it the 
    // if (remote) decision needs to be done 
    // -- so folded both methods into one again to avoid the NPE [Clemens]
    synchronized void addClient(final IClient client, final String playerName,
        final boolean remote)
    {
        LOGGER.log(Level.FINEST, "Calling Server.addClient() for "
            + playerName);

        Player player = null;
        if (remote)
        {
            player = game.findNetworkPlayer(playerName);
        }
        else
        {
            player = game.getPlayer(playerName);
        }

        if (player == null)
        {
            LOGGER.log(Level.WARNING, "Could not add client, "
                + "because no Player was found for playerName " + playerName);
            return;
        }

        clients.add(client);
        clientMap.put(player, client);

        if (remote)
        {
            addRemoteClient(client, player);
        }

        logToStartLog((remote ? "Remote" : "Local") + " player "
            + player.getName() + " signed on.");
        game.getNotifyWebServer().gotClient(player, remote);
        waitingForClients--;
        LOGGER.log(Level.INFO, "Decremented waitingForClients to "
            + waitingForClients);

        if (waitingForClients > 0)
        {
            String pluralS = (waitingForClients > 1 ? "s" : "");
            logToStartLog(" ==> Waiting for " + waitingForClients
                + " more client" + pluralS + " to sign on.\n");
        }

        if (waitingForClients <= 0)
        {
            logToStartLog("\nGot all clients, starting the game.\n");
            if (game.isLoadingGame())
            {
                game.loadGame2();
            }
            else
            {
                game.newGame2();
            }
            game.getNotifyWebServer().allClientsConnected();
            if (startLog != null)
            {
                startLog.setCompleted();
            }
        }
    }

    private void addRemoteClient(final IClient client, final Player player)
    {
        RemoteLogHandler remoteLogHandler = new RemoteLogHandler();
        remoteLogHandler.setServer(this);
        LOGGER.addHandler(remoteLogHandler);
        remoteLogHandlers.add(remoteLogHandler);

        remoteClients.add(client);

        if (!game.isLoadingGame())
        {
            player.setName(game.getUniqueName(player.getName()));
            // In case we had to change a duplicate name.
            setPlayerName(player, player.getName());
        }
    }

    void disposeAllClients()
    {
        synchronized (disposeAllClientsDoneMutex)
        {
            if (disposeAllClientsDone)
            {
                return;
            }
            disposeAllClientsDone = true;
        }

        for (IClient client : clients)
        {
            client.dispose();
        }
        clients.clear();
        clientMap.clear();
        remoteClients.clear();

        for (RemoteLogHandler handler : remoteLogHandlers)
        {
            LOGGER.removeHandler(handler);
            handler.close();
        }
        remoteLogHandlers.clear();

        if (serverSocket != null)
        {
            shuttingDown = true;
            try
            {
                serverSocket.close();
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "Could not close server socket", ex);
            }
            shuttingDown = false;
        }
    }

    public void cleanup()
    {
        if (startLog != null)
        {
            startLog.dispose();
            startLog = null;
        }
        game = null;
    }

    void allUpdatePlayerInfo(boolean treatDeadAsAlive)
    {
        for (IClient client : clients)
        {
            client.updatePlayerInfo(getPlayerInfo(treatDeadAsAlive));
        }
    }

    void allUpdatePlayerInfo()
    {
        allUpdatePlayerInfo(false);
    }

    void allUpdateCreatureCount(CreatureType type, int count, int deadCount)
    {
        for (IClient client : clients)
        {
            client.updateCreatureCount(type, count, deadCount);
        }
    }

    void allTellMovementRoll(int roll)
    {
        for (IClient client : clients)
        {
            client.tellMovementRoll(roll);
        }
    }

    public void leaveCarryMode()
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called leaveCarryMode()");
            return;
        }
        BattleServerSide battle = game.getBattle();
        battle.leaveCarryMode();
    }

    public void doneWithBattleMoves()
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doneWithBattleMoves()");
            getClient(getPlayer()).nak(Constants.doneWithBattleMoves,
                "Illegal attempt to end phase");
            return;
        }
        BattleServerSide battle = game.getBattle();
        battle.doneWithMoves();
    }

    public synchronized void doneWithStrikes()
    {
        BattleServerSide battle = game.getBattle();
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doneWithStrikes()");
            getClient(getPlayer()).nak(Constants.doneWithStrikes,
                "Wrong player");
        }
        else if (!battle.getBattlePhase().isFightPhase())
        {
            getClient(getPlayer()).nak(Constants.doneWithStrikes,
                "Wrong phase");
        }
        else if (!battle.doneWithStrikes())
        {
            getClient(getPlayer()).nak(Constants.doneWithStrikes,
                "Forced strikes remain");
        }
    }

    private IClient getClient(Player player)
    {
        if (clientMap.containsKey(player))
        {
            return clientMap.get(player);
        }
        else
        {
            return null;
        }
    }

    synchronized void allInitBoard()
    {
        for (Player player : game.getPlayers())
        {
            if (!player.isDead())
            {
                IClient client = getClient(player);
                if (client != null)
                {
                    client.initBoard();
                }
            }
        }
    }

    synchronized void allTellAllLegionLocations()
    {
        List<Legion> legions = game.getAllLegions();
        for (Legion legion : legions)
        {
            allTellLegionLocation(legion);
        }
    }

    void allTellLegionLocation(Legion legion)
    {
        for (IClient client : clients)
        {
            client.tellLegionLocation(legion, legion.getCurrentHex());
        }
    }

    void allRemoveLegion(Legion legion)
    {
        for (IClient client : clients)
        {
            client.removeLegion(legion);
        }
    }

    // TODO The History class still needs the string version of this method, try
    // to get rid of that
    void allTellPlayerElim(String eliminatedPlayerName, String slayerName,
        boolean updateHistory)
    {
        allTellPlayerElim(game.getPlayer(eliminatedPlayerName), game
            .getPlayer(slayerName), updateHistory);
    }

    void allTellPlayerElim(Player eliminatedPlayer, Player slayer,
        boolean updateHistory)
    {
        for (IClient client : clients)
        {
            client.tellPlayerElim(eliminatedPlayer, slayer);
        }

        if (updateHistory)
        {
            game.playerElimEvent(eliminatedPlayer, slayer);
        }
    }

    void allTellGameOver(String message)
    {
        for (IClient client : clients)
        {
            client.tellGameOver(message);
        }
    }

    /** Needed if loading game outside the split phase. */
    synchronized void allSetupTurnState()
    {
        for (IClient client : clients)
        {
            client
                .setupTurnState(game.getActivePlayer(), game.getTurnNumber());
        }
    }

    void allSetupSplit()
    {
        for (Player player : game.getPlayers())
        {
            IClient client = getClient(player);
            if (client != null)
            {
                client
                    .setupSplit(game.getActivePlayer(), game.getTurnNumber());
            }
        }
        allUpdatePlayerInfo();
    }

    void allSetupMove()
    {
        for (IClient client : clients)
        {
            client.setupMove();
        }
    }

    void allSetupFight()
    {
        for (IClient client : clients)
        {
            client.setupFight();
        }
    }

    void allSetupMuster()
    {
        for (IClient client : clients)
        {
            client.setupMuster();
        }
    }

    void allSetupBattleSummon()
    {
        BattleServerSide battle = game.getBattle();
        for (IClient client : clients)
        {
            client.setupBattleSummon(battle.getActivePlayer(), battle
                .getTurnNumber());
        }
    }

    void allSetupBattleRecruit()
    {
        BattleServerSide battle = game.getBattle();
        for (IClient client : clients)
        {
            client.setupBattleRecruit(battle.getActivePlayer(), battle
                .getTurnNumber());
        }
    }

    void allSetupBattleMove()
    {
        BattleServerSide battle = game.getBattle();
        for (IClient client : clients)
        {
            client.setupBattleMove(battle.getActivePlayer(), battle
                .getTurnNumber());
        }
    }

    void allSetupBattleFight()
    {
        BattleServerSide battle = game.getBattle();
        for (IClient client : clients)
        {
            if (battle != null)
            {
                client.setupBattleFight(battle.getBattlePhase(), battle
                    .getActivePlayer());
            }
        }
    }

    synchronized void allPlaceNewChit(CreatureServerSide critter)
    {
        for (IClient client : clients)
        {
            client.placeNewChit(critter.getName(), critter.getLegion().equals(
                game.getBattle().getDefendingLegion()), critter.getTag(),
                critter.getCurrentHex().getLabel());
        }
    }

    void allRemoveDeadBattleChits()
    {
        for (IClient client : clients)
        {
            client.removeDeadBattleChits();
        }
    }

    void allTellEngagementResults(Legion winner, String method, int points,
        int turns)
    {
        for (IClient client : clients)
        {
            client.tellEngagementResults(winner, method, points, turns);
        }
    }

    void nextEngagement()
    {
        IClient client = getClient(game.getActivePlayer());
        client.nextEngagement();
    }

    /** Find out if the player wants to acquire an angel or archangel. */
    synchronized void askAcquireAngel(PlayerServerSide player, Legion legion,
        List<String> recruits)
    {
        if (((LegionServerSide)legion).getHeight() < 7)
        {
            IClient client = getClient(player);
            if (client != null)
            {
                client.askAcquireAngel(legion, recruits);
            }
        }
    }

    public synchronized void acquireAngel(Legion legion, String angelType)
    {
        if (legion != null)
        {
            if (!getPlayer().equals(legion.getPlayer()))
            {
                LOGGER.log(Level.SEVERE, getPlayerName()
                    + " illegally called acquireAngel()");
                return;
            }
            ((LegionServerSide)legion).addAngel(angelType);
        }
    }

    void createSummonAngel(Legion legion)
    {
        IClient client = getClient(legion.getPlayer());
        client.createSummonAngel(legion);
    }

    void reinforce(Legion legion)
    {
        IClient client = getClient(legion.getPlayer());
        client.doReinforce(legion);
    }

    public void doSummon(Legion legion, Legion donor, String angel)
    {
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doSummon()");
            return;
        }
        CreatureType creature = null;
        if (angel != null)
        {
            creature = game.getVariant().getCreatureByName(angel);
        }
        game.doSummon(legion, donor, creature);
    }

    /**
     * Handle mustering for legion.
     * if recruiting with nothing, recruiterName is a non-null String
     * that contains "null".
     */
    public void doRecruit(Legion legion, String recruitName,
        String recruiterName)
    {
        IClient client = getClient(getPlayer());

        // we can't do the "return" inside the if blocks, because then we miss
        // the doneReinforcing at the end...
        // E.g. SimpleAI tried to muster after being attacked, won, acquired
        // angel (=> legion full) => canRecruit false => "illegal recruit".
        //   => game hangs.

        if (legion == null)
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doRecruit(): no legion");
            client.nak(Constants.doRecruit, "Null legion");
        }

        else if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doRecruit()");
            client.nak(Constants.doRecruit, "Wrong player");
        }

        else if (!((LegionServerSide)legion).canRecruit())
        {
            LOGGER.log(Level.SEVERE, "Illegal legion " + legion
                + " for recruit: " + recruitName + " recruiterName "
                + recruiterName);
            client.nak(Constants.doRecruit, "Illegal recruit");
        }

        else if (((LegionServerSide)legion).hasMoved()
            || game.getPhase() == Constants.Phase.FIGHT)
        {
            ((LegionServerSide)legion).sortCritters();
            CreatureType recruit = null;
            CreatureType recruiter = null;
            if (recruitName != null)
            {
                recruit = game.getVariant().getCreatureByName(recruitName);
                recruiter = game.getVariant().getCreatureByName(recruiterName);
                if (recruit != null)
                {
                    game.doRecruit(legion, recruit, recruiter);
                }
            }

            if (!((LegionServerSide)legion).canRecruit())
            {
                didRecruit(legion, recruit, recruiter);
            }
        }
        else
        {
            LOGGER.log(Level.SEVERE,
                "Illegal recruit (not moved, not in battle) with legion "
                    + legion + " recruit: " + recruitName + " recruiterName "
                    + recruiterName);
            client.nak(Constants.doRecruit, "Illegal recruit");
        }

        // Need to always call this to keep game from hanging.
        if (game.getPhase() == Constants.Phase.FIGHT)
        {
            if (game.getBattle() != null)
            {
                game.getBattle().doneReinforcing();
            }
            else
            {
                game.doneReinforcing();
            }
        }
    }

    void didRecruit(Legion legion, CreatureType recruit, CreatureType recruiter)
    {
        allUpdatePlayerInfo();

        int numRecruiters = (recruiter == null ? 0 : TerrainRecruitLoader
            .numberOfRecruiterNeeded(recruiter, recruit, legion
                .getCurrentHex().getTerrain(), legion.getCurrentHex()));
        String recruiterName = null;
        if (recruiter != null)
        {
            recruiterName = recruiter.getName();
        }

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.didRecruit(legion, recruit.getName(), recruiterName,
                numRecruiters);
        }

        // reveal only if there is something to tell
        if (recruiter != null)
        {
            List<String> recruiterNames = new ArrayList<String>();
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiterNames.add(recruiterName);
            }
            game.revealEvent(true, null, legion, recruiterNames);
        }
        game.addCreatureEvent(legion, recruit.getName());
    }

    void undidRecruit(Legion legion, String recruitName)
    {
        allUpdatePlayerInfo();
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.undidRecruit(legion, recruitName);
        }
        game.removeCreatureEvent(legion, recruitName);
    }

    public synchronized void engage(MasterHex hex)
    {
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called engage()");
            return;
        }
        game.engage(hex);
    }

    void allTellEngagement(MasterHex hex, Legion attacker, Legion defender)
    {
        LOGGER.log(Level.FINEST, "allTellEngagement() " + hex);
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellEngagement(hex, attacker, defender);
        }
    }

    /** Ask ally's player whether he wants to concede with ally. */
    void askConcede(Legion ally, Legion enemy)
    {
        IClient client = getClient(ally.getPlayer());
        client.askConcede(ally, enemy);
    }

    public void concede(Legion legion)
    {
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called concede()");
            return;
        }
        game.concede(legion.getMarkerId());
    }

    public void doNotConcede(Legion legion)
    {
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doNotConcede()");
            return;
        }
        game.doNotConcede(legion);
    }

    /** Ask ally's player whether he wants to flee with ally. */
    void askFlee(Legion ally, Legion enemy)
    {
        IClient client = getClient(ally.getPlayer());
        client.askFlee(ally, enemy);
    }

    public void flee(Legion legion)
    {
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called flee()");
            return;
        }
        game.flee(legion);
    }

    public void doNotFlee(Legion legion)
    {
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doNotFlee()");
            return;
        }
        game.doNotFlee(legion);
    }

    void twoNegotiate(Legion attacker, Legion defender)
    {
        IClient client1 = getClient(defender.getPlayer());
        client1.askNegotiate(attacker, defender);

        IClient client2 = getClient(attacker.getPlayer());
        client2.askNegotiate(attacker, defender);
    }

    /** playerName makes a proposal. */
    public void makeProposal(String proposalString)
    {
        // TODO Validate calling player
        game.makeProposal(getPlayerName(), proposalString);
    }

    /** Tell playerName about proposal. */
    void tellProposal(Player player, Proposal proposal)
    {
        IClient client = getClient(player);
        client.tellProposal(proposal.toString());
    }

    public void fight(MasterHex hex)
    {
        // TODO Validate calling player
        game.fight(hex);
    }

    public void doBattleMove(int tag, String hexLabel)
    {
        IClient client = getClient(getPlayer());
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doBattleMove()");
            client.nak(Constants.doBattleMove, "Wrong player");
            return;
        }
        boolean moved = game.getBattle().doMove(tag, hexLabel);
        if (!moved)
        {
            LOGGER.log(Level.SEVERE, "Battle move failed");
            client.nak(Constants.doBattleMove, "Illegal move");
        }
    }

    void allTellBattleMove(int tag, String startingHex, String endingHex,
        boolean undo)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellBattleMove(tag, startingHex, endingHex, undo);
        }
    }

    public synchronized void strike(int tag, String hexLabel)
    {
        IClient client = getClient(getPlayer());
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called strike()");
            client.nak(Constants.strike, "Wrong player");
            return;
        }
        BattleServerSide battle = game.getBattle();
        if (battle == null)
        {
            LOGGER.log(Level.SEVERE, "null battle in Server.strike()");
            client.nak(Constants.strike, "No battle");
            return;
        }
        LegionServerSide legion = battle.getActiveLegion();
        if (legion == null)
        {
            LOGGER.log(Level.SEVERE, "null active legion in Server.strike()");
            client.nak(Constants.strike, "No active legion");
            return;
        }
        CreatureServerSide critter = legion.getCritterByTag(tag);
        if (critter == null)
        {
            LOGGER.log(Level.SEVERE, "No critter with tag " + tag
                + " in Server.strike()");
            client.nak(Constants.strike, "No critter with that tag");
            return;
        }
        CreatureServerSide target = battle.getCritter(hexLabel);
        if (target == null)
        {
            LOGGER.log(Level.SEVERE, "No target in hex " + hexLabel
                + " in Server.strike()");
            client.nak(Constants.strike, "No target in that hex");
            return;
        }
        if (target.getPlayer() == critter.getPlayer())
        {
            LOGGER.log(Level.SEVERE, critter.getDescription()
                + " tried to strike allied " + target.getDescription());
            client.nak(Constants.strike, "Target is friendly");
            return;
        }
        if (critter.hasStruck())
        {
            LOGGER.log(Level.SEVERE, critter.getDescription()
                + " tried to strike twice");
            client.nak(Constants.strike, "Critter already struck");
            return;
        }
        critter.strike(target);
    }

    public synchronized void applyCarries(String hexLabel)
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called applyCarries()");
            return;
        }
        BattleServerSide battle = game.getBattle();
        CreatureServerSide target = battle.getCritter(hexLabel);
        battle.applyCarries(target);
    }

    public void undoBattleMove(String hexLabel)
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called undoBattleMove()");
            return;
        }
        game.getBattle().undoMove(hexLabel);
    }

    synchronized void allTellStrikeResults(CreatureServerSide striker,
        CreatureServerSide target, int strikeNumber, List<String> rolls,
        int damage, int carryDamageLeft, Set<String> carryTargetDescriptions)
    {
        // Save strike info so that it can be reused for carries.
        this.striker = striker;
        this.target = target;
        this.strikeNumber = strikeNumber;
        this.rolls = rolls;

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellStrikeResults(striker.getTag(), target.getTag(),
                strikeNumber, rolls, damage, target.isDead(), false,
                carryDamageLeft, carryTargetDescriptions);
        }
    }

    synchronized void allTellCarryResults(CreatureServerSide carryTarget,
        int carryDamageDone, int carryDamageLeft,
        Set<String> carryTargetDescriptions)
    {
        if (striker == null || target == null || rolls == null)
        {
            LOGGER.log(Level.SEVERE,
                "Called allTellCarryResults() without setup.");
            if (striker == null)
            {
                LOGGER.log(Level.SEVERE, "null striker");
            }
            if (target == null)
            {
                LOGGER.log(Level.SEVERE, "null target");
            }
            if (rolls == null)
            {
                LOGGER.log(Level.SEVERE, "null rolls");
            }
            return;
        }
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellStrikeResults(striker.getTag(), carryTarget.getTag(),
                strikeNumber, rolls, carryDamageDone, carryTarget.isDead(),
                true, carryDamageLeft, carryTargetDescriptions);
        }
    }

    synchronized void allTellHexDamageResults(CreatureServerSide target,
        int damage)
    {
        this.target = target;

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellStrikeResults(Constants.hexDamage, target.getTag(), 0,
                null, damage, target.isDead(), false, 0, null);
        }
    }

    /** Takes a Set of PenaltyOptions. */
    void askChooseStrikePenalty(SortedSet<PenaltyOption> penaltyOptions)
    {
        Player player = game.getBattle().getActivePlayer();
        IClient client = getClient(player);
        List<String> choices = new ArrayList<String>();
        Iterator<PenaltyOption> it = penaltyOptions.iterator();
        while (it.hasNext())
        {
            PenaltyOption po = it.next();
            striker = po.getStriker();
            choices.add(po.toString());
        }
        client.askChooseStrikePenalty(choices);
    }

    public void assignStrikePenalty(String prompt)
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called assignStrikePenalty()");
            getClient(getPlayer()).nak(Constants.assignStrikePenalty,
                "Wrong player");
        }
        else if (striker.hasStruck())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " assignStrikePenalty -- already struck");
            getClient(getPlayer()).nak(Constants.assignStrikePenalty,
                "Critter already struck");
        }
        else
        {
            striker.assignStrikePenalty(prompt);
        }
    }

    synchronized void allInitBattle(MasterHex masterHex)
    {
        BattleServerSide battle = game.getBattle();
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.initBattle(masterHex, battle.getTurnNumber(), battle
                .getActivePlayer(), battle.getBattlePhase(), battle
                .getAttackingLegion(), battle.getDefendingLegion());
        }
    }

    void allCleanupBattle()
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.cleanupBattle();
        }
    }

    public void mulligan()
    {
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called mulligan()");
            return;
        }
        int roll = game.mulligan();
        LOGGER.log(Level.INFO, getPlayerName()
            + " takes a mulligan and rolls " + roll);
    }

    public void undoSplit(Legion splitoff)
    {
        if (!isActivePlayer())
        {
            return;
        }
        game.getActivePlayer().undoSplit(splitoff);
    }

    void undidSplit(Legion splitoff, Legion survivor, boolean updateHistory,
        int turn)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.undidSplit(splitoff, survivor, turn);
        }
        if (updateHistory)
        {
            game.mergeEvent(splitoff.getMarkerId(), survivor.getMarkerId());
        }
    }

    public void undoMove(Legion legion)
    {
        if (!isActivePlayer())
        {
            return;
        }
        MasterHex formerHex = legion.getCurrentHex();
        game.getActivePlayer().undoMove(legion);
        MasterHex currentHex = legion.getCurrentHex();

        PlayerServerSide player = game.getPlayer(game.getActivePlayerName());
        // needed in undidMove to decide whether to dis/enable button
        boolean splitLegionHasForcedMove = player.splitLegionHasForcedMove();

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.undidMove(legion, formerHex, currentHex,
                splitLegionHasForcedMove);
        }
    }

    public void undoRecruit(Legion legion)
    {
        if (!isActivePlayer())
        {
            return;
        }
        game.getActivePlayer().undoRecruit(legion);
    }

    public void doneWithSplits()
    {
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally (wrong player) called "
                + "doneWithSplits() - active player is "
                + game.getActivePlayer().getName());
            getClient(getPlayer()).nak(Constants.doneWithSplits,
                "Wrong player");
        }
        else if (game.getTurnNumber() == 1
            && game.getActivePlayer().getLegions().size() == 1)
        {
            getClient(getPlayer()).nak(Constants.doneWithSplits,
                "Must split on first turn");
        }
        else
        {
            game.advancePhase(Constants.Phase.SPLIT, getPlayer());
        }
    }

    public void doneWithMoves()
    {
        PlayerServerSide player = game.getActivePlayer();
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doneWithMoves()");
            getClient(getPlayer())
                .nak(Constants.doneWithMoves, "Wrong player");
        }
        // If any legion has a legal non-teleport move, then
        // the player must move at least one legion.
        else if (player.legionsMoved() == 0 && player.countMobileLegions() > 0)
        {
            LOGGER.log(Level.FINEST, "At least one legion must move.");
            getClient(getPlayer()).nak(Constants.doneWithMoves,
                "Must move at least one legion");
        }
        // If legions share a hex and have a legal
        // non-teleport move, force one of them to take it.
        else if (player.splitLegionHasForcedMove())
        {
            LOGGER.log(Level.FINEST, "Split legions must be separated.");
            getClient(getPlayer()).nak(Constants.doneWithMoves,
                "Must separate split legions");
        }
        // Otherwise, recombine all split legions still in
        // the same hex, and move on to the next phase.
        else
        {
            player.recombineIllegalSplits();
            game.advancePhase(Constants.Phase.MOVE, getPlayer());
        }
    }

    public void doneWithEngagements()
    {
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doneWithEngagements()");
            getClient(getPlayer()).nak(Constants.doneWithEngagements,
                "Wrong player");
        }
        // Advance only if there are no unresolved engagements.
        else if (game.findEngagements().size() > 0)
        {
            getClient(getPlayer()).nak(Constants.doneWithEngagements,
                "Must resolve engagements");
        }
        else
        {
            game.advancePhase(Constants.Phase.FIGHT, getPlayer());
        }
    }

    public void doneWithRecruits()
    {
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doneWithRecruits()");
            getClient(getPlayer()).nak(Constants.doneWithRecruits,
                "Wrong player");
        }
        else
        {
            PlayerServerSide player = game.getActivePlayer();
            player.commitMoves();

            // Mulligans are only allowed on turn 1.
            if (!game.getOption(Options.unlimitedMulligans))
            {
                player.setMulligansLeft(0);
            }

            game.advancePhase(Constants.Phase.MUSTER, getPlayer());
        }
    }

    // made synchronized by Clemens 1.10.2007:
    // a bunch of calls inside this all go into methods in Game which
    // are synchronized; if several threads do withdrawFromGame nearly
    // same time,they will block each other.

    // XXX Notify all players.
    public synchronized void withdrawFromGame()
    {
        if (obsolete || game.isOver())
        {
            return;
        }

        PlayerServerSide player = getPlayer();

        String name = player.getName();
        LOGGER.log(Level.FINE, "Player " + name + " withdraws from the game.");

        if (player.isDead())
        {
            return;
        }

        // If player quits while engaged, set slayer.
        Player slayer = null;
        Legion legion = player.getTitanLegion();
        if (legion != null && game.isEngagement(legion.getCurrentHex()))
        {
            slayer = game.getFirstEnemyLegion(legion.getCurrentHex(), player)
                .getPlayer();
        }
        player.die(slayer, true);

        // if it returns, it returns true and that means game shall go on.
        if (game.checkAutoQuitOrGoOn())
        {
            if (player == game.getActivePlayer())
            {
                game.advancePhase(game.getPhase(), getPlayer());
            }
        }
    }

    public void disconnect()
    {
        // nothing to do. ServerSocketThread handled this
        //  ( = closed the socket and exits the loop etc.)
    }

    public void stopGame()
    {
        if (game != null)
        {
            game.dispose();
        }
    }

    public void setDonor(Legion donor)
    {
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called setDonor()");
            return;
        }
        PlayerServerSide player = game.getActivePlayer();
        if (donor != null && donor.getPlayer() == player)
        {
            player.setDonor((LegionServerSide)donor);
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Bad arg to Server.getDonor() for "
                + donor);
        }
    }

    private List<String> getPlayerInfo(boolean treatDeadAsAlive)
    {
        List<String> info = new ArrayList<String>(game.getNumPlayers());
        Iterator<PlayerServerSide> it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            PlayerServerSide player = it.next();
            info.add(player.getStatusInfo(treatDeadAsAlive));
        }
        return info;
    }

    public void doSplit(Legion parent, String childId, String results)
    {
        LOGGER.log(Level.FINER, "Server.doSplit " + parent + " " + childId
            + " " + results);
        IClient client = getClient(getPlayer());
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doSplit()");
            client.nak(Constants.doSplit, "Wrong player");
            return;
        }
        if (!game.doSplit(parent, childId, results))
        {
            LOGGER.log(Level.SEVERE, "split failed for " + parent);
            client.nak(Constants.doSplit, "Illegal split");
        }
    }

    /** Callback from game after this legion was split off. */
    void didSplit(MasterHex hex, Legion parent, Legion child, int height)
    {
        LOGGER.log(Level.FINER, "Server.didSplit " + hex + " " + parent + " "
            + child + " " + height);
        allUpdatePlayerInfo();

        IClient activeClient = getClient(game.getActivePlayer());

        List<String> splitoffs = ((LegionServerSide)child).getImageNames();
        activeClient.didSplit(hex, parent, child, height, splitoffs, game
            .getTurnNumber());

        game.splitEvent(parent, child, splitoffs);

        if (!game.getOption(Options.allStacksVisible))
        {
            splitoffs.clear();
        }

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            if (client != activeClient)
            {
                client.didSplit(hex, parent, child, height, splitoffs, game
                    .getTurnNumber());
            }
        }
    }

    /** Call from History during load game only */
    void didSplit(Legion parent, Legion child, List<String> splitoffs, int turn)
    {
        IClient activeClient = getClient(game.getActivePlayer());
        int childSize = splitoffs.size();
        activeClient.didSplit(null, parent, child, childSize, splitoffs, turn);

        if (!game.getOption(Options.allStacksVisible))
        {
            splitoffs.clear();
        }

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            if (client != activeClient)
            {
                client.didSplit(null, parent, child, childSize, splitoffs,
                    turn);
            }
        }
    }

    public void doMove(Legion legion, MasterHex hex, String entrySide,
        boolean teleport, String teleportingLord)
    {
        IClient client = getClient(getPlayer());
        if (!isActivePlayer())
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called doMove()");
            client.nak(Constants.doMove, "Wrong player");
            return;
        }

        MasterHex startingHex = legion.getCurrentHex();
        String reasonFail = game.doMove(legion, hex, entrySide, teleport,
            teleportingLord);
        if (reasonFail == null)
        {
            allTellDidMove(legion, startingHex, hex, entrySide, teleport,
                teleportingLord);
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Move failed");
            client.nak(Constants.doMove, "Illegal move: " + reasonFail);
        }
    }

    void allTellDidMove(Legion legion, MasterHex startingHex, MasterHex hex,
        String entrySide, boolean teleport, String teleportingLord)
    {
        PlayerServerSide player = game.getPlayer(game.getActivePlayerName());
        // needed in didMove to decide whether to dis/enable button
        boolean splitLegionHasForcedMove = player.splitLegionHasForcedMove();

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.didMove(legion, startingHex, hex, entrySide, teleport,
                teleportingLord, splitLegionHasForcedMove);
        }
    }

    void allTellDidSummon(Legion receivingLegion, Legion donorLegion,
        String summon)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.didSummon(receivingLegion, donorLegion, summon);
        }
    }

    void allTellAddCreature(Legion legion, String creatureName,
        boolean updateHistory, String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.addCreature(legion, creatureName, reason);
        }
        if (updateHistory)
        {
            game.addCreatureEvent(legion, creatureName);
        }
    }

    void allTellRemoveCreature(Legion legion, String creatureName,
        boolean updateHistory, String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.removeCreature(legion, creatureName, reason);
        }
        if (updateHistory)
        {
            game.removeCreatureEvent(legion, creatureName);
        }
    }

    void allRevealLegion(Legion legion, String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.revealCreatures(legion, ((LegionServerSide)legion)
                .getImageNames(), reason);
        }
        game.revealEvent(true, null, legion, ((LegionServerSide)legion)
            .getImageNames());
    }

    /** pass to all clients the 'revealEngagedCreatures' message,
     * then fire an 'revealEvent' to the history.
     * @author Towi, copied from allRevealLegion
     * @param legion the legion marker to reveal which is in a battle
     * @param isAttacker true if the 'legion' is the atackker in the
     *   battle, false for the defender.
     */
    void allRevealEngagedLegion(final Legion legion, final boolean isAttacker,
        String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.revealEngagedCreatures(legion, ((LegionServerSide)legion)
                .getImageNames(), isAttacker, reason);
        }
        game.revealEvent(true, null, legion, ((LegionServerSide)legion)
            .getImageNames());
    }

    /** Call from History during load game only */
    void allRevealLegion(Legion legion, List<String> creatureNames,
        String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.revealCreatures(legion, creatureNames, reason);
        }
    }

    void oneRevealLegion(Legion legion, Player player, String reason)
    {
        IClient client = getClient(player);
        if (client != null)
        {
            client.revealCreatures(legion, ((LegionServerSide)legion)
                .getImageNames(), reason);
        }
        List<String> li = new ArrayList<String>();
        li.add(player.getName());
        game.revealEvent(false, li, legion, ((LegionServerSide)legion)
            .getImageNames());
    }

    /** Call from History during load game only */
    void oneRevealLegion(Player player, Legion legion,
        List<String> creatureNames, String reason)
    {
        IClient client = getClient(player);
        if (client != null)
        {
            client.revealCreatures(legion, creatureNames, reason);
        }
    }

    void allFullyUpdateLegionStatus()
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            if (client != null)
            {
                for (Legion legion : game.getAllLegions())
                {
                    client.setLegionStatus(legion, ((LegionServerSide)legion)
                        .hasMoved(), ((LegionServerSide)legion)
                        .hasTeleported(), ((LegionServerSide)legion)
                        .getEntrySide(), ((LegionServerSide)legion)
                        .getRecruitName());
                }
            }
        }
    }

    void allFullyUpdateAllLegionContents(String reason)
    {
        for (Legion legion : game.getAllLegions())
        {
            allRevealLegion(legion, reason);
        }
    }

    void allRevealCreatures(Legion legion, List<String> creatureNames,
        String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.revealCreatures(legion, creatureNames, reason);
        }
        game.revealEvent(true, null, legion, creatureNames);
    }

    // XXX Disallow these in network games?
    public void newGame()
    {
        // Nothing special to do, just stop everything and return
        // back to the main, so that it goes to top of loop and
        // brings up a new GetPlayers dialog.
    }

    public void loadGame(String filename)
    {
        game.loadGame(filename);
    }

    public void saveGame(String filename)
    {
        game.saveGame(filename);
    }

    /** Used to change a player name after color is assigned. */
    void setPlayerName(Player player, String newName)
    {
        LOGGER.log(Level.FINEST, "Server.setPlayerName() from "
            + player.getName() + " to " + newName);
        IClient client = getClient(player);
        client.setPlayerName(newName);
    }

    synchronized void askPickColor(Player player, final List<String> colorsLeft)
    {
        IClient client = getClient(player);
        if (client != null)
        {
            client.askPickColor(colorsLeft);
        }
    }

    public synchronized void assignColor(String color)
    {
        if (!getPlayer().equals(game.getNextColorPicker()))
        {
            LOGGER.log(Level.SEVERE, getPlayerName()
                + " illegally called assignColor()");
            return;
        }
        if (getPlayer() == null || getPlayer().getColor() == null)
        {
            game.assignColor(getPlayer(), color);
        }
    }

    void askPickFirstMarker(Player player)
    {
        IClient client = getClient(player);
        if (client != null)
        {
            client.askPickFirstMarker();
        }
    }

    public void assignFirstMarker(String markerId)
    {
        PlayerServerSide player = game.getPlayer(getPlayerName());
        assert player.getMarkersAvailable().contains(markerId) : getPlayerName()
            + " illegally called assignFirstMarker()";
        player.setFirstMarker(markerId);
        game.nextPickColor();
    }

    /** Hack to set color on load game. */
    synchronized void allSetColor()
    {
        Iterator<PlayerServerSide> it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            PlayerServerSide player = it.next();
            String color = player.getColor();
            IClient client = getClient(player);
            if (client != null)
            {
                client.setColor(color);
            }
        }
    }

    // XXX We use Server as a hook for PhaseAdvancer to get to options,
    // but this is ugly.
    int getIntOption(String optname)
    {
        return game.getIntOption(optname);
    }

    void oneSetOption(Player player, String optname, String value)
    {
        IClient client = getClient(player);
        if (client != null)
        {
            client.setOption(optname, value);
        }
    }

    void oneSetOption(Player player, String optname, boolean value)
    {
        oneSetOption(player, optname, String.valueOf(value));
    }

    void allSetOption(String optname, String value)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.setOption(optname, value);
        }
    }

    void allSetOption(String optname, boolean value)
    {
        allSetOption(optname, String.valueOf(value));
    }

    void allSetOption(String optname, int value)
    {
        allSetOption(optname, String.valueOf(value));
    }

    /** DO NOT USE:
     * package so that it can be called from Log4J Appender.
     *
     */
    void allLog(String message)
    {
        Iterator<IClient> it = remoteClients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.log(message);
        }
    }

    void logToStartLog(String message)
    {
        if (startLog != null)
        {
            startLog.append(message);
        }
    }

    /* Called from ServerStartupProgress, if user wants to cancel during load
     * (e.g. one client did not come in). Clean up everything and get
     *  back to the GetPlayers / Game Setup dialog.
     */
    public void startupProgressAbort()
    {
        Start.setCurrentWhatToDoNext(Start.GetPlayersDialog);
        stopServerRunning();
        if (startLog != null)
        {
            startLog.dispose();
            startLog.cleanRef();
        }
    }

    /* if the startupProgressAbort did not succeed well, the button
     * there changes to a QUIT button, and with that one can request
     * the whole application
     */
    public void startupProgressQuit()
    {
        LOGGER.log(Level.SEVERE,
            "User pressed 'QUIT' in startupProgress window "
                + "- doing System.exit(1)");
        System.exit(1);
    }

    public GameServerSide getGame()
    {
        return game;
    }
}
