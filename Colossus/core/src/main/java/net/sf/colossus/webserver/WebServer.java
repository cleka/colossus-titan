package net.sf.colossus.webserver;


import java.awt.GraphicsEnvironment;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
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
 *    "potential" and "running" games (both GameInfo objects),
 *    and tell the GameInfo objects when to start the game.
 *
 *  @version $Id$
 *  @author Clemens Katzer
 */


public class WebServer implements IWebServer, IRunWebServer
{

    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

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
    private boolean runGUI = !GraphicsEnvironment.isHeadless();

    private boolean shutdownRequested = false;
    private boolean shutdownInitiatedByGUI = false;

    private int numClients = 0;
    private int maxClients;

    private ArrayList potentialGames = null;
    private ArrayList runningGames = null;
    private ArrayList endingGames = null;

    // Server socket port where we listen for WebClient connections
    private int port;

    private ServerSocket serverSocket;
    private List activeSocketList =
        Collections.synchronizedList(new ArrayList());

    private List lastNChatMessages = new ArrayList();

    public static void main(String[] args)
    {
        String optionsFileName = WebServerConstants.defaultOptionsFilename;

        if (args.length > 0)
        {
            optionsFileName = args[0];
        }

        WebServer server = new WebServer(optionsFileName);

        // System.out.println("before init socket");
        server.initSocketServer();

        // execution comes to here only when server is shut down
        // (shutDownRequested set to true, so that the loop quits)
        server = null;

        try { Thread.sleep(1000);
        }
        catch (InterruptedException e) {/* ignore */
        }
        System.gc();
        System.runFinalization();
        System.out.println("WebServer.main() will end...");

        // JVM should do a clean exit now, no System.exit() needed.
        // @TODO: does it work on all platforms, all Java versions?
        //        If not, build here a demon timer that does the
        //        System.exit() after a few seconds...?        
    }

    public WebServer(String optionsFile)
    {
        this.options = new WebServerOptions(optionsFile);
        options.loadOptions();

        this.port = options.getIntOptionNoUndef(WebServerConstants.optServerPort);
        this.maxClients = options.getIntOptionNoUndef(WebServerConstants.optMaxClients);

        int portRangeFrom = options.getIntOptionNoUndef(WebServerConstants.optPortRangeFrom);
        int availablePorts = options.getIntOptionNoUndef(WebServerConstants.optAvailablePorts);

        portBookKeeper = new PortBookKeeper(portRangeFrom, availablePorts);

        String usersFile = options.getStringOption(WebServerConstants.optUsersFile);
        int maxUsers = options.getIntOption(WebServerConstants.optMaxUsers);

        User.readUsersFromFile(usersFile, maxUsers);

        System.out.println("OK: port " + port + ", maxClients " + maxClients);

        long now = new Date().getTime();
        ChatMessage startMsg = new ChatMessage(IWebServer.generalChatName, now,
            "SYSTEM", "WebServer started. Welcome!!");
        storeMessage(lastNChatMessages, startMsg);

        if (runGUI)
        {
            this.gui = new WebServerGUI(this);
        }
        else
        {
            this.gui = new NullWebServerGUI();
        }

        potentialGames = new ArrayList();
        runningGames = new ArrayList();
        endingGames = new ArrayList();

        /*
         boolean runGameConsole = false;
         if (runGameConsole)
         {
         console();
         }
         */

        LOGGER.log(Level.FINEST, "WebServer instantiated, maxClients = " +
            maxClients + " , port = " + port);
    }

    void initSocketServer()
    {
        numClients = 0;

        int socketQueueLen = options.getIntOptionNoUndef(WebServerConstants.optSocketQueueLen);

        LOGGER.log(Level.FINEST,
            "About to create server socket on port " + port);
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
            LOGGER.log(Level.FINEST, "Could not create socket. " +
                "Configure networking in OS.", ex);
            System.exit(1);
        }

        LOGGER.log(Level.FINEST,
            "\nUser-server starting up, waiting for clients");

        while (!shutdownRequested)
        {
            boolean rejected = waitForUser();
            if (rejected)
            {
                // System.out.println("accepted one client but rejected " + 
                //        "it - maxClients limit reached.");
            }
            else
            {
                // System.out.println("added one client");
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
        catch(Exception e)
        {
            System.out.println("initiateShutdown - got exception " + e.toString());
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
            System.out.println("WebServer.initiateShutdown: got exception " +
                e.toString());
        }
    }
    
    private boolean waitForUser()
    {
        boolean rejected = false;
        Socket clientSocket = null;
        try
        {
            clientSocket = serverSocket.accept();
            LOGGER.log(Level.FINEST, "Got client connection from " +
                clientSocket.getInetAddress().toString());
            LOGGER.log(Level.FINEST,
                "Got client connection from IP: " +
                clientSocket.getInetAddress().toString());

            if (shutdownRequested)
            {
                serverSocket.close();
                return false;
            }
            else if (numClients >= maxClients)
            {
                rejected = true;
                WebServerClientSocketThread.reject(clientSocket);
            }
            else
            {
                synchronized (activeSocketList)
                {
                    activeSocketList.add(clientSocket);
                    numClients++;
                }
                gui.setUserInfo(numClients + " users connected.");
            }
        }
        catch (IOException ex)
        {
            if (shutdownRequested)
            {
                LOGGER.log(Level.SEVERE,
                    "Waiting for user did throw exception: " +
                    ex.toString());
            }
            else
            {
                // does not matter
                LOGGER.log(Level.FINEST,
                    "ShutdownRequested, closing caused an exception: " +
                    ex.toString());

            }
            return false;
        }

        if (!rejected)
        {
            WebServerClientSocketThread cst = new WebServerClientSocketThread(this,
                clientSocket);
            cst.start();
        }

        return rejected;
    }

    private void closeAllWscst()
    {
        ArrayList toDo = new ArrayList();

        // copy them first to own list. 
        // Otherwise we get a ConcurrentModificationException
        Iterator it2 = User.getLoggedInUsersIterator();
        while (it2.hasNext())
        {
            User u = (User)it2.next();
            toDo.add(u);
        }

        Iterator it = toDo.iterator();
        while (it.hasNext())
        {
            User u = (User)it.next();

            WebServerClientSocketThread thread = (WebServerClientSocketThread)u.getThread();
            try
            {
                thread.interrupt();
            }
            // It's funny. It seems the interrupt above always gives a null pointer
            // exception, but the interrupting has done it's job anyway...
            catch (NullPointerException e)
            {
                System.out.println("all right, the familiar NPE here...");
            }
            catch (Exception e)
            {
                System.out.println("Different exception than usual while tried to interrupt 'other': " +
                    e.toString());
            }

            // System.out.println("WebServer.closeAllWscst's: before join");
            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                System.out.println("thread.join() interrupted?? Well...");
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
        gui.setPotentialGamesInfo(potentialGames.size() +
            " potential games stored");
        gui.setRunningGamesInfo(runningGames.size() + " running games");
        gui.setEndingGamesInfo(endingGames.size() + " games just ending");
    }

    public GameInfo proposeGame(String initiator, String variant,
        String viewmode, String expire,
        boolean unlimitedMulligans, boolean balancedTowers,
        int min, int target, int max)
    {
        GameInfo gi = new GameInfo(initiator, variant, viewmode, expire,
            unlimitedMulligans, balancedTowers, min, target, max);
        potentialGames.add(gi);

        updateGUI();
        allTellGameInfo(gi);

        return gi;
    }

    public void tellAllPotentialGamesToOne(WebServerClientSocketThread cst)
    {
        IWebClient client = cst;

        Iterator it = potentialGames.iterator();
        while (it.hasNext())
        {
            GameInfo gi = (GameInfo)it.next();
            client.gameInfo(gi);
        }
    }

    public void reEnrollIfNecessary(WebServerClientSocketThread newCst)
    {
        IWebClient client = newCst;
        User newUser = newCst.getUser();

        Iterator it = potentialGames.iterator();
        while (it.hasNext())
        {
            GameInfo gi = (GameInfo)it.next();
            if (gi.isEnrolled(newUser))
            {
                // System.out.println("\n++++\nTelling user " + newUser.getName() + 
                //        " that he is still enrolled in game " + gi.getGameId());
                // userMap finds already new user for that name
                client.didEnroll(gi.getGameId(),
                    newUser.getName());
            }
        }
    }

    public void tellAllRunningGamesToOne(WebServerClientSocketThread cst)
    {
        IWebClient client = cst;

        Iterator it = runningGames.iterator();
        while (it.hasNext())
        {
            GameInfo gi = (GameInfo)it.next();
            client.gameInfo(gi);
        }
    }

    public void allTellGameInfo(GameInfo gi)
    {
        Iterator it = User.getLoggedInUsersIterator();
        while (it.hasNext())
        {
            User u = (User)it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameInfo(gi);
        }
    }

    public void gameFailed(GameInfo gi, String reason)
    {
        System.out.println("GAME starting/running failed!!! Reason: " + reason);
    }

    public void tellEnrolledGameStartsSoon(GameInfo gi)
    {
        String gameId = gi.getGameId();

        ArrayList players = gi.getPlayers();
        Iterator it = players.iterator();

        while (it.hasNext())
        {
            User u = (User)it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameStartsSoon(gameId);
        }
    }

    public void tellEnrolledGameStartsNow(GameInfo gi, int port)
    {
        String gameId = gi.getGameId();

        ArrayList players = gi.getPlayers();
        Iterator it = players.iterator();

        while (it.hasNext())
        {
            User u = (User)it.next();
            IWebClient client = (IWebClient)u.getThread();
            client.gameStartsNow(gameId, port);
        }
    }

    public void tellEnrolledGameStarted(GameInfo gi)
    {
        String gameId = gi.getGameId();

        ArrayList players = gi.getPlayers();
        Iterator it = players.iterator();

        while (it.hasNext())
        {
            User u = (User)it.next();
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
            gi.start();   // does nothing, just to get it GC'd and finalized

            Iterator it = User.getLoggedInUsersIterator();
            while (it.hasNext())
            {
                User u = (User)it.next();
                IWebClient client = (IWebClient)u.getThread();
                client.gameCancelled(gameId, byUser);
            }

            potentialGames.remove(gi);
            updateGUI();
        }
    }

    public void startGame(String gameId)
    {
        GameInfo gi = findByGameId(gameId);
        if (gi != null)
        {
            boolean success = startOneGame(gi);

            // System.out.println("Found gi, got port " + port);
            if ( !success )
            {
                System.out.println("\n#####\nWebServer, fatal error: " +
                    "starting/running game " + gameId + " failed!!\n#####\n");
            }
        }
    }

    public void chatSubmit(String chatId, String sender, String message)
    {
        if (chatId.equals(IWebServer.generalChatName))
        {
            Iterator it = User.getLoggedInUsersIterator();
            while (it.hasNext())
            {
                User u = (User)it.next();
                IWebClient client = (IWebClient)u.getThread();
                long now = new Date().getTime();
                ChatMessage msg = new ChatMessage(chatId, now, sender, message);
                storeMessage(lastNChatMessages, msg);
                client.chatDeliver(chatId, now, sender, message, false);
            }
        }
        else
        {
            System.out.println("Chat for chatId " + chatId +
                " not implemented.");
        }
    }

    public void tellLastChatMessagesToOne(WebServerClientSocketThread cst,
        String chatId)
    {
        List messageList;
        if (chatId.equals(IWebServer.generalChatName))
        {
            messageList = lastNChatMessages;
        }
        else
        {
            System.out.println("tellLastChatMessagesToOne: illegal chat id " +
                chatId + " - doing nothing");
            return;
        }
        IWebClient client = cst;

        synchronized (messageList)
        {
            Iterator it = messageList.iterator();
            while (it.hasNext())
            {
                ChatMessage m = (ChatMessage)it.next();
                client.chatDeliver(m.getChatId(), m.getWhen(),
                    m.getSender(), m.getMessage(), true);
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

    public String registerUser(String username, String password, String email, boolean isAdmin)
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

    public void withdrawFromServer(Socket socket)
    {
        synchronized (activeSocketList)
        {
            activeSocketList.remove(activeSocketList.indexOf(socket));
            numClients--;
        }

        gui.setUserInfo(numClients + " users connected.");

        LOGGER.log(Level.FINEST, "User client withdraws from server...");
    }

    // =========== internal workers ============

    private GameInfo findByGameId(String gameId)
    {
        GameInfo gi_found = null;

        Iterator it = potentialGames.iterator();
        while (it.hasNext())
        {
            GameInfo gi = (GameInfo)it.next();
            if (gi.getGameId().equals(gameId))
            {
                gi_found = gi;
            }
        }

        return gi_found;
    }

    private boolean startOneGame(GameInfo gi)
    {
        boolean success = false;

        int port = portBookKeeper.getFreePort();
        if (port == -1)
        {
            System.out.println("No free ports!!");
            return false;
        }

        // System.out.println("Running a game...");
        String workFilesBaseDir = getStringOption(WebServerConstants.optWorkFilesBaseDir);
        String template = getStringOption(WebServerConstants.optLogPropTemplate);
        String javaCommand = getStringOption(WebServerConstants.optJavaCommand);
        String colossusJar = getStringOption(WebServerConstants.optColossusJar);

        boolean ok = gi.makeRunningGame(this, workFilesBaseDir, template,
            javaCommand, colossusJar, port);

        if (!ok )
        {
            System.out.println("makeRunningGame returned false?!?");
            return false;
        }
        else
        {
            gi.start();
            // System.out.println("Returned from starter");

            potentialGames.remove(gi);
            runningGames.add(gi);

            updateGUI();
        }

        // System.out.println("port is " + port);
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
            // System.out.println("trying to remove...");
            if (runningGames.contains(st))
            {
                // System.out.println("removing...");
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
        // System.out.println("GameThreadReaper started");

        updateGUI();

    }

    // #####################################################################

    /*  
     // was used during development, commandline control    
     private void console()
     {
     String currentLine;
     
     try
     {
     System.out.print("> ");
     
     BufferedReader in = new BufferedReader(  
     new InputStreamReader(System.in));
     
     boolean done = false;
     
     while (! done)
     {
     if ( in.ready() )
     {
     currentLine = in.readLine ();
     // System.out.println ("GOT: '" + currentLine + "'");
     if (currentLine.equals("run"))
     {
     System.out.println("run command not in use...");
     // startGame(2, 0);
     
     }
     
     else if (currentLine.equals("quit") || currentLine.equals("q"))
     {
     done = true;
     System.exit(0);
     }
     else if (currentLine.equals(""))
     {
     // do nothing...                 
     }
     else if (currentLine.equals("status"))
     {
     status();
     }
     else
     {
     System.out.println("???");
     }
     
     System.out.print("> ");
     }
     
     else
     {

     Thread.sleep(10);
     }

     boolean didSomething = gameReaper();
     if (didSomething)
     {
     System.out.println();
     System.out.print("> ");
     }
     }
     
     }
     catch(Exception e)
     {
     System.out.println("Caught exception: " + e.toString());
     }
     
     System.out.println("After loop - console exiting.");
     
     }
     */

    /*  
     // was used during development, commandline control
     public void status()
     {
     int cnt = Thread.activeCount();
     System.out.println("Active count: " + cnt);
     
     ThreadGroup tg = Thread.currentThread().getThreadGroup();
     System.out.println(tg.toString());
     
     Thread list[] = new Thread[tg.activeCount()];
     tg.enumerate(list);
     
     for (int i = 0; i < list.length; i++) {
     Thread t = (Thread) list[i];
     System.out.println("Thread: " + t.getName());
     }
     
     System.out.println();
     System.out.println("> ");
     }
     */

    /*    
     // used during development from commandline control
     public void startGame(int players, int ais)
     {
     System.out.println("Running a game...");
     RunningGame runningGame = new RunningGame(this, players, ais);
     if (runningGame == null )
     {
     System.out.println("new STarter(...) returned null ?!?");
     }
     else if (runningGame.getPort() == -1)
     {
     System.out.println("new RunningGame failed: port == -1 !!");
     if (runningGame.isAlive())
     {
     System.out.println("still alive ?");
     }
     else
     {
     System.out.println("thread has ended... fine.");
     }
     }
     else
     {
     runningGame.start();    
     System.out.println("Returned from starter");
     runningGames.add(runningGame);
     }

     }
     */


    /**
     * A Null Object for the web server GUI interface.
     *
     * Avoids having to check for null everywhere.
     */
    private static final class NullWebServerGUI implements IWebServerGUI
    {
        public void setEndingGamesInfo(String s)
        {
            // nothing
        }

        public void setPotentialGamesInfo(String s)
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


    class WebServerOptions
    {
        private Properties props = new Properties();
        private String filename;

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
                System.out.println("Couldn't read options from " + filename);
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
                System.out.println("Invalid or not set value for " + optname +
                    " from WebServer config file " + filename);
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

        public void run()
        {
            boolean didSomething = false;

            synchronized (endingGames)
            {
                if (!endingGames.isEmpty())
                {
                    didSomething = true;
                    Iterator it = endingGames.iterator();
                    while (it.hasNext())
                    {
                        GameInfo game = (GameInfo)it.next();
                        String name = game.getName();
                        System.out.println("REAPER: wait for '" + name +
                            "' to end...");
                        try
                        {
                            game.join();
                            System.out.println("        ok, ended...");
                        }
                        catch (InterruptedException e)
                        {
                            System.out.println("Ups??? Catched exception " +
                                e.toString());
                        }
                        it.remove();
                    }

                    // System.out.println("Reaper ended");
                }
                else
                {
                    System.out.println("Reaper: nothing to do...");
                }

                if (didSomething)
                {
                    updateGUI();
                }
            }

            // System.out.println("GameThreadReaper ending");
        }

        public void finalize()
        {
            // System.out.println("finalize(): " + this.getClass().getName());
        }
    }


    private class ChatMessage
    {
        String chatId;
        long when;
        String sender;
        String message;

        public ChatMessage(String chatId, long when, String sender, String message)
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

        public void finalize()
        {
            // System.out.println("finalize(): " + this.getClass().getName());
        }
    }

    private void storeMessage(List list, ChatMessage msg)
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

