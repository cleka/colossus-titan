package net.sf.colossus.webclient;


import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webclient.WebClientSocketThread.WcstException;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;


/**
 * A first start to create a cmdline WebClient. For example for automated
 * testing or for shutting down the server.
 *
 * Still "Work in Progress", just committing it to get my work copy clean...
 *
 */
public class CmdLineWebClient implements IWebClient
{
    private static final Logger LOGGER = Logger
        .getLogger(CmdLineWebClient.class.getName());

    private final static String DEFAULT_USERNAME = "clemens";
    private final static String DEFAULT_PASSWORD = "secret";

    private IWebServer server = null;
    private WebClientSocketThread wcst = null;

    private final String hostname = "localhost";
    private final int port = 26766;

    /**
     * NOTE: shared with SocketThread, because WCST needs it to restore
     * game tokens to an GameInfo object
     */
    private final HashMap<String, GameInfo> gameHash = new HashMap<String, GameInfo>();

    public CmdLineWebClient()
    {
        LOGGER.info("Cmdline WebClient instantiated.");

    }

    public int getClientVersion()
    {
        return WebClient.WEB_CLIENT_VERSION;
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String useUsername = DEFAULT_USERNAME;
        String usePassword = DEFAULT_PASSWORD;

        // TODO Auto-generated method stub
        boolean doShutdown = false;
        boolean doLogin = false;

        if (args.length >= 2)
        {
            doLogin = true;
            useUsername = args[0];
            usePassword = args[1];
        }
        else
        {
            doLogin = true;
        }

        if (args.length == 1 && args[0].equals("shutdown") || args.length == 3
            && args[2].equals("shutdown"))
        {
            doShutdown = true;
        }

        CmdLineWebClient client = new CmdLineWebClient();

        if (doLogin)
        {
            System.out.println("Logging in as '" + useUsername
                + "', password '" + usePassword + "'");
            client.login(true, useUsername, usePassword);
            // Give some time for receiving all the chat messages etc.
            sleepFor(1000);
            if (doShutdown)
            {
                System.out.println(".. and initiating shutdown.");
                client.shutdownServer();
                client.logout();
            }
            else
            {
                interactiveLoop(client);
            }
        }
        else
        {
            interactiveLoop(client);
        }
    }

    private static void interactiveLoop(CmdLineWebClient cwClient)
    {
        String line;
        BufferedReader br = new BufferedReader(
            new InputStreamReader(System.in));
        try
        {
            while ((line = getOneLineFromStdin(br)) != null)
            {
                if (line.equals("login") || line.startsWith("login "))
                {
                    String usernameToUse = DEFAULT_USERNAME;
                    String passwordToUse = DEFAULT_PASSWORD;

                    String[] tokens = line.split(" +");
                    if (tokens.length >= 3)
                    {
                        passwordToUse = tokens[2];
                    }
                    if (tokens.length >= 2)
                    {
                        usernameToUse = tokens[1];
                    }

                    cwClient.login(true, usernameToUse, passwordToUse);
                    // Give some time for receiving all the chat messages etc.
                    sleepFor(1000);
                }

                else if (line.equals("exit") || line.equals("quit"))
                {
                    // No point to quit without logout - JVM will stay alive
                    // because thread is still alive.
                    cwClient.logout();
                    break;
                }

                else if (line.equals(""))
                {
                    // peacefully ignore empty lines
                }

                else if (!cwClient.isLoggedIn())
                {
                    System.out.println("<not logged in>");
                }
                else if (line.equals("shutdown"))
                {
                    cwClient.shutdownServer();
                }

                else if (line.equals("logout"))
                {
                    cwClient.logout();
                }

                else if (line.startsWith("chat "))
                {
                    cwClient.typedInChat(line.substring(5));
                }

                else
                {
                    System.out.println("???");
                }

            }
        }

        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "IOException while reading from sdtin??",
                e);
        }

        System.out.println("Outside loop...");
    }

    private static String getOneLineFromStdin(BufferedReader br)
        throws IOException
    {
        // give some time to process possible reply from server, so that
        // the "fromServer" line printed by WCST (if it prints it) and the
        // prompt don't garble so often...
        sleepFor(200);
        System.out.print("cmd > ");
        return br.readLine();
    }

    private static void sleepFor(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            LOGGER.log(Level.FINEST,
                "InterruptException caught... ignoring it...");
        }
    }

    /**
     * Create a commandline client (CmdLineWebClient) and login with given
     * parameters
     *
     * @param force
     * @param username
     * @param password
     * @return
     */
    public String login(boolean force, String username, String password)
    {
        String reason = null;

        System.out.println("Loggin in with username " + username
            + " and password " + password);
        // email is null: WCST does login
        wcst = new WebClientSocketThread(this, hostname, port, username,
            password, force, null, null, gameHash);

        WcstException e = wcst.getException();
        if (e == null)
        {
            wcst.start();
            server = wcst;

            updateStatus("Logged in", Color.green);
        }
        else
        {
            // I would have liked to let the constructor throw an exception
            // and catch this here, but then the returned pointer was null,
            // so could not do anything with it (and start() not be run),
            // so GC did not clean it up. Sooo... let's do it this way,
            // a little bit clumsy...

            if (wcst.stillNeedsRun())
            {
                wcst.start();
            }

            wcst = null;
            server = null;

            reason = e.getMessage();

            if (reason == null)
            {
                reason = "Unknown reason";
            }

            updateStatus("Login failed", Color.red);
            return reason;
        }

        return reason;
    }

    /**
     * Logout the active CmdlineClient
     * @return
     */
    private boolean logout()
    {
        boolean success = false;

        if (server != null)
        {
            server.logout();
            server = null;
            wcst = null;
        }

        updateStatus("Not connected", Color.green);
        return success;
    }

    private boolean isLoggedIn()
    {
        return wcst != null;
    }

    private String getUsername()
    {
        if (wcst != null)
        {
            return wcst.getUsername();
        }
        return null;
    }

    private void shutdownServer()
    {
        server.shutdownServer();
    }

    private void typedInChat(String message)
    {
        String chatId = IWebServer.generalChatName;
        server.chatSubmit(chatId, getUsername(), message);
    }

    public void updateStatus(String text, Color color)
    {
        String indicator = color == Color.red ? " ! " : "   ";
        System.out.println("STATUS " + indicator + ": " + text);
    }

    //
    // =======================================================================
    // ** Here start the methods needed to satisfy the IWebClient interface **
    //=======================================================================
    //

    public void chatDeliver(String chatId, long when, String sender,
        String message, boolean resent)
    {
        // TODO Auto-generated method stub

    }

    public void deliverGeneralMessage(long when, boolean error, String title,
        String message)
    {
        System.out.println((error ? "ERROR" : "INFO")
            + " general message: title '" + title + "', message text: '"
            + message + "'");
    }

    public void systemMessage(long when, String message)
    {
        System.out.println("System message (" + when + "): '" + message + "'");
    }

    public void requestAttention(long when, String byUser, boolean byAdmin,
        String message, int beepCount, long beepInterval, boolean windows)
    {
        String who = (byAdmin ? "Administrator" : "User") + byUser;
        String title = who + " requests your attention!";

        System.out.println(who + title + "\nMessage text: '" + message
            + "'\nBeeping " + beepCount + " times with interval "
            + beepInterval);
    }

    public void connectionReset(boolean forcedLogout)
    {
        // TODO Auto-generated method stub

    }

    public void didEnroll(String gameId, String username)
    {
        // TODO Auto-generated method stub

    }

    public void didUnenroll(String gameId, String username)
    {
        // TODO Auto-generated method stub

    }

    public void gameCancelled(String gameId, String byUser)
    {
        // TODO Auto-generated method stub

    }

    public void gameInfo(GameInfo gi)
    {
        // TODO Auto-generated method stub

    }

    public void gameStartsNow(String gameId, int port, String hostingHost,
        int inactivityCheckInterval, int inactivityWarningInterval,
        int inactivityTimeout)
    {
        // TODO Auto-generated method stub

    }

    public void gameStartsSoon(String gameId, String byUser)
    {
        // TODO Auto-generated method stub

    }

    public void grantAdminStatus()
    {
        // TODO Auto-generated method stub

    }

    public void userInfo(int loggedin, int enrolled, int playing, int dead,
        long ago, String text)
    {
        // TODO Auto-generated method stub

    }

    public void watchGameInfo(String gameId, String host, int port)
    {
        // TODO Auto-generated method stub

    }

    public void tellOwnInfo(String email)
    {
        // TODO Auto-generated method stub
    }
}
