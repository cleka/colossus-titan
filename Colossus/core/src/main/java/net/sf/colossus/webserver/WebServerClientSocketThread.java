package net.sf.colossus.webserver;


import java.net.Socket;
import java.net.SocketException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

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
    private WebServer server;
    private Socket socket;
    private InputStream is;
    private BufferedReader in;
    private PrintWriter out;
    private User user = null;
    private String cachedUsername = "<not set>";  // only for finalizer

    private boolean loggedIn = false;

    private final static String sep =
        net.sf.colossus.server.Constants.protocolTermSeparator;

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
            try { Thread.sleep(500);
            }
            catch (InterruptedException e) {/* ignore */
            }
            socket.close();
        }
        catch (IOException ex)
        {
            System.out.println("Rejecting a user did throw exception: " +
                ex.toString());
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
    public void run()
    {
        String fromClient = "dummy";

        // System.out.println("A new WSCST started running: " + this.toString());
        try
        {
            is = socket.getInputStream();
            in = new BufferedReader(new InputStreamReader(is));
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (IOException ex)
        {
            System.out.println("Preparing socket streams caused IOException: " +
                ex.toString());
            return;
        }

        while (!done && fromClient != null )
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
                    // System.out.println("Interrupted");
                }
            }
            catch (Exception e)
            {
                System.out.println("WebServerClientSocketThread in read " +
                    "loop, whatever Exception" + e.toString());
                Thread.dumpStack();
            }

            if (fromClient != null)
            {
                done = parseLine(fromClient);
                // System.out.println("parseLine returns done = " + done);
            }
            else
            {
                done = true;
            }
        }

        withdrawFromServer();

        // Shut down the client.
        // System.out.println("(Trying to) shut down the client.");
        try
        {
            out.println(connectionClosed);
            socket.close();
        }
        catch (IOException ex)
        {
            System.out.println("socket.close caused IOException: " +
                ex.toString());
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
        socket = null;
        server = null;
    }

    private void withdrawFromServer()
    {
        if (server != null)
        {
            server.withdrawFromServer(socket);
            server = null;
        }
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
            // System.out.println("client attempts login.");
            ok = false;
            if (tokens.length >= 3)
            {
                String username = tokens[1];
                String password = tokens[2];
                boolean force   = Boolean.valueOf(tokens[3]).booleanValue();

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
                    WebServerClientSocketThread cst =
                        (WebServerClientSocketThread)user.getThread();
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
                        // System.out.println("ok, not logged in yet");
                    }
                }

                // login accepted
                if (reason == null)
                {
                    user = User.findUserByName(username);
                    loggedIn = true;
                    ok = true;

                    user.setThread(this);

                    this.cachedUsername = username;

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

        else if (!loggedIn && command.equals(IWebServer.RegisterUser))
        {
            ok = false;
            if (tokens.length >= 4)
            {
                String username = tokens[1];
                String password = tokens[2];
                String email    = tokens[3];

                reason = server.registerUser(username, password, email, false);
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
            String expire = tokens[4];
            boolean unlMullis = Boolean.valueOf(tokens[5]).booleanValue();
            boolean balTowers = Boolean.valueOf(tokens[6]).booleanValue();
            int nmin = Integer.parseInt(tokens[7]);
            int ntarget = Integer.parseInt(tokens[8]);
            int nmax = Integer.parseInt(tokens[9]);

            gi = server.proposeGame(initiator, variant, viewmode, expire,
                unlMullis, balTowers, nmin, ntarget, nmax);
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
            reason = server.changeProperties(username, oldPassword, newPassword,
                email, isAdminObj);
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
                        server.initiateShutdown(false);
                    }
                }
                );
            }
            else
            {
                System.out.println("Non-admin user " + user.getName() +
                        " requested shutdown - ignored.");
            }
        }
        else
        {
            System.out.println("Unexpected command '"+command+"' from client");
            ok = false;
        }

        if (ok)
        {
            if (command.equals(IWebServer.Logout))
            {
                // Client cannont handle both the ACK:Logout and
                // ConnectionClosed before connection is gone...
                // so don't send a ACK for this one.
            }
            else
            {
                //                System.out.println("ACK: " + command + sep + reason);
                out.println("ACK: " +
                    command + sep + reason);
            }
        }
        else
        {
            //            System.out.println("NACK: " + command + sep + reason);
            out.println("NACK: " +
                command + sep + reason);
        }

        if (command.equals(IWebServer.Propose) && gi != null )
        {
            server.enrollUserToGame(gi.getGameId(), user.getName());
        }

        if (ok && loggedIn && command.equals(IWebServer.Login))
        {
            // System.out.println("a new user logged in, sending proposed Games");
            if (user.isAdmin())
            {
                grantAdminStatus();
            }
            server.tellAllPotentialGamesToOne(this);
            server.tellAllRunningGamesToOne(this);
            server.tellLastChatMessagesToOne(this, IWebServer.generalChatName);
            server.reEnrollIfNecessary(this);
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
            System.out.println(
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
            // It's funny. It seems the interrupt above always gives a null pointer
            // exception, but the interrupting has done it's job anyway...
            catch (NullPointerException e)
            {
                // System.out.println("all right, the familiar NPE here...");
            }
            catch (Exception e)
            {
                System.out.println("Different exception than usual while tried to interrupt 'other': " +
                    e.toString());
            }

            // System.out.println("after tellToTerminate");
            // System.out.println("In CST before join");
            other.join();
            // System.out.println("In CST after  join");


        }
        catch (Exception e)
        {
            System.out.println("Oups couldn't stop the other WebServerClientSocketThread" +
                e);
            Thread.dumpStack();
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
        //        System.out.println("WSCST.chatDeliver() to client " + user.getName() + ": "+ chatId + ", " + sender + 
        //                ": " + message);
        sendToClient(chatDeliver +
            sep + chatId + sep + when + sep + sender +
            sep + message + sep + resent);
    }

    public void finalize()
    {
        if (false)
        {
            System.out.println("finalize(): " + this.getClass().getName() +
                " for user " + this.cachedUsername);
        }
    }
}

