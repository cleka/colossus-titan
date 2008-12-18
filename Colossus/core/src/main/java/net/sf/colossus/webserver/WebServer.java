package net.sf.colossus.webserver;


import java.awt.GraphicsEnvironment;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IRunWebServer;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;


/** The main class for the WebServer
 *  - brings up the WebServer GUI
 *  - starts the ServerSocket and listens there for WebClients
 *  - based on actions coming from clients, keeps book of 
 *    "instant" and "running" games (both GameInfo objects),
 *    and tell the GameInfo objects when to start the game.
 *
 *  @version $Id$
 *  @author Clemens Katzer
 */

public class WebServer implements IWebServer, IRunWebServer
{
    private static final Logger LOGGER = Logger.getLogger(WebServer.class
        .getName());

    private WebServerOptions options = null;
    private PortBookKeeper portBookKeeper = null;
    private IWebServerGUI gui = null;

    /**
     * Controls whether the GUI is shown or not.
     *
     * At the moment this is configured only by the possibility of doing so -- if
     * the environment supports running a GUI, we will, if not, we won't. It could
     * be combined with a command line option to suppress the GUI even if it would
     * be possible to show one.
     */
    private final boolean runGUI = !GraphicsEnvironment.isHeadless();

    private boolean shutdownRequested = false;
    private boolean shutdownInitiatedByGUI = false;

    private final int maxClients;

    private ArrayList<GameInfo> scheduledGames = null;
    private ArrayList<GameInfo> instantGames = null;
    private ArrayList<GameInfo> runningGames = null;
    private ArrayList<GameInfo> endingGames = null;

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

        portBookKeeper = new PortBookKeeper(portRangeFrom, availablePorts);

        String usersFile = options
            .getStringOption(WebServerConstants.optUsersFile);
        int maxUsers = options.getIntOption(WebServerConstants.optMaxUsers);

        User.readUsersFromFile(usersFile, maxUsers);

        LOGGER.log(Level.ALL, "Server started: port " + port + ", maxClients "
            + maxClients);

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

        scheduledGames = new ArrayList<GameInfo>();
        instantGames = new ArrayList<GameInfo>();
        runningGames = new ArrayList<GameInfo>();
        endingGames = new ArrayList<GameInfo>();

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

        if (!shutdownInitiatedByGUI)
        {
            gui.cleanup();
            gui.dispose();
        }

        gui = null;
        options = null;
        portBookKeeper = null;

        LOGGER.log(Level.FINEST, "User Server after main loop.");
    }

    // called by WebServerGUI.closeWindow() event 
    // OR     by WebServerSocketThread.shutdownServer().
    // If the latter ( = admin user requested it remotely), need to close
    // also the GUI window -- if there is one.
    public void initiateShutdown(boolean byGUI)
    {
        shutdownInitiatedByGUI = byGUI;
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
        gui.setScheduledGamesInfo(scheduledGames.size()
            + " scheduled games stored");
        gui.setInstantGamesInfo(instantGames.size()
            + " instant games stored");
        gui.setRunningGamesInfo(runningGames.size() + " running games");
        gui.setEndingGamesInfo(endingGames.size() + " games just ending");
    }

    public GameInfo proposeGame(String initiator, String variant,
        String viewmode, long startAt, int duration, String summary,
        String expire, boolean unlimitedMulligans,
        boolean balancedTowers, int min, int target, int max)
    {
        System.out.println("\n=============\nWebServer.proposeGame, startAt = " + startAt);
        
        GameInfo gi = new GameInfo(initiator, variant, viewmode,
            startAt, duration, summary, expire,
            unlimitedMulligans, balancedTowers, min, target, max);

        if (startAt == -1)
        {
            // startAt -1 means: no starttime, i.e. instantly
            instantGames.add(gi);
        }
        else
        {
            scheduledGames.add(gi);
        }

        updateGUI();
        allTellGameInfo(gi);

        System.out.println("Webserver, game created: " + gi.toString());
        return gi;
    }

    public void reEnrollIfNecessary(WebServerClientSocketThread newCst)
    {
        IWebClient client = newCst;
        User newUser = newCst.getUser();

        Iterator<GameInfo> it = instantGames.iterator();
        while (it.hasNext())
        {
            GameInfo gi = it.next();
            if (gi.isEnrolled(newUser))
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

    public void tellAllScheduledGamesToOne(WebServerClientSocketThread cst)
    {
        tellAllGamesFromListToOne(cst, scheduledGames);
    }

    public void tellAllInstantGamesToOne(WebServerClientSocketThread cst)
    {
        tellAllGamesFromListToOne(cst, instantGames);
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

    public void gameFailed(GameInfo gi, String reason)
    {
        LOGGER.log(Level.WARNING, "GAME starting/running failed!!! Reason: "
            + reason);
    }

    public void tellEnrolledGameStartsSoon(GameInfo gi)
    {
        String gameId = gi.getGameId();

        ArrayList<User> players = gi.getPlayers();
        Iterator<User> it = players.iterator();

        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameStartsSoon(gameId);
        }
    }

    public void tellEnrolledGameStartsNow(GameInfo gi, int port)
    {
        String gameId = gi.getGameId();

        ArrayList<User> players = gi.getPlayers();
        Iterator<User> it = players.iterator();

        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameStartsNow(gameId, port);
        }
    }

    public void tellEnrolledGameStarted(GameInfo gi)
    {
        String gameId = gi.getGameId();

        ArrayList<User> players = gi.getPlayers();
        Iterator<User> it = players.iterator();

        while (it.hasNext())
        {
            User u = it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameStarted(gameId);
        }
    }

    // =========== Client actions ========== 

    public void enrollUserToGame(String gameId, String username)
    {
        GameInfo gi = findByGameId(gameId);
        User user = User.findUserByName(username);

        if (gi != null)
        {
            String reasonFail = gi.enroll(user);
            if (reasonFail == null)
            {
                allTellGameInfo(gi);
                IWebClient client = (IWebClient)user.getThread();
                client.didEnroll(gameId, user.getName());
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
        if (gi != null)
        {
            gi.setServerNull();
            gi.start(); // does nothing, just to get it GC'd and finalized

            Iterator<User> it = User.getLoggedInUsersIterator();
            while (it.hasNext())
            {
                User u = it.next();
                IWebClient client = (IWebClient)u.getThread();
                client.gameCancelled(gameId, byUser);
            }

            boolean scheduled = gi.isScheduledGame();
            if (scheduled)
            {
                scheduledGames.remove(gi);
            }
            else
            {
                instantGames.remove(gi);
            }
            updateGUI();
        }
    }

    public void startGame(String gameId)
    {
        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            boolean success = startOneGame(gi);

            LOGGER.log(Level.FINEST, "Found gi, got port " + port);
            if (!success)
            {
                LOGGER.log(Level.SEVERE, "\nstarting/running game " + gameId
                    + " failed!!\n");
            }
        }
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

    public String registerUser(String username, String password, String email,
        boolean isAdmin)
    {
        String reason = User.registerUser(username, password, email, isAdmin);
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

    private GameInfo findByGameId(String gameId)
    {
        for (GameInfo gi : instantGames)
        {
            if (gi.getGameId().equals(gameId))
            {
                return gi;
            }
        }
        for (GameInfo gi : scheduledGames)
        {
            if (gi.getGameId().equals(gameId))
            {
                return gi;
            }
        }

        return null;
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

        LOGGER.log(Level.FINEST, "startOneGame, id " + gi.getGameId());
        String workFilesBaseDir = getStringOption(WebServerConstants.optWorkFilesBaseDir);
        String template = getStringOption(WebServerConstants.optLogPropTemplate);
        String javaCommand = getStringOption(WebServerConstants.optJavaCommand);
        String colossusJar = getStringOption(WebServerConstants.optColossusJar);

        boolean ok = gi.makeRunningGame(this, workFilesBaseDir, template,
            javaCommand, colossusJar, port);

        if (!ok)
        {
            LOGGER.log(Level.WARNING, "makeRunningGame returned false?!?");
            return false;
        }
        else
        {
            gi.start();
            LOGGER.log(Level.FINEST, "Returned from starter");

            instantGames.remove(gi);
            runningGames.add(gi);

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

        synchronized (runningGames)
        {
            LOGGER.log(Level.FINEST, "trying to remove...");
            if (runningGames.contains(st))
            {
                LOGGER.log(Level.FINEST, "removing...");
                runningGames.remove(st);
            }
        }
        synchronized (endingGames)
        {
            endingGames.add(st);
        }
        st.setState(GameInfo.Ending);
        allTellGameInfo(st);

        GameThreadReaper r = new GameThreadReaper();
        r.start();
        LOGGER.log(Level.FINEST, "GameThreadReaper started");

        updateGUI();

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

        public void cleanup()
        {
            // nothing
        }

        public void dispose()
        {
            // nothing
        }
    }

    private static class WebServerOptions
    {
        private final Properties props = new Properties();
        private final String filename;

        public WebServerOptions(String filename)
        {
            this.filename = filename;
        }

        public void loadOptions()
        {
            try
            {
                FileInputStream in = new FileInputStream(filename);
                props.load(in);
            }
            catch (IOException e)
            {
                LOGGER.log(Level.SEVERE, "Couldn't read options from "
                    + filename, e);
                return;
            }
        }

        public void setOption(String optname, String value)
        {
            props.setProperty(optname, value);
        }

        public void setOption(String optname, boolean value)
        {
            setOption(optname, String.valueOf(value));
        }

        public void setOption(String optname, int value)
        {
            setOption(optname, String.valueOf(value));
        }

        public String getStringOption(String optname)
        {
            String value = props.getProperty(optname);
            return value;
        }

        public boolean getOption(String optname)
        {
            String value = getStringOption(optname);
            return (value != null && value.equals("true"));
        }

        /** Return -1 if the option's value has not been set. */
        public int getIntOption(String optname)
        {
            String buf = getStringOption(optname);
            int value = -1;
            try
            {
                value = Integer.parseInt(buf);
            }
            catch (NumberFormatException ex)
            {
                value = -1;
            }
            return value;
        }

        public int getIntOptionNoUndef(String optname)
        {
            int val = getIntOption(optname);
            if (val == -1)
            {
                LOGGER.log(Level.SEVERE, "Invalid or not set value for "
                    + optname + " from WebServer config file " + filename);
                System.exit(1);
            }
            return val;
        }

        public void removeOption(String optname)
        {
            props.remove(optname);
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
                        GameInfo game = it.next();
                        String name = game.getName();
                        LOGGER.log(Level.FINE, "REAPER: wait for '" + name
                            + "' to end...");
                        try
                        {
                            game.join();
                            LOGGER.log(Level.FINE, "        ok, ended...");
                        }
                        catch (InterruptedException e)
                        {
                            LOGGER.log(Level.WARNING,
                                "Ups??? Caught exception ", e);
                        }
                        it.remove();
                    }

                    LOGGER.log(Level.FINEST, "Reaper ended");
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
