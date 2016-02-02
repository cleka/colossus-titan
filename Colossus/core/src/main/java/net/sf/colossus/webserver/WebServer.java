package net.sf.colossus.webserver;


import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.colossus.util.BuildInfo;
import net.sf.colossus.webclient.WebClient;
import net.sf.colossus.webcommon.FormatWhen;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.GameInfo.GameState;
import net.sf.colossus.webcommon.IColossusMail;
import net.sf.colossus.webcommon.IGameRunner;
import net.sf.colossus.webcommon.IPortProvider;
import net.sf.colossus.webcommon.IRunWebServer;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;
import net.sf.colossus.webcommon.UserDB;


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

    private final static int MIN_FREE_GAME_PORTS = 5;

    private final int INACTIVITY_CHECK_INTERVAL = 10;
    private final int INACTIVITY_WARNING_INTERVAL = 30;
    private final int INACTIVITY_TIMEOUT = 90;

    private final static ArrayList<String> loginMessage = new ArrayList<String>();

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

    private UserDB userDB;
    private final HashMap<String, GameInfo> allGames = new HashMap<String, GameInfo>();

    private final ArrayList<GameInfo> proposedGames = new ArrayList<GameInfo>();
    private final ArrayList<GameInfo> runningGames = new ArrayList<GameInfo>();
    private final ArrayList<GameInfo> suspendedGames = new ArrayList<GameInfo>();
    private final ArrayList<GameInfo> endingGames = new ArrayList<GameInfo>();

    // Used also as separator for storing proposed games to file:
    private final static String sep = IWebServer.WebProtocolSeparator;

    /** Server port where we listen for WebClient connections */
    private final int serverPort;

    /** Server actual socket where we listen for WebClient connections */
    private ServerSocket serverSocket;

    private final ChatChannel generalChat;

    private final ClientWatchDog watchDog;

    private final FormatWhen whenFormatter;

    private PrintWriter dumpInfoFile;

    public static void main(String[] args)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        LOGGER.log(
            Level.INFO,
            "Start for ColossusWeb version '"
                + BuildInfo.getFullBuildInfoString() + "' at "
                + dateFormat.format(new Date()));

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

        this.whenFormatter = new FormatWhen();

        try
        {
            PrintWriter p = new PrintWriter(new FileOutputStream(
                "dump-info.txt", true));
            dumpInfoFile = p;
            dumpInfoFile.println("");
            dumpInfoFile.println("WebServer started.");
        }
        catch (IOException e)
        {
            LOGGER.warning("Can't create/append to dump-info.txt");
            dumpInfoFile = null;
        }

        this.serverPort = options
            .getIntOptionNoUndef(WebServerConstants.optServerPort);
        this.maxClients = options
            .getIntOptionNoUndef(WebServerConstants.optMaxClients);

        int portRangeFrom = options
            .getIntOptionNoUndef(WebServerConstants.optPortRangeFrom);
        int availablePorts = options
            .getIntOptionNoUndef(WebServerConstants.optAvailablePorts);
        if (availablePorts % 2 != 0)
        {
            LOGGER.warning("Suspicious option value " + availablePorts
                + " for available ports - should be an even (nr % 2 == 0) "
                + "value (every 2nd port is used as file serving port)!");
        }
        int availableGamePorts = availablePorts / 2;

        if (availableGamePorts < MIN_FREE_GAME_PORTS)
        {
            LOGGER
                .severe("Available ports from cf file is "
                    + availableGamePorts
                    + " but according to MIN_FREE_GAME_PORTS it should be at least "
                    + MIN_FREE_GAME_PORTS + "! Exiting.");
            System.exit(0);
        }
        mailObject = new ColossusMail(options);

        portBookKeeper = new PortBookKeeper(portRangeFrom, availablePorts);
        int freePorts = portBookKeeper.countFreePorts();

        LOGGER.info("Actually free ports: " + freePorts);

        if (freePorts < MIN_FREE_GAME_PORTS)
        {
            LOGGER.severe("Too few (only " + freePorts
                + ") free playing ports! Exiting.");
            System.exit(0);
        }

        if (freePorts < availableGamePorts)
        {
            LOGGER.warning("Only " + freePorts + " free ports, instead of "
                + availableGamePorts);
        }
        else if (freePorts < (availablePorts / 2))
        {
            LOGGER.severe("Not even half the amount of expected ports ("
                + freePorts + " of " + availablePorts + ") is free!");
            System.exit(0);
        }

        LOGGER.log(Level.INFO, "Server started: port " + serverPort
            + ", maxClients " + maxClients);

        doReadUsersFromFile();

        doReadGamesFromFile();

        doReadLoginMessage();

        this.generalChat = new ChatChannel(IWebServer.generalChatName,
            options, userDB);
        generalChat.createWelcomeMessage();

        if (runGUI)
        {
            this.gui = new WebServerGUI(this);
        }
        else
        {
            this.gui = new NullWebServerGUI();
        }

        watchDog = new ClientWatchDog(userDB);
        watchDog.start();

        updateGUI();
        /*
         boolean runGameConsole = false;
         if (runGameConsole)
         {
         console();
         }
         */

        LOGGER.log(Level.FINEST, "WebServer instantiated, maxClients = "
            + maxClients + " , port = " + serverPort);
    }

    /**
     *
     */
    private void doReadGamesFromFile()
    {
        proposedGamesFilename = options
            .getStringOption(WebServerConstants.optGamesFile);
        if (proposedGamesFilename == null)
        {
            proposedGamesFilename = WebServerConstants.DEFAULT_GAMES_FILE;
            LOGGER
                .warning("Filename for storing games not defined in cfg file!"
                    + " Using default " + proposedGamesFilename);
        }
        readGamesFromFile(proposedGamesFilename);
    }

    /**
     *
     */
    private void doReadUsersFromFile()
    {
        String usersFile = options
            .getStringOption(WebServerConstants.optUsersFile);
        int maxUsers = options.getIntOption(WebServerConstants.optMaxUsers);
        userDB = new UserDB(usersFile, maxUsers);
    }

    /**
     *
     */
    private void doReadLoginMessage()
    {
        String LoginMessageFilename = options
            .getStringOption(WebServerConstants.optLoginMessageFile);
        if (LoginMessageFilename != null)
        {
            readLoginMessageFromFile(LoginMessageFilename);
        }
    }

    /**
     * Triggered by remode admin connection
     */
    public void rereadLoginMessage()
    {
        doReadLoginMessage();
    }

    void runSocketServer()
    {
        int socketQueueLen = options
            .getIntOptionNoUndef(WebServerConstants.optSocketQueueLen);

        LOGGER.log(Level.FINE, "About to create web server socket on port "
            + serverPort);
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
                serverSocket = null;
            }
            serverSocket = new ServerSocket(serverPort, socketQueueLen);
            serverSocket.setReuseAddress(true);
        }
        catch (IOException ex)
        {

            LOGGER.log(Level.SEVERE, "Could not create socket on port "
                + serverPort + ": " + ex.getMessage());
            System.exit(1);
        }

        LOGGER.log(Level.INFO, "User-server started, waiting for clients");

        while (!shutdownRequested)
        {
            boolean rejected = waitForUser();
            if (rejected)
            {
                LOGGER.log(Level.WARNING, "accepted one client but "
                    + "rejected it - maxClients limit reached.");
            }
        }

        writeBackUsers();
        userDB.cleanup();

        generalChat.dispose();

        gui.shutdown();
        gui = null;
        options = null;
        portBookKeeper = null;

        LOGGER.log(Level.FINE, "Web Server after main loop.");
    }

    public ChatChannel getGeneralChat()
    {
        return this.generalChat;
    }

    public void writeBackUsers()
    {
        userDB.storeUsersToFile();
    }

    public void updateLoggedinStatus(User u, WebServerClient wsc)
    {
        userDB.updateLoggedinStatus(u, wsc);
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
        watchDog.shutdown();
        closeAllWebServerClientSocketThreads();
        makeDummyConnection();
    }

    public void makeDummyConnection()
    {
        // make a dummy connection, to get the thread out of the
        // accept().
        try
        {
            Socket socket = new Socket("localhost", serverPort);
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
            LOGGER.log(Level.FINEST, "Got client connection from IP: "
                + clientSocket.getInetAddress().toString());

            if (shutdownRequested)
            {
                serverSocket.close();
                return false;
            }
            else if (userDB.getLoggedInCount() >= maxClients)
            {
                rejected = true;
                reject(clientSocket);
            }
            else
            {
                // ok, can log in
            }

            if (!rejected)
            {
                WebServerClient client = new WebServerClient(this,
                    clientSocket);
                client.startThread();
                updateUserCounts();
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
                LOGGER.log(
                    Level.FINEST,
                    "ShutdownRequested, closing caused an exception: "
                        + ex.toString());

            }
            return false;
        }

        catch (Throwable any)
        {
            LOGGER.log(Level.SEVERE,
                "!!! WebServer waitForUser loop caught throwable: ", any);
        }

        return rejected;
    }

    private void reject(Socket socket)
    {
        try
        {
            PrintWriter rejectedClientWriter = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream())), true);

            rejectedClientWriter.println(IWebClient.tooManyUsers);
            rejectedClientWriter.println(IWebClient.connectionClosed);
            // give client some time to process the response
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                /* ignore */
            }
            socket.close();
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING,
                "Rejecting a user did throw exception: ", ex);
        }
    }

    private void closeAllWebServerClientSocketThreads()
    {
        Collection<User> users = userDB.getLoggedInUsers();
        for (User u : users)
        {
            u.updateLastLogout();

            // TODO: should this really deal directly with the actual
            // WebServerClientSocketThread, or indirectly via the
            // WebServerClient instead?

            WebServerClient wsc = (WebServerClient)u.getWebserverClient();
            WebServerClientSocketThread cst = wsc.getWSCSThread();

            if (cst == null)
            {
                LOGGER.log(Level.FINE,
                    "Thread for user is empty - skipping interrupt and join.");
                continue;
            }

            cst.tellToTerminate();

            LOGGER.log(Level.FINEST,
                "WebServer.closeAllWebServerClientSocketThreads: before join");
            try
            {
                cst.join();
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

    public UserDB getUserDB()
    {
        return userDB;
    }

    public User findUserByName(String name)
    {
        return userDB.findUserByName(name);
    }

    public String verifyLogin(String username, String password)
    {
        return userDB.verifyLogin(username, password);
    }

    public PortBookKeeper getPortBookKeeper()
    {
        return this.portBookKeeper;
    }

    public IPortProvider getPortProvider()
    {
        return this.portBookKeeper;
    }

    public void updateGUI()
    {
        if (gui == null)
        {
            // skip it if called too early or already going down...
            return;
        }
        gui.setScheduledGamesInfo(countProposedGames(true)
            + " scheduled games stored");
        gui.setInstantGamesInfo(countProposedGames(false)
            + " instant games stored");
        gui.setRunningGamesInfo(runningGames.size() + " running games");
        gui.setEndingGamesInfo(endingGames.size() + " games just ending");
        gui.setSuspendedGamesInfo(suspendedGames.size() + " games suspended");
        gui.setUsedPortsInfo(portBookKeeper.calculateUsedPorts()
            + " ports in use");
    }

    public void watchGame(String gameId, String userName)
    {
        LOGGER.info("Got request from client " + userName + " to watch game "
            + gameId);
        GameInfo gi = findFromRunningGames(gameId);
        if (gi != null && gi.isRunning())
        {
            String host = gi.getHostingHost();
            int port = gi.getPort();
            LOGGER.info("Sending connect info: host=" + host + ", port="
                + port);
            sendWatchGameInfo(userName, gameId, host, port);
        }
        else
        {
            LOGGER.warning("No running game with id " + gameId + " found!");
        }
    }

    public void sendWatchGameInfo(String userName, String gameId, String host,
        int port)
    {
        IWebClient client = null;
        String reasonFail = null;

        User user = userDB.findUserByName(userName);
        if (user != null)
        {
            client = user.getWebserverClient();
            if (client != null)
            {
                client.watchGameInfo(gameId, host, port);
            }
            else
            {
                reasonFail = "User " + userName + " is not online";
            }
        }
        else
        {
            reasonFail = "Unknown user '" + userName + "'";
        }

        if (reasonFail != null)
        {
            LOGGER.warning("Sending watchGameInfo for game " + gameId
                + " to user " + userName + " failed: " + reasonFail);
        }
    }

    /**
     * Dump server state info into a static file, at the moment dump-info.txt
     */
    public void dumpInfo()
    {
        if (dumpInfoFile != null)
        {
            dumpInfo(dumpInfoFile);
            dumpInfoFile.flush();
        }
        else
        {
            LOGGER.warning("Can't dump info because dumpInfoFile is null!");
        }
    }

    /**
     * Dump info into given PrintWriter
     * @param pw PrintWriter for the file to print dump to
     */
    public void dumpInfo(PrintWriter pw)
    {
        long now = new Date().getTime();

        String nowString = whenFormatter.timeAndDateAsString(now);
        pw.println("Dump at " + nowString);

        StringBuilder ul = new StringBuilder();
        Collection<User> users = userDB.getLoggedInUsers();
        for (User u : users)
        {
            String name;
            if (u != null)
            {
                name = u.getName();
            }
            else
            {
                name = "<null user?>";
            }
            if (ul.length() > 0)
            {
                ul.append(", ");
            }
            ul.append(name);
        }

        pw.println(users.size() + " users connected: " + ul.toString());

        pw.println("All games:");
        for (GameInfo gi : allGames.values())
        {
            pw.println("  #" + gi.getGameId() + ", state "
                + gi.getStateString());
        }

        ArrayList<String> igList = new ArrayList<String>();
        ArrayList<String> sgList = new ArrayList<String>();
        for (GameInfo gi : proposedGames)
        {
            (gi.isScheduledGame() ? sgList : igList).add(gi.getGameId());
        }

        pw.println(sgList.size() + " proposed scheduled games stored: "
            + sgList.toString());
        pw.println(igList.size() + " proposed instant games stored:"
            + igList.toString());

        ArrayList<String> temporaryGameList = new ArrayList<String>();
        for (GameInfo gi : runningGames)
        {
            temporaryGameList.add(gi.getGameId());
        }
        pw.println(runningGames.size() + " running games: "
            + temporaryGameList.toString());

        temporaryGameList.clear();
        for (GameInfo gi : suspendedGames)
        {
            temporaryGameList.add(gi.getGameId());
        }
        pw.println(suspendedGames.size() + " suspended games: "
            + temporaryGameList.toString());

        temporaryGameList.clear();
        for (GameInfo gi : endingGames)
        {
            temporaryGameList.add(gi.getGameId());
        }
        pw.println(endingGames.size() + " games just ending: "
            + temporaryGameList.toString());
        pw.println(portBookKeeper.getStatus());
        pw.println("");
        pw.println("");
    }

    private GameInfo isInvolvedInInstantGame(String initiatorName)
    {
        ArrayList<GameInfo> games = new ArrayList<GameInfo>(allGames.values());
        for (GameInfo gi : games)
        {
            if (!gi.isScheduledGame()
                && (gi.isEnrolled(initiatorName) || gi.getInitiator().equals(
                    initiatorName))
                // This relies on that that the client side does not allow
                // proposing if one is actually still playing; but allow it
                // here, because we can't distinct still playing from being
                // dead, and thus one could never propose as long as the other
                // game is still ongoing.
                // TODO: fix when webserver has better knowledge of game
                // state, i.e. which players are alive in which game...
                && !gi.getGameState().equals(GameState.RUNNING)
                && !gi.getGameState().equals(GameState.SUSPENDED)
                && !gi.getGameState().equals(GameState.DELETED))
            {
                return gi;
            }
        }
        return null;
    }

    public GameInfo proposeGame(String initiator, String variant,
        String viewmode, long startAt, int duration, String summary,
        String expire, List<String> gameOptions, List<String> teleportOptions,
        int min, int target, int max)
    {
        if (GameInfo.wouldBeInstantGame(startAt))
        {
            GameInfo involvedGame = isInvolvedInInstantGame(initiator);
            if (involvedGame != null)
            {
                LOGGER.warning("User " + initiator
                    + " proposes instant game, "
                    + "but user is already involved in instant game "
                    + involvedGame.getGameId() + "!");
                // no game created from proposal
                return null;
            }
            else
            {
                LOGGER.info("User " + initiator + " proposed instant game "
                    + "- not involved in game yet, so that's ok!");
            }
        }

        GameInfo gi = new GameInfo(initiator, variant, viewmode, startAt,
            duration, summary, expire, gameOptions, teleportOptions, min,
            target, max);

        String scheduleType = gi.isScheduledGame() ? "scheduled" : "instant";
        LOGGER.info("Game " + gi.getGameId() + " (" + scheduleType
            + ") was proposed by " + initiator + ". Adding to list.");

        allGames.put(gi.getGameId(), gi);
        proposedGames.add(gi);
        proposedGamesListModified = true;

        updateGUI();
        allTellGameInfo(gi);
        systemMessageToAll("Game " + gi.getGameId() + " (" + scheduleType
            + ") was proposed by " + initiator + ": " + gi.getVariant() + ", "
            + gi.getViewmode());
        return gi;
    }

    public void cancelIfNecessary(User user)
    {
        LOGGER.fine("Checking if any cancelling is needed for user "
            + user.getName());

        ArrayList<GameInfo> games = new ArrayList<GameInfo>(allGames.values());
        for (GameInfo gi : games)
        {
            if (gi.getInitiator().equals(user.getName())
                && !gi.isScheduledGame()
                && !gi.getGameState().equals(GameState.RUNNING)
                && !gi.getGameState().equals(GameState.SUSPENDED)
                && !gi.getGameState().equals(GameState.ENDING))

            {
                LOGGER.info("Auto-cancelling instant game " + gi.getGameId()
                    + ", state=" + gi.getGameState().toString()
                    + " because initiator " + user.getName()
                    + " is going to be gone...");
                cancelGame(gi.getGameId(), user.getName());
            }
        }
        // if players connection broke unexpectedly, due to the exception the
        // saveGameIsNeeded at the end of parseLine will not be done
        saveGamesIfNeeded();
    }

    public void reEnrollIfNecessary(WebServerClient newclient)
    {
        IWebClient client = newclient;
        User newUser = newclient.getUser();

        ArrayList<GameInfo> games = new ArrayList<GameInfo>(allGames.values());
        for (GameInfo gi : games)
        {
            if (gi.reEnrollIfNecessary(newUser) && gi.isProposedOrDue())
            {
                LOGGER.log(Level.FINEST, "Telling user " + newUser.getName()
                    + " that he is still enrolled in game " + gi.getGameId());
                // userMap finds already new user for that name
                LOGGER.fine("Player " + newUser.getName()
                    + " re-enrolled to game " + gi.getGameId());
                client.didEnroll(gi.getGameId(), newUser.getName());
                allTellGameInfo(gi);
            }
        }
    }

    public void tellAllGamesFromListToOne(WebServerClient client,
        ArrayList<GameInfo> games)
    {
        Iterator<GameInfo> it = games.iterator();
        while (it.hasNext())
        {
            GameInfo gi = it.next();
            client.gameInfo(gi);
        }
    }

    public void tellAllProposedGamesToOne(WebServerClient client)
    {
        tellAllGamesFromListToOne(client, proposedGames);
    }

    public void tellAllRunningGamesToOne(WebServerClient client)
    {
        tellAllGamesFromListToOne(client, runningGames);
    }

    public void tellAllSuspendedGamesToOne(WebServerClient client)
    {
        tellAllGamesFromListToOne(client, suspendedGames);
    }

    public void allTellGameInfo(GameInfo gi)
    {
        Collection<User> users = userDB.getLoggedInUsers();
        for (User u : users)
        {
            IWebClient client = u.getWebserverClient();
            if (client != null)
            {
                client.gameInfo(gi);
            }
        }
    }

    public void tellEnrolledGameStartsSoon(GameInfo gi)
    {
        String gameId = gi.getGameId();

        ArrayList<User> players = gi.getPlayers();
        Iterator<User> it = players.iterator();

        // should have been set, but who knows...
        String byUserName = "unknown";
        User byUser = gi.getStartingUser();
        if (byUser != null)
        {
            byUserName = byUser.getName();
        }

        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = u.getWebserverClient();
            if (client != null)
            {
                LOGGER.finest("Sending gameStartsSoon to client for user "
                    + u.getName());
                client.gameInfo(gi);
                client.gameStartsSoon(gameId, byUserName);
                ((WebServerClient)client).requestPingNow();
            }
            else
            {
                LOGGER.warning("getThread for user " + u.getName()
                    + " (of game " + gi.getGameId()
                    + ") returned null client!");
            }
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
            IWebClient client = u.getWebserverClient();
            if (client != null)
            {
                client.gameInfo(gi);
                LOGGER.info("sending gamestartsnow to client " + u.getName());
                client.gameStartsNow(gameId, port, host,
                    INACTIVITY_CHECK_INTERVAL, INACTIVITY_WARNING_INTERVAL,
                    INACTIVITY_TIMEOUT);
                ((WebServerClient)client).requestPingNow();
            }
            else
            {
                LOGGER.warning("getThread for user " + u.getName()
                    + " (of game " + gi.getGameId()
                    + ") returned null client!");
            }
        }
    }

    public void gameStarted(GameInfo gi)
    {
        gi.setState(GameState.RUNNING);
        proposedGames.remove(gi);
        suspendedGames.remove(gi);
        runningGames.add(gi);
        proposedGamesListModified = true;
        updateGUI();
        allTellGameInfo(gi);
    }

    public void gameFailed(GameInfo gi, String reason)
    {
        LOGGER.log(Level.WARNING, "GAME starting/running failed!!! Reason: "
            + reason);
    }

    // =========== Client actions ==========

    public void enrollUserToGame(String gameId, String username)
    {
        User user = userDB.findUserByName(username);
        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            synchronized (gi)
            {
                if (!gi.isStarting())
                {
                    String reasonFail = gi.enroll(user);
                    proposedGamesListModified = true;
                    if (reasonFail == null)
                    {
                        updateOnline(gi);
                        allTellGameInfo(gi);
                        IWebClient client = user.getWebserverClient();
                        LOGGER.fine("Player " + username
                            + " enrolled to game " + gameId);
                        client.didEnroll(gameId, user.getName());
                    }
                    else
                    {
                        LOGGER.info("Player " + username
                            + " failed to enroll to game " + gameId
                            + ", reason=" + reasonFail);
                        long when = 0;
                        IWebClient webClient = user.getWebserverClient();
                        if (webClient != null)
                        {
                            webClient.deliverGeneralMessage(when, false,
                                "Can't enroll!", reasonFail);
                        }
                    }
                }
                else
                {
                    LOGGER.warning("Player " + username
                        + " tried to enroll to game " + gameId
                        + " but game is already starting!");
                    IWebClient webClient = user.getWebserverClient();
                    if (webClient != null)
                    {
                        String message = "Enrolling to " + gi.getGameId()
                            + " failed, game is already starting.";
                        long when = 0;
                        webClient.deliverGeneralMessage(when, false,
                            "Can't enroll!", message);
                    }
                }
            }
        }
    }

    public void unenrollUserFromGame(String gameId, String username)
    {
        GameInfo gi = findByGameId(gameId);
        User user = userDB.findUserByName(username);

        if (gi != null)
        {
            synchronized (gi)
            {
                // TODO HACK!! don't check for now, otherwise can't unenroll
                // from games that failed to start.
                boolean preventUnenroll = false;
                if (gi.isStarting() && preventUnenroll)
                {
                    LOGGER.warning("Player " + username
                        + " tried to unenroll from game " + gameId
                        + ", but it is already starting.");
                    IWebClient webClient = user.getWebserverClient();
                    if (webClient != null)
                    {
                        String message = "Unenrolling from " + gi.getGameId()
                            + " failed, game is already starting.";
                        long when = 0;
                        webClient.deliverGeneralMessage(when, false,
                            "Can't unenroll!", message);
                    }
                }
                else
                {
                    String reasonFail = gi.unenroll(user);
                    proposedGamesListModified = true;
                    if (reasonFail == null)
                    {
                        updateOnline(gi);
                        allTellGameInfo(gi);
                        IWebClient client = user.getWebserverClient();
                        LOGGER.fine("Player " + username
                            + " unenrolled from game " + gameId);
                        client.didUnenroll(gameId, user.getName());
                    }
                }
            }
        }
    }

    public void cancelGame(String gameId, String byUser)
    {
        LOGGER.info("User " + byUser + " requests to cancel game " + gameId);
        GameInfo gi = findByGameId(gameId);

        if (gi == null)
        {
            LOGGER.info("Attempt to cancel game with id " + gameId
                + " but no GameInfo found for that id.");
            return;
        }

        if (gi.wasAlreadyStarted())
        {
            LOGGER.warning("Attempt to cancel game " + gameId
                + " but it is/was already starting! Ignoring that attempt.");
            return;
        }

        if (gi.getPort() != -1)
        {
            portBookKeeper.releasePort(gi);
        }

        IGameRunner gr = gi.getGameRunner();
        if (gr != null)
        {
            LOGGER.info("For Cancel: game " + gameId
                + " has already GameRunner, not touching it.");
        }
        else
        {
            LOGGER.info("For Cancel: no GameRunner for GameInfo with gameId "
                + gameId);
        }

        Collection<User> users = userDB.getLoggedInUsers();
        for (User u : users)
        {
            IWebClient client = u.getWebserverClient();
            if (client != null)
            {
                client.gameCancelled(gameId, byUser);
            }
        }

        allGames.remove(gi.getGameId());
        proposedGames.remove(gi);
        proposedGamesListModified = true;
        updateGUI();
    }

    public void startGame(String gameId, User byUser)
    {
        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            synchronized (gi)
            {
                attemptStartOnServer(gi, byUser);
            }
        }
        else
        {
            LOGGER.warning("Did not find a GameInfo for gameId " + gameId
                + " to start it on the server!");
        }

    }

    public void resumeGame(String gameId, String loadGame, User byUser)
    {
        LOGGER.info("User " + byUser.getName() + " wants to resume game "
            + gameId + ", from file " + loadGame);
        GameInfo gi = findFromSuspendedGames(gameId);
        if (gi != null)
        {
            gi.setResumeFromFilename(loadGame);
            synchronized (gi)
            {
                attemptStartOnServer(gi, byUser);
            }
        }
        else
        {
            LOGGER.warning("Did not find a GameInfo for gameId " + gameId
                + " to resume it on the server!");
        }
    }

    public void deleteSuspendedGame(String gameId, User user)
    {
        LOGGER.info("User " + user.getName()
            + " wants to delete suspended game " + gameId);
        GameInfo gi = findFromSuspendedGames(gameId);
        {
            if (gi != null)
            {
                gi.setState(GameState.DELETED);
                allGames.remove(gi.getGameId());
                proposedGames.remove(gi);
                suspendedGames.remove(gi);
                proposedGamesListModified = true;
                allTellGameInfo(gi);
                updateGUI();
            }
        }
    }

    private void attemptStartOnServer(GameInfo gi, User byUser)
    {
        // if (!gi.allEnrolledOnline())
        // {
        //    LOGGER.warning("User " + byUser.getName()
        //        + " requested to start game " + gi.getGameId()
        //        + ", but not all enrolled players are online.");
        //    String reason = "Not all enrolled players online!";
        //    informAllEnrolledThatStartFailed(gi, reason, byUser);
        //}
        // else if (!gi.isStartable())
        if (!gi.isStartable())
        {
            LOGGER.warning("User " + byUser.getName() + " attempted to start"
                + " game " + gi.getGameId() + ", but it is already running or"
                + " start attempt in progress (state=" + gi.getGameState()
                + ")!");
        }
        else
        {
            gi.markStarting(byUser);
            allTellGameInfo(gi);
            String reason = startOneGame(gi);
            proposedGamesListModified = true;
            if (reason == null)
            {
                LOGGER.log(Level.FINE, "Found gi, got port " + gi.getPort());
            }
            else
            {
                LOGGER.warning("starting/running game " + gi.getGameId()
                    + " failed!! Reason: " + reason);
                informAllEnrolledThatStartFailed(gi, reason, byUser);
                gi.cancelStarting();
            }
        }
    }

    /**
     *  A game was started by a WebClient user locally on his computer
     *  and is ready to accept the other players as remote client;
     *  so we notify them and tell them host and port to where to connect.
     *
     *  AT THE MOMENT THIS FUNCTIONALITY IS NOT IN USE AT ALL!
     */
    public void startGameOnPlayerHost(String gameId, String hostingPlayer,
        String playerHost, int port)
    {
        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            ArrayList<User> users = gi.getPlayers();
            for (User u : users)
            {
                LOGGER.info("Informing player " + u.getName()
                    + " that game starts at host of hosting player "
                    + hostingPlayer);
                IWebClient webClient = u.getWebserverClient();
                webClient.gameStartsNow(gameId, port, playerHost,
                    INACTIVITY_CHECK_INTERVAL, INACTIVITY_WARNING_INTERVAL,
                    INACTIVITY_TIMEOUT);
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
            gameStarted(gi);
        }
        else
        {
            LOGGER.severe("Got request informGameStarted but did not find "
                + "any game for gameId " + gameId);
        }
        proposedGamesListModified = true;
    }

    public void informAllEnrolledAbout(GameInfo gi, String message)
    {
        long when = new Date().getTime();
        ArrayList<User> users = gi.getPlayers();
        for (User u : users)
        {
            IWebClient webClient = u.getWebserverClient();
            if (webClient == null)
            {
                LOGGER.warning("Skip informing player " + u.getName()
                    + " (webclient null) about: " + message);
            }
            else
            {
                webClient.systemMessage(when, message);
            }
        }
    }

    public void systemMessageToAll(String message)
    {
        long when = new Date().getTime();
        Collection<User> users = userDB.getLoggedInUsers();
        for (User u : users)
        {
            IWebClient client = u.getWebserverClient();
            if (client == null)
            {
                LOGGER.warning("Skip sending systemMessage to player "
                    + u.getName() + " (webclient null): " + message);
            }
            else
            {
                client.systemMessage(when, message);
            }
        }
    }

    public void informAllEnrolledThatStartFailed(GameInfo gi, String reason,
        User byUser)
    {
        ArrayList<User> users = gi.getPlayers();
        String message = "Starting game with gameId " + gi.getGameId()
            + " (initiated by player " + byUser.getName()
            + ") failed. Reason: " + reason;

        for (User u : users)
        {
            IWebClient webClient = u.getWebserverClient();
            if (webClient == null)
            {
                LOGGER.warning("Skip informing player " + u.getName()
                    + " (webclient null): " + message);
            }
            else if (webClient.getClientVersion() >= WebClient.WC_VERSION_GENERAL_MESSAGE)
            {
                LOGGER
                    .info("Informing player " + u.getName() + ": " + message);

                // for the starting user it's an error, others just info
                boolean error = u.getName().equals(byUser.getName());
                // when == 0: do not show a specific time
                long when = 0;
                webClient.deliverGeneralMessage(when, error,
                    "Game start failed!", message);
            }
            else
            {
                LOGGER.warning("Skip informing player " + u.getName()
                    + " (too old webclient): " + message);
            }
        }
    }

    public void requestUserAttention(long when, String sender,
        boolean isAdmin, String recipient, String message, int beepCount,
        long beepInterval, boolean windows)
    {
        IWebClient recipientClient = null;
        String reasonFail = null;

        User user = userDB.findUserByName(recipient);
        if (user != null)
        {
            recipientClient = user.getWebserverClient();
            if (recipientClient != null)
            {
                recipientClient.requestAttention(when, sender, isAdmin,
                    message, beepCount, beepInterval, windows);
                informPingDone(sender, recipient, message);
            }
            else
            {
                reasonFail = "User " + recipient + " is not online";
            }
        }
        else
        {
            reasonFail = "Unknown user '" + recipient + "'";
        }

        if (reasonFail != null)
        {
            informPingFailed(sender, reasonFail, message);
        }
    }

    private void informPingDone(String sender, String recipient, String message)
    {
        if ("SYSTEM".equals(sender))
        {
            return;
        }
        User senderUser = userDB.findUserByName(sender);
        IWebClient senderWebClient = senderUser.getWebserverClient();
        String[] lines = new String[] { "You /ping'ed to " + recipient + ": "
            + message };
        ChatChannel gc = getGeneralChat();
        if (senderWebClient != null)
        {
            gc.sendLinesToClient(gc.getChannelId(), senderWebClient,
                Arrays.asList(lines), false, "");
        }
        else
        {
            LOGGER.warning("requestUserAttention to " + recipient
                + " done ok, but could not find client for sender " + sender
                + " to send confirmation message!");
            LOGGER.info("Failed message was: " + message);
        }
    }

    private void informPingFailed(String sender, String reasonFail,
        String message)
    {
        User senderUser = userDB.findUserByName(sender);
        IWebClient senderWebClient = senderUser.getWebserverClient();
        String[] lines = new String[] {
            "Sorry, your ping request failed, reason: " + reasonFail,
            "Your text was: " + message };
        ChatChannel gc = getGeneralChat();
        if (senderWebClient != null)
        {
            gc.sendLinesToClient(gc.getChannelId(), senderWebClient,
                Arrays.asList(lines), true, "");
        }
        else
        {
            LOGGER.warning("requestUserAttention failed (" + reasonFail
                + ") but could not find client"
                + " to send error message to sender either!");
        }
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
        int connected = userDB.getLoggedInCount();
        allTellUserCounts();
        gui.setUserInfo(connected + " users connected.");
        ArrayList<GameInfo> games = new ArrayList<GameInfo>(allGames.values());
        for (GameInfo gi : games)
        {
            // returns true if changed
            if (updateOnline(gi))
            {
                allTellGameInfo(gi);
            }
        }
    }

    public void allTellUserCounts()
    {
        if (userDB.getLoggedInCount() > 0)
        {
            int loggedin = userDB.getLoggedInCount();

            // the other five are still dummies.
            int enrolled = userDB.getEnrolledCount();
            int playing = userDB.getPlayingCount();
            int dead = userDB.getDeadCount();
            long ago = 0;

            StringBuffer text = new StringBuffer("");
            Collection<User> users = userDB.getLoggedInUsers();
            for (User u : users)
            {
                if (text.length() != 0)
                {
                    text.append(", ");
                }
                text.append(u.getName());
            }
            for (User u : users)
            {
                IWebClient client = u.getWebserverClient();
                if (client != null)
                {
                    client.userInfo(loggedin, enrolled, playing, dead, ago,
                        text.toString());
                }
            }
        }
    }

    public void chatSubmit(String chatId, String sender, String message)
    {
        generalChat.createStoreAndDeliverMessage(sender, message);
    }

    public void handlePingQuotedName(String sender, String pingCommand)
    {
        long when = new Date().getTime();
        boolean isAdmin = userDB.findUserByName(sender).isAdmin();
        //   /ping "
        //   01234567
        String args = pingCommand.substring(7);
        // split at the closing quote, eat up trailing spaces in name and
        // leading spaces of the message
        String[] tokens = args.split(" *\" *", 2);
        if (tokens.length != 2)
        {
            LOGGER.warning("invalid pingCommand with quotes '" + pingCommand
                + "' from user " + sender + "!");
            String reasonFail = "Invalid /ping syntax. Use: /ping \"RECIPIENT NAME\" [optionally some message]";
            informPingFailed(sender, reasonFail, pingCommand);
        }
        else
        {
            String recipient = tokens[0];
            String message = "<no message specified by sender>";
            String msg = tokens[1];
            if (msg != null && !msg.matches(" *"))
            {
                message = msg;
            }
            requestUserAttention(when, sender, isAdmin, recipient, message, 3,
                500, true);
        }
    }

    public void handlePing(String sender, String pingCommand)
    {
        long when = new Date().getTime();
        boolean isAdmin = userDB.findUserByName(sender).isAdmin();
        String[] tokens = pingCommand.split(" +", 3);
        if (tokens.length < 2)
        {
            // Just  /ping   : inform about the usage:
            String reasonFail = "Invalid /ping syntax. Use: /ping RECIPIENT [optionally some message]";
            informPingFailed(sender, reasonFail, pingCommand);
        }
        else
        {
            String recipient = tokens[1];
            String message = "<no message specified by sender>";
            if (tokens.length >= 3)
            {
                message = tokens[2];
            }
            requestUserAttention(when, sender, isAdmin, recipient, message, 3,
                500, true);
        }
    }

    public void tellLastChatMessagesToOne(WebServerClient client, String chatId)
    {
        if (!chatId.equals(IWebServer.generalChatName))
        {
            LOGGER.log(Level.WARNING, "tellLastChatMessagesToOne: "
                + "illegal chat id " + chatId + " - doing nothing");
            return;
        }

        generalChat.tellLastMessagesToOne(client);
    }

    public void sendMessageOfTheDayToOne(WebServerClient client, String chatId)
    {
        if (!chatId.equals(IWebServer.generalChatName))
        {
            LOGGER.log(Level.WARNING, "sendMessageOfTheDayToOne: "
                + "illegal chat id " + chatId + " - doing nothing");
            return;
        }

        generalChat.deliverMessageOfTheDayToClient(chatId, client,
            loginMessage);
    }

    public void sendOldVersionWarningToOne(WebServerClient client,
        String userName, String chatId)
    {
        if (!chatId.equals(IWebServer.generalChatName))
        {
            LOGGER.log(Level.WARNING, "sendMessageOfTheDayToOne: "
                + "illegal chat id " + chatId + " - doing nothing");
            return;
        }
        generalChat.deliverOldVersionWarning(chatId, userName, client);
    }

    private void readLoginMessageFromFile(String filename)
    {
        try
        {
            File loginMessageFile = new File(filename);
            if (!loginMessageFile.exists())
            {
                return;
            }
            else
            {
                LOGGER.info("Reading login message from file " + filename);
            }

            ArrayList<String> temp = new ArrayList<String>();
            BufferedReader loginMessagesReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(loginMessageFile),
                    WebServerConstants.charset));

            String line = null;
            while ((line = loginMessagesReader.readLine()) != null)
            {
                temp.add(line);
            }
            loginMessagesReader.close();

            loginMessage.clear();
            loginMessage.addAll(temp);

            LOGGER.info("Read " + loginMessage.size() + " lines from file "
                + filename);
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Login message file " + filename
                + " not found!", e);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE,
                "IOException while reading login message file " + filename
                    + "!", e);
        }

    }

    public void logout()
    {
        // Handled by WebServerSocketClientThread main loop;
        // only listed here to satisfy the interface.
    }

    public void messageToAdmin(long when, String fromUser, String fromMail,
        List<String> message)
    {
        generalChat.writeMessageToAdminToChatlog(when, fromUser, fromMail, message);
        mailObject.sendMessageToAdminMail(when, fromUser, fromMail, message);
    }

    public String registerUser(String username, String password, String email)
    {
        String reason = userDB.registerUser(username, password, email,
            mailObject);
        return reason;
    }

    public String confirmRegistration(String username, String confirmationCode)
    {
        String reason = userDB.confirmRegistration(username, confirmationCode);
        return reason;
    }

    public String changeProperties(String username, String oldPW,
        String newPW, String email, Boolean isAdminObj)
    {
        String reason = userDB.changeProperties(username, oldPW, newPW, email,
            isAdminObj);
        return reason;
    }

    // =========== internal workers ============

    /**
     * When a user logged in or out, this is called for every GameInfo to update
     * how many of the enrolled players are currently online.
     * @param userDB TODO
     *
     * @return true if the count of online users was changed i.e. GameInfo
     * needs to be updated to all clients
     */
    boolean updateOnline(GameInfo gi)
    {
        int found = 0;
        for (User u : gi.getPlayers())
        {
            if (userDB.isUserOnline(u))
            {
                found++;
            }
        }
        // TODO in reEnrollIfNecessary case this is now wrong??
        // perhaps because in the moment of update user is just not online...

        return gi.updateOnlineCount(found);
    }

    private int countProposedGames(boolean shallBeScheduled)
    {
        int count = 0;
        for (GameInfo gi : proposedGames)
        {
            if (gi.isScheduledGame() == shallBeScheduled)
            {
                count++;
            }
        }
        return count;
    }

    private GameInfo findByGameId(String gameId)
    {
        return allGames.get(gameId);
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

    private GameInfo findFromSuspendedGames(String gameId)
    {
        GameInfo foundGi = null;
        for (GameInfo gi : suspendedGames)
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
            LOGGER.severe("GameInfo with GameId " + gi.getGameId()
                + " returned null as GameRunner");
        }
        return gr;
    }

    private String startOneGame(GameInfo gi)
    {
        // Reason for failure
        String reason = null;

        RunGameInOwnJVM gr = new RunGameInOwnJVM(this, options, gi);
        boolean ok = gr.makeRunningGame();

        if (!ok)
        {
            reason = gr.getReasonStartFailed();
            return reason;
        }
        else
        {
            gr.tryToStart();
            LOGGER.fine("Returned from starter for game " + gi.getGameId());

            updateGUI();
        }

        LOGGER.fine("Successfully started game " + gi.getGameId()
            + " on port " + gi.getPort());

        // failureReason == null means success
        return reason;
    }

    /**
     * unregister a game from runningGames (or proposedGames),
     * and keep in endingGames until it's reaped
     */
    public void unregisterGame(GameInfo gi, int port)
    {
        synchronized (allGames)
        {
            LOGGER.log(Level.FINEST, "unregister: trying to remove...");
            if (runningGames.contains(gi))
            {
                LOGGER.log(Level.FINEST, "Removing game " + gi.getGameId()
                    + " from running games list");
                runningGames.remove(gi);
                if (gi.getGameState().equals(GameState.SUSPENDED))
                {
                    synchronized (suspendedGames)
                    {
                        LOGGER.log(Level.FINEST,
                            "Adding game " + gi.getGameId()
                                + " to suspended games list");
                        suspendedGames.add(gi);
                    }
                }
                else
                {
                    gi.setState(GameState.ENDING);
                }
            }
            else if (suspendedGames.contains(gi))
            {
                LOGGER.log(Level.FINEST,
                    "Reaper: first removing game " + gi.getGameId()
                        + " from suspended list");
                suspendedGames.remove(gi);
                if (gi.getGameState().equals(GameState.SUSPENDED))
                {
                    LOGGER
                        .log(Level.FINEST,
                            "When reaping, still/again suspended; adding game "
                                + gi.getGameId()
                                + " back to suspended games list");
                    suspendedGames.add(gi);
                }
                else
                {
                    gi.setState(GameState.ENDING);
                }
            }

            // If game starting did not succeed might still be in proposed list
            else if (proposedGames.contains(gi))
            {
                LOGGER.log(Level.FINEST, "Removing game " + gi.getGameId()
                    + " from proposed games list");
                proposedGames.remove(gi);
                proposedGamesListModified = true;
            }
            else
            {
                LOGGER.warning("Neither proposed, running nor suspended games"
                    + " list contains game " + gi.getGameId());
            }
        }

        synchronized (endingGames)
        {
            LOGGER.log(Level.FINEST, "Putting game " + gi.getGameId()
                + " to ending games list");
            endingGames.add(gi);

        }

        proposedGamesListModified = true;
        saveGamesIfNeeded();
        allTellGameInfo(gi);

        boolean suspended = gi.getGameState().equals(GameState.SUSPENDED);
        GameThreadReaper r = new GameThreadReaper();
        r.start();

        LOGGER.finest("GameThreadReaper started for"
                + (suspended ? " suspended" : " ending ") + " game "
                + gi.getGameId());

        updateGUI();
    }

    /**
    * unregister a game (run on player's PC) from runningGames,
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
            // TODO: also add the "if start failed check proposed games list, too" here?

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

    private void readGamesFromFile(String filename)
    {
        int maximumFileId = getMaximumGameIdFromFiles();
        GameInfo.setNextFreeGameId(maximumFileId + 1);

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
                new FileInputStream(gamesFile), WebServerConstants.charset));

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
                    GameInfo gi = GameInfo.fromString(tokens, allGames, true);
                    if (gi.getGameState().equals(GameState.PROPOSED))
                    {
                        proposedGames.add(gi);
                    }
                    else if (gi.getGameState().equals(GameState.SUSPENDED))
                    {
                        suspendedGames.add(gi);
                    }
                    else
                    {
                        LOGGER.warning("restored game " + gi.getGameId()
                            + " is " + gi.getGameState()
                            + ", should be proposed or suspended?");
                    }
                }
            }
            games.close();
            LOGGER.info("Restored " + allGames.size() + " games from file "
                + filename);
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Games file " + filename + " not found!",
                e);
            System.exit(1);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "IOException while reading games file "
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

        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                filename), WebServerConstants.charset));

            ArrayList<GameInfo> games = new ArrayList<GameInfo>(
                allGames.values());
            for (GameInfo gi : games)
            {
                if (gi.relevantForSaving())
                {
                    String asString = gi.toString(sep);
                    out.println(asString);
                }
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

        public void setSuspendedGamesInfo(String s)
        {
            // nothing
        }

        public void setUsedPortsInfo(String s)
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
            LOGGER.finest("GameThreadReaper started running.");
            synchronized (endingGames)
            {
                didSomething = handleGamesFromList(endingGames, "ending games");
            }

            if (didSomething)
            {
                updateGUI();
            }

            LOGGER.log(Level.FINEST, "GameThreadReaper run() ends");
        }
    }

    private boolean handleGamesFromList(List<GameInfo> list, String listName)
    {
        boolean didSomething = false;
        if (!list.isEmpty())
        {
            LOGGER.finest("There are " + list.size() + " games in " + listName
                + " list.");
            didSomething = true;
            Iterator<GameInfo> it = list.iterator();
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
                        LOGGER.log(
                            Level.FINE,
                            "        ok, ended... releasing port "
                                + gi.getPort());
                        portBookKeeper.releasePort(gi);
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.log(Level.WARNING, "Ups??? Caught exception ",
                            e);
                    }
                }
                else
                {
                    LOGGER.warning("GameThreadReaper can handle only "
                        + "GameRunners of type RunGameInOwnJVM, "
                        + "but we got something else!");
                }
                if (!gi.getGameState().equals(GameState.SUSPENDED))
                {
                    LOGGER.finest("Removing ending game " + gi.getGameId()
                        + " from allGames.");
                    allGames.remove(gi.getGameId());
                }
                else
                {
                    LOGGER.finest("NOT removing suspended game "
                        + gi.getGameId() + " from allGames.");

                }
                it.remove();
            }
            LOGGER.log(Level.INFO, "Reaper loop for " + listName
                + "list ended");
        }
        else
        {
            LOGGER.finest("List for " + listName + " is empty, nothing to do");
            // nothing to do
        }

        return didSomething;
    }

    /**
     * Searches the game directory tree for highest game number for which
     * a game directory had been earlier created.
     * Tree is expected to have groups per each 100 games; example:
     *
     * base
     * base/nn00-nn99
     * base/nn00-nn99/nn00
     * base/nn00-nn99/nn04
     * base/nn00-nn99/nn98
     * base/mm00-mm99/mm12
     * base/mm00-mm99/mm87
     *
     * Eventually, when we reach gameId 10000+, groupdirs will have the form
     * base/kkk00-kkk99
     * base/kkk00-kkk99/kkk02
     * base/kkk00-kkk99/kkk87
     *
     * Example, if last created game dir was 6789  ( "base/6700-6799/6789" ),
     * this returns 6789.
     *
     * @return The highest game number for which a directory already exists,
     * (otherwise 0 if therre is no dir at all)
     */
    private int getMaximumGameIdFromFiles()
    {
        // Server will create next as maxId + 1
        int maxId = 0;

        String workFilesBaseDir = options
            .getStringOption(WebServerConstants.optWorkFilesBaseDir);
        File baseDir = new File(workFilesBaseDir);
        if (baseDir.isDirectory())
        {
            String maxGroup = null;
            int maxNumber = -1;

            Pattern p = Pattern.compile("(\\d+)-\\d+");

            String[] dirNames = baseDir.list();
            if (dirNames != null && dirNames.length > 0)
            {
                for (int grp = 0; grp < dirNames.length; grp++)
                {
                    String dirName = dirNames[grp];
                    Matcher m = p.matcher(dirName);
                    if (m.matches())
                    {
                        String firstNumber = m.group(1);
                        int number = Integer.parseInt(firstNumber);
                        if (number > maxNumber)
                        {
                            maxNumber = number;
                            maxGroup = dirName;
                        }
                    }
                }

                if (maxGroup != null)
                {
                    File groupDir = new File(baseDir, maxGroup);
                    if (groupDir.isDirectory())
                    {
                        String[] names = groupDir.list();
                        for (int i = 0; i < names.length; i++)
                        {
                            String name = names[i];
                            try
                            {
                                int number = Integer.parseInt(name);
                                if (number > maxId)
                                {
                                    maxId = number;
                                }
                            }
                            catch (NumberFormatException e)
                            {
                                // Ignore non-number filenames
                            }
                        }
                    }
                    else
                    {
                        LOGGER.severe("Group '" + groupDir
                            + "' is not a directory?!?");
                    }
                }
            }
        }

        return maxId;
    }
}
