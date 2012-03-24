package net.sf.colossus.webclient;


import java.awt.Color;
import java.util.HashMap;
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

    private IWebServer server = null;
    private WebClientSocketThread wcst = null;

    private final String hostname = "localhost";
    private final int port = 26766;
    private final String username = "clemens";
    private final String password = "secret";

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
        // TODO Auto-generated method stub

        CmdLineWebClient client = new CmdLineWebClient();

        client.createLoginWebClientSocketThread(true);

        client.logout();
    }

    public String createLoginWebClientSocketThread(boolean force)
    {
        String reason = null;

        // email is null: WCST does login
        wcst = new WebClientSocketThread(null, hostname, port, username,
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

    public void updateStatus(String text, Color color)
    {
        String indicator = color == Color.red ? " ! " : "   ";
        System.out.println("STATUS " + indicator + ": " + text);
    }

    private boolean logout()
    {
        boolean success = false;

        server.logout();
        server = null;
        wcst = null;

        updateStatus("Not connected", Color.red);
        return success;
    }

    public void chatDeliver(String chatId, long when, String sender,
        String message, boolean resent)
    {
        // TODO Auto-generated method stub

    }

    public void deliverGeneralMessage(long when, boolean error,
        String title, String message)
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

    public void gameStartsNow(String gameId, int port, String hostingHost)
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

}
