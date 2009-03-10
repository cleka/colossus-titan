package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
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
 *  on the client.
 *  
 *  @version $Id$
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

    private boolean loggedIn = false;

    private final static String sep = net.sf.colossus.server.Constants.protocolTermSeparator;

    private Thread stopper = null;

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
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
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
            out.println(connectionClosed);
            is.close();
            socket.close();
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
            out = new PrintWriter(socket.getOutputStream(), true);
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
                done = parseLine(fromClient);
                LOGGER.log(Level.FINEST, "parseLine returns done = " + done);
            }
            else
            {
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

        if (!loggedIn && command.equals(IWebServer.Login))
        {
            LOGGER.log(Level.FINEST, "client attempts login.");
            ok = false;
            if (tokens.length >= 3)
            {
                String username = tokens[1];
                String password = tokens[2];
                boolean force = Boolean.valueOf(tokens[3]).booleanValue();

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
                    ok = true;
                    user.setThread(this);
                    setName("WebServerClientSocketThread " + username);
                }
            }
            else
            {
                reason = "Username or password missing.";
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
            if (tokens.length >= 2)
            {
                String username = tokens[1];
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
            if (tokens.length >= 3)
            {
                String username = tokens[1];
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
            server.startGame(gameId);
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
        else
        {
            LOGGER.log(Level.INFO, "Unexpected command '" + command
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
                LOGGER.log(Level.FINEST, "ACK: " + command + sep + reason);
                out.println("ACK: " + command + sep + reason);
            }
        }
        else
        {
            LOGGER.log(Level.FINEST, "NACK: " + command + sep + reason);
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
            server.tellAllScheduledGamesToOne(this);
            server.tellAllInstantGamesToOne(this);
            server.tellAllRunningGamesToOne(this);
            server.tellLastChatMessagesToOne(this, IWebServer.generalChatName);
            server.reEnrollIfNecessary(this);
            server.updateUserCounts();
        }

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

    public void gameStartsSoon(String gameId)
    {
        sendToClient(gameStartsSoon + sep + gameId);
    }

    public void gameStartsNow(String gameId, int port)
    {
        sendToClient(gameStartsNow + sep + gameId + sep + port);
    }

    public void gameStarted(String gameId)
    {
        sendToClient(gameStarted + sep + gameId);
    }

    public void chatDeliver(String chatId, long when, String sender,
        String message, boolean resent)
    {
        LOGGER.log(Level.FINEST, "chatDeliver() to client " + user.getName()
            + ": " + chatId + ", " + sender + ": " + message);
        sendToClient(chatDeliver + sep + chatId + sep + when + sep + sender
            + sep + message + sep + resent);
    }
}
