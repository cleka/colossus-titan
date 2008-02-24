package net.sf.colossus.webcommon;


/** Interface for what WebServer sends to WebClient
 *
 *  @version $Id$
 *  @author Clemens Katzer
 */

public interface IWebClient
{
    public static String alreadyLoggedIn = "alreadyLoggedIn";
    public static String grantAdmin = "grantAdmin";
    public static String tooManyUsers = "tooManyUsers";
    public static String connectionClosed = "connectionClosed";
    public static String forcedLogout = "forcedLogout";
    public static String didEnroll = "didEnroll";
    public static String didUnenroll = "didUnenroll";
    public static String gameInfo = "gameInfo";
    public static String userInfo = "userInfo";
    public static String gameStarted = "gameStarted";
    public static String gameStartsNow = "gameStartsNow";
    public static String gameStartsSoon = "gameStartsSoon";
    public static String gameCancelled = "gameCancelled";
    public static String chatDeliver = "chatDeliver";

    public void grantAdminStatus();

    public void didEnroll(String gameId, String username);

    public void didUnenroll(String gameId, String username);

    public void userInfo(int loggedin, int enrolled, int playing, int dead,
        long ago, String text);

    public void gameInfo(GameInfo gi);

    public void gameStartsNow(String gameId, int port);

    public void gameStartsSoon(String gameId);

    public void gameStarted(String gameId);

    public void gameCancelled(String gameId, String byUser);

    public void chatDeliver(String chatId, long when, String sender,
        String message, boolean resent);
}
