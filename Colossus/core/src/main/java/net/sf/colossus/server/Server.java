package net.sf.colossus.server;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.IClient;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.game.events.AddCreatureEvent;
import net.sf.colossus.game.events.RecruitEvent;
import net.sf.colossus.game.events.SummonEvent;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Class Server lives on the server side and handles all communcation with
 *  the clients.  It talks to the server classes locally, and to the Clients
 *  via the network protocol.
 *
 *  @author David Ripton
 */
public final class Server extends Thread implements IServer
{
    private static final Logger LOGGER = Logger.getLogger(Server.class
        .getName());
    private static StartupProgress startLog;

    private GameServerSide game;

    private final WhatNextManager whatNextManager;

    /**
     *  Maybe also save things like the originating IP, in case a
     *  connection breaks and we need to authenticate reconnects.
     *  Do not share these references. */
    private final List<IClient> clients = new ArrayList<IClient>();
    private final List<IClient> remoteClients = new ArrayList<IClient>();
    private final List<IClient> spectatorClients = new ArrayList<IClient>();
    private final List<RemoteLogHandler> remoteLogHandlers = new ArrayList<RemoteLogHandler>();

    /** Map of players to their clients. */
    private final Map<Player, IClient> clientMap = new HashMap<Player, IClient>();

    // list of SocketChannels that are currently active
    private final List<SocketChannel> activeSocketChannelList = new ArrayList<SocketChannel>();

    /** Number of player clients we're waiting for. */
    private int waitingForPlayers;
    private int spectators = 0;

    /** Server socket port. */
    private final int port;

    // Cached strike information.
    private CreatureServerSide striker;
    private CreatureServerSide target;
    private int strikeNumber;
    private List<String> rolls;

    // Network stuff
    private ServerSocket serverSocket;
    private Selector selector = null;
    private SelectionKey acceptKey = null;
    private boolean stopAcceptingFlag = false;

    private final Object guiRequestMutex = new Object();
    private boolean guiRequestSaveFlag = false;
    private String guiRequestSaveFilename = null;
    private boolean guiSuspendOngoing = false;

    /* static so that new instance of Server can destroy a
     * previously allocated FileServerThread */
    private static Thread fileServerThread = null;

    private boolean serverRunning = false;
    private boolean obsolete = false;
    private boolean shuttingDown = false;
    private boolean forceShutDown = false;
    private boolean initiateDisposal = false;
    private String caughtUpAction = "";

    private final int timeoutDuringGame = 0;
    private final int timeoutDuringShutdown = 1000;

    // Earlier I have locked on an Boolean object itself,
    // which I modify... and when this is done too often,
    // e.g. in ClientSocketThread every read, it caused
    // StockOverflowException... :-/
    private final Object disposeAllClientsDoneMutex = new Object();
    private boolean disposeAllClientsDone = false;

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    // The ClientHandler of which the input is currently processed
    ClientHandler processingCH = null;

    // Channels are queued into here, to be removed from selector on
    // next possible opportunity ( = when all waiting-to-be-processed keys
    // have been processed).
    private final List<ClientHandler> channelChanges = new ArrayList<ClientHandler>();

    Server(GameServerSide game, WhatNextManager whatNextMgr, int port)
    {
        this.game = game;
        this.port = port;
        this.whatNextManager = whatNextMgr;

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

        waitingForPlayers = game.getNumLivingPlayers();
        InstanceTracker.register(this, "only one");
    }

    @Override
    public void run()
    {
        int timeout = timeoutDuringGame;
        int disposeRound = 0;
        while (!shuttingDown && disposeRound < 60)
        {
            waitOnSelector(timeout);
            // The following is handling the case that game did initiate
            // the dispose by itself  due to AutoQuit when game over
            if (initiateDisposal)
            {
                if (disposeRound == 0)
                {
                    LOGGER.info("Game disposal initiated. Waiting for clients"
                        + " to catch up...");
                    timeout = timeoutDuringShutdown;
                    // Requesting all clients to confirm that they have caught
                    // up with the processing of all the messages at game end.
                    // When all caught up, this will triger game.dispose(),
                    //   which which do stopServerRunning,
                    //     which will do stopFileServer, disposeAllClients,
                    //     and set shuttingDown and serverRunning to false.
                    allRequestConfirmCatchup("DisposeGame");
                }
                disposeRound++;
            }
        }

        if (serverRunning && disposeRound >= 60)
        {
            LOGGER.warning("The game.dispose() not triggered by caughtUp"
                + " - doing it now.");
            game.dispose();
        }

        if (shuttingDown)
        {
            closeSocketAndSelector();
        }

        notifyThatGameFinished();
        LOGGER.info("Server.run() ends.");
    }

    void initFileServer()
    {
        stopFileServer();

        if (game.getNumRemoteRemaining() > 0)
        {
            fileServerThread = new FileServerThread(this, port + 1);
            fileServerThread.start();
        }
        else
        {
            LOGGER
                .finest("No alive remote client, not launching the file server.");
        }
    }

    // FileServerThread asks this
    public boolean isKnownClient(InetAddress requester)
    {
        boolean knownIP = false;
        synchronized (activeSocketChannelList)
        {
            Iterator<SocketChannel> it = activeSocketChannelList.iterator();
            while (it.hasNext() && !knownIP)
            {
                SocketChannel sc = it.next();
                InetAddress cIP = sc.socket().getInetAddress();
                knownIP = requester.equals(cIP);
            }
        }
        return knownIP;
    }

    void initSocketServer()
    {
        LOGGER.log(Level.FINEST, "initSocketServer, expecting "
            + game.getNumLivingPlayers() + " player clients.");
        LOGGER.log(Level.FINEST, "About to create server socket on port "
            + port);

        try
        {
            selector = Selector.open();

            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);

            serverSocket = ssc.socket();
            serverSocket.setReuseAddress(true);
            InetSocketAddress address = new InetSocketAddress(port);
            serverSocket.bind(address);
            acceptKey = ssc.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch (IOException ex)
        {
            String message = "Could not create server side socket.\n"
                + "Configure networking in OS, or check that no previous "
                + "Colossus instance got stuck and is blocking the socket.\n"
                + "Got IOException: " + ex;

            LOGGER.log(Level.SEVERE, message);
            JOptionPane.showMessageDialog(startLog.getFrame(), message,
                "Starting game (server side) failed!",
                JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        }
        catch (Exception anyex)
        {
            String message = "Could not create server side socket.\n"
                + "Configure networking in OS, or check that no previous "
                + "Colossus instance got stuck and is blocking the socket.\n"
                + "Got Exception: " + anyex;

            LOGGER.log(Level.SEVERE, message);
            JOptionPane.showMessageDialog(startLog.getFrame(), message,
                "Starting game (server side) failed!",
                JOptionPane.ERROR_MESSAGE);

            System.exit(1);

        }
        createLocalClients();
    }

    boolean waitForClients()
    {
        logToStartLog("\nStarting up, waiting for " + waitingForPlayers
            + " player clients at port " + port + "\n");
        StringBuilder living = new StringBuilder("");
        StringBuilder dead = new StringBuilder("");
        for (Player p : game.getPlayers())
        {
            String name = p.getName();
            StringBuilder list = p.isDead() ? dead : living;

            if (list.length() > 0)
            {
                list.append(", ");
            }
            list.append(name);
        }
        logToStartLog("Players expected to join (= alive): " + living + "\n");
        if (dead.length() > 0)
        {
            logToStartLog("Players already dead before save  : " + dead + "\n");
        }
        serverRunning = true;
        while (waitingForPlayers > 0 && serverRunning && !shuttingDown)
        {
            waitOnSelector(timeoutDuringGame);
        }

        return (waitingForPlayers == 0);
    }

    public void waitOnSelector(int timeout)
    {
        try
        {
            if (stopAcceptingFlag)
            {
                LOGGER.info("cancelDummy was set...");
                stopAccepting();
                stopAcceptingFlag = false;
            }

            LOGGER.log(Level.FINEST, "before select()");
            int num = selector.select(timeout);
            LOGGER.log(Level.FINEST, "select returned, " + num
                + " channels are ready to be processed.");

            if (handleGuiRequests())
            {
                // ok, select returned due to a wakeup call
            }
            else if (num == 0)
            {
                LOGGER.info("Server side select timeout...");
            }

            if (forceShutDown)
            {
                LOGGER.log(Level.FINEST,
                    "waitOnSelector: force shutdown now true! num=" + num);
                stopAccepting();
                stopServerRunning();
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> readyKeys = selectedKeys.iterator();
            while (readyKeys.hasNext() && !shuttingDown)
            {
                SelectionKey key = readyKeys.next();
                readyKeys.remove();

                if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT)
                {
                    // Accept the new connection
                    SocketChannel sc = ((ServerSocketChannel)key.channel())
                        .accept();
                    sc.configureBlocking(false);
                    LOGGER.log(Level.FINE, "Accepted: sc = " + sc);
                    SelectionKey selKey = sc.register(selector,
                        SelectionKey.OP_READ);
                    ClientHandler ch = new ClientHandler(this, sc, selKey);
                    selKey.attach(ch);
                    // This is sent only for the reason that the client gets
                    // an initial response quickly.
                    ch.sendToClient("SignOn: processing");
                    synchronized (activeSocketChannelList)
                    {
                        activeSocketChannelList.add(sc);
                    }
                }
                else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ)
                {
                    // Read the data
                    SocketChannel sc = (SocketChannel)key.channel();
                    ClientHandler ch = (ClientHandler)key.attachment();
                    if (ch == null)
                    {
                        LOGGER.severe("No ClientHandler for socket channel "
                            + sc);
                    }
                    else
                    {
                        processingCH = ch;

                        int read = readFromChannel(key, sc);
                        if (read > 0)
                        {
                            byteBuffer.flip();
                            // NOTE that the following might cause trouble
                            // if logging is set to FINEST for server,
                            // and the disconnect does not properly set the
                            // isGone flag...
                            LOGGER.finest("* before ch.processInput()");
                            ch.processInput(byteBuffer);
                            LOGGER.finest("* after  ch.processInput()");
                        }
                        else
                        {
                            LOGGER.finest("readFromChannel: 0 bytes read.");
                        }
                        processingCH = null;
                    }
                }
                else
                {
                    LOGGER.warning("Unexpected type of ready Operation: "
                        + key.readyOps());
                }
            }
            // just to be sure.
            selectedKeys.clear();

            synchronized (channelChanges)
            {
                // Can't use iterator, because e.g. removal of last human/observer
                // will add more items to the channelChanges list.
                while (!channelChanges.isEmpty())
                {
                    ClientHandler nextCH = channelChanges.remove(0);
                    SocketChannel sc = nextCH.getSocketChannel();
                    SelectionKey key = nextCH.getKey();
                    if (key == null)
                    {
                        LOGGER
                            .warning("key for to-be-closed-channel is null!");
                    }
                    else if (sc.isOpen())
                    {
                        // sending dispose and setIsGone is done by ClientHandler
                        disconnectChannel(sc, key);
                    }
                    else
                    {
                        // TODO this should not happen, but it does regularly
                        // - find out why and fix. Until then, just info
                        // of warning
                        LOGGER.info("to-be-closed-channel is not open!");
                    }
                }
                channelChanges.clear();
            }
        }

        catch (ClosedChannelException cce)
        {
            LOGGER.log(Level.SEVERE, "socketChannel.register() failed: ", cce);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE,
                "IOException while waiting or processing ready channels", ex);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Exception while waiting on selector", e);
            String message = "Woooah. An exception was caught while "
                + "waiting on Selector";
            ErrorUtils.showExceptionDialog(null, "Exception caught!",
                message, false);
        }
    }

    /** Shutdown initiated by outside, i.e. NOT by the Server thread itself.
     *  Right now, this is used by StartupProgressDialog's abort button.
     */
    public void externShutdown()
    {
        forceShutDown = true;
        selector.wakeup();
    }

    private void stopAccepting() throws IOException
    {
        LOGGER.info("Canceling connection accepting key.");
        acceptKey.cancel();
        acceptKey = null;

        LOGGER.info("Closing server socket");
        serverSocket.close();
        serverSocket = null;
    }

    /**
     * Close serverSocket and selector, if needed
     */
    private void closeSocketAndSelector()
    {
        LOGGER.info("After selector loop, shuttingDown flag is set. "
            + "Closing socket and selector, if necessary");
        if (serverSocket != null && !serverSocket.isClosed())
        {
            try
            {
                serverSocket.close();
                LOGGER.fine("ServerSocket now closed.");
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "Could not close server socket", ex);
            }
        }
        else
        {
            LOGGER.fine("After loop: ok, serverSocket was already closed");
        }

        if (selector != null && selector.isOpen())
        {
            try
            {
                selector.close();
                LOGGER.fine("Server selector closed.");
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "Could not close server socket", ex);
            }
        }
        else
        {
            LOGGER.fine("After loop: ok, server Selector was already closed");
        }
    }

    // I took as model this page:
    //   http://www.javafaq.nu/java-article1102.html
    // Throws IOException when closing the channel fails.

    private int readFromChannel(SelectionKey key, SocketChannel sc)
        throws IOException
    {
        byteBuffer.clear();
        int read = 0;
        while (true)
        {
            try
            {
                int r = sc.read(byteBuffer);
                if (r <= 0)
                {
                    if (r == -1)
                    {
                        processingCH.setIsGone(true);
                        LOGGER.info("EOF on channel for client "
                            + getPlayerName() + " setting isGone true");
                        // Remote entity did shut the socket down.
                        // Do the same from our end and cancel the channel.

                        LOGGER
                            .log(Level.FINE,
                                "Remote side cleanly closed the connection (r == -1)");
                        withdrawFromGame();
                        disconnectChannel(sc, key);

                    }
                    break;
                }
                read += r;
            }
            catch (IOException e)
            {
                // set isGone first, to prevent from sending log info to
                // client channel - channel is gone anyway...
                processingCH.setIsGone(true);
                LOGGER.log(Level.WARNING, "IOException '" + e.getMessage()
                    + "' while reading from channel for player "
                    + getPlayerName(), e);

                // The remote forcibly/unexpectedly closed the connection,
                // cancel the selection key and close the channel.
                withdrawFromGame();
                disconnectChannel(sc, key);
                return 0;
            }
        }

        LOGGER.log(Level.FINEST, "Read " + read + " bytes from " + sc);
        return read;
    }

    /**
     *  Put the ClientHandler into the queue to be removed
     *  from selector on next possible opportunity
     */
    void disposeClientHandler(ClientHandler ch)
    {
        if (currentThread().equals(this))
        {
            // OK, activity which is originated by client, so we will
            // come to the "after select loop" point when this was
            // completely processed
            synchronized (channelChanges)
            {
                channelChanges.add(ch);
            }
        }
        else
        {
            // some other thread wants to tell the ServerThread to
            // dispose one client - for example EDT when user pressed
            // "Abort" button in StartupProgress dialog.
            synchronized (channelChanges)
            {
                channelChanges.add(ch);
            }
        }
    }

    private final Object waitUntilOverMutex = new Object();

    public void notifyThatGameFinished()
    {
        synchronized (waitUntilOverMutex)
        {
            waitUntilOverMutex.notify();
        }
    }

    public void waitUntilGameFinishes()
    {
        LOGGER.finest("Before synchronized(waitUntilOverMutex)");
        synchronized (waitUntilOverMutex)
        {
            try
            {
                waitUntilOverMutex.wait();
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.SEVERE, "interrupted while waiting to be "
                    + "notified that game is over: ", e);
            }
        }

        LOGGER.finest("After synchronized(waitUntilOverMutex) (=completed).");
    }

    /*
     * Stops the file server (closes FileServerSocket),
     * disposes all clients, and closes ServerSockets
     */
    public void stopServerRunning()
    {
        if (!game.isGameOver())
        {
            LOGGER
                .info("stopServerRunning called when game was not over yet.");
            game.setGameOver(true, "Game stopped by system");
        }

        stopFileServer();

        // particularly to remove the loggers
        if (!clients.isEmpty())
        {
            disposeAllClients();
        }

        serverRunning = false;
        shuttingDown = true;
    }

    public boolean isServerRunning()
    {
        return serverRunning;
    }

    // last SocketClientThread going down calls this
    // Synchronized because *might* also be called from Abort button
    public synchronized void stopFileServer()
    {
        LOGGER
            .finest("About to stop file server socket on port " + (port + 1));

        if (fileServerThread != null)
        {
            try
            {
                LOGGER.info("Stopping the FileServerThread ");
                ((FileServerThread)fileServerThread).stopGoingOn();
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING,
                    "Couldn't stop FileServerThread, got Exception: " + e);
            }
            fileServerThread = null;
        }
        else
        {
            // no fileserver running
        }
    }

    private void disconnectChannel(SocketChannel sc, SelectionKey key)
        throws IOException
    {
        sc.close();
        key.cancel();
        unregisterSocketChannel(sc);
    }

    public void unregisterSocketChannel(SocketChannel socketChannel)
    {
        if (activeSocketChannelList == null)
        {
            LOGGER.finest("activeSocketChannelList null");
            return;
        }

        LOGGER.finest("activeSocketChannelList before synch ");
        synchronized (activeSocketChannelList)
        {
            LOGGER.finest("activeSocketChannelList   IN   synch ");
            int index = activeSocketChannelList.indexOf(socketChannel);
            LOGGER.finest("activeSocketChannelList index = " + index);
            if (index == -1)
            {
                return;
            }
            activeSocketChannelList.remove(index);

            if (!serverRunning)
            {
                LOGGER.finest("serverRunning false");
                return;
            }

            // no client whatsoever left => end the game and close server stuff
            // Even if socket list is empty, client list may not be empty yet,
            // and need to empty it and close all loggers.
            if (activeSocketChannelList.isEmpty())
            {
                LOGGER.finest("Server.unregisterSocketChannel(): "
                    + "activeSocketChannelList empty - stopping server...");
                stopServerRunning();
            }

            else if (game.getOption(Options.goOnWithoutObserver))
            {
                LOGGER.finest("\n==========\nOne socket went away, "
                    + "but we go on because goOnWithoutObserver is set...\n");
            }
            // or, if only AI player clients left as "observers",
            // then close everything, too
            else if (!anyNonAiSocketsLeft())
            {
                LOGGER.finest("Server.unregisterSocket(): "
                    + "All connections to human or network players gone "
                    + "(no point to keep AIs running if noone sees it) "
                    + "- stopping server...");
                stopServerRunning();
            }
            else
            {
                LOGGER.finest("Server.unregisterSocket(): ELSE case "
                    + "(i.e. someone is left, so it makes sense to go on)");
            }
        }
        LOGGER.finest("activeSocketChannelList after synch ");
    }

    public void setBoardVisibility(Player player, boolean val)
    {
        getClient(player).setBoardActive(val);
    }

    public boolean isClientGone(Player player)
    {
        ClientHandler ch = (ClientHandler)getClient(player);
        if (ch == null || ch.isGone())
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

        for (Player player : game.getPlayers())
        {
            if (!player.isAI() && !isClientGone(player))
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
     * Name of the player, for which data from socket is currently processed.
     */
    String getPlayerName()
    {
        // Return the playerName for the processingCH.
        // processingCH holds the ClientHandler of the client/player for
        // which data is currently read or processed, then and only then
        // when it is reading or processing. While the selector is waiting
        // for next input, it's always set to null.
        assert processingCH != null : "No processingCH!";
        assert processingCH.getPlayerName() != null : "Name for processingCH must not be null!";
        return processingCH.getPlayerName();
    }

    /**
     * The player, for which data from socket is currently processed.
     */
    private Player getPlayer()
    {
        Player p = game.getPlayerByName(getPlayerName());
        assert p != null : "game.getPlayer returned null player for name "
            + getPlayerName();

        return p;
    }

    /**
     * returns true if the active player is the player owning the connection
     * from which data is currently processed
     */
    private boolean isActivePlayer()
    {
        return getPlayer().equals(game.getActivePlayer());
    }

    private PlayerServerSide getActivePlayerSS()
    {
        return (PlayerServerSide)game.getActivePlayer();
    }

    private boolean isBattleActivePlayer()
    {
        return game.getBattle() != null
            && game.getBattle().getActivePlayer() != null
            && getPlayer().equals(game.getBattle().getActivePlayer());
    }

    public void createLocalClients()
    {
        boolean atLeastOneBoardNeeded = Constants.FORCE_VIEW_BOARD;
        for (Player player : game.getPlayers())
        {
            String type = player.getType();
            // getDeadBeforeSave to Game instead?
            if (!((PlayerServerSide)player).getDeadBeforeSave()
                && !type.endsWith(Constants.network))
            {

                if (player.getName().equals(game.getHostingPlayer()))
                {
                    LOGGER.info("Skipping creation of local client for "
                        + "hosting player " + player.getName());
                }
                else
                {
                    boolean createGUI = !player.isAI();
                    if (atLeastOneBoardNeeded)
                    {
                        // Yes, only depending on the atLeast..., no matter
                        // whether for this player true or not!
                        // This relies on the fact that for cmdline setup
                        // Human players are created before AI players.
                        createGUI = true;
                        atLeastOneBoardNeeded = false;
                    }
                    LOGGER.info("Creating local client for player "
                        + player.getName() + ", type " + type);
                    createLocalClient((PlayerServerSide)player, createGUI,
                        type);
                }
            }
        }
    }

    private void createLocalClient(PlayerServerSide player, boolean createGUI,
        String type)
    {
        String playerName = player.getName();
        boolean dontUseOptionsFile = player.isAI();
        LOGGER.finest("Called Server.createLocalClient() for " + playerName);

        Client.createClient("127.0.0.1", port, playerName, type,
            whatNextManager, this, false, dontUseOptionsFile, createGUI);
    }

    boolean addClient(final IClient client, final String playerName,
        final boolean remote)
    {
        LOGGER.finest("Calling Server.addClient() for " + playerName);

        Player player = null;
        if (playerName.equalsIgnoreCase("spectator"))
        {
            LOGGER.info("addClient for a spectator.");
        }
        else if (remote)
        {
            player = game.findNetworkPlayer(playerName);
        }
        else
        {
            player = game.getPlayerByNameIgnoreNull(playerName);
        }

        String name = "<undefined>";
        if (player == null && playerName.equalsIgnoreCase("spectator"))
        {
            ++spectators;
            name = "spectator_" + spectators;
            LOGGER.info("Adding spectator " + name);
        }
        else if (player == null)
        {
            LOGGER.warning("No Player was found for non-spectator playerName "
                + playerName + "!");
            logToStartLog("NOTE: One client attempted to join with player name "
                + playerName
                + " - rejected, because no such player is expected!");
            return false;
        }
        else if (clientMap.containsKey(player))
        {
            LOGGER.warning("Could not add client, "
                + "because Player for playerName " + playerName
                + " had already signed on.");
            logToStartLog("NOTE: One client attempted to join with player name "
                + playerName
                + " - rejected, because same player name already " + "joined.");
            return false;
        }
        else
        {
            name = playerName;
        }

        clients.add(client);
        if (player != null)
        {
            clientMap.put(player, client);
        }
        else
        {
            spectatorClients.add(client);
        }

        if (remote)
        {
            addRemoteClient(client, player);
        }

        if (player != null)
        {
            logToStartLog((remote ? "Remote" : "Local") + " player " + name
                + " signed on.");
            game.getNotifyWebServer().gotClient(player, remote);
            waitingForPlayers--;
            LOGGER.info("Decremented waitingForPlayers to "
                + waitingForPlayers);

            if (waitingForPlayers > 0)
            {
                String pluralS = (waitingForPlayers > 1 ? "s" : "");
                logToStartLog(" ==> Waiting for " + waitingForPlayers
                    + " more player client" + pluralS + " to sign on.\n");
            }
            else
            {
                logToStartLog("\nGot clients for all players, game can start now.\n");
            }
        }
        else
        {
            logToStartLog((remote ? "Remote" : "Local") + " spectator ("
                + name + ") signed on.");
        }
        return true;
    }

    public void startGameIfAllPlayers()
    {
        if (waitingForPlayers <= 0)
        {
            stopAcceptingFlag = true;
            if (game.isLoadingGame())
            {
                logToStartLog("Loading game, sending replay data to clients...");
                boolean ok = game.loadGame2();
                if (ok)
                {
                    logToStartLog("Waiting for clients to catch up with replay data...\n");
                }
                else
                {
                    logToStartLog("Loading/Replay failed!!\n");
                    logToStartLog("\n-- Press Abort button "
                        + "to return to Start Game dialog --\n");
                    loadFailed();
                    return;
                }
            }
            else
            {
                game.newGame2();
            }

            logToStartLog("\nStarting the game now.\n");
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

        if (player == null)
        {
            LOGGER.info("addRemoteClient for observer skips name stuff.");
            return;
        }
        if (!game.isLoadingGame())
        {
            // Returns original name if no other player has this name
            String uName = game.getUniqueName(player.getName(), player);
            if (!uName.equals(player.getName()))
            {
                // set in player
                player.setName(uName);
            }
            // set playerName + thread name in ClientHandler, and send
            // playerName to client:
            // It's necessary to send to client only for that reason, that
            // it otherwise might time out if it does not get quick response
            // (5 seconds) from server to it's initial signOn request
            setPlayerName(player, uName);
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

        LOGGER.info("BEGIN disposing all clients...");
        for (IClient client : clients)
        {
            // This sends the dispose message, and queues ClientHandler's
            // channel for being removed from selector.
            // Actual removal happens after all selector-keys are processed.
            // @TODO: does that make even sense? shuttingDown is set true,
            // so the selector loop does not even reach the removal part...
            client.dispose();
        }
        clients.clear();
        clientMap.clear();
        remoteClients.clear();

        LOGGER.fine("Removing all loggers...");
        for (RemoteLogHandler handler : remoteLogHandlers)
        {
            LOGGER.removeHandler(handler);
            handler.close();
        }
        remoteLogHandlers.clear();
        LOGGER.info("COMPLETED disposing all clients...");
    }

    public void loadFailed()
    {
        JOptionPane.showMessageDialog(startLog.getFrame(),
            "Loading, replay of history and comparison between saved "
                + "state and replay result failed!!\n\n"
                + "Click Abort on the Startup Progress Dialog to return to "
                + "Game setup dialog to start a different or new one.",
            "Loading game failed!", JOptionPane.ERROR_MESSAGE);
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
            LOGGER.severe(getPlayerName()
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
            LOGGER.severe(getPlayerName()
                + " illegally called doneWithBattleMoves()");
            getClient(getPlayer()).nak(Constants.doneWithBattleMoves,
                "Illegal attempt to end phase (wrong player)");
            return;
        }

        BattleServerSide battle = game.getBattle();
        if (!battle.getBattlePhase().isMovePhase())
        {
            LOGGER.severe(getPlayerName()
                + " illegally called doneWithBattleMoves()");
            getClient(getPlayer()).nak(
                Constants.doneWithBattleMoves,
                "Illegal attempt to end phase (wrong phase "
                    + battle.getBattlePhase().toString() + ")");
            return;
        }
        battle.doneWithMoves();
    }

    public void doneWithStrikes()
    {
        BattleServerSide battle = game.getBattle();
        if (!isBattleActivePlayer())
        {
            LOGGER.severe(getPlayerName()
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

    void allInitBoard()
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
        for (IClient client : spectatorClients)
        {
            client.initBoard();
        }
    }

    void allTellReplay(boolean val, int maxTurn)
    {
        for (IClient client : clients)
        {
            client.tellReplay(val, maxTurn);
        }
    }

    void allRequestConfirmCatchup(String action)
    {
        // First put them all to the list, send messages after that
        synchronized (waitingToCatchup)
        {
            caughtUpAction = action;
            waitingToCatchup.clear();
            for (IClient client : clients)
            {
                waitingToCatchup.add(client);
            }

            /* better to do the sending not inside the notify. It *might*
             * happen that the sendTo goes an extra way around the wait on
             * selector if the queue is just full and if that reply would
             * want to e.g. remove already from waitingToCatchup list
             * (which is blocked by the sync. up here) we have a deadlock...
             */
            for (IClient client : clients)
            {
                client.confirmWhenCaughtUp();
            }
        }
    }

    void allTellAllLegionLocations()
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

    void allTellGameOver(String message, boolean disposeFollows)
    {
        for (IClient client : clients)
        {
            client.tellGameOver(message, disposeFollows);
        }
    }

    /** Needed if loading game outside the split phase. */
    void allSetupTurnState()
    {
        for (IClient client : clients)
        {
            client
                .setupTurnState(game.getActivePlayer(), game.getTurnNumber());
        }
    }

    void allSetupSplit()
    {
        for (IClient client : clients)
        {
            client.setupSplit(game.getActivePlayer(), game.getTurnNumber());
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

    void allPlaceNewChit(CreatureServerSide critter)
    {
        for (IClient client : clients)
        {
            client.placeNewChit(critter.getName(), critter.getLegion().equals(
                game.getBattle().getDefendingLegion()), critter.getTag(),
                critter.getCurrentHex());
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
    void askAcquireAngel(PlayerServerSide player, Legion legion,
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

    public void acquireAngel(Legion legion, String angelType)
    {
        if (legion != null)
        {
            if (!getPlayer().equals(legion.getPlayer()))
            {
                LOGGER.severe(getPlayerName()
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

    public void doSummon(SummonEvent event)
    {
        if (!isActivePlayer())
        {
            LOGGER.severe(getPlayerName() + " illegally called doSummon()");
            return;
        }
        game.doSummon(event);
    }

    /**
     * Handle mustering for legion.
     * if recruiting with nothing, recruiterName is a non-null String
     * that contains "null".
     */
    public void doRecruit(RecruitEvent event)
    {
        IClient client = getClient(getPlayer());

        // we can't do the "return" inside the if blocks, because then we miss
        // the doneReinforcing at the end...
        // E.g. SimpleAI tried to muster after being attacked, won, acquired
        // angel (=> legion full) => canRecruit false => "illegal recruit".
        //   => game hangs.

        if (event.getLegion() == null)
        {
            LOGGER.severe(getPlayerName()
                + " illegally called doRecruit(): no legion");
            client.nak(Constants.doRecruit, "Null legion");
        }

        else if (!getPlayer().equals(event.getLegion().getPlayer()))
        {
            LOGGER.severe(getPlayerName() + " illegally called doRecruit()");
            client.nak(Constants.doRecruit, "Wrong player");
        }
        else
        {
            CreatureType recruiter = event.getRecruiter();
            String recruiterName = (recruiter == null) ? null : recruiter
                .getName();
            if (!((LegionServerSide)event.getLegion()).canRecruit())
            {
                int size = event.getLegion().getHeight();
                LOGGER.severe("Illegal legion " + event.getLegion() + " (height="
                    + size + ") for recruit: " + event.getAddedCreatureType().getName()
                    + " recruiterName "
                    + recruiterName);
                client.nak(Constants.doRecruit, "Illegal recruit");
            }

            else if (event.getLegion().hasMoved()
                || game.getPhase() == Phase.FIGHT)
            {
                ((LegionServerSide)event.getLegion()).sortCritters();
                CreatureType recruit = event.getAddedCreatureType();
                if (recruit != null)
                {
                    // TODO pass event in
                    game.doRecruit(event.getLegion(), recruit, recruiter);
                }

                if (!((LegionServerSide)event.getLegion()).canRecruit())
                {
                    didRecruit(event, recruiter);
                }
            }
            else
            {
                LOGGER
                    .severe("Illegal recruit (not moved, not in battle) with legion "
                        + event.getLegion().getMarkerId()
                        + " recruit: "
                        + event.getAddedCreatureType().getName()
                        + " recruiterName "
                        + recruiterName);
                client.nak(Constants.doRecruit, "Illegal recruit");
            }
        }

        // Need to always call this to keep game from hanging.
        if (game.getPhase() == Phase.FIGHT)
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

    // TODO should use RecruitEvent
    void didRecruit(AddCreatureEvent event, CreatureType recruiter)
    {
        allUpdatePlayerInfo();

        int numRecruiters = (recruiter == null ? 0 : TerrainRecruitLoader
            .numberOfRecruiterNeeded(recruiter, event.getAddedCreatureType(), event
                .getLegion().getCurrentHex().getTerrain(), event.getLegion()
                .getCurrentHex()));
        String recruiterName = null;
        if (recruiter != null)
        {
            recruiterName = recruiter.getName();
        }

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            // TODO pass event around
            client.didRecruit(event.getLegion(), event.getAddedCreatureType()
                .getName(), recruiterName,
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
            game.revealEvent(true, null, event.getLegion(), recruiterNames);
        }
        game.addCreatureEvent(event);
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

    public void engage(MasterHex hex)
    {
        if (!isActivePlayer())
        {
            LOGGER.severe(getPlayerName() + " illegally called engage()");
            return;
        }
        game.engage(hex);
    }

    void allTellEngagement(MasterHex hex, Legion attacker, Legion defender)
    {
        LOGGER.finest("allTellEngagement() " + hex);
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
        // TODO the next line can throw NPEs when quitting the game
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.severe(getPlayerName() + " illegally called concede()");
            return;
        }
        game.concede(legion);
    }

    public void doNotConcede(Legion legion)
    {
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER
                .severe(getPlayerName() + " illegally called doNotConcede()");
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
            LOGGER.severe(getPlayerName() + " illegally called flee()");
            return;
        }
        game.flee(legion);
    }

    public void doNotFlee(Legion legion)
    {
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.severe(getPlayerName() + " illegally called doNotFlee()");
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

    public void doBattleMove(int tag, BattleHex hex)
    {
        IClient client = getClient(getPlayer());
        if (!isBattleActivePlayer())
        {
            LOGGER
                .severe(getPlayerName() + " illegally called doBattleMove()");
            client.nak(Constants.doBattleMove, "Wrong player");
            return;
        }
        boolean moved = game.getBattle().doMove(tag, hex);
        if (!moved)
        {
            LOGGER.severe("Battle move failed");
            client.nak(Constants.doBattleMove, "Illegal move");
        }
    }

    void allTellBattleMove(int tag, BattleHex startingHex,
        BattleHex endingHex, boolean undo)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellBattleMove(tag, startingHex, endingHex, undo);
        }
    }

    public void strike(int tag, BattleHex hex)
    {
        IClient client = getClient(getPlayer());
        if (!isBattleActivePlayer())
        {
            LOGGER.severe(getPlayerName() + " illegally called strike()");
            client.nak(Constants.strike, "Wrong player");
            return;
        }
        BattleServerSide battle = game.getBattle();
        if (battle == null)
        {
            LOGGER.severe("null battle in Server.strike()");
            client.nak(Constants.strike, "No battle");
            return;
        }
        LegionServerSide legion = battle.getActiveLegion();
        if (legion == null)
        {
            LOGGER.severe("null active legion in Server.strike()");
            client.nak(Constants.strike, "No active legion");
            return;
        }
        CreatureServerSide critter = legion.getCritterByTag(tag);
        if (critter == null)
        {
            LOGGER
                .severe("No critter with tag " + tag + " in Server.strike()");
            client.nak(Constants.strike, "No critter with that tag");
            return;
        }
        CreatureServerSide strikeTarget = battle.getCritter(hex);
        if (strikeTarget == null)
        {
            LOGGER.severe("No target in hex " + hex.getLabel()
                + " in Server.strike()");
            client.nak(Constants.strike, "No target in that hex");
            return;
        }
        if (strikeTarget.getPlayer() == critter.getPlayer())
        {
            LOGGER.severe(critter.getDescription()
                + " tried to strike allied " + strikeTarget.getDescription());
            client.nak(Constants.strike, "Target is friendly");
            return;
        }
        if (critter.hasStruck())
        {
            LOGGER.severe(critter.getDescription() + " tried to strike twice");
            client.nak(Constants.strike, "Critter already struck");
            return;
        }
        critter.strike(strikeTarget);
    }

    public void applyCarries(BattleHex hex)
    {
        if (!isBattleActivePlayer())
        {
            LOGGER
                .severe(getPlayerName() + " illegally called applyCarries()");
            return;
        }
        BattleServerSide battle = game.getBattle();
        CreatureServerSide ourTarget = battle.getCritter(hex);
        battle.applyCarries(ourTarget);
    }

    public void undoBattleMove(BattleHex hex)
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.severe(getPlayerName()
                + " illegally called undoBattleMove()");
            return;
        }
        game.getBattle().undoMove(hex);
    }

    void allTellStrikeResults(CreatureServerSide striker,
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

    void allTellCarryResults(CreatureServerSide carryTarget,
        int carryDamageDone, int carryDamageLeft,
        Set<String> carryTargetDescriptions)
    {
        if (striker == null || target == null || rolls == null)
        {
            LOGGER.severe("Called allTellCarryResults() without setup.");
            if (striker == null)
            {
                LOGGER.severe("null striker");
            }
            if (target == null)
            {
                LOGGER.severe("null target");
            }
            if (rolls == null)
            {
                LOGGER.severe("null rolls");
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

    void allTellHexDamageResults(CreatureServerSide target, int damage)
    {
        this.target = target;

        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellStrikeResults(Constants.HEX_DAMAGE, target.getTag(), 0,
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
            LOGGER.severe(getPlayerName()
                + " illegally called assignStrikePenalty()");
            getClient(getPlayer()).nak(Constants.assignStrikePenalty,
                "Wrong player");
        }
        else if (striker.hasStruck())
        {
            LOGGER.severe(getPlayerName()
                + " assignStrikePenalty -- already struck");
            getClient(getPlayer()).nak(Constants.assignStrikePenalty,
                "Critter already struck");
        }
        else
        {
            striker.assignStrikePenalty(prompt);
        }
    }

    void allInitBattle(MasterHex masterHex)
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
            LOGGER.severe(getPlayerName() + " illegally called mulligan()");
            return;
        }
        int roll = game.mulligan();
        LOGGER.info(getPlayerName() + " takes a mulligan and rolls " + roll);
    }

    public void undoSplit(Legion splitoff)
    {
        if (!isActivePlayer())
        {
            return;
        }
        getActivePlayerSS().undoSplit(splitoff);
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

        PlayerServerSide activePlayer = (PlayerServerSide)game
            .getActivePlayer();
        activePlayer.undoMove(legion);
        MasterHex currentHex = legion.getCurrentHex();

        // needed in undidMove to decide whether to dis/enable button
        boolean splitLegionHasForcedMove = activePlayer
            .splitLegionHasForcedMove();

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
        getActivePlayerSS().undoRecruit(legion);
    }

    public void doneWithSplits()
    {
        if (!isActivePlayer())
        {
            LOGGER.severe(getPlayerName()
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
            game.advancePhase(Phase.SPLIT, getPlayer());
        }
    }

    public void doneWithMoves()
    {
        PlayerServerSide player = getActivePlayerSS();
        if (!isActivePlayer())
        {
            LOGGER.severe(getPlayerName()
                + " illegally called doneWithMoves()");
            getClient(getPlayer())
                .nak(Constants.doneWithMoves, "Wrong player");
        }
        // If any legion has a legal non-teleport move, then
        // the player must move at least one legion.
        else if (player.legionsMoved() == 0 && player.countMobileLegions() > 0)
        {
            LOGGER.finest("At least one legion must move.");
            getClient(getPlayer()).nak(Constants.doneWithMoves,
                "Must move at least one legion");
        }
        // If legions share a hex and have a legal
        // non-teleport move, force one of them to take it.
        else if (player.splitLegionHasForcedMove())
        {
            LOGGER.finest("Split legions must be separated.");
            getClient(getPlayer()).nak(Constants.doneWithMoves,
                "Must separate split legions");
        }
        // Otherwise, recombine all split legions still in
        // the same hex, and move on to the next phase.
        else
        {
            player.recombineIllegalSplits();
            game.advancePhase(Phase.MOVE, getPlayer());
        }
    }

    public void doneWithEngagements()
    {
        if (!isActivePlayer())
        {
            LOGGER.severe(getPlayerName()
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
            game.advancePhase(Phase.FIGHT, getPlayer());
        }
    }

    public void doneWithRecruits()
    {
        if (!isActivePlayer())
        {
            LOGGER.severe(getPlayerName()
                + " illegally called doneWithRecruits()");
            getClient(getPlayer()).nak(Constants.doneWithRecruits,
                "Wrong player");
        }
        else
        {
            PlayerServerSide player = getActivePlayerSS();
            player.commitMoves();

            // Mulligans are only allowed on turn 1.
            if (!game.getOption(Options.unlimitedMulligans))
            {
                player.setMulligansLeft(0);
            }

            game.advancePhase(Phase.MUSTER, getPlayer());
        }
    }

    public void withdrawFromGame()
    {
        if (obsolete || game == null || game.isGameOver())
        {
            return;
        }

        // spectators or rejected clients:
        if (getPlayerName() == null || getPlayerName().startsWith("spectator"))
        {
            return;
        }

        Player player = getPlayer();

        game.handlePlayerWithdrawal(player);
    }

    // client will dispose itself soon,
    // do not attempt to further read from there.
    public void disconnect()
    {
        processingCH.setIsGone(true);
        disposeClientHandler(processingCH);
    }

    public void stopGame()
    {
        if (game != null)
        {
            game.dispose();
        }
    }

    void triggerDispose()
    {
        initiateDisposal = true;
    }

    private List<String> getPlayerInfo(boolean treatDeadAsAlive)
    {
        List<String> info = new ArrayList<String>(game.getNumPlayers());
        for (Player player : game.getPlayers())
        {
            info.add(((PlayerServerSide)player)
                .getStatusInfo(treatDeadAsAlive));
        }
        return info;
    }

    public void doSplit(Legion parent, String childId,
        List<CreatureType> creaturesToSplit)
    {
        LOGGER.log(Level.FINER, "Server.doSplit " + parent + " " + childId
            + " " + Glob.glob(",", creaturesToSplit));
        IClient client = getClient(getPlayer());
        if (!isActivePlayer())
        {
            LOGGER.warning(getPlayerName() + " illegally called doSplit() "
                + "- activePlayer is " + game.getActivePlayer());
            client.nak(Constants.doSplit, "Cannot split: Wrong player "
                + "(active player is " + game.getActivePlayer());
            return;
        }
        if (!game.doSplit(parent, childId, creaturesToSplit))
        {
            LOGGER.warning(getPlayerName() + " tried split for " + parent
                + ", failed!");
            client.nak(Constants.doSplit, "Illegal split / Split failed!");
        }
    }

    /** Called from game after this legion was split off, or by history */
    void allTellDidSplit(Legion parent, Legion child, int turn, boolean history)
    {
        MasterHex hex = parent.getCurrentHex();
        int childSize = child.getHeight();

        LOGGER.log(Level.FINER, "Server.didSplit " + hex + " " + parent + " "
            + child + " " + childSize);
        allUpdatePlayerInfo();

        IClient activeClient = getClient(game.getActivePlayer());

        List<String> splitoffs = ((LegionServerSide)child).getImageNames();
        activeClient.didSplit(hex, parent, child, childSize, splitoffs, turn);

        if (history)
        {
            game.splitEvent(parent, child, splitoffs);
        }

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
                client
                    .didSplit(hex, parent, child, childSize, splitoffs, turn);
            }
        }
    }

    public void doMove(Legion legion, MasterHex hex, EntrySide entrySide,
        boolean teleport, String teleportingLord)
    {
        IClient client = getClient(getPlayer());
        if (!isActivePlayer())
        {
            LOGGER.severe(getPlayerName() + " illegally called doMove()");
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
            LOGGER.severe("Move failed");
            client.nak(Constants.doMove, "Illegal move: " + reasonFail);
        }
    }

    void allTellDidMove(Legion legion, MasterHex startingHex, MasterHex hex,
        EntrySide entrySide, boolean teleport, String teleportingLord)
    {
        PlayerServerSide player = getActivePlayerSS();
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

    void allTellAddCreature(AddCreatureEvent event,
        boolean updateHistory)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            // TODO pass event into client (requires adding the reason as property of the event)
            client.addCreature(event.getLegion(), event.getAddedCreatureType()
                .getName(), event.getReason());
        }
        if (updateHistory)
        {
            game.addCreatureEvent(event);
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
                    client.setLegionStatus(legion, legion.hasMoved(), legion
                        .hasTeleported(), legion.getEntrySide(),
                        ((LegionServerSide)legion).getRecruitName());
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

    // This was earlier called from Client via network message
    // TODO: to be perhaps removed soon? See also SocketClientThread
    public void saveGame(String filename)
    {
        saveGame(filename, false);
    }

    public void saveGame(String filename, boolean autoSave)
    {
        game.saveGameInTry(filename, autoSave);
    }

    // User has requested to save game via File=>Save Game or Save Game as...
    // Inject that into the "handle incoming messages from clients" select
    // loop, to be sure it does not run concurrently while some message
    // from client is currently processed and would change the state of the
    // game while the save is ongoing.
    public void initiateSaveGame(String filename)
    {
        synchronized (guiRequestMutex)
        {
            guiRequestSaveFlag = true;
            guiRequestSaveFilename = filename;
            selector.wakeup();
        }
    }

    public void setGuiSuspendOngoing(boolean newState)
    {
        synchronized (guiRequestMutex)
        {
            if (newState == guiSuspendOngoing)
            {
                return;
            }
            guiSuspendOngoing = newState;
            if (guiSuspendOngoing)
            {
                // Just did set it to true, get the selector thread out of
                // select(), if necessary
                selector.wakeup();
            }
            else
            {
                // Flag was cleared to end the being-suspended
                guiRequestMutex.notify();
            }
        }
    }

    public boolean handleGuiRequests()
    {
        boolean didSomething = false;

        synchronized (guiRequestMutex)
        {
            if (guiRequestSaveFlag)
            {
                game.saveGameInTry(guiRequestSaveFilename, false);
                guiRequestSaveFlag = false;
                guiRequestSaveFilename = null;
                didSomething = true;
            }
            else if (guiSuspendOngoing)
            {
                while (guiSuspendOngoing)
                {
                    try
                    {
                        guiRequestMutex.wait();
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.warning("InterruptedException while waiting "
                            + "on mutes in suspend-ongoing-state!");
                    }
                }
            }
        }

        return didSomething;
    }

    public void checkServerConnection()
    {
        LOGGER.info("Server received checkServerConnection request from "
            + "client " + getPlayerName() + " - sending confirmation.");

        processingCH.serverConfirmsConnection();
    }

    private final HashSet<IClient> waitingToCatchup = new HashSet<IClient>();

    public void clientConfirmedCatchup()
    {
        ClientHandler ch = processingCH;
        String playerName = ch.getPlayerName();

        synchronized (waitingToCatchup)
        {
            if (waitingToCatchup.contains(ch))
            {
                waitingToCatchup.remove(ch);
            }
            else
            {
                LOGGER.warning("Client for " + playerName
                    + " not found from waitingForCatchup list!");
            }

            int remaining = waitingToCatchup.size();
            LOGGER.info("Client " + playerName + " confirmed catch-up. "
                + "Remaining: " + remaining);
            if (remaining <= 0)
            {
                if (caughtUpAction.equals("KickstartGame"))
                {
                    LOGGER.info("All caught up - doing game.kickstartGame()");
                    game.kickstartGame();
                }
                else if (caughtUpAction.equals("DisposeGame"))
                {
                    LOGGER.info("All caught up - doing game.dispose()");
                    game.dispose();
                }
                else
                {
                    LOGGER
                        .severe("All clients caught up, but no action set??");
                }
            }
        }
    }

    /** Used to change a player name after color is assigned. */
    void setPlayerName(Player player, String newName)
    {
        LOGGER.finest("Server.setPlayerName() from " + player.getName()
            + " to " + newName);
        IClient client = getClient(player);
        client.setPlayerName(newName);
    }

    void askPickColor(Player player, final List<PlayerColor> colorsLeft)
    {
        IClient activeClient = getClient(player);
        for (IClient client : clients)
        {
            if (client != null && client != activeClient)
            {
                client.tellWhatsHappening("(Player " + player.getName()
                    + " picks color)");
            }
        }
        // Do this after loop, so that chances are better that this one
        // is active/in top at the end.
        if (activeClient != null)
        {
            activeClient.askPickColor(colorsLeft);
        }
    }

    public void assignColor(PlayerColor color)
    {
        Player p = getPlayer();
        assert p != null : "getPlayer returned null player (in thread "
            + Thread.currentThread().getName() + ")";

        if (!getPlayer().equals(game.getNextColorPicker()))
        {
            LOGGER.severe(getPlayerName() + " illegally called assignColor()");
            return;
        }
        if (getPlayer() == null || getPlayer().getColor() == null)
        {
            game.assignColor(getPlayer(), color);
        }
    }

    void askPickFirstMarker(Player player)
    {
        IClient activeClient = getClient(player);
        for (IClient client : clients)
        {
            if (client != null && client != activeClient)
            {
                client.tellWhatsHappening("(Player " + player.getName()
                    + " picks initial marker)");
            }
        }
        // Do this after loop, so that chances are better that this one
        // is active/in top at the end.
        if (activeClient != null)
        {
            activeClient.askPickFirstMarker();
        }
    }

    public void assignFirstMarker(String markerId)
    {
        Player player = game.getPlayerByName(getPlayerName());
        assert player.getMarkersAvailable().contains(markerId) : getPlayerName()
            + " illegally called assignFirstMarker()";
        ((PlayerServerSide)player).setFirstMarker(markerId);
        game.nextPickColor();
    }

    /** Hack to set color on load game. */
    void allSetColor()
    {
        for (Player player : game.getPlayers())
        {
            PlayerColor color = player.getColor();
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
            client.syncOption(optname, value);
        }
    }

    void oneSetOption(Player player, String optname, boolean value)
    {
        oneSetOption(player, optname, String.valueOf(value));
    }

    void allSyncOption(String optname, String value)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.syncOption(optname, value);
        }
    }

    void allSyncOption(String optname, boolean value)
    {
        allSyncOption(optname, String.valueOf(value));
    }

    void allSyncOption(String optname, int value)
    {
        allSyncOption(optname, String.valueOf(value));
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

    /*
     * Called by GameServerSide, to initiate the "Quit All"
     */
    public void doSetWhatToDoNext(WhatToDoNext whatToDoNext,
        boolean triggerQuitTimer)
    {
        whatNextManager.setWhatToDoNext(whatToDoNext, triggerQuitTimer);
    }

    /* Called from ServerStartupProgress, if user wants to cancel during load
     * (e.g. one client did not come in). Clean up everything and get
     *  back to the GetPlayers / Game Setup dialog.
     */
    public void startupProgressAbort()
    {
        whatNextManager
            .setWhatToDoNext(WhatToDoNext.GET_PLAYERS_DIALOG, false);
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
        LOGGER.severe("User pressed 'QUIT' in startupProgress window "
            + "- doing System.exit(1)");
        System.exit(1);
    }

    public GameServerSide getGame()
    {
        return game;
    }
}
