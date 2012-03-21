package net.sf.colossus.webserver;


import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;


/**
 *  This class represents an actual WebServer client.
 *
 *  Mostly it contains the client's state data (logged in, client version,
 *  user and user name, and such).
 *
 *  It holds the reference to the actual socket thread that is waiting
 *  for input from client.
 *
 *  This class here provides the parsing of commands coming from clients
 *  to convert them into actual calls to to on the server object, and it
 *  also provides the methods which the server calls on the client
 *  (=translates method calls into text to send them over the socket).
 *
 *  TODO:
 *  The "parse messages" still contains some blocks and processing
 *  which should rather be in the actual socket thread object
 *  (during split into client and actual socket thread just copied the
 *  whole if-elseif-else block to here).
 *
 */

public class WebServerClient implements IWebClient
{
    private static final Logger LOGGER = Logger
        .getLogger(WebServerClient.class.getName());


    private final static String sep = IWebServer.WebProtocolSeparator;

    /** The client socket thread that handled the low-level connection stuff */
    private final WebServerClientSocketThread cst;

    /** The web server object that is managing all WebServerClients */
    private WebServer server;

    /** Whether or not this WebServerClient is at the moment logged in */
    private boolean loggedIn = false;

    /** Client side version */
    private int clientVersion;

    /** The user associated with this WebClient connection */
    private User user = null;

    /**
     * During registration request and sending of confirmation code,
     * we do not have a user yet. The parseLine sets then this variable
     * according to the username argument which was send from client.
     */
    private String unverifiedUsername = null;

    /** Time when last gameStartsNowSent was sent (in ms since epoch) */
    private long gameStartsNowSent = -1;

    /** Time when last gameStartsSoonSent was sent (in ms since epoch) */
    private long gameStartsSoonSent = -1;


    public WebServerClient(WebServer server, Socket socket)
    {
        // default initialization for clients that do not send this
        setClientVersion(0);

        this.server = server;
        this.cst = new WebServerClientSocketThread(this, socket);
    }

    public void startThread()
    {
        cst.start();
    }

    public WebServerClientSocketThread getWSCSThread()
    {
        return cst;
    }

    private void setClientVersion(int version)
    {
        clientVersion = version;
    }

    public int getClientVersion()
    {
        return clientVersion;
    }

    private void setUser(User u)
    {
        this.user = u;
    }

    User getUser()
    {
        return this.user;
    }

    String getUsername()
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

    public void setUnverifiedUsername(String name)
    {
        this.unverifiedUsername = name;
    }

    public String getUnverifiedUsername()
    {
        return unverifiedUsername;
    }

    public void requestPingIfNeeded(long now)
    {
        cst.requestPingIfNeeded(now);
    }

    public void requestPingNow()
    {
        cst.requestPingNow();
    }

    public void setLoggedIn(boolean val)
    {
        this.loggedIn = val;
    }

    public boolean getLoggedIn()
    {
        return this.loggedIn;
    }

    public void handleLogout()
    {
        // if login did not succeed (wrong password, or duplicate name and without force),
        // user will still be null; skip all this here then:
        if (user != null)
        {
            user.updateLastLogout();
            server.writeBackUsers();

            if (user.getWebserverClient() == this)
            {
                // after here, user is not in loggedInUsersList any more, i.e.
                // game updates during game cancelling are NOT sent to him.
                user.setWebClient(null);
                server.updateLoggedinStatus(user, null);

                if (!cst.wasForcedLogout())
                {
                    server.cancelIfNecessary(user);
                }
                setUser(null);
            }
            else
            {
                String name = ((WebServerClient)user.getWebserverClient())
                    .getUsername();
                LOGGER.warning("handleLogout called for a "
                    + "different WebServerClient: " + name + " than ourself.");
            }
        }

        if (server != null)
        {
            server.updateUserCounts();
            server = null;
        }
    }

    public boolean parseLine(String fromClient)
    {
        boolean done = false;
        boolean ok = true;

        String reason = null;
        GameInfo gi = null;

        String[] tokens = fromClient.split(sep);
        String command = tokens[0];

        if (!command.equals(IWebServer.PingResponse))
        {
            cst.clearIdleWarningsSent();
        }

        if (user == null && unverifiedUsername == null)
        {
            unverifiedUsername = "<unknown>";
        }

        if (!loggedIn && command.equals(IWebServer.Login))
        {
            ok = false;
            if (tokens.length >= 4)
            {
                String username = tokens[1];
                unverifiedUsername = username;
                String password = tokens[2];
                boolean force = Boolean.valueOf(tokens[3]).booleanValue();
                if (tokens.length >= 5)
                {
                    // Only clients version 1 or later send this,
                    // it remains 0 (default initialization).
                    setClientVersion(Integer.parseInt(tokens[4]));
                }

                LOGGER.info("User " + username
                    + " attempts login with client version " + clientVersion);

                if (clientVersion < 2)
                {
                    reason = "Your Colossus software is too old. Please upgrade to Release 0.10.3!";
                }
                else
                {
                    reason = server.verifyLogin(username, password);
                }

                if (reason == null)
                {
                    reason = ensureNotAlreadyLoggedIn(username, force);
                }

                // login accepted
                if (reason == null)
                {
                    setUser(server.findUserByName(username));
                    loggedIn = true;
                    user.updateLastLogin();
                    server.writeBackUsers();
                    ok = true;
                    user.setWebClient(this);
                    server.updateLoggedinStatus(user, this);

                    cst.setName("WSCST " + username);
                    LOGGER.info("User successfully logged in: "
                        + cst.getClientInfo());
                }
                else
                {
                    LOGGER.info("Login for " + unverifiedUsername
                        + " not accepted, setting done to true.");
                    ok = false;
                    done = true;
                }
            }
            else
            {
                LOGGER.log(Level.FINEST,
                    "A client attempted login with too few arguments.");

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
                // TODO in practice this can never be true.
                // in best/standard case it will be PROV_CONFCODE
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
            LOGGER.info("Received Logout request from user "
                + cst.getClientInfo());
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
                if (byUserName.equalsIgnoreCase(user.getName()))
                {
                    byUserName = user.getName();
                }
                if (!byUserName.equals(user.getName()))
                {
                    LOGGER.warning("startGame received byUserName is '"
                        + byUserName + "', but received from user '"
                        + user.getName() + "'?!?");
                }
                else
                {
                    byUser = server.findUserByName(byUserName);
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
            processChatLine(chatId, sender, message);
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
                cst.createStopper(new Runnable()
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

        else if (command.equals(IWebServer.DumpInfo))
        {
            server.dumpInfo();
        }

        else if (command.equals(IWebServer.PingResponse))
        {
            long requestSentTime = Long.parseLong(tokens[1]);
            int counter = Integer.parseInt(tokens[2]);
            String name = getUsername();
            long requestResponseArriveTime = new Date().getTime();
            long roundtripTime = requestResponseArriveTime - requestSentTime;
            String msg = "Received ping response #" + counter + " from user "
                + name
                + ", request roundtrip time is " + roundtripTime + " ms.";
            if (roundtripTime > 3000)
            {
                LOGGER.warning(msg);
            }
            else
            {
                LOGGER.info(msg);
            }
            cst.storeEntry(requestResponseArriveTime, roundtripTime);
        }

        else if (command.equals(IWebServer.ConfirmCommand))
        {
            long now = new Date().getTime();
            // long confirmationSentTime = Long.parseLong(tokens[1]);
            String cmd = tokens[2];
            /*
            String arg1 = tokens[3];
            String arg2 = tokens[4];
            String arg3 = tokens[5];
            */
            long cmdRTT = 0;
            if (cmd.equals(gameStartsSoon))
            {
                if (gameStartsSoonSent != -1)
                {
                    cmdRTT = now - gameStartsSoonSent;
                }
                else
                {
                    LOGGER.warning("Got ConfirmCommand " + cmd
                        + " but no cmdSent time set!");
                }
                gameStartsSoonSent = -1;
            }
            if (cmd.equals(gameStartsNow))
            {
                if (gameStartsNowSent != -1)
                {
                    cmdRTT = now - gameStartsNowSent;
                }
                else
                {
                    LOGGER.warning("Got ConfirmCommand " + cmd
                        + " but no cmdSent time set!");
                }
                gameStartsNowSent = -1;
            }

            LOGGER.info("Got confirmCommand for command " + cmd
                + " - time between cmd sent and conf got is " + cmdRTT);
        }

        else if (command.equals(IWebServer.RereadLoginMessage))
        {
            if (user.isAdmin())
            {
                server.rereadLoginMessage();
            }
            else
            {
                LOGGER.log(Level.INFO, "Non-admin user " + user.getName()
                    + " used rereadLoginMessage command - ignored.");
            }
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

        /* ============================================================
         *  Act based on the ok or not ok value
         * ============================================================
         */
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
                // TODO: do we really need to send ACK for *every* line we
                // got from client?!?
                sendToClient("ACK: " + command + sep + reason);
            }
        }
        else
        {
            LOGGER.log(Level.FINE, "NACK: " + command + sep + reason);
            sendToClient("NACK: " + command + sep + reason);
        }

        // after them connection will be closed. Make sure everything
        // from output queue is written first.
        if (!ok && command.equals(IWebServer.Login)
            || command.equals(IWebServer.RegisterUser)
            || command.equals(IWebServer.ConfirmRegistration))
        {
            cst.flushMessages();
        }

        // TODO: why is this done after the if-elseif, and not inside the
        // proposeGame block?
        // (perhaps because client needs the ACK first??)

        if (command.equals(IWebServer.Propose))
        {
            if (gi != null)
            {
                server.enrollUserToGame(gi.getGameId(), user.getName());
            }
            else
            {
                long now = new Date().getTime();
                requestAttention(now, "SYSTEM", true,
                    "Don't click 'Propose' multiple times!", 1, 500, true);
            }
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
            server.sendMessageOfTheDayToOne(this, IWebServer.generalChatName);
            if (clientVersion < 2)
            {
                server.sendOldVersionWarningToOne(this, getUsername(),
                    IWebServer.generalChatName);
            }
            server.reEnrollIfNecessary(this);
            server.updateUserCounts();
            LOGGER.info("loggedIn postprocessing for user " + user.getName()
                + " completed!");
            // just before readline() command will print a log message that
            // thread is now back available to read input from client
            cst.setLastWasLogin();
            // Request a Ping, so we see when client was earliest able to respond
            requestPingNow();
        }

        server.saveGamesIfNeeded();

        return done;
    }

    public void processChatLine(String chatId, String sender, String message)
    {
        if (!chatId.equals(IWebServer.generalChatName))
        {
            LOGGER.log(Level.WARNING, "Chat for chatId " + chatId
                + " not implemented.");
            return;
        }
        LOGGER.finest("Chat msg from user " + sender + ": " + message);
        String msgAllLower = message.toLowerCase();

        if (msgAllLower.startsWith("/?") || msgAllLower.startsWith("/h")
            || msgAllLower.startsWith("/help"))
        {
            server.getGeneralChat()
                .sendHelpToClient(msgAllLower, chatId, this);
        }
        else if (msgAllLower.startsWith("/ping \""))
        {
            server.handlePingQuotedName(sender, message);
        }
        else if (msgAllLower.startsWith("/ping"))
        {
            server.handlePing(sender, message);
        }
        else if (msgAllLower.startsWith("/contact"))
        {
            server.getGeneralChat().showContactHelp(chatId, this);
        }
        else if (msgAllLower.startsWith("/"))
        {
            server.getGeneralChat().handleUnknownCommand(msgAllLower, chatId,
                this);
        }
        else
        {
            server.chatSubmit(chatId, sender, message);
        }
    }

    /**
     * if password is okay, check first whether same user is already
     * logged in with another connection; if yes,
     * when force is not set (1st try), send back the "already logged in";
     * reacting on that, client will prompt whether to force the old
     * connection out, and if user answers yes, will send a 2nd login
     * message, this time with force flag set.
     */

    private String ensureNotAlreadyLoggedIn(String username, boolean force)
    {
        String reason = null;
        // Do not set the real user here, otherwise in the re-login case
        // the first reject would lead to autoCancelling games, too.
        WebServerClientSocketThread otherCst = null;
        User tmpUser = server.findUserByName(username);
        WebServerClient otherWsc = (WebServerClient)tmpUser
            .getWebserverClient();
        if (otherWsc != null)
        {
            otherCst = otherWsc.getWSCSThread();
        }

        if (otherCst != null)
        {
            if (force)
            {
                LOGGER.fine("User " + username
                    + " already logged in ("
                    + otherCst + ") - forcing Logout");
                otherCst.forceLogout(otherCst);
            }
            else
            {
                LOGGER.fine("User " + username + " already logged in ("
                    + otherCst + ") "
                    + "- replying with alreadyLoggedIn reject message");
                reason = IWebClient.alreadyLoggedIn;
            }
        }
        else
        {
            LOGGER.finest("ok, " + username
                + " is not logged in at the moment");
        }
        return reason;
    }

    public void systemMessage(long now, String message)
    {
        if (getClientVersion() >= 3)
        {
            chatDeliver(IWebServer.generalChatName, now, "SYSTEM", message,
                false);
            // sendToClient(systemMessage + sep + now + sep + message);
        }
        else
        {
            chatDeliver(IWebServer.generalChatName, now, "SYSTEM", message, false);
        }
    }

    private void sendToClient(String s)
    {
        cst.sendToClient(s);
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
        gameStartsSoonSent = new Date().getTime();
        sendToClient(gameStartsSoon + sep + gameId + sep + byUser);
        long spentTime = new Date().getTime() - gameStartsSoonSent;
        LOGGER.info("Sending gameStartsSoon to " + getUsername() + " took "
            + spentTime
            + " milliseconds.");
    }

    public void gameStartsNow(String gameId, int port, String hostingHost)
    {
        gameStartsNowSent = new Date().getTime();
        sendToClient(gameStartsNow + sep + gameId + sep + port + sep
            + hostingHost);
        long spentTime = new Date().getTime() - gameStartsNowSent;
        LOGGER.info("Sending gameStartsNow to " + getUsername() + " took "
            + spentTime
            + " milliseconds.");
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

    public void requestAttention(long when, String byUser, boolean byAdmin,
        String message, int beepCount, long beepInterval, boolean windows)
    {
        sendToClient(requestAttention + sep + when + sep + byUser + sep
            + byAdmin + sep + message + sep + beepCount + sep + beepInterval
            + sep + windows);
    }

    // TODO should this be rather totally in clientsocketthread?
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
