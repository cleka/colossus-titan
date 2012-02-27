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
import java.util.Date;
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
import net.sf.colossus.game.actions.AddCreatureAction;
import net.sf.colossus.game.actions.Recruitment;
import net.sf.colossus.game.actions.Summoning;
import net.sf.colossus.util.BuildInfo;
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

    private final MessageRecorder recorder;

    /**
     *  Maybe also save things like the originating IP, in case a
     *  connection breaks and we need to authenticate reconnects.
     *  Do not share these references. */
    private final List<IClient> clients = new ArrayList<IClient>();
    private final List<IClient> remoteClients = new ArrayList<IClient>();
    private final List<IClient> spectatorClients = new ArrayList<IClient>();
    private final List<RemoteLogHandler> remoteLogHandlers = new ArrayList<RemoteLogHandler>();

    /** Map of players to their clients. */
    private final Map<Player, IClient> playerToClientMap = new HashMap<Player, IClient>();

    // list of SocketChannels that are currently active
    private final List<SocketChannel> activeSocketChannelList = new ArrayList<SocketChannel>();

    /** Number of player clients we're waiting for to *connect* */
    private int waitingForPlayers;

    /** Number of player clients we're waiting for to *join*
     *  - when last one has joined, then kick of newGame2() or loadGame2()
     */
    private int waitingForPlayersToJoin = 0;

    /** Semaphor for synchronized access to waitingForPlayersToJoin */
    private final Object wfptjSemaphor = new Object();

    /** Will be set to true after all clients are properly connected */
    private boolean sendPingRequests = false;

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
    private boolean inPauseState = false;

    /* static so that new instance of Server can destroy a
     * previously allocated FileServerThread */
    private static Thread fileServerThread = null;

    private boolean serverRunning = false;
    private boolean obsolete = false;
    private boolean shuttingDown = false;
    private boolean forceShutDown = false;
    private boolean initiateDisposal = false;
    private String caughtUpAction = "";

    private final int timeoutDuringStart = 1000;
    private final int timeoutDuringGame = 1000;
    private final int timeoutDuringShutdown = 1000;

    /** How long in public server games socket shall wait for Clients. */
    private final static int WEBGAMES_STARTUP_TIMEOUT_SECS = 20;
    private final int PING_REQUEST_INTERVAL_SEC = 30;

    /**
     * How many ms ago last ping round was done.
     */
    private long lastPingRound = 0;

    /**
     * When server started to listed for clients
     */
    private long startInititatedTime = 0;

    /**
     * Timeout how long server waits for clients before giving up;
     * in normal/local games 0, meaning forever;
     * in public server usage set to WEBGAMES_STARTUP_TIMEOUT_SECS
     */
    private int gameStartupTimeoutSecs = 0;

    // Earlier I have locked on an Boolean object itself,
    // which I modify... and when this is done too often,
    // e.g. in ClientSocketThread every read, it caused
    // StockOverflowException... :-/
    private final Object disposeAllClientsDoneMutex = new Object();
    private boolean disposeAllClientsDone = false;

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    // The ClientHandler of which the input is currently processed
    ClientHandler processingCH = null;

    // During processing of redoLog, need to override the processing player
    // with the one who did that event originally
    // (because right now, all redo is processed while processingCH is the
    // one of the last player that joined and triggered the loadGame2() etc.)
    ClientHandler overriddenCH = null;

    // Channels are queued into here, to be removed from selector on
    // next possible opportunity ( = when all waiting-to-be-processed keys
    // have been processed).
    private final List<ClientHandler> channelChanges = new ArrayList<ClientHandler>();

    Server(GameServerSide game, WhatNextManager whatNextMgr, int port)
    {
        this.game = game;
        this.port = port;
        this.whatNextManager = whatNextMgr;
        this.recorder = new MessageRecorder();

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
        initWaitingForPlayersToJoin(waitingForPlayers);
        InstanceTracker.register(this, "only one");
    }

    @Override
    public void run()
    {
        startInititatedTime = new Date().getTime();
        if (game.getNotifyWebServer().isActive())
        {
            gameStartupTimeoutSecs = WEBGAMES_STARTUP_TIMEOUT_SECS;
        }
        boolean gotAll = waitForClients();
        game.actOnWaitForClientsCompleted(gotAll);
        if (!remoteClients.isEmpty())
        {
            sendPingRequests = true;
        }

        int timeout = timeoutDuringGame;
        int disposeRound = 0;
        while (!shuttingDown && disposeRound < 60)
        {
            waitOnSelector(timeout, false);
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
                    // When all caught up, this will trigger game.dispose(),
                    //   which which do stopServerRunning,
                    //     which will do stopFileServer, disposeAllClients,
                    //     and set shuttingDown and serverRunning to false.
                    // Skip those that are "in trouble" ( where ClientHandler
                    // currently wouldn't in practice send anything anyway...)
                    allRequestConfirmCatchup("DisposeGame", true);
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

            LOGGER.severe(message);
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

            LOGGER.severe(message);
            JOptionPane.showMessageDialog(startLog.getFrame(), message,
                "Starting game (server side) failed!",
                JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        }
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
            LOGGER.info("Waiting for clients, before waitOnSelector()");
            waitOnSelector(timeoutDuringStart, true);
        }

        return (waitingForPlayers == 0);
    }

    public void overrideProcessingCH(Player player)
    {
        overriddenCH = processingCH;
        processingCH = (ClientHandler)getClient(player);
    }

    public void restoreProcessingCH()
    {
        processingCH = overriddenCH;
        overriddenCH = null;
    }

    public void waitOnSelector(int timeout, boolean stillWaitingForClients)
    {
        try
        {
            handleOutsideChanges(timeout, stillWaitingForClients);
            handleSelectedKeys();
            handleChannelChanges();
            repeatTellOneHasNetworkTrouble();
            if (sendPingRequests)
            {
                allRequestPingIfNeeded();
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
                + "waiting on Selector!" + "\nStack trace:\n"
                + ErrorUtils.makeStackTraceString(e);
            ErrorUtils.showExceptionDialog(null, message, "Exception caught!",
                false);
        }
    }

    public class WithdrawInfo
    {
        public long deadline;
        public long intervalLen;
        public long intervals;
        public long lastNotification;
        public ClientHandler ch;

        public WithdrawInfo(ClientHandler ch, int intervals, long intervalLen)
        {
            long now = new Date().getTime();
            this.deadline = now + (intervals * intervalLen);
            this.lastNotification = now;
            this.ch = ch;
            this.intervalLen = intervalLen;
            this.intervals = intervals;
        }

        public long getLastNotification()
        {
            return lastNotification;
        }

        public void setLastNotification(long when)
        {
            this.lastNotification = when;
        }
    }

    private final Map<String, WithdrawInfo> forcedWithdraws = new HashMap<String, WithdrawInfo>();

    private void handleOutsideChanges(int timeout,
        boolean stillWaitingForClients) throws IOException
    {
        if (stopAcceptingFlag)
        {
            LOGGER.info("stopAccepting flag was set...");
            stopAccepting();
            stopAcceptingFlag = false;
        }

        LOGGER.log(Level.FINEST, "before select()");
        int num = selector.select(timeout);
        LOGGER.log(Level.FINEST, "select returned, " + num
            + " channels are ready to be processed.");

        if (!forcedWithdraws.isEmpty())
        {
            Set<String> keys = forcedWithdraws.keySet();
            for (String name : keys)
            {
                WithdrawInfo info = forcedWithdraws.get(name);
                long now = new Date().getTime();
                int timeLeft = (int)((info.deadline - now) / 1000);
                if (timeLeft > 0)
                {
                    long timeSinceLastNotif = now - info.getLastNotification();
                    if (timeSinceLastNotif >= info.intervalLen)
                    {
                        othersTellRemainingTime(info.ch, timeLeft);
                        info.setLastNotification(now);
                    }
                    LOGGER.fine("forcedWithdraw for player " + name + ": "
                        + timeLeft + " seconds left.");
                }
                else
                {
                    LOGGER.info("Forced withdraw for player " + name);
                    forcedWithdraws.remove(name);
                    String message = "You were too long disconnected - Game did automatic withdraw! Sorry.";
                    info.ch.messageFromServer(message);
                    game.handlePlayerWithdrawal(game.getPlayerByName(name));
                }
            }
        }

        if (handleGuiRequests())
        {
            // ok, select returned due to a wakeup call
        }
        else if (num == 0)
        {
            // LOGGER.info("Server side select timeout...");
            if (stillWaitingForClients)
            {
                long now = new Date().getTime();
                int alreadyTrying = ((int)(now - startInititatedTime)) / 1000;
                if (gameStartupTimeoutSecs > 0
                    && alreadyTrying > gameStartupTimeoutSecs)
                {
                    String reason = "Waiting for clients timed out - giving up!";
                    LOGGER.warning(reason);
                    logToStartLog(reason);
                    game.getNotifyWebServer().gameStartupFailed(reason);
                    forceShutDown = true;
                }
            }
        }

        if (forceShutDown)
        {
            LOGGER.log(Level.FINEST,
                "waitOnSelector: force shutdown now true! num=" + num);
            stopAccepting();
            stopServerRunning();
        }
    }

    private void handleSelectedKeys() throws IOException,
        ClosedChannelException
    {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> readyKeys = selectedKeys.iterator();
        while (readyKeys.hasNext() && !shuttingDown)
        {
            SelectionKey key = readyKeys.next();
            readyKeys.remove();

            if (key.isAcceptable())
            {
                // Accept the new connection
                SocketChannel sc = ((ServerSocketChannel)key.channel())
                    .accept();
                sc.configureBlocking(false);
                LOGGER.log(Level.FINE, "Accepted: sc = " + sc);
                SelectionKey readKey = sc.register(selector,
                    SelectionKey.OP_READ);

                LOGGER.info("Another client accepted.");
                ClientHandler ch = new ClientHandler(this, sc, readKey);
                readKey.attach(ch);
                // This is sent only for the reason that the client gets
                // an initial response quickly.
                ch.sendToClient("SignOn: processing");
                synchronized (activeSocketChannelList)
                {
                    activeSocketChannelList.add(sc);
                }
            }
            else
            {
                boolean anythingDone = false;

                SocketChannel sc = (SocketChannel)key.channel();
                ClientHandler ch = (ClientHandler)key.attachment();

                if (ch != null)
                {
                    if (key.isReadable())
                    {
                        processingCH = ch;
                        handleReadFromChannel(key, sc);
                        processingCH = null;
                        anythingDone = true;
                    }

                    if (key.isValid() && key.isWritable())
                    {
                        LOGGER.info("Channel for " + ch.getPlayerName()
                            + " got writable again.");

                        // unregister write. If next time fails, it will register again.
                        key.interestOps(SelectionKey.OP_READ);

                        // just call it to flush out what is still there
                        ch.clearTemporarilyInTrouble();
                        ch.sendToClient(null);

                        anythingDone = true;
                    }
                    if (!anythingDone)
                    {
                        // can this happen? Just to be sure...
                        LOGGER.warning("Unexpected type of ready Operation: "
                            + key.readyOps());
                    }
                }
                else
                {
                    // can this happen? Just to be sure...
                    LOGGER.warning("ClientHandler for ready key is null?");
                }
            }
        }
        // just to be sure.
        selectedKeys.clear();
    }

    public void sleepFor(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            LOGGER.log(Level.FINEST,
                "sleepFor: InterruptException caught... ignoring it...");
        }
    }

    private void handleChannelChanges() throws IOException
    {
        synchronized (channelChanges)
        {
            // Can't use iterator, because e.g. removal of last human/observer
            // will add more items to the channelChanges list.
            while (!channelChanges.isEmpty())
            {
                ClientHandler nextCH = channelChanges.remove(0);
                SocketChannel sc = nextCH.getSocketChannel();
                SelectionKey key = nextCH.getSelectorKey();
                if (key == null)
                {
                    LOGGER.warning("key for to-be-closed-channel is null!");
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
        if (acceptKey == null)
        {
            LOGGER.warning("request to stopAccepting but acceptKey is null!");
            return;
        }
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

    private int handleReadFromChannel(SelectionKey key, SocketChannel sc)
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
                        // Remote entity did shut the socket down.
                        // Do the same from our end and cancel the channel.
                        processingCH.setIsGone(true, "EOF on channel");
                        if (read > 0)
                        {
                            LOGGER.info("Before EOF processing, calling "
                                + "processByteBuffer to handle the " + read
                                + " bytes that were read before.");
                            processByteBuffer();
                            read = 0;
                        }
                        withdrawFromGameIfRelevant(null, processingCH.didExplicitDisconnect());
                        disconnectChannel(sc, key);
                    }
                    break;
                }
                read += r;

                // THIS HERE EXISTS ONLY FOR DEBUG/DEVELOPMENT PURPOSES
                if (processingCH.wasFakeDisconnectFlagSet())
                {
                    // TODO Improve: right now it needs something to be processed from
                    // client before the exception strikes. Fix better, e.g. wakeup
                    // Selector?
                    processingCH.clearDisconnectClient();
                    LOGGER.warning("After read, throwing the fake exception!");
                    throw new IOException(
                        "ClientTriggeredFakeServerDisconnectException");
                }
            }
            catch (IOException e)
            {
                // set isGone first, to prevent from sending log info to
                // client channel - channel is gone anyway...
                processingCH.setIsGone(true, "REASON");
                LOGGER.log(Level.WARNING, "IOException '" + e.getMessage()
                    + "' while reading from channel for player "
                    + getPlayerName(), e);

                if (read > 0)
                {
                    LOGGER.warning("Before IOException handling processing, "
                        + "calling processByteBuffer to handle the " + read
                        + " bytes that were read before.");
                    processByteBuffer();
                    read = 0;
                }

                // The remote forcibly/unexpectedly closed the connection,
                // cancel the selection key and close the channel.
                withdrawFromGameIfRelevant(e, processingCH.didExplicitDisconnect());
                disconnectChannel(sc, key);
                return 0;
            }
        }

        if (read > 0)
        {
            LOGGER.finest("Calling processByteBuffer to process the " + read
                + " bytes received from channel" + sc);
            processByteBuffer();
        }
        else
        {
            LOGGER.finest("readFromChannel: 0 bytes read.");
        }

        return read;
    }

    private void processByteBuffer()
    {
        byteBuffer.flip();
        // NOTE that the following might cause trouble
        // if logging is set to FINEST for server,
        // and the disconnect does not properly set the
        // isGone flag...
        // No problem any more as currently "send log
        // stuff to remote clients" is removed.
        LOGGER.finest("* before ch.processInput()");
        processingCH.processInput(byteBuffer);
        LOGGER.finest("* after  ch.processInput()");
    }

    /**
     * Something with the connection of "processingCH" which makes perhaps Withdraw necessary.
     *
     * If client seems to support reconnect, mark CH to be temp. disconnected,
     * otherwise take care of the proper withdrawal.
     * @param gotException An exception, if calling this was caused by an (IO)Exception,
     * otherwise null, i.e. it was triggered by EOF.
     * @param didDisconnect whether an explicit dicsonnect request message had been
     * received already from that client ( = no point to wait for reconnect attempt).
     */
    private void withdrawFromGameIfRelevant(Exception gotException, boolean didDisconnect)
    {
        if (isWithdrawalIrrelevant())
        {
            return;
        }

        Player player = getPlayer();
        if (player == null)
        {
            LOGGER.warning("Skipping withdrawFromGame processing for "
                + " ClientHandler of " + getPlayerName()
                + " - no player found for the name");
            return;
        }

        if (player.isDead())
        {
            LOGGER.info("Skipping withdrawFromGame processing for "
                + getPlayerName() + " since player is already dead");
            return;
        }

        String reason;
        if (gotException != null)
        {
            reason = "Caught: " + gotException.getMessage();
        }
        else
        {
            reason = "EOF";
        }

        try
        {
            if (didDisconnect)
            {
                LOGGER.info(reason + " on channel for client "
                    + getPlayerName() + " after Client explicitly requested"
                    + " disconnect - proceeding withDraw and Disconnecting");
                withdrawFromGame();
            }
            else if (!game.getOption(Options.keepAccepting))
            {
                LOGGER.warning(reason + " on channel for client "
                    + getPlayerName() + " - game is not configured to accept "
                    + "reconnects, withDrawing player");
                withdrawFromGame();
            }
            else if (processingCH.supportsReconnect())
            {
                LOGGER.warning(reason + " on channel for client " + getPlayerName()
                    + " - skipping withDraw, waiting for reconnect attempt");
                processingCH.setTemporarilyDisconnected();
                triggerWithdrawIfDoesNotReconnect(30000, 6);
            }
            else
            {
                LOGGER.warning(reason + " on channel for client " + getPlayerName()
                    + " - can't reconnect, withDrawing player");
                withdrawFromGame();
            }
        }
        // just in case. To make sure the disconnect one level up really happens, no matter what.
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE,
                "Exception while withdrawFromGameIfRelevant: ", e);
        }
    }

    private void triggerWithdrawIfDoesNotReconnect(final long intervalLen,
        final int intervals)
    {
        String withdrawName = processingCH.getPlayerName();

        if (forcedWithdraws.containsKey(withdrawName))
        {
            LOGGER.warning("Removing _still_ existing entry for '"
                + withdrawName
                + "' from forcedWithdraws list.");
            forcedWithdraws.remove(withdrawName);
        }
        long howLong = (long)(intervals * intervalLen / 1000.0);
        LOGGER.info("Initiating delayed withdraw for player " + withdrawName
            + " intervalLen = " + intervalLen + " count " + intervals + " (= "
            + howLong + " seconds)");
        appendToConnLogs(processingCH, "NOTE: Connection to client '"
            + withdrawName + "' lost; waiting " + howLong
            + " seconds for possible reconnect...");
        forcedWithdraws.put(withdrawName, new WithdrawInfo(processingCH,
            intervals, intervalLen));
    }

    /**
     * Called when EOF encountered on a clienthandler. Store which is the currently
     * processed CH, and after a timeout withdraw that player.
     * @param intervalLen
     * @param intervals
     */
    /*
    private void old_triggerWithdrawIfDoesNotReconnect(final long intervalLen,
        final int intervals)
    {
        final ClientHandler currentProcessingCH = processingCH;
        Runnable r = new Runnable()
        {
            public void run()
            {
                String withdrawName = currentProcessingCH.getPlayerName();

                LOGGER.info("Initiating delayed withdraw for player "
                    + withdrawName);
                for (int i = 0; i < intervals; i++)
                {
                    WhatNextManager.sleepFor(intervalLen);

                    int remaining = intervals - i;
                    LOGGER.fine("countdown for withdraw: " + remaining
                        + " intervals of " + intervalLen + " ms left.");
                }
                LOGGER.info("Time's up! Withdrawing player " + withdrawName);
                forcedWithdraws.add(withdrawName);
                selector.wakeup();
            }
        };
        new Thread(r).start();
    }
    */

    /**
     *  Put the ClientHandler into the queue to be removed
     *  from selector on next possible opportunity
     */
    void queueClientHandlerForChannelChanges(ClientHandler ch)
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
            waitUntilOverMutex.notifyAll();
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

    /**
     * Close the SocketChannel, cancel the selection key and unregister
     * the SocketChannel from list of active SocketChannels.
     *
     * @param sc SocketChannel of the client
     * @param key Key for that SocketChannel
     * @throws IOException
     */
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
        if (playerToClientMap.isEmpty())
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
        return game.getBattleSS() != null
            && game.getBattleSS().getBattleActivePlayer() != null
            && getPlayer().equals(game.getBattleSS().getBattleActivePlayer());
    }

    /**
     * Add a Client.
     *
     * @param client
     * @param playerName
     * @param remote
     * @param clientVersion
     * @return Reason why adding Client was refused, null if all is fine.
     */
    String addClient(final IClient client, final String playerName,
        final boolean remote, final int clientVersion, String buildInfo)
    {
        boolean isReconnect = false;

        LOGGER.info("Server.addClient() called with: " + "playerName: '"
            + playerName + "', remote: '" + remote + "', client version '"
            + clientVersion + "', client build info: '" + buildInfo + "'");
        if (!buildInfo.equals(BuildInfo.getFullBuildInfoString()))
        {
            LOGGER.info("NOTE: client build info differs from "
                + "server build info.");
        }

        if (clientVersion <= IServer.MINIMUM_CLIENT_VERSION)
        {
            String versionText = (clientVersion == -1 ? "-1 (=no Version defined)"
                : clientVersion + "");
            LOGGER.warning("Rejecting client " + playerName + " because it "
                + "uses too old Client version: " + versionText
                + " but server requires at least: "
                + IServer.MINIMUM_CLIENT_VERSION);

            // We do not disable the autoCloseStartupLog here, because since a
            // player will be missing the startup will not reach the point
            // where to close it automatically.
            logToStartLog("PROBLEM: One client attempted to join with player"
                + " name " + playerName + "\n- rejected, because client uses "
                + "too old version: " + versionText);
            return "You are using too old Client Version: " + versionText
                + " - expected at least: " + IServer.MINIMUM_CLIENT_VERSION;
        }
        else if (clientVersion != IServer.CLIENT_VERSION)
        {
            String diffWhat = (clientVersion < IServer.CLIENT_VERSION ? "older"
                : "newer");
            logToStartLog("NOTE: Client version mismatch detected!!!\n"
                + "One client attempted to join with player name "
                + playerName + ", using different (" + diffWhat
                + ") client version: " + clientVersion
                + " - trying it anyway.");
            disableAutoCloseStartupLog();
            LOGGER.info("Client " + playerName + " uses Client Version: "
                + clientVersion + " but we would expect "
                + IServer.CLIENT_VERSION + " - trying it anyway.");
        }

        Player player = null;
        if (playerName.equalsIgnoreCase("spectator"))
        {
            LOGGER.info("addClient for a spectator.");
        }
        else if (remote)
        {
            player = game.findNetworkPlayer(playerName);
            if (player == null)
            {
                player = game.getPlayerByNameIgnoreNull(playerName);
            }
        }
        else
        {
            player = game.getPlayerByNameIgnoreNull(playerName);
        }

        LOGGER
            .info("Trying to identify player/client for new connection which identifies itself as player "
                + playerName);
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
            return "No player with name " + playerName + " expected.";
        }
        /*
         *  The special Re-Connect case starts here:
         */
        else if (isReconnectAttempt(player))
        {
            ClientHandler existingCH = (ClientHandler)playerToClientMap.get(player);
            if (existingCH != null && existingCH.getPlayerName().equals(player.getName()))
            {
                othersTellReconnectOngoing(existingCH);
                isReconnect = true;
                LOGGER.info("All right, reconnection of disconnected player!");
                ((ClientHandler)client).cloneRedoQueue(existingCH);
                playerToClientMap.remove(player);
                clients.remove(existingCH);
                existingCH.declareObsolete();
                LOGGER.info("Removing player with name " + player.getName()
                    + " from forcedWithDrawlist. Size was "
                    + forcedWithdraws.size());
                forcedWithdraws.remove(player.getName());
                LOGGER.info("Removed  player with name " + player.getName()
                    + " from forcedWithDrawlist. Size now "
                    + forcedWithdraws.size());
            }
            else
            {
                LOGGER.warning("Could not add client, "
                    + "because Player for playerName " + playerName
                    + " had already signed on.");
                logToStartLog("NOTE: One client attempted to join with player name "
                    + playerName
                    + " - rejected, because same player name already "
                    + "joined.");
                return "Other player with same name already connected.";
            }
        }
        else
        {
            name = playerName;
        }

        clients.add(client);

        /*
        // Note, actually last joined client signonName will always be still null...
         *
        System.out.println("\n\n******\nclients contains now "
            + clients.size() + " clients.");
        for (IClient c : clients)
        {
            String displayName = "unknown";
            if ((c != null) && ((ClientHandler)c).getPlayerName() != null)
            {
                displayName = ((ClientHandler)c).getPlayerName();
            }
            else if (c != null && ((ClientHandler)c).getSignonName() != null)
            {
                displayName = ((ClientHandler)c).getSignonName();
            }
            System.out.println("Client: " + displayName + " .");
        }
        */

        if (player != null)
        {
            playerToClientMap.put(player, client);
        }
        else
        {
            spectatorClients.add(client);
        }

        if (remote)
        {
            addRemoteClient(client, player);
        }

        if (player != null && isReconnect)
        {
            logToStartLog("\nPlayer " + player.getName()
                + " reconnected, game can continue now.\n");

        }
        else if (player != null)
        {
            logToStartLog((remote ? "Remote" : "Local") + " player " + name
                + " signed on.");
            game.getNotifyWebServer().gotClient(player.getName(), remote);
            --waitingForPlayers;

            LOGGER.info("Decremented waitingForPlayers (to connect) to "
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

        // ReasonFail == null means "everything is fine.":
        return null;
    }

    private boolean isReconnectAttempt(Player player)
    {
        IClient client = playerToClientMap.get(player);

        if (client != null)
        {
            return true;
        }
        return false;
    }

    /**
     *  When the last player has *joined* (not just connected), he calls this
     *  here, and this will proceed with either loadGame2() or newGame2().
     */
    public void startGame()
    {
        boolean sa = !game.getOption(Options.keepAccepting);
        LOGGER.info("Game started, setting stopAcceptingFlag to " + sa);
        stopAcceptingFlag = sa;
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
                if (Options.isStartupTest())
                {
                    ErrorUtils
                        .setErrorDuringFunctionalTest("Loading/Replay failed!");
                    game.stopAllDueToFunctionalTestCompleted();
                }
                else
                {
                    logToStartLog("\n-- Press Abort button "
                        + "to return to Start Game dialog --\n");
                    loadFailed();
                }
                return;
            }
        }
        else
        {
            game.newGame2();
        }

        logToStartLog("\nStarting the game now.\n");
        game.getNotifyWebServer().gameStartupCompleted();
        if (startLog != null)
        {
            startLog.setCompleted();
        }
    }

    /**
     *  Initialize the number of players we wait for to join (thread-safe)
     *
     *  @param count the number of players that are expected to join
     */
    private void initWaitingForPlayersToJoin(int count)
    {
        synchronized (wfptjSemaphor)
        {
            waitingForPlayersToJoin = count;
        }
    }

    /**
     *  Decrement the number of players we wait for to join by one in a
     *  thread-safe way, and return the new value.
     *
     *  @return The number of players that still need to join
     */
    private int decrementWaitingForPlayersToJoin()
    {
        synchronized (wfptjSemaphor)
        {
            return --waitingForPlayersToJoin;
        }
    }

    public int getClientCount()
    {
        return clients.size();
    }

    private void addRemoteClient(final IClient client, final Player player)
    {
        RemoteLogHandler remoteLogHandler = new RemoteLogHandler(this);
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
            client.disposeClientHandler();
        }
        clients.clear();
        playerToClientMap.clear();
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
        ErrorUtils.showErrorDialog(startLog.getFrame(),
            "Loading game failed!",
            "Loading, replay of history and comparison between saved "
                + "state and replay result failed!!\n\n"
                + "Click Abort on the Startup Progress Dialog to return to "
                + "Game setup dialog to start a different or new one.");
    }

    public void cleanupStartlog()
    {
        if (startLog != null)
        {
            startLog.dispose();
            startLog = null;
        }
    }

    public void doCleanup()
    {
        cleanupStartlog();
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
            LOGGER
                .warning(getPlayerName()
                    + " illegally called leaveCarryMode(): not battle active player");
            return;
        }
        BattleServerSide battle = game.getBattleSS();
        battle.leaveCarryMode();
    }

    public void doneWithBattleMoves()
    {
        if (!isBattleActivePlayer())
        {
            LOGGER
                .warning(getPlayerName()
                    + " illegally called doneWithBattleMoves(): not battle active player");
            LOGGER.info(processingCH.dumpLastProcessedLines());
            getClient(getPlayer()).nak(Constants.doneWithBattleMoves,
                "Illegal attempt to end phase (wrong player)");
            return;
        }

        BattleServerSide battle = game.getBattleSS();
        if (!battle.getBattlePhase().isMovePhase())
        {
            LOGGER.warning(getPlayerName()
                + " illegally called doneWithBattleMoves(): not move phase");
            LOGGER.info(processingCH.dumpLastProcessedLines());
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
        String reason = isDoneWithStrikesOk();
        if (reason != null)
        {
            LOGGER.warning(getPlayerName()
                + " illegally called doneWithStrikes(): " + reason);
            LOGGER.info(processingCH.dumpLastProcessedLines());
            getClient(getPlayer()).nak(Constants.doneWithStrikes, reason);
        }
        else
        {
            game.getBattleSS().doneWithStrikes();
        }
    }

    /**
     * Validates that it it OK to be "done with strikes" now for executing player
     * @return reason why it's not OK; null if all is ok
     */
    private String isDoneWithStrikesOk()
    {
        BattleServerSide battle = game.getBattleSS();
        if (!isBattleActivePlayer())
        {
            return "Not Battle Active player";
        }
        else if (!battle.getBattlePhase().isFightPhase())
        {
            return "Not a fight phase: battle phase is "
                + battle.getBattlePhase().toString();
        }
        else if (battle.isForcedStrikeRemaining())
        {
            return "Forced strikes remain";
        }
        return null;
    }

    private IClient getClient(Player player)
    {
        if (playerToClientMap.containsKey(player))
        {
            return playerToClientMap.get(player);
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

    void allTellRedo(boolean val)
    {
        for (IClient client : clients)
        {
            client.tellRedo(val);
        }
    }

    void allRequestConfirmCatchup(String action, boolean skipInTrouble)
    {
        // First put them all to the list, send messages after that
        synchronized (waitingToCatchup)
        {
            caughtUpAction = action;
            waitingToCatchup.clear();
            for (IClient client : clients)
            {
                boolean skip = false;

                // Do not wait for clients that are already gone, e.g. when
                // one remote disconnected this might cause a withdrawal
                // which might cause GameOver.
                if (((ClientHandler)client).isGone())
                {
                    skip = true;
                }

                // In some case (currently: during game dispose) do not send
                // to clients in trouble, they won't respond probably anyway...
                if (skipInTrouble
                    && ((ClientHandler)client).isTemporarilyInTrouble())
                {
                    skip = true;
                }

                if (!skip)
                {
                    waitingToCatchup.add(client);
                }
            }

            for (IClient client : waitingToCatchup)
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

    void repeatTellOneHasNetworkTrouble()
    {
        ArrayList<ClientHandler> troubleCHs = new ArrayList<ClientHandler>();
        for (IClient client : clients)
        {
            ClientHandler ch = (ClientHandler)client;
            if (ch.isTemporarilyInTrouble())
            {
                troubleCHs.add(ch);
            }
        }

        if (!troubleCHs.isEmpty())
        {
            for (ClientHandler chInTrouble : troubleCHs)
            {
                othersTellOneHasNetworkTrouble(chInTrouble);
            }
        }
    }

    void othersTellOneHasNetworkTrouble(ClientHandler chInTrouble)
    {
        String playerInTrouble = chInTrouble.getPlayerName();
        int howLong = (int)(chInTrouble.howLongAlreadyInTrouble() / 1000L);
        String message = "Problems writing to client of player "
            + playerInTrouble + " (" + howLong + " secs) - still trying...";
        appendToConnLogs(chInTrouble, message);
    }

    void othersTellOnesTroubleIsOver(ClientHandler chInTrouble)
    {
        String name = chInTrouble.getPlayerName();
        String message = "It seems writing to player " + name
            + " succeeded now.";
        appendToConnLogs(chInTrouble, message);
    }

    void othersTellReconnectOngoing(ClientHandler chInTrouble)
    {
        // Note that we need to use getSignOnName here!!
        String playerInTrouble = chInTrouble.getSignonName();
        String message = "Player " + playerInTrouble
            + " reconnected! Data synchronization ongoing";
        appendToConnLogs(chInTrouble, message);
    }

    void othersTellRemainingTime(ClientHandler chInTrouble, int secondsLeft)
    {
        // Note that we need to use getSignOnName here!!
        String playerInTrouble = chInTrouble.getSignonName();
        String message = "Player " + playerInTrouble + " has still "
            + secondsLeft + " seconds left before being withdrawn";
        appendToConnLogs(chInTrouble, message);
    }

    void othersTellReconnectCompleted(ClientHandler chInTrouble)
    {
        String name = chInTrouble.getPlayerName();
        String message = "Client of player " + name
            + " reconnect now successfully completed.";
        appendToConnLogs(chInTrouble, message);
    }

    void appendToConnLogs(ClientHandler chInTrouble, String message)
    {
        for (IClient client : clients)
        {
            // no need/use to inform the troubled one itself...
            if (client != chInTrouble)
            {
                client.appendToConnectionLog(message);
            }
        }
    }


    /**
     * IF last ping round is at least PING_REQUEST_INTERVAL_SEC seconds ago,
     * then send a ping request to all clients (except those which are in
     * trouble anyway).
     */
    void allRequestPingIfNeeded()
    {
        long now = new Date().getTime();
        if (now - lastPingRound > 1000 * PING_REQUEST_INTERVAL_SEC)
        {
            long ago = (now - lastPingRound) / 1000;
            LOGGER.info("Last ping round is " + ago
                + " secs ago - doing another.");
            lastPingRound = now;
            for (IClient client : clients)
            {
                if (!((ClientHandler)client).isTemporarilyInTrouble())
                {
                    client.pingRequest();
                }
            }
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

    void kickPhase()
    {
        // XXX TODO Should do only for the active Client!
        for (IClient client : clients)
        {
            client.kickPhase();
        }
    }

    void allSetupBattleSummon()
    {
        BattleServerSide battle = game.getBattleSS();
        for (IClient client : clients)
        {
            client.setupBattleSummon(battle.getBattleActivePlayer(), battle
                .getBattleTurnNumber());
        }
    }

    void allSetupBattleRecruit()
    {
        BattleServerSide battle = game.getBattleSS();
        for (IClient client : clients)
        {
            client.setupBattleRecruit(battle.getBattleActivePlayer(), battle
                .getBattleTurnNumber());
        }
    }

    void allSetupBattleMove()
    {
        BattleServerSide battle = game.getBattleSS();
        for (IClient client : clients)
        {
            client.setupBattleMove(battle.getBattleActivePlayer(), battle
                .getBattleTurnNumber());
        }
    }

    void allSetupBattleFight()
    {
        BattleServerSide battle = game.getBattleSS();
        for (IClient client : clients)
        {
            if (battle != null)
            {
                client.setupBattleFight(battle.getBattlePhase(), battle
                    .getBattleActivePlayer());
            }
        }
    }

    void allPlaceNewChit(CreatureServerSide critter)
    {
        for (IClient client : clients)
        {
            client.placeNewChit(critter.getName(), critter.getLegion().equals(
                game.getBattleSS().getDefendingLegion()), critter.getTag(),
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
        List<CreatureType> recruits)
    {
        if (legion.getHeight() < 7)
        {
            IClient client = getClient(player);
            if (client != null)
            {
                client.askAcquireAngel(legion, recruits);
            }
        }
    }

    public void acquireAngel(Legion legion, CreatureType angelType)
    {
        if (legion != null)
        {
            if (!getPlayer().equals(legion.getPlayer()))
            {
                LOGGER.warning(getPlayerName()
                    + " illegally called acquireAngel(): player "
                    + getPlayer().getName() + " is not owner of legion "
                    + legion.getMarkerId());
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

    public void doSummon(Summoning event)
    {
        if (!isActivePlayer())
        {
            LOGGER.warning(getPlayerName()
                + " illegally called doSummon(): not battle active player");
            return;
        }
        game.doSummon(event);
    }

    /**
     * Handle mustering for legion.
     * if recruiting with nothing, recruiterName is a non-null String
     * that contains "null".
     */
    public void doRecruit(Recruitment event)
    {
        IClient client = getClient(getPlayer());

        // we can't do the "return" inside the if blocks, because then we miss
        // the doneReinforcing at the end...
        // E.g. SimpleAI tried to muster after being attacked, won, acquired
        // angel (=> legion full) => canRecruit false => "illegal recruit".
        //   => game hangs.

        if (event.getLegion() == null)
        {
            LOGGER.warning(getPlayerName()
                + " illegally called doRecruit(): null legion");
            client.nak(Constants.doRecruit, "Null legion");
        }

        else if (!getPlayer().equals(event.getLegion().getPlayer()))
        {
            LOGGER.warning(getPlayerName()
                + " illegally called doRecruit(): does not own legion "
                + event.getLegion().getMarkerId());
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
                LOGGER.warning("Illegal legion " + event.getLegion()
                    + " (height=" + size + ") for recruit: "
                    + event.getAddedCreatureType().getName()
                    + " recruiterName " + recruiterName);
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
                LOGGER.warning("Illegal recruit (not moved, not in battle) "
                    + "with legion " + event.getLegion().getMarkerId()
                    + " recruit: " + event.getAddedCreatureType().getName()
                    + " recruiterName " + recruiterName);
                client.nak(Constants.doRecruit, "Illegal recruit");
            }
        }

        // Need to always call this to keep game from hanging.
        if (game.getPhase() == Phase.FIGHT)
        {
            if (game.getBattleSS() != null)
            {
                game.getBattleSS().doneReinforcing();
            }
            else
            {
                game.doneReinforcing();
            }
        }
    }

    // TODO should use RecruitEvent
    void didRecruit(AddCreatureAction event, CreatureType recruiter)
    {
        allUpdatePlayerInfo();

        int numRecruiters = (recruiter == null ? 0 : TerrainRecruitLoader
            .numberOfRecruiterNeeded(recruiter, event.getAddedCreatureType(),
                event.getLegion().getCurrentHex().getTerrain(), event
                    .getLegion().getCurrentHex()));
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            // TODO pass event around
            client.didRecruit(event.getLegion(), event.getAddedCreatureType(),
                recruiter, numRecruiters);
        }

        // reveal only if there is something to tell
        if (recruiter != null)
        {
            List<CreatureType> recruiters = new ArrayList<CreatureType>();
            for (int i = 0; i < numRecruiters; i++)
            {
                recruiters.add(recruiter);
            }
            game.revealEvent(true, null, event.getLegion(), recruiters,
                Constants.reasonRecruiter);
        }
        game.addCreatureEvent(event, Constants.reasonRecruited);
    }

    void undidRecruit(Legion legion, CreatureType recruit, boolean reinforced)
    {
        allUpdatePlayerInfo();
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.undidRecruit(legion, recruit);
        }
        game.undoRecruitEvent(legion);
        String reason = reinforced ? Constants.reasonReinforced
            : Constants.reasonRecruited;
        game.removeCreatureEvent(legion, recruit, reason);
    }

    public void engage(MasterHex hex)
    {
        if (!isActivePlayer())
        {
            LOGGER.warning(getPlayerName()
                + " illegally called engage(): not active player");
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
        // Should not happen but at least once did - legion was just
        // eliminated and player still conceded?
        if (legion == null)
        {
            LOGGER.warning(getPlayerName()
                + " illegally called concede(): null legion!");
            return;
        }

        // TODO the next line can throw NPEs when quitting the game
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.warning(getPlayerName()
                + " illegally called concede(): does not own legion "
                + legion.getMarkerId());
            return;
        }
        game.concede(legion);
    }

    public void doNotConcede(Legion legion)
    {
        // Can this happen? Just to be sure, similar as in Concede(legion).
        // Should not happen but at least once did - legion was just
        // eliminated and player still conceded?
        if (legion == null)
        {
            LOGGER.warning(getPlayerName()
                + " illegally called doNotconcede(): null legion!");
            return;
        }

        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.warning(getPlayerName()
                + " illegally called doNotConcede(): does not own legion "
                + legion.getMarkerId());
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
            LOGGER.warning(getPlayerName() + " illegally called flee(): "
                + "does not own legion " + legion.getMarkerId());
            return;
        }
        game.flee(legion);
    }

    public void doNotFlee(Legion legion)
    {
        if (!getPlayer().equals(legion.getPlayer()))
        {
            LOGGER.warning(getPlayerName() + " illegally called doNotFlee(): "
                + " does not own legion " + legion.getMarkerId());
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
            LOGGER.warning(getPlayerName()
                + " illegally called doBattleMove(): "
                + "not battle active player");
            client.nak(Constants.doBattleMove, "Wrong player");
            return;
        }
        String reasonFail = game.getBattleSS().doMove(tag, hex);
        if (reasonFail != null)
        {
            if (processingCH.canHandleBattleMoveNak())
            {
                LOGGER.info("Battle move failed - giving Client a nak "
                    + "doBattleMove (illegal move)");
                client.nak(Constants.doBattleMove, "Illegal move: "
                    + reasonFail);
            }
            else
            {
                LOGGER.info("Battle move failed - skipping the nak for "
                    + "doBattleMove (illegal move) because client can't "
                    + "handle it");
            }
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
            LOGGER.warning(getPlayerName() + " illegally called strike(): "
                + "not battle active player");
            client.nak(Constants.strike, "Wrong player");
            return;
        }
        BattleServerSide battle = game.getBattleSS();
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
        CreatureServerSide strikeTarget = battle.getCreatureSS(hex);
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
            LOGGER.warning(getPlayerName()
                + " illegally called applyCarries(): : "
                + "not battle active player");
            return;
        }
        BattleServerSide battle = game.getBattleSS();
        CreatureServerSide ourTarget = battle.getCreatureSS(hex);
        battle.applyCarries(ourTarget);
    }

    public void undoBattleMove(BattleHex hex)
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.warning(getPlayerName()
                + " illegally called undoBattleMove(): "
                + "not battle active player");
            return;
        }
        game.getBattleSS().undoMove(hex);
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

    void allTellHexSlowResults(CreatureServerSide target, int slowValue)
    {
        this.target = target;
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.tellSlowResults(target.getTag(), slowValue);
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
        Player player = game.getBattleSS().getBattleActivePlayer();
        IClient client = getClient(player);
        List<String> choices = new ArrayList<String>();
        Iterator<PenaltyOption> it = penaltyOptions.iterator();
        while (it.hasNext())
        {
            PenaltyOption po = it.next();
            striker = (CreatureServerSide)po.getStriker();
            choices.add(po.toString());
        }
        client.askChooseStrikePenalty(choices);
    }

    public void assignStrikePenalty(String prompt)
    {
        if (!isBattleActivePlayer())
        {
            LOGGER.warning(getPlayerName()
                + " illegally called assignStrikePenalty(): "
                + "not battle active player");
            getClient(getPlayer()).nak(Constants.assignStrikePenalty,
                "Wrong player");
        }
        else if (striker.hasStruck())
        {
            LOGGER.warning(getPlayerName()
                + " illegally called assignStrikePenalty(): "
                + "already struck");
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
        BattleServerSide battle = game.getBattleSS();
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.initBattle(masterHex, battle.getBattleTurnNumber(), battle
                .getBattleActivePlayer(), battle.getBattlePhase(), battle
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
            LOGGER.warning(getPlayerName() + " illegally called mulligan(): "
                + "not active player");
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

        game.undoMove(legion);
    }

    public void allTellUndidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove)
    {
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
            LOGGER.warning(getPlayerName()
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
            LOGGER.warning(getPlayerName()
                + " illegally called doneWithMoves(): not active player");
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
            LOGGER.warning(getPlayerName()
                + " illegally called doneWithEngagements(): "
                + "not active player");
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
            LOGGER.warning(getPlayerName()
                + " illegally called doneWithRecruits(): not active player");
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

    public boolean isWithdrawalIrrelevant()
    {
        return (obsolete || game == null || game.isGameOver());
    }

    /** Withdraw the player for which data was currently processed on socket
     *  (if it is a real one, and withdrawal still makes sense).
     */
    public void withdrawFromGame()
    {
        if (isWithdrawalIrrelevant())
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

    /** Withdraw a specific player of which we know only the name; e.g.
     *  when one clientHandler when trying to write to another clientHandler
     *  encountered closed socket.
     *  @param playerName Name of the player to withdraw
     */
    public void withdrawFromGame(String playerName)
    {
        LOGGER.info("Withdrawal for specific player " + playerName
            + " requested.");

        if (obsolete || game == null || game.isGameOver())
        {
            LOGGER.finest("No need for withdraw - game over etc.");
            return;
        }

        // spectators or rejected clients:
        if (playerName == null || playerName.startsWith("spectator"))
        {
            LOGGER.finest("No need for withdraw - null player or spectator.");
            return;
        }

        Player player = game.getPlayerByName(playerName);
        if (player != null)
        {
            LOGGER.finest("Doing game.handlePlayerWithdrawal for "
                + playerName);
            game.handlePlayerWithdrawal(player);
        }
        else
        {
            LOGGER.warning("Can't do game.handlePlayerWithdrawal for "
                + playerName + " because getPlayerName gave null player!");
        }
    }

    // client will dispose itself soon,
    // do not attempt to further read from there.
    public void sendDisconnect()
    {
        queueClientHandlerForChannelChanges(processingCH);
        clientWontConfirmCatchup(processingCH, "Client disconnected.");
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

        List<CreatureType> splitoffs = child.getCreatureTypes();
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
        boolean teleport, CreatureType teleportingLord)
    {
        IClient client = getClient(getPlayer());
        // Check for "is it the right player", but not during replay / redo
        if (!game.isReplayOngoing() && !isActivePlayer())
        {
            LOGGER.warning(getPlayerName() + " illegally called doMove(): "
                + "not active player");
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
            LOGGER.severe("Move failed, reason " + reasonFail);
            client.nak(Constants.doMove, "Illegal move: " + reasonFail);
        }
    }

    void allTellDidMove(Legion legion, MasterHex startingHex, MasterHex hex,
        EntrySide entrySide, boolean teleport, CreatureType teleportingLord)
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
        CreatureType summon)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.didSummon(receivingLegion, donorLegion, summon);
        }
    }

    void allTellAddCreature(AddCreatureAction event, boolean updateHistory,
        String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            // TODO pass event into client (requires adding the reason as property of the event)
            client.addCreature(event.getLegion(),
                event.getAddedCreatureType(), event.getReason());
        }
        if (updateHistory)
        {
            game.addCreatureEvent(event, reason);
        }
    }

    void allTellRemoveCreature(Legion legion, CreatureType creature,
        boolean updateHistory, String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.removeCreature(legion, creature, reason);
        }
        if (updateHistory)
        {
            game.removeCreatureEvent(legion, creature, reason);
        }
    }

    void allRevealLegion(Legion legion, String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.revealCreatures(legion, legion.getCreatureTypes(), reason);
        }
        game
            .revealEvent(true, null, legion, legion.getCreatureTypes(), reason);
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
            client.revealEngagedCreatures(legion, legion.getCreatureTypes(),
                isAttacker, reason);
        }
        game
            .revealEvent(true, null, legion, legion.getCreatureTypes(), reason);
    }

    /** Call from History during load game only */
    void allRevealLegion(Legion legion, List<CreatureType> creatures,
        String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.revealCreatures(legion, creatures, reason);
        }
    }

    void oneRevealLegion(Legion legion, Player player, String reason)
    {
        IClient client = getClient(player);
        if (client != null)
        {
            client.revealCreatures(legion, legion.getCreatureTypes(), reason);
        }
        List<Player> li = new ArrayList<Player>();
        li.add(player);
        game.revealEvent(false, li, legion, legion.getCreatureTypes(), reason);
    }

    /** Call from History during load game only */
    void oneRevealLegion(Player player, Legion legion,
        List<CreatureType> creatureNames, String reason)
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
                        .hasTeleported(), legion.getEntrySide(), legion
                        .getRecruit());
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

    void allRevealCreatures(Legion legion, List<CreatureType> creatureNames,
        String reason)
    {
        Iterator<IClient> it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = it.next();
            client.revealCreatures(legion, creatureNames, reason);
        }
        game.revealEvent(true, null, legion, creatureNames, reason);
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
        LOGGER.warning("Got from client via socket the request to load a game"
            + ", but 'via Socket' is not supported any more! Ignoring it.");
    }

    // This was earlier called from Client via network message
    // TODO: to be perhaps removed soon? See also SocketClientThread
    public void saveGame(String filename)
    {
        saveGame(filename, false);
    }

    public void saveGame(String filename, boolean autoSave)
    {
        game.saveGameWithErrorHandling(filename, autoSave);
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

    public void setPauseState(boolean newState)
    {
        synchronized (guiRequestMutex)
        {
            if (newState == inPauseState)
            {
                return;
            }
            inPauseState = newState;
            if (inPauseState)
            {
                // Just did set it to true, get the selector thread out of
                // select(), if necessary
                selector.wakeup();
            }
            else
            {
                // Flag was cleared to end the pause
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
                game.saveGameWithErrorHandling(guiRequestSaveFilename, false);
                guiRequestSaveFlag = false;
                guiRequestSaveFilename = null;
                didSomething = true;
            }
            else if (inPauseState)
            {
                while (inPauseState)
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
        if (Constants.USE_RECORDER)
        {
            recorder.printMessagesToConsole(processingCH);
        }
    }

    private final HashSet<IClient> waitingToCatchup = new HashSet<IClient>();

    /**
     * Check whether client is currently expected to send a caught-Up
     * confirmation.
     * If yes: it won't happen, so act accordingly.
     * If no : even better so, so just do nothing.
     * @param reason Reason why client won't send the confirmation
     *        (typically disconnected or something).
     */
    public void clientWontConfirmCatchup(ClientHandler ch, String reason)
    {
        String playerName = ch.getPlayerName();

        synchronized (waitingToCatchup)
        {
            if (waitingToCatchup.contains(ch))
            {
                waitingToCatchup.remove(ch);
                int remaining = waitingToCatchup.size();
                LOGGER.info("Client " + playerName
                    + " won't confirm catch-up (" + reason + "). Remaining: "
                    + remaining);
                if (remaining <= 0)
                {
                    actOnAllCaughtUp();
                }
            }
        }
    }

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
                actOnAllCaughtUp();
            }
        }
    }

    private void actOnAllCaughtUp()
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
            LOGGER.severe("All clients caught up, but no action set??");
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
            LOGGER
                .warning(getPlayerName() + " illegally called assignColor()");
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

    // To disable the auto close when warnings where displayed
    private void disableAutoCloseStartupLog()
    {
        if (startLog != null)
        {
            startLog.disableAutoClose();
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

    public MessageRecorder getRecorder()
    {
        return recorder;
    }

    public void replyToRequestGameInfo()
    {
        processingCH.tellInitialGameInfo(game.getVariant().getName(),
            getGame().getPreliminaryPlayerNames());
    }

    public void requestSyncDelta(int lastReceivedMessageNr, int syncRequestNumber)
    {
        LOGGER.info("Client requests sync #" + syncRequestNumber
            + " after reconnect, last msg nr was " + lastReceivedMessageNr);
        processingCH.syncAfterReconnect(lastReceivedMessageNr,
            syncRequestNumber);
    }

    public void joinGame(String playerName)
    {
        // @TODO: move to outside Select loop
        //   => notify main thread to do this?
        int stillMissing = decrementWaitingForPlayersToJoin();
        if (stillMissing == 0)
        {
            LOGGER
                .info("waitingForPlayersToJoin now zero - calling startGame.");
            startGame();
        }
        else
        {
            LOGGER.info("waitingForPlayersToJoin now " + stillMissing);
        }
    }

    public void enforcedDisconnectClient(String name)
    {
        try
        {
            Player p = game.getPlayerByName(name);
            if (p != null)
            {
                IClient client = playerToClientMap.get(p);
                ((ClientHandler)client).fakeDisconnectClient();
            }
        }
        catch(Exception e)
        {
            // ignore it... it's for develop/debugging purpose only
            // (at the moment, at least...;-)
        }
    }
}
