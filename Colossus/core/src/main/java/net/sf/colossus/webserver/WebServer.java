package net.sf.colossus.webserver;


import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IColossusMail;
import net.sf.colossus.webcommon.IGameRunner;
import net.sf.colossus.webcommon.IRunWebServer;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;
import net.sf.colossus.webcommon.GameInfo.GameState;


/**
 *  The main class for the WebServer
 *  - brings up the WebServer GUI
 *  - starts the ServerSocket and listens there for WebClients
 *  - based on actions coming from clients, keeps book of
 *    "instant" and "running" games (both GameInfo objects),
 *    and tell the GameInfo objects when to start the game.
 *
 *  @author Clemens Katzer
 */
public class WebServer implements IWebServer, IRunWebServer
{
    private static final Logger LOGGER = Logger.getLogger(WebServer.class
        .getName());

    private WebServerOptions options = null;
    private PortBookKeeper portBookKeeper = null;
    private IWebServerGUI gui = null;

    private final IColossusMail mailObject;

    /**
     * Controls whether the GUI is shown or not.
     *
     * At the moment this is configured only by the possibility of doing so:
     * if the environment supports running a GUI, we will, if not, we won't.
     * It could be combined with a command line option to suppress the GUI
     * even if it would be possible to show one.
     */
    private final boolean runGUI = !GraphicsEnvironment.isHeadless();

    private boolean shutdownRequested = false;

    private String proposedGamesFilename;
    private boolean proposedGamesListModified = false;

    private final int maxClients;

    private final HashMap<String, GameInfo> proposedGames = new HashMap<String, GameInfo>();

    private final ArrayList<GameInfo> runningGames = new ArrayList<GameInfo>();
    private final ArrayList<GameInfo> endingGames = new ArrayList<GameInfo>();

    // Used also as separator for storing proposed games to file:
    private final static String sep = IWebServer.WebProtocolSeparator;

    // Server socket port where we listen for WebClient connections
    private final int port;

    private ServerSocket serverSocket;

    private final List<ChatMessage> lastNChatMessages = new ArrayList<ChatMessage>();

    public static void main(String[] args)
    {
        String optionsFileName = WebServerConstants.defaultOptionsFilename;

        if (args.length > 0)
        {
            optionsFileName = args[0];
        }

        WebServer server = new WebServer(optionsFileName);

        LOGGER.log(Level.FINEST, "before init socket");
        server.runSocketServer();

        // execution comes to here only when server is shut down
        // (shutDownRequested set to true, so that the loop quits)
        server = null;

        LOGGER.log(Level.ALL, "WebServer.main() will end...");

        // JVM should do a clean exit now, no System.exit() needed.
        // @TODO: does it work on all platforms, all Java versions?
        //        If not, build here a demon timer that does the
        //        System.exit() after a few seconds...?
    }

    public WebServer(String optionsFile)
    {
        this.options = new WebServerOptions(optionsFile);
        options.loadOptions();

        this.port = options
            .getIntOptionNoUndef(WebServerConstants.optServerPort);
        this.maxClients = options
            .getIntOptionNoUndef(WebServerConstants.optMaxClients);

        int portRangeFrom = options
            .getIntOptionNoUndef(WebServerConstants.optPortRangeFrom);
        int availablePorts = options
            .getIntOptionNoUndef(WebServerConstants.optAvailablePorts);

        mailObject = new ColossusMail(options);

        portBookKeeper = new PortBookKeeper(portRangeFrom, availablePorts);

        // Load users from file:
        String usersFile = options
            .getStringOption(WebServerConstants.optUsersFile);
        int maxUsers = options.getIntOption(WebServerConstants.optMaxUsers);
        User.readUsersFromFile(usersFile, maxUsers);

        // Restore proposed games from file:
        proposedGamesFilename = options
            .getStringOption(WebServerConstants.optGamesFile);
        if (proposedGamesFilename == null)
        {
            proposedGamesFilename = WebServerConstants.DEFAULT_GAMES_FILE;
            LOGGER
                .warning("Filename for storing games not defined in cfg file!"
                    + " Using default " + proposedGamesFilename);
        }
        readGamesFromFile(proposedGamesFilename, proposedGames);

        LOGGER.log(Level.INFO, "Server started: port " + port
            + ", maxClients " + maxClients);

        long now = new Date().getTime();
        ChatMessage startMsg = new ChatMessage(IWebServer.generalChatName,
            now, "SYSTEM", "WebServer started. Welcome!!");
        storeMessage(lastNChatMessages, startMsg);

        if (runGUI)
        {
            this.gui = new WebServerGUI(this);
        }
        else
        {
            this.gui = new NullWebServerGUI();
        }

        updateGUI();
        /*
         boolean runGameConsole = false;
         if (runGameConsole)
         {
         console();
         }
         */

        LOGGER.log(Level.FINEST, "WebServer instantiated, maxClients = "
            + maxClients + " , port = " + port);
    }

    void runSocketServer()
    {
        int socketQueueLen = options
            .getIntOptionNoUndef(WebServerConstants.optSocketQueueLen);

        LOGGER.log(Level.FINEST, "About to create server socket on port "
            + port);
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
                serverSocket = null;
            }
            serverSocket = new ServerSocket(port, socketQueueLen);
            serverSocket.setReuseAddress(true);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.FINEST, "Could not create socket. "
                + "Configure networking in OS.", ex);
            System.exit(1);
        }

        LOGGER.log(Level.FINEST,
            "\nUser-server starting up, waiting for clients");

        while (!shutdownRequested)
        {
            boolean rejected = waitForUser();
            if (rejected)
            {
                LOGGER.log(Level.FINEST, "accepted one client but "
                    + "rejected it - maxClients limit reached.");
            }
            else
            {
                LOGGER.log(Level.FINEST, "added one client");
            }
        }

        User.storeUsersToFile();
        User.cleanup();

        gui.shutdown();
        gui = null;
        options = null;
        portBookKeeper = null;

        LOGGER.log(Level.FINE, "Web Server after main loop.");
    }

    // called by WebServerGUI.closeWindow() event
    // OR     by WebServerSocketThread.shutdownServer().
    // If the latter ( = admin user requested it remotely), need to close
    // also the GUI window -- if there is one.
    public void initiateShutdown(String byUserName)
    {
        if (byUserName == null)
        {
            LOGGER.info("Web Server shut down by GUI");
        }
        else
        {
            LOGGER.info("Web server shut down remotely by user '" + byUserName
                + "'");
        }
        try
        {
            shutdownServer();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "exception in initiateShutdown() ", e);
        }
    }

    public void shutdownServer()
    {
        shutdownRequested = true;
        closeAllWscst();
        makeDummyConnection();
    }

    public void makeDummyConnection()
    {
        // make a dummy connection, to get the thread out of the
        // accept().
        try
        {
            Socket socket = new Socket("localhost", port);
            socket.close();
        }
        // UnknownHostException, IOException, IllegalBlockingModeException
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "exception in makeDummyConnection", e);
        }
    }

    private boolean waitForUser()
    {
        boolean rejected = false;
        Socket clientSocket = null;
        try
        {
            clientSocket = serverSocket.accept();
            LOGGER.log(Level.FINEST, "Got client connection from "
                + clientSocket.getInetAddress().toString());
            LOGGER.log(Level.FINEST, "Got client connection from IP: "
                + clientSocket.getInetAddress().toString());

            if (shutdownRequested)
            {
                serverSocket.close();
                return false;
            }
            else if (User.getLoggedInCount() >= maxClients)
            {
                rejected = true;
                WebServerClientSocketThread.reject(clientSocket);
            }
            else
            {
                // ok, can log in
            }
        }
        catch (IOException ex)
        {
            if (shutdownRequested)
            {
                LOGGER.log(Level.SEVERE,
                    "Waiting for user did throw exception: " + ex.toString());
            }
            else
            {
                // does not matter
                LOGGER.log(Level.FINEST,
                    "ShutdownRequested, closing caused an exception: "
                        + ex.toString());

            }
            return false;
        }

        if (!rejected)
        {
            WebServerClientSocketThread cst = new WebServerClientSocketThread(
                this, clientSocket);
            cst.start();
            updateUserCounts();
        }

        return rejected;
    }

    private void closeAllWscst()
    {
        ArrayList<User> toDo = new ArrayList<User>();

        // copy them first to own list.
        // Otherwise we get a ConcurrentModificationException
        Iterator<User> it2 = User.getLoggedInUsersIterator();
        while (it2.hasNext())
        {
            User u = it2.next();
            toDo.add(u);
        }

        Iterator<User> it = toDo.iterator();
        while (it.hasNext())
        {
            User u = it.next();
            WebServerClientSocketThread thread = (WebServerClientSocketThread)u
                .getThread();
            if (thread == null)
            {
                LOGGER.log(Level.FINE,
                    "Thread for user is empty - skipping interrupt and join.");
                continue;
            }
            try
            {
                thread.interrupt();
            }
            catch (NullPointerException e)
            {
                // It's funny. It seems the interrupt above always gives a
                // null pointer exception, but the interrupting has done
                // it's job anyway...
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Different exception than usual "
                    + "while tried to interrupt 'other': ", e);
            }

            LOGGER.log(Level.FINEST, "WebServer.closeAllWscst's: before join");
            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.FINE,
                    "thread.join() interrupted?? Ignoring it.", e);
            }
        }

    }

    /** Each user server thread's name is set to it user's name. */
    String getPlayerName()
    {
        return Thread.currentThread().getName();
    }

    public PortBookKeeper getPortBookKeeper()
    {
        return this.portBookKeeper;
    }

    public void updateGUI()
    {
        assert gui != null;
        gui.setScheduledGamesInfo(countProposedGames(true)
            + " scheduled games stored");
        gui.setInstantGamesInfo(countProposedGames(false)
            + " instant games stored");
        gui.setRunningGamesInfo(runningGames.size() + " running games");
        gui.setEndingGamesInfo(endingGames.size() + " games just ending");
    }

    public GameInfo proposeGame(String initiator, String variant,
        String viewmode, long startAt, int duration, String summary,
        String expire, boolean unlimitedMulligans, boolean balancedTowers,
        int min, int target, int max)
    {
        GameInfo gi = new GameInfo(initiator, variant, viewmode, startAt,
            duration, summary, expire, unlimitedMulligans, balancedTowers,
            min, target, max);

        proposedGames.put(gi.getGameId(), gi);
        proposedGamesListModified = true;

        // System.out.println("Game " + gi.getGameId()
        //     + " was proposed. Adding to list.");

        updateGUI();
        allTellGameInfo(gi);

        return gi;
    }

    public void reEnrollIfNecessary(WebServerClientSocketThread newCst)
    {
        IWebClient client = newCst;
        User newUser = newCst.getUser();

        for (GameInfo gi : proposedGames.values())
        {
            if (gi.reEnrollIfNecessary(newUser))
            {
                LOGGER.log(Level.FINEST, "Telling user " + newUser.getName()
                    + " that he is still enrolled in game " + gi.getGameId());
                // userMap finds already new user for that name
                client.didEnroll(gi.getGameId(), newUser.getName());
            }
        }
    }

    public void tellAllGamesFromListToOne(WebServerClientSocketThread cst,
        ArrayList<GameInfo> games)
    {
        IWebClient client = cst;

        Iterator<GameInfo> it = games.iterator();
        while (it.hasNext())
        {
            GameInfo gi = it.next();
            client.gameInfo(gi);
        }
    }

    public void tellAllProposedGamesToOne(WebServerClientSocketThread cst)
    {
        ArrayList<GameInfo> list = new ArrayList<GameInfo>(proposedGames
            .values());
        tellAllGamesFromListToOne(cst, list);
    }

    public void tellAllRunningGamesToOne(WebServerClientSocketThread cst)
    {
        tellAllGamesFromListToOne(cst, runningGames);
    }

    public void allTellGameInfo(GameInfo gi)
    {
        Iterator<User> it = User.getLoggedInUsersIterator();
        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameInfo(gi);
        }
    }

    public void tellEnrolledGameStartsSoon(GameInfo gi)
    {
        String gameId = gi.getGameId();
        gi.setState(GameState.ACTIVATED);

        ArrayList<User> players = gi.getPlayers();
        Iterator<User> it = players.iterator();

        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameInfo(gi);
            client.gameStartsSoon(gameId);
        }
    }

    public void tellEnrolledGameStartsNow(GameInfo gi, String host, int port)
    {
        String gameId = gi.getGameId();
        gi.setState(GameState.READY_TO_CONNECT);

        ArrayList<User> players = gi.getPlayers();
        Iterator<User> it = players.iterator();

        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameInfo(gi);
            client.gameStartsNow(gameId, port, host);
        }
    }

    public void tellEnrolledGameStarted(GameInfo gi)
    {
        gi.setState(GameState.RUNNING);
        String gameId = gi.getGameId();
        // System.out.println("tellEnrolledGameStarted adds it to running");
        proposedGames.remove(gameId);
        runningGames.add(gi);
        // System.out.println("Running: " + runningGames.toString());
        updateGUI();

        for (User u : gi.getPlayers())
        {
            IWebClient client = (IWebClient)u.getThread();
            // might be null, for example if automatic
            // "on game start close web client" is in use
            if (client != null)
            {
                client.gameInfo(gi);
            }
        }
    }

    public void gameFailed(GameInfo gi, String reason)
    {
        LOGGER.log(Level.WARNING, "GAME starting/running failed!!! Reason: "
            + reason);
    }

    // =========== Client actions ==========

    public void enrollUserToGame(String gameId, String username)
    {
        GameInfo gi = findByGameId(gameId);
        User user = User.findUserByName(username);

        if (gi != null)
        {
            String reasonFail = gi.enroll(user);
            proposedGamesListModified = true;
            if (reasonFail == null)
            {
                allTellGameInfo(gi);
                IWebClient client = (IWebClient)user.getThread();
                client.didEnroll(gameId, user.getName());
                proposedGamesListModified = true;
            }
        }
    }

    public void unenrollUserFromGame(String gameId, String username)
    {
        GameInfo gi = findByGameId(gameId);
        User user = User.findUserByName(username);

        if (gi != null)
        {
            String reasonFail = gi.unenroll(user);
            proposedGamesListModified = true;
            if (reasonFail == null)
            {
                allTellGameInfo(gi);
                IWebClient client = (IWebClient)user.getThread();
                client.didUnenroll(gameId, user.getName());
            }
        }
    }

    public void cancelGame(String gameId, String byUser)
    {
        GameInfo gi = findByGameId(gameId);

        if (gi == null)
        {
            LOGGER.warning("Attempt to cancel game with id " + gameId
                + " but no GameInfo found for that id.");
            return;
        }

        IGameRunner gr = gi.getGameRunner();
        if (gr != null)
        {
            gr.setServerNull();
            gr.start(); // does nothing, just to get it GC'd and finalized
        }
        else
        {
            LOGGER.info("For Cancel: no GameRunner for GameInfo with gameId "
                + gameId);
        }

        Iterator<User> it = User.getLoggedInUsersIterator();
        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameCancelled(gameId, byUser);
        }

        proposedGames.remove(gi.getGameId());
        proposedGamesListModified = true;
        updateGUI();
    }

    public void startGame(String gameId)
    {
        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            boolean success = startOneGame(gi);
            proposedGamesListModified = true;

            LOGGER.log(Level.FINEST, "Found gi, got port " + port);
            if (!success)
            {
                LOGGER.log(Level.SEVERE, "\nstarting/running game " + gameId
                    + " failed!!\n");
            }
        }
    }

    /**
     *  A game was started by a WebClient user locally on his computer
     *  and is ready to accept the other players as remote client;
     *  so we notify them and tell them host and port to where to connect.
     */
    public void startGameOnPlayerHost(String gameId, String hostingPlayer,
        String playerHost, int port)
    {
        // System.out.println("WebServer.startGameOnPlayerHost " + playerHost);

        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            ArrayList<User> users = gi.getPlayers();
            for (User u : users)
            {
                LOGGER.info("Informing player " + u.getName()
                    + " that game starts at host of hosting player "
                    + hostingPlayer);
                IWebClient webClient = (IWebClient)u.getThread();
                webClient.gameStartsNow(gameId, port, playerHost);
            }
        }
        else
        {
            LOGGER.warning("Did not find a GameInfo for gameId " + gameId
                + " to inform the other players to connect to host "
                + playerHost + " port " + port);
        }
        proposedGamesListModified = true;
    }

    public void informStartedByPlayer(String gameId)
    {
        LOGGER.info("Tell enrolled players that game " + gameId
            + " was started by a player.");
        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            tellEnrolledGameStarted(gi);
        }
        else
        {
            LOGGER.severe("Got request informGameStarted but did not find "
                + "any game for gameId " + gameId);
        }
        proposedGamesListModified = true;
    }

    public void informLocallyGameOver(String gameId)
    {
        LOGGER.info("WebServer informLocallyGameOver id " + gameId);
        GameInfo gi = findFromRunningGames(gameId);
        unregisterGamePlayerPC(gi);
        LOGGER.info("WebServer informLocallyGameOver id " + gameId + " ENDS");
    }

    public void updateUserCounts()
    {
        int connected = User.getLoggedInCount();
        allTellUserCounts();
        gui.setUserInfo(connected + " users connected.");
    }

    public void allTellUserCounts()
    {
        if (User.getLoggedInCount() > 0)
        {
            int loggedin = User.getLoggedInCount();

            // the other five are still dummies.
            int enrolled = User.getEnrolledCount();
            int playing = User.getPlayingCount();
            int dead = User.getDeadCount();
            long ago = 0;
            String text = "";

            Iterator<User> it = User.getLoggedInUsersIterator();
            while (it.hasNext())
            {
                User u = it.next();
                IWebClient client = (IWebClient)u.getThread();
                client.userInfo(loggedin, enrolled, playing, dead, ago, text);
            }
        }
    }

    public void chatSubmit(String chatId, String sender, String message)
    {
        if (chatId.equals(IWebServer.generalChatName))
        {
            Iterator<User> it = User.getLoggedInUsersIterator();
            while (it.hasNext())
            {
                User u = it.next();
                IWebClient client = (IWebClient)u.getThread();
                long now = new Date().getTime();
                ChatMessage msg = new ChatMessage(chatId, now, sender, message);
                storeMessage(lastNChatMessages, msg);
                client.chatDeliver(chatId, now, sender, message, false);
            }
        }
        else
        {
            LOGGER.log(Level.WARNING, "Chat for chatId " + chatId
                + " not implemented.");
        }
    }

    public void tellLastChatMessagesToOne(WebServerClientSocketThread cst,
        String chatId)
    {
        List<ChatMessage> messageList;
        if (chatId.equals(IWebServer.generalChatName))
        {
            messageList = lastNChatMessages;
        }
        else
        {
            LOGGER.log(Level.WARNING, "tellLastChatMessagesToOne: "
                + "illegal chat id " + chatId + " - doing nothing");
            return;
        }
        IWebClient client = cst;

        synchronized (messageList)
        {
            Iterator<ChatMessage> it = messageList.iterator();
            while (it.hasNext())
            {
                ChatMessage m = it.next();
                client.chatDeliver(m.getChatId(), m.getWhen(), m.getSender(),
                    m.getMessage(), true);
            }
            long now = new Date().getTime();
            client.chatDeliver(chatId, now, null, null, true);
        }
    }

    public void logout()
    {
        // Handled by WebServerSocketClientThread main loop;
        // only listed here to satisfy the interface.
    }

    public String registerUser(String username, String password, String email)
    {
        String reason = User.registerUser(username, password, email,
            mailObject);
        return reason;
    }

    public String confirmRegistration(String username, String confirmationCode)
    {
        String reason = User.confirmRegistration(username, confirmationCode);
        return reason;
    }

    public String changeProperties(String username, String oldPW,
        String newPW, String email, Boolean isAdminObj)
    {
        String reason = User.changeProperties(username, oldPW, newPW, email,
            isAdminObj);
        return reason;
    }

    // =========== internal workers ============

    private int countProposedGames(boolean shallBeScheduled)
    {
        int count = 0;
        for (GameInfo gi : proposedGames.values())
        {
            if (gi.isScheduledGame() == shallBeScheduled)
            {
                count++;
            }
        }
        // System.out.println("proposed for shallbe " + shallBeScheduled
        //     + " is: " + count);
        return count;
    }

    private GameInfo findByGameId(String gameId)
    {
        return proposedGames.get(gameId);
    }

    private GameInfo findFromRunningGames(String gameId)
    {
        GameInfo foundGi = null;
        for (GameInfo gi : runningGames)
        {
            if (gi.getGameId().equals(gameId))
            {
                foundGi = gi;
                break;
            }
        }
        return foundGi;
    }

    private IGameRunner getGameOnServer(GameInfo gi)
    {
        assert gi != null : "Cannot find GameOnServer for GameInfo that is null!";

        IGameRunner gr = gi.getGameRunner();
        if (gr == null)
        {
            LOGGER.severe("GameInfo with GameId returned null as GameRunner"
                + gi.getGameId());
        }
        return gr;
    }

    private boolean startOneGame(GameInfo gi)
    {
        boolean success = false;

        int port = portBookKeeper.getFreePort();
        if (port == -1)
        {
            LOGGER.log(Level.SEVERE, "No free ports!!");
            return false;
        }

        gi.setPort(port);
        LOGGER.log(Level.FINEST, "startOneGame, id " + gi.getGameId());

        RunGameInOwnJVM gr = new RunGameInOwnJVM(this, options, gi);
        gi.setGameRunner(gr);
        boolean ok = gr.makeRunningGame();

        if (!ok)
        {
            LOGGER.log(Level.WARNING, "makeRunningGame returned false?!?");
            return false;
        }
        else
        {
            // System.out.println("starting GameRunner thread");
            gr.start();
            LOGGER.log(Level.FINEST, "Returned from starter");

            updateGUI();
        }

        LOGGER.log(Level.FINEST, "port is " + port);
        success = true;
        return success;
    }

    /*
     * unregister a game from runningGames,
     * keep in endingGames until it's reaped
     */

    public void unregisterGame(GameInfo st, int port)
    {
        portBookKeeper.releasePort(port);
        //        System.out.println("runningGames: " + runningGames.toString());
        synchronized (runningGames)
        {
            LOGGER.log(Level.FINEST, "trying to remove...");
            if (runningGames.contains(st))
            {
                // System.out.println("removing");
                LOGGER.log(Level.FINEST, "removing...");
                runningGames.remove(st);
            }
            else
            {
                LOGGER.warning("runningGames does not contain game "
                    + st.getGameId());
            }
        }
        synchronized (endingGames)
        {
            endingGames.add(st);
        }
        st.setState(GameState.ENDING);
        allTellGameInfo(st);

        GameThreadReaper r = new GameThreadReaper();
        r.start();
        LOGGER.log(Level.FINEST, "GameThreadReaper started");

        updateGUI();

    }

    /*
     * unregister a game from runningGames,
     * keep in endingGames until it's reaped
     */

    public void unregisterGamePlayerPC(GameInfo gi)
    {
        if (gi == null)
        {
            LOGGER
                .warning("unregisterGamePlayerPC called with a null GameInfo object?");
            return;
        }

        synchronized (runningGames)
        {
            LOGGER.log(Level.FINEST, "trying to remove...");
            if (runningGames.contains(gi))
            {
                LOGGER.log(Level.FINEST, "removing...");
                runningGames.remove(gi);
            }
            else
            {
                LOGGER.warning("runningGames does not contain game "
                    + gi.getGameId());
            }
        }
        gi.setState(GameState.ENDING);
        allTellGameInfo(gi);

        updateGUI();
    }

    private void readGamesFromFile(String filename,
        HashMap<String, GameInfo> proposedGames)
    {
        try
        {
            File gamesFile = new File(filename);
            if (!gamesFile.exists())
            {
                LOGGER.warning("Games file " + filename
                    + " does not exist yet. I'll create an empty one now.");
                gamesFile.createNewFile();
            }
            else
            {
                LOGGER.info("Reading games from file " + filename);
            }
            BufferedReader games = new BufferedReader(new InputStreamReader(
                new FileInputStream(gamesFile)));

            String line = null;
            while ((line = games.readLine()) != null)
            {
                if (line.startsWith("#"))
                {
                    // ignore comment line
                }
                else if (line.matches("\\s*"))
                {
                    // ignore empty line
                }
                else
                {
                    // GameInfo.fromString expects the token[0]
                    // to be the command name:
                    String lineWithCmd = "Dummy" + sep + line;
                    String[] tokens = lineWithCmd.split(sep);
                    GameInfo.fromString(tokens, proposedGames);

                }
            }
            games.close();
            LOGGER.info("Restored " + proposedGames.size()
                + " games from file " + filename);
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Users file " + filename + " not found!",
                e);
            System.exit(1);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "IOException while reading users file "
                + filename + "!", e);
            System.exit(1);
        }
        proposedGamesListModified = false;
    }

    public void saveGamesIfNeeded()
    {
        if (proposedGamesListModified)
        {
            storeGamesToFile(proposedGamesFilename);
        }
    }

    private void storeGamesToFile(String filename)
    {
        if (filename == null)
        {
            LOGGER.log(Level.SEVERE, "filename must not be null, but it is!");
            throw new RuntimeException("gameFile filename is null!");
        }

        LOGGER.log(Level.FINE, "Storing scheduled games to file " + filename);

        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new FileOutputStream(filename));
            for (GameInfo gi : proposedGames.values())
            {
                String asString = gi.toString(sep);
                out.println(asString);
            }
            out.close();
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Writing games file " + filename
                + "failed: FileNotFoundException: ", e);
            throw new RuntimeException("FileNotFound exception while "
                + "creating/closing scheduled-Games file!");
        }
        proposedGamesListModified = false;
    }

    /**
     * A Null Object for the web server GUI interface.
     *
     * Avoids having to check for null everywhere.
     */
    private static final class NullWebServerGUI implements IWebServerGUI
    {
        public void setScheduledGamesInfo(String s)
        {
            // nothing
        }

        public void setEndingGamesInfo(String s)
        {
            // nothing
        }

        public void setInstantGamesInfo(String s)
        {
            // nothing
        }

        public void setRunningGamesInfo(String s)
        {
            // nothing
        }

        public void setUserInfo(String s)
        {
            // nothing
        }

        public void shutdown()
        {
            // nothing
        }
    }

    public String getStringOption(String key)
    {
        return this.options.getStringOption(key);
    }

    /**
     * Check whether any game executed in an own process has been
     * finished.
     *
     */

    class GameThreadReaper extends Thread
    {
        public GameThreadReaper()
        {
            // nothing to do
        }

        @Override
        public void run()
        {
            boolean didSomething = false;

            synchronized (endingGames)
            {
                if (!endingGames.isEmpty())
                {
                    didSomething = true;
                    Iterator<GameInfo> it = endingGames.iterator();
                    while (it.hasNext())
                    {
                        GameInfo gi = it.next();
                        IGameRunner gos = getGameOnServer(gi);
                        if (gos == null)
                        {
                            LOGGER.warning("No GameRunner found for GameInfo"
                                + " with id " + gi.getGameId()
                                + " to reap it's process.");
                        }
                        else if (gos instanceof RunGameInOwnJVM)
                        {
                            String name = ((RunGameInOwnJVM)gos).getName();
                            LOGGER.log(Level.FINE, "REAPER: wait for '" + name
                                + "' to end...");
                            try
                            {
                                ((RunGameInOwnJVM)gos).join();
                                LOGGER.log(Level.FINE, "        ok, ended...");
                            }
                            catch (InterruptedException e)
                            {
                                LOGGER.log(Level.WARNING,
                                    "Ups??? Caught exception ", e);
                            }
                        }
                        else
                        {
                            LOGGER.warning("GameThreadReaper can handle only "
                                + "GameRunners of type RunGameInOwnJVM, "
                                + "but we got something else!");
                        }
                        it.remove();
                    }

                    LOGGER.log(Level.INFO, "Reaper ended");
                }
                else
                {
                    // nothing to do
                }

                if (didSomething)
                {
                    updateGUI();
                }
            }

            LOGGER.log(Level.FINEST, "GameThreadReaper ending");
        }
    }

    private static class ChatMessage
    {
        String chatId;
        long when;
        String sender;
        String message;

        public ChatMessage(String chatId, long when, String sender,
            String message)
        {
            this.chatId = chatId;
            this.when = when;
            this.sender = sender;
            this.message = message;
        }

        public String getChatId()
        {
            return this.chatId;
        }

        public long getWhen()
        {
            return this.when;
        }

        public String getSender()
        {
            return this.sender;
        }

        public String getMessage()
        {
            return this.message;
        }
    }

    private void storeMessage(List<ChatMessage> list, ChatMessage msg)
    {
        synchronized (list)
        {
            list.add(msg);
            if (list.size() > WebServerConstants.keepLastNMessages)
            {
                // if longer than max, remove oldest one
                list.remove(0);
            }
        }
    }
}
