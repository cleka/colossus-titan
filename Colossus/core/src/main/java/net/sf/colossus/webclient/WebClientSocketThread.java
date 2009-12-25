package net.sf.colossus.webclient;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;


/**
 *  This implements the webserver/client communication at client side.
 *  It implements the server interface on client side;
 *  i.e. something server wanted to execute for a client, is read
 *  from the client socket input stream, parsed, and executed
 *  by the (WebClient)SocketThread.
 *
 *  This also contains the methods which are called by the client
 *  (WebClient's GUI) and are sent over the socket to the server
 *  (note that those calls mostly happen in the EDT).
 *
 *  @author Clemens Katzer
 */
public class WebClientSocketThread extends Thread implements IWebServer
{
    private static final Logger LOGGER = Logger
        .getLogger(WebClientSocketThread.class.getName());

    private IWebClient webClient = null;
    private final HashMap<String, GameInfo> gameHash;

    private String hostname = null;
    private final int port;

    private String username = null;
    private String password = null;
    private boolean force = false;
    private String email = null;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean stillNeedsRun = true;

    private final static String sep = IWebServer.WebProtocolSeparator;

    private boolean loggedIn = false;
    private AckWaiter ackWaiter;
    private WcstException failedException = null;

    public WebClientSocketThread(WebClient wcGUI, String hostname, int port,
        String username, String password, boolean force, String email,
        String confCode, HashMap<String, GameInfo> gameHash)
    {
        super("WebClientSocketThread for user " + username);
        this.webClient = wcGUI;
        this.gameHash = gameHash;
        this.hostname = hostname;
        LOGGER.info("WCST constructor: user " + username + " host " + hostname
            + " port " + port + " password " + password);

        this.port = port;
        this.username = username;
        this.password = password;
        this.force = force;
        this.email = email;
        this.ackWaiter = new AckWaiter();

        net.sf.colossus.util.InstanceTracker
            .register(this, "WCST " + username);

        try
        {
            // connect, as well as any of the three below
            // might throw an exception if they fail:
            connect();

            // If client GUI provided a confirmation code, then in is a
            // confirmation for a previously sent registration; otherwise:
            // If client GUI provided an email, it's a registration attempt,
            // otherwise just a normal login.
            if (confCode != null)
            {
                // confirmation
                confirm(confCode);
            }
            else if (email != null)
            {
                // initial registration
                register();
            }
            else
            {
                // no email, login instead
                login();
            }
        }
        catch (WcstException e)
        {
            this.failedException = e;
        }
    }

    public String getOneLine() throws IOException
    {
        String line = "No line - got exception!";
        try
        {
            line = this.in.readLine();
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Exception during read from socket!", e);
            Thread.dumpStack();
            throw e;
        }
        return line;
    }

    public WcstException getException()
    {
        return failedException;
    }

    private void connect() throws WcstException
    {
        String info = null;
        writeLog("About to connect client socket to " + hostname + ":" + port);
        try
        {
            socket = new Socket(hostname, port);
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (UnknownHostException e)
        {
            info = "Unknown host: " + e.getMessage() + "\" - wrong address?";
            writeLog(e.toString());
        }

        catch (ConnectException e)
        {
            info = "Could not connect: '" + e.getMessage()
                + "' - wrong address/port, or server not running?";
            writeLog(e.toString());
        }
        catch (Exception e) // IOException, IllegalBlockingModeException
        {
            info = "Exception during connect: " + e.toString();
            writeLog(e.toString());
        }

        if (info != null)
        {
            String message = info;
            throw new WcstException(message);
        }
    }

    /**
     * Initial registration attempt
     *
     * @throws WcstException
     */
    private void register() throws WcstException
    {
        String info = null;

        try
        {
            this.in = new BufferedReader(new InputStreamReader(socket
                .getInputStream()));

            send(RegisterUser + sep + username + sep + password + sep + email);
            String fromServer = null;

            if ((fromServer = getOneLine()) != null)
            {
                if (fromServer.startsWith("ACK:"))
                {
                    // ("WCST.register(): ok, got ACK! ("+fromServer+")");
                }
                else
                {
                    // TODO: why do we handle this with NACKs ?
                    String prefix = "NACK: " + IWebServer.RegisterUser + sep;
                    if (fromServer.startsWith(prefix))
                    {
                        info = fromServer.substring(prefix.length());
                    }
                    else
                    {
                        info = fromServer;
                    }
                }
            }
            else
            {
                info = "NULL reply from server (socket closed??)!";
            }
        }
        catch (Exception ex)
        {
            writeLog(ex.toString());
            info = "Creating or reading from buffered reader failed";
        }

        if (info != null)
        {
            LOGGER.info("register() : info != null, info: " + info);
            String message = info;

            throw new WcstException(message);
        }
    }

    /**
     * Send the confirmation code
     * @throws WcstException
     */
    private void confirm(String confCode) throws WcstException
    {
        String info = null;

        try
        {
            this.in = new BufferedReader(new InputStreamReader(socket
                .getInputStream()));

            send(ConfirmRegistration + sep + username + sep + confCode);
            String fromServer = null;

            if ((fromServer = getOneLine()) != null)
            {
                if (fromServer.startsWith("ACK:"))
                {
                    // ("WCST.confirm(): ok, got ACK! ("+fromServer+")");
                }
                else
                {
                    String prefix = "NACK: " + IWebServer.ConfirmRegistration
                        + sep;
                    if (fromServer.startsWith(prefix))
                    {
                        info = fromServer.substring(prefix.length());
                    }
                    else
                    {
                        info = fromServer;
                    }
                }
            }
            else
            {
                info = "NULL reply from server (socket closed??)!";
            }
        }
        catch (Exception ex)
        {
            writeLog(ex.toString());
            info = "Creating or reading from buffered reader failed";
        }

        if (info != null)
        {
            throw new WcstException(info);
        }
    }

    private void login() throws WcstException
    {
        String info = null;
        boolean duplicateLogin = false;
        try
        {
            this.in = new BufferedReader(new InputStreamReader(socket
                .getInputStream()));

            send(Login + sep + username + sep + password + sep + force);
            String fromServer = null;

            if ((fromServer = getOneLine()) != null)
            {
                if (fromServer.startsWith("ACK:"))
                {
                    loggedIn = true;
                }
                else if (fromServer.equals("NACK: " + IWebServer.Login + sep
                    + IWebClient.alreadyLoggedIn))
                {
                    duplicateLogin = true;
                    info = "Already logged in!";
                }
                else
                {
                    String prefix = "NACK: " + IWebServer.Login + sep;
                    if (fromServer.startsWith(prefix))
                    {
                        info = fromServer.substring(prefix.length());
                    }
                    else
                    {
                        info = fromServer;
                    }
                }
            }
            else
            {
                info = "NULL reply from server (socket closed??)!";
            }
        }
        catch (Exception ex)
        {
            writeLog(ex.toString());
            info = "Creating or reading from buffered reader failed";
        }

        if (info != null)
        {
            String message = "Login failed: " + info;
            throw new WcstException(message, duplicateLogin);
        }
    }

    public boolean stillNeedsRun()
    {
        return stillNeedsRun;
    }

    @Override
    public void run()
    {
        stillNeedsRun = false;
        if (this.socket == null)
        {
            // All right. We were just called to get the run()
            // done, even if the constructor threw exception.
            // Otherwise the GC won't clean up this thread.
            cleanup();
            return;
        }

        String fromServer = null;
        boolean done = false;
        boolean forcedLogout = false;
        try
        {
            while (!done && (fromServer = getOneLine()) != null)
            {
                String[] tokens = fromServer.split(sep, -1);
                String command = tokens[0];
                if (fromServer.startsWith("ACK: "))
                {
                    command = tokens[0].substring(5);
                    handleAckNack(command, tokens);
                }
                else if (fromServer.startsWith("NACK: "))
                {
                    command = tokens[0].substring(6);
                    handleAckNack(command, tokens);
                }

                else if (fromServer.equals(IWebClient.connectionClosed))
                {
                    done = true;
                }

                else if (fromServer.equals(IWebClient.forcedLogout))
                {
                    forcedLogout = true;
                    done = true;
                }
                else if (command.equals(IWebClient.gameInfo))
                {
                    GameInfo gi = restoreGameInfo(tokens);
                    webClient.gameInfo(gi);
                }
                else if (command.equals(IWebClient.userInfo))
                {
                    int loggedin = Integer.parseInt(tokens[1]);
                    int enrolled = Integer.parseInt(tokens[2]);
                    int playing = Integer.parseInt(tokens[3]);
                    int dead = Integer.parseInt(tokens[4]);
                    long ago = Long.parseLong(tokens[5]);
                    String text = tokens[6];
                    webClient.userInfo(loggedin, enrolled, playing, dead, ago,
                        text);
                }

                else if (command.equals(IWebClient.didEnroll))
                {
                    String gameId = tokens[1];
                    String user = tokens[2];
                    webClient.didEnroll(gameId, user);
                }
                else if (command.equals(IWebClient.didUnenroll))
                {
                    String gameId = tokens[1];
                    String user = tokens[2];
                    webClient.didUnenroll(gameId, user);
                }

                else if (command.equals(IWebClient.gameCancelled))
                {
                    String gameId = tokens[1];
                    String byUser = tokens[2];
                    webClient.gameCancelled(gameId, byUser);
                }

                else if (command.equals(IWebClient.gameStartsNow))
                {
                    String gameId = tokens[1];
                    int port = Integer.parseInt(tokens[2]);
                    String host = tokens[3];
                    webClient.gameStartsNow(gameId, port, host);
                }

                else if (command.equals(IWebClient.gameStartsSoon))
                {
                    String gameId = tokens[1];
                    webClient.gameStartsSoon(gameId);
                }

                else if (command.equals(IWebClient.gameStarted))
                {
                    String gameId = tokens[1];
                    webClient.gameStarted(gameId);
                }

                else if (command.equals(IWebClient.chatDeliver))
                {
                    String chatId = tokens[1];
                    long when = Long.parseLong(tokens[2]);
                    String sender = tokens[3];
                    String message = tokens[4];
                    boolean resent = Boolean.valueOf(tokens[5]).booleanValue();
                    webClient.chatDeliver(chatId, when, sender, message,
                        resent);
                }
                else if (command.equals(IWebClient.grantAdmin))
                {
                    webClient.grantAdminStatus();
                }

                else
                {
                    if (webClient != null)
                    {
                        if (webClient instanceof WebClient)
                        {
                            ((WebClient)webClient).showAnswer(fromServer);
                        }
                        /*
                        else if (webClient instanceof WebClient)
                        {
                            ((WebClient)webClient).showAnswer(fromServer);
                        }
                        */
                    }
                }

            } // while !done && readLine != null

            writeLog("End of SocketClientThread while loop, done = " + done
                + " readline "
                + (fromServer == null ? " null " : "'" + fromServer + "'"));
            if (loggedIn)
            {
                // Unexpectedly got a connection closed, at least we did not
                // initiate the logout ourself. So, to be sure, reset the GUI
                // to be "empty".
                webClient.connectionReset(forcedLogout);
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "WebClientSocketThread IOException!");
            webClient.connectionReset(false);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING,
                "WebClientSocketThread whatever Exception!", e);
            Thread.dumpStack();
            webClient.connectionReset(false);
        }

        cleanup();
    }

    private GameInfo restoreGameInfo(String[] tokens)
    {
        GameInfo gi = GameInfo.fromString(tokens, gameHash, false);
        return gi;
    }

    private void cleanup()
    {
        if (socket != null)
        {
            try
            {
                socket.close();
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.WARNING,
                    "WebClientSocketThread close() IOException!", ex);
            }
        }
        socket = null;
        webClient = null;
        ackWaiter = null;
    }

    public void dispose()
    {
        cleanup();
    }

    private void send(String s)
    {
        out.println(s);
    }

    private class AckWaiter
    {
        String command;
        String result;
        boolean waiting = false;

        public AckWaiter()
        {
            // nothing special to do
        }

        public boolean isWaiting()
        {
            return waiting;
        }

        public synchronized String sendAndWait(String command, String args)
        {
            waiting = true;
            setCommand(command);
            send(command + sep + args);

            // will wait() until SocketThread has set the result and called notify.
            String result = waitForAck();
            waiting = false;

            return result;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        public String getCommand()
        {
            return command;
        }

        public synchronized String waitForAck()
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.WARNING, " got exception " + e.toString());
            }
            return result;
        }

        public synchronized void setResult(String result)
        {
            this.result = result;
            this.notify();
        }
    }

    public void logout()
    {
        loggedIn = false;
        send(Logout);
    }

    public String changeProperties(String username, String oldPW,
        String newPW, String email, Boolean isAdminObj)
    {
        String reason = ackWaiter.sendAndWait(ChangePassword, username + sep
            + oldPW + sep + newPW + sep + email + sep + isAdminObj);
        return reason;
    }

    private void handleAckNack(String command, String[] tokens)
    {
        if (ackWaiter != null && ackWaiter.isWaiting())
        {
            String cmd = ackWaiter.getCommand();
            if (cmd != null && cmd.equals(command))
            {
                ackWaiter.setResult(tokens[1]);
            }
            else
            {
                LOGGER.log(Level.WARNING, "Waiting for (N)ACK for command "
                    + cmd + " but " + "got " + command);
            }
        }
    }

    public GameInfo proposeGame(String initiator, String variant,
        String viewmode, long startAt, int duration, String summary,
        String expire, boolean unlimitedMulligans, boolean balancedTowers,
        int min, int target, int max)
    {
        send(Propose + sep + initiator + sep + variant + sep + viewmode + sep
            + startAt + sep + duration + sep + summary + sep + expire + sep
            + unlimitedMulligans + sep + balancedTowers + sep + min + sep
            + target + sep + max);
        return null;
    }

    public void enrollUserToGame(String gameId, String username)
    {
        send(Enroll + sep + gameId + sep + username);
    }

    public void unenrollUserFromGame(String gameId, String username)
    {
        send(Unenroll + sep + gameId + sep + username);
    }

    public void cancelGame(String gameId, String byUser)
    {
        send(Cancel + sep + gameId + sep + byUser);
    }

    public void startGame(String gameId)
    {
        send(Start + sep + gameId);
    }

    public void informStartedByPlayer(String gameId)
    {
        send(StartedByPlayer + sep + gameId);
    }

    public void informLocallyGameOver(String gameId)
    {
        send(LocallyGameOver + sep + gameId);
    }

    public void startGameOnPlayerHost(String gameId, String hostingPlayer,
        String playerHost, int port)
    {
        send(StartAtPlayer + sep + gameId + sep + hostingPlayer + sep
            + playerHost + sep + port);
    }

    public void chatSubmit(String chatId, String sender, String message)
    {
        String sending = ChatSubmit + sep + chatId + sep + sender + sep
            + message;
        send(sending);
    }

    public void shutdownServer()
    {
        send(IWebServer.ShutdownServer);
    }

    public void submitAnyText(String text)
    {
        if (text.equals("die"))
        {
            System.exit(1);
        }

        send(text);
    }

    private void writeLog(String s)
    {
        if (true)
        {
            LOGGER.log(Level.INFO, s);

        }
    }

    public class WcstException extends Exception
    {
        boolean failedBecauseAlreadyLoggedIn = false;

        public WcstException(String message, boolean dupl)
        {
            super(message);
            failedBecauseAlreadyLoggedIn = dupl;
        }

        public WcstException(String message)
        {
            super(message);
            failedBecauseAlreadyLoggedIn = false;
        }

        public boolean failedBecauseAlreadyLoggedIn()
        {
            return failedBecauseAlreadyLoggedIn;
        }
    }
}
