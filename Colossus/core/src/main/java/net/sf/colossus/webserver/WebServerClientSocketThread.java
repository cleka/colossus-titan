package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;


/**
 *  Thread to handle one user client connection at the webserver
 *  Reads always one line from the socket, parses it and
 *  executes the corresponding action.
 *  This class also provides the methods which the server calls
 *  on the client (=translates method calls to sending them as text
 *  messages over the socket).
 *
 *  @author Clemens Katzer
 */
public class WebServerClientSocketThread extends Thread implements IWebClient
{
    private static final Logger LOGGER = Logger
        .getLogger(WebServerClientSocketThread.class.getName());

    private WebServer server;
    private Socket socket;
    private InputStream is;
    private BufferedReader in;
    private PrintWriter out;
    private User user = null;
    private int clientVersion = 0;
    private long lastPacketReceived = 0;
    private int pingsTried = 0;
    private int idleWarningsSent = 0;

    private boolean loggedIn = false;

    private final static String sep = IWebServer.WebProtocolSeparator;

    private Thread stopper = null;

    private static final long PING_REQUEST_INTERVAL_SECONDS = 60;
    private static final int PING_MAX_TRIES = 3;

    private static final int IDLE_WARNING_INTERVAL_MINUTES = 10;
    private static final int IDLE_WARNING_MAXCOUNT = 12;


    /* During registration request and sending of confirmation code,
     * we do not have a user yet. The parseLine sets then this variable
     * according to the username argument which was send from client.
     */
    private String unverifiedUsername = null;


    public WebServerClientSocketThread(WebServer server, Socket socket)
    {
        super("WebServerClientSocketThread");
        this.server = server;
        this.socket = socket;
    }

    public static void reject(Socket socket)
    {
        try
        {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())), true);

            out.println(tooManyUsers);
            out.println(connectionClosed);
            // give client some time to process the response
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {/* ignore */
            }
            socket.close();
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING,
                "Rejecting a user did throw exception: ", ex);
        }
    }

    User getUser()
    {
        return this.user;
    }

    private String getUsername()
    {
        if (user != null)
        {
            return user.getName();
        }
        else
        {
            return "<username undefined?>";
        }
    }

    public int getClientVersion()
    {
        return clientVersion;
    }

    public void requestPingIfNeeded(long now)
    {
        long deltaMillis = now - lastPacketReceived;

        if (deltaMillis >= (pingsTried + 1) * PING_REQUEST_INTERVAL_SECONDS
            * 1000)
        {
            // Only clients >= 2 have this feature
            if (getClientVersion() >= 2)
            {
                // too many already done without response => assume dead
                if (pingsTried >= PING_MAX_TRIES)
                {
                    LOGGER.info("After " + pingsTried
                        + " pings, still no response from client "
                        + getUsername()
                        + " - assuming connection lost and closing it.");
                    String message = "@@@ Hello " + getUsername() + ", after "
                        + pingsTried
                        + " ping requests, still no response from your "
                        + "WebClientclient - assuming connection problems "
                        + "and closing connection from server side. @@@";
                    chatDeliver(IWebServer.generalChatName, now, "SYSTEM",
                        message, false);
                    if (!done)
                    {
                        // this requestPingIfNeeded code is run in the WatchDog
                        // thread; the interrupt interrupts the actual
                        // WebServerClientThread.
                        this.interrupt();
                        // prevent from checking the maxIdleMinutes stuff...
                        return;
                    }
                    else
                    {
                        LOGGER
                            .info("done already true, let's skip the interrupting...");
                    }
                }
                // otherwise, send another one
                else
                {
                    LOGGER.fine("Client " + getUsername()
                        + ": no activity for " + (deltaMillis / 1000f)
                        + " seconds; " + "requesting "
                        + (pingsTried + 1)
                        + ". ping!");
                    requestPing("dummy1", "dummy2", "dummy3");
                    pingsTried++;
                }
            }
        }
        else if (deltaMillis >= pingsTried * PING_REQUEST_INTERVAL_SECONDS
            * 1000)
        {
            // not time for next ping, but still no response
        }
        else
        {
            // idle time < previous request time: got something
            pingsTried = 0;
        }
        return;
    }

    /**
     * Currently this will log out only older clients, because they do not
     * respond to the ping packets.
     * TODO in future, distinct between ping packets and all other
     * activities, and log out user which hasn't done anything and left
     * WebClient standing around idle for very long.
     * @param now
     */
    public void checkMaxIdleTime(long now)
    {
        if (done)
        {
            // already gone, probably because we just logged him out because
            // of too many missing ping requests.
            return;
        }
        long deltaMillis = now - lastPacketReceived;
        if (user != null && loggedIn)
        {
            LOGGER.finest("Checking maxIdleTime of client " + user.getName()
                + ": " + (deltaMillis / 1000) + " seconds");
        }
        else
        {
            LOGGER.info("When trying to check maxIdleTime of client, "
                + "user null or not logged in ?!? ...");
            return;
        }

        long idleSeconds = deltaMillis / 1000;
        int idleMinutes = (int)(idleSeconds / 60);

        if (idleWarningsSent >= IDLE_WARNING_MAXCOUNT)
        {
            LOGGER.info("Client " + getUsername() + " has been idle "
                + idleMinutes + " minutes - logging him out!");
            String message = "@@@ Hello " + getUsername() + ", you have been "
                + idleMinutes
                + " minutes idle; server will log you out now! @@@";
            chatDeliver(IWebServer.generalChatName, now, "SYSTEM", message,
                false);
            this.interrupt();

        }
        else if (idleSeconds >= (idleWarningsSent + 1)
            * IDLE_WARNING_INTERVAL_MINUTES * 60)
        {
            String message = "@@@ Hello " + getUsername() + ", you have been "
                + idleMinutes + " minutes idle; after "
                + (IDLE_WARNING_MAXCOUNT * IDLE_WARNING_INTERVAL_MINUTES)
                + " minutes idle time WebClient server will log you out!"
                + " (Type or do something to prevent that...) @@@";
            chatDeliver(IWebServer.generalChatName, now, "SYSTEM", message,
                false);
            idleWarningsSent++;
            LOGGER.fine("Idle warning sent to user " + getUsername()
                + ", idleWarnings now " + idleWarningsSent);
        }
    }


    public synchronized void tellToTerminate()
    {
        this.interrupt();
    }

    private boolean done = false;

    @Override
    public void interrupt()
    {
        super.interrupt();
        done = true;
        try
        {
            if (out != null)
            {
                out.println(connectionClosed);
            }
            if (socket != null)
            {
                socket.close();
            }
        }
        catch (IOException e)
        {
            // quietly close
        }
    }

    /**
     * prepare socket to read/write, and then loop as long
     * as lines from client come, and parse them
     */
    @Override
    public void run()
    {
        String fromClient = "dummy";

        LOGGER.log(Level.FINEST, "A new WSCST started running: "
            + this.toString());
        try
        {
            is = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(is));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream())), true);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING,
                "Preparing socket streams caused IOException: ", ex);
            return;
        }

        while (!done && fromClient != null)
        {
            fromClient = null;
            try
            {
                // when remote admin user requested shutdown, the method
                // called by parseLine() created the stopper Runnable;
                // we start that one here, to minimize the risk it tries
                // to stop ("interrupt") us where we are still processing
                // instead of being back and blocked in readLine().
                if (stopper != null)
                {
                    stopper.start();
                }
                fromClient = in.readLine();
            }
            catch (InterruptedIOException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (SocketException ex)
            {
                done = true;
            }
            catch (IOException e)
            {
                if (!isInterrupted())
                {
                    e.printStackTrace();
                }
                else
                {
                    LOGGER.log(Level.FINEST, "Interrupted");
                }
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE,
                    "WebServerClientSocketThread in read loop", e);
            }

            if (fromClient != null)
            {
                lastPacketReceived = new Date().getTime();

                done = parseLine(fromClient);

                String username = "<unknown>";

                if (user != null)
                {
                    username = user.getName();
                }
                else if (unverifiedUsername != null)
                {
                    username = unverifiedUsername;
                }
                else
                {
                    LOGGER.warning("Try to get username, but user is null?");
                }

                if (done)
                {
                    LOGGER.finest("user " + username + ": parseLine for '"
                        + fromClient + "' returns done = " + done);
                }
                /*
                else
                {
                    LOGGER.finest("user " + username + ": parseLine for '"
                        + fromClient + "' returns done = " + done);
                }
                */
            }
            else
            {
                LOGGER.finest("fromClient is null; setting done = true.");
                done = true;
            }
        }

        // Shut down the client.
        LOGGER.log(Level.FINEST, "(Trying to) shut down the client.");
        try
        {
            out.println(connectionClosed);
            socket.close();
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "IOException while closing connection",
                ex);
        }

        // if did not log in (wrong password, or duplicate name and without force,
        // user is not set yet.
        if (user != null)
        {
            if (user.getThread() == this)
            {
                user.setThread(null);
            }
            user = null;
        }

        if (server != null)
        {
            server.updateUserCounts();
            server = null;
        }
        socket = null;
    }

    private boolean parseLine(String fromClient)
    {
        boolean done = false;
        boolean ok = true;

        String reason = null;
        GameInfo gi = null;

        String[] tokens = fromClient.split(sep);
        String command = tokens[0];

        if (!command.equals(IWebServer.PingResponse))
        {
            idleWarningsSent = 0;
        }

        if (user == null && unverifiedUsername == null)
        {
            unverifiedUsername = "<unknown>";
        }

        if (!loggedIn && command.equals(IWebServer.Login))
        {
            LOGGER.log(Level.FINEST, "client attempts login.");
            ok = false;
            if (tokens.length >= 4)
            {
                String username = tokens[1];
                unverifiedUsername = username;
                String password = tokens[2];
                boolean force = Boolean.valueOf(tokens[3]).booleanValue();
                if (tokens.length >= 5)
                {
                    // Only clients version 1 or later send this, otherwise it
                    // says 0 (default initialization).
                    clientVersion = Integer.parseInt(tokens[4]);
                }

                LOGGER.info("User " + username
                    + " attempts login with client version " + clientVersion);

                reason = User.verifyLogin(username, password);

                /*
                 * if password is okay, check first whether same user is already
                 * logged in with another connection; if yes,
                 * when force is not set (1st try), send back the "already logged in";
                 * reacting on that, client will prompt whether to force the old
                 * connection out, and if user answers yes, will send a 2nd login
                 * message, this time with force flag set.
                 */
                if (reason == null)
                {
                    user = User.findUserByName(username);
                    WebServerClientSocketThread cst = (WebServerClientSocketThread)user
                        .getThread();
                    if (cst != null)
                    {
                        if (force)
                        {
                            cst.forceLogout(cst);
                        }
                        else
                        {
                            reason = IWebClient.alreadyLoggedIn;
                        }
                    }
                    else
                    {
                        LOGGER.log(Level.FINEST, "ok, not logged in yet");
                    }
                }

                // login accepted
                if (reason == null)
                {
                    user = User.findUserByName(username);
                    loggedIn = true;
                    user.updateLastLogin();
                    User.storeUsersToFile();
                    ok = true;
                    user.setThread(this);
                    setName("WebServerClientSocketThread " + username);
                }
            }
            else
            {
                reason = "Username, password or 'force' parameter is missing.";
                ok = false;
                done = true;
            }

            if (!ok)
            {
                // Login rejected - close connection immediately:
                done = true;
            }
        }

        else if (!loggedIn && command.equals(IWebServer.ConfirmRegistration))
        {
            ok = false;
            if (tokens.length >= 3)
            {
                String username = tokens[1];
                unverifiedUsername = username;
                String confCode = tokens[2];

                reason = server.confirmRegistration(username, confCode);
                if (reason == null)
                {
                    ok = true;
                }
            }
            else
            {
                reason = "Username or confirmation code missing.";
                ok = false;
            }

            done = true;
        }

        else if (!loggedIn && command.equals(IWebServer.RegisterUser))
        {
            ok = false;
            if (tokens.length >= 4)
            {
                String username = tokens[1];
                unverifiedUsername = username;
                String password = tokens[2];
                String email = tokens[3];

                reason = server.registerUser(username, password, email);
                if (reason == null)
                {
                    ok = true;
                }
            }
            else
            {
                reason = "Username, password or email missing.";
                ok = false;
            }

            done = true;
        }

        else if (!loggedIn)
        {
            ok = false;
            reason = "You are not logged in";
            done = true;
        }

        else if (command.equals(IWebServer.Logout))
        {
            user.updateLastLogout();
            User.storeUsersToFile();
            ok = true;
            done = true;
        }

        else if (command.equals(IWebServer.Propose))
        {
            String initiator = tokens[1];
            String variant = tokens[2];
            String viewmode = tokens[3];
            long startAt = Long.parseLong(tokens[4]);
            int duration = Integer.parseInt(tokens[5]);
            String summary = tokens[6];
            String expire = tokens[7];
            boolean unlMullis = Boolean.valueOf(tokens[8]).booleanValue();
            boolean balTowers = Boolean.valueOf(tokens[9]).booleanValue();
            int nmin = Integer.parseInt(tokens[10]);
            int ntarget = Integer.parseInt(tokens[11]);
            int nmax = Integer.parseInt(tokens[12]);

            gi = server.proposeGame(initiator, variant, viewmode, startAt,
                duration, summary, expire, unlMullis, balTowers, nmin,
                ntarget, nmax);
        }

        else if (command.equals(IWebServer.Enroll))
        {
            String gameId = tokens[1];
            String username = tokens[2];
            server.enrollUserToGame(gameId, username);
        }
        else if (command.equals(IWebServer.Unenroll))
        {
            String gameId = tokens[1];
            String username = tokens[2];
            server.unenrollUserFromGame(gameId, username);
        }
        else if (command.equals(IWebServer.Cancel))
        {
            String gameId = tokens[1];
            String byUser = tokens[2];
            server.cancelGame(gameId, byUser);
        }
        else if (command.equals(IWebServer.Start))
        {
            String gameId = tokens[1];
            User byUser = user;
            if (tokens.length >= 3)
            {
                String byUserName = tokens[2];
                if (!byUserName.equals(user.getName()))
                {
                    LOGGER.warning("startGame received byUserName is '"
                        + byUserName + "', but received from user '"
                        + user.getName() + "'?!?");
                }
                else
                {
                    byUser = User.findUserByName(byUserName);
                }
            }
            server.startGame(gameId, byUser);
        }
        else if (command.equals(IWebServer.StartAtPlayer))
        {
            String gameId = tokens[1];
            String hostingPlayer = tokens[2];
            String playerHost = tokens[3];
            int port = Integer.parseInt(tokens[4]);
            server.startGameOnPlayerHost(gameId, hostingPlayer, playerHost,
                port);
        }

        else if (command.equals(IWebServer.StartedByPlayer))
        {
            String gameId = tokens[1];
            server.informStartedByPlayer(gameId);
        }

        else if (command.equals(IWebServer.LocallyGameOver))
        {
            String gameId = tokens[1];
            server.informLocallyGameOver(gameId);
        }

        else if (command.equals(IWebServer.ChatSubmit))
        {
            String chatId = tokens[1];
            String sender = tokens[2];
            String message = tokens[3];
            server.chatSubmit(chatId, sender, message);
        }
        else if (command.equals(IWebServer.ChangePassword))
        {
            String username = tokens[1];
            String oldPassword = tokens[2];
            String newPassword = tokens[3];
            String email = null;
            Boolean isAdminObj = null;
            reason = server.changeProperties(username, oldPassword,
                newPassword, email, isAdminObj);
            if (reason != null)
            {
                ok = false;
            }
        }
        else if (command.equals(IWebServer.ShutdownServer))
        {
            if (user.isAdmin())
            {
                stopper = new Thread(new Runnable()
                {
                    public void run()
                    {
                        server.initiateShutdown(user.getName());
                    }
                });
            }
            else
            {
                LOGGER.log(Level.INFO, "Non-admin user " + user.getName()
                    + " requested shutdown - ignored.");
            }
        }
        else if (command.equals(IWebServer.RequestUserAttention))
        {
            if (user.isAdmin())
            {
                long when = Long.parseLong(tokens[1]);
                String sender = tokens[2];
                boolean isAdmin = Boolean.valueOf(tokens[3]).booleanValue();
                String recipient = tokens[4];
                String message = tokens[5];
                int beepCount = Integer.parseInt(tokens[6]);
                long beepInterval = Long.parseLong(tokens[7]);
                boolean windows = Boolean.valueOf(tokens[8]).booleanValue();

                server.requestUserAttention(when, sender, isAdmin, recipient,
                    message, beepCount, beepInterval, windows);
            }
            else
            {
                LOGGER.warning("Non-admin user " + user.getName()
                    + " attempted to use RequestUserAttention method!");
            }
        }
        else if (command.equals(IWebServer.PingResponse))
        {
            String name = "<user not defined>";
            if (user != null)
            {
                name = user.getName();
            }

            LOGGER
                .info("Received a ping response from user " + name);
        }

        else if (command.equals(IWebServer.Echo))
        {
            if (user.isAdmin())
            {
                sendToClient(fromClient);
            }
            else
            {
                LOGGER.log(Level.INFO, "Non-admin user " + user.getName()
                    + " used echo command - ignored.");
            }
        }
        else
        {
            LOGGER.log(Level.WARNING, "Unexpected command '" + command
                + "' from client");
            ok = false;
        }

        if (ok)
        {
            if (command.equals(IWebServer.Logout))
            {
                // Client cannot handle both the ACK:Logout and
                // ConnectionClosed before connection is gone...
                // so don't send a ACK for this one.
            }
            else
            {
                // LOGGER.log(Level.FINEST, "ACK: " + command + sep + reason);
                out.println("ACK: " + command + sep + reason);
            }
        }
        else
        {
            LOGGER.log(Level.FINE, "NACK: " + command + sep + reason);
            out.println("NACK: " + command + sep + reason);
        }

        if (command.equals(IWebServer.Propose) && gi != null)
        {
            server.enrollUserToGame(gi.getGameId(), user.getName());
        }

        if (ok && loggedIn && command.equals(IWebServer.Login))
        {
            LOGGER.log(Level.FINEST,
                "a new user logged in, sending proposed Games");
            if (user.isAdmin())
            {
                grantAdminStatus();
            }
            server.tellAllProposedGamesToOne(this);
            server.tellAllRunningGamesToOne(this);
            server.tellLastChatMessagesToOne(this, IWebServer.generalChatName);
            server.reEnrollIfNecessary(this);
            server.updateUserCounts();
        }

        server.saveGamesIfNeeded();

        return done;
    }

    /*
     * Called by another WebSocketClientSocketThread,
     * when user does a login with force flag set;
     * for example, when a connection got lost but client
     * somehow still logged on... ?
     */

    protected void forceLogout(WebServerClientSocketThread other)
    {
        if (other == null)
        {
            LOGGER.log(Level.WARNING,
                "In forceLogout(), parameter other is null!");
            return;
        }

        try
        {
            other.sendToClient(IWebClient.forcedLogout);
            try
            {
                other.interrupt();
            }
            catch (NullPointerException e)
            {
                // It's funny. It seems the interrupt above always gives a
                // null pointer exception, but the interrupting has done
                // it's job anyway...
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING,
                    "Different exception than usual while tried to "
                        + "interrupt 'other': ", e);
            }

            // LOGGER.log(Level.FINEST, "In CST before join");
            other.join();
            // LOGGER.log(Level.FINEST, "In CST after  join");
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING,
                "Oups couldn't stop the other WebServerClientSocketThread", e);
        }

        this.loggedIn = false;

    }

    private void sendToClient(String s)
    {
        out.println(s);
    }

    public void grantAdminStatus()
    {
        sendToClient(grantAdmin);
    }

    public void didEnroll(String gameId, String username)
    {
        sendToClient(didEnroll + sep + gameId + sep + username);
    }

    public void didUnenroll(String gameId, String username)
    {
        sendToClient(didUnenroll + sep + gameId + sep + username);
    }

    public void gameCancelled(String gameId, String byUser)
    {
        sendToClient(gameCancelled + sep + gameId + sep + byUser);
    }

    public void userInfo(int loggedin, int enrolled, int playing, int dead,
        long ago, String text)
    {
        sendToClient(userInfo + sep + loggedin + sep + enrolled + sep
            + playing + sep + dead + sep + ago + sep + text);
    }

    public void gameInfo(GameInfo gi)
    {
        sendToClient(gameInfo + sep + gi.toString(sep));
    }

    public void gameStartsSoon(String gameId, String byUser)
    {
        sendToClient(gameStartsSoon + sep + gameId + sep + byUser);
    }

    public void gameStartsNow(String gameId, int port, String hostingHost)
    {
        sendToClient(gameStartsNow + sep + gameId + sep + port + sep
            + hostingHost);
    }

    public void chatDeliver(String chatId, long when, String sender,
        String message, boolean resent)
    {
        // LOGGER.log(Level.FINEST, "chatDeliver() to client " + user.getName()
        //    + ": " + chatId + ", " + sender + ": " + message);
        sendToClient(chatDeliver + sep + chatId + sep + when + sep + sender
            + sep + message + sep + resent);
    }

    public void deliverGeneralMessage(long when, boolean error, String title,
        String message)
    {
        sendToClient(generalMessage + sep + when + sep + error + sep + title
            + sep + message);
    }

    public void requestAttention(long when, String byUser,
        boolean byAdmin, String message, int beepCount, long beepInterval, boolean windows)
    {
        sendToClient(requestAttention + sep + when + sep + byUser + sep
            + byAdmin + sep
            + message + sep + beepCount + sep + beepInterval + sep + windows);
    }

    public void requestPing(String arg1, String arg2, String arg3)
    {
        sendToClient(pingRequest + sep + arg1 + sep + arg2 + sep + arg3);
    }

    public void connectionReset(boolean forcedLogout)
    {
        // Not really needed here, only on client side.
        // Only implemented to satisfy the interface
    }
}
