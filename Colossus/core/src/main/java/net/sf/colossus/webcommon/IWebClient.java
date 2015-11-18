package net.sf.colossus.webcommon;


/**
 *  Interface for what WebServer (Public Game Server) sends to WebClient
 *
 *  @author Clemens Katzer
 */
public interface IWebClient
{
    public static final String alreadyLoggedIn = "alreadyLoggedIn";
    public static final String grantAdmin = "grantAdmin";
    public static final String tooManyUsers = "tooManyUsers";
    public static final String connectionClosed = "connectionClosed";
    public static final String forcedLogout = "forcedLogout";
    public static final String didEnroll = "didEnroll";
    public static final String didUnenroll = "didUnenroll";
    public static final String gameInfo = "gameInfo";
    public static final String userInfo = "userInfo";
    public static final String tellOwnInfo = "tellOwnInfo";
    public static final String gameStarted = "gameStarted";
    public static final String gameStartsNow = "gameStartsNow";
    public static final String gameStartsSoon = "gameStartsSoon";
    public static final String gameCancelled = "gameCancelled";
    public static final String chatDeliver = "chatDeliver";
    public static final String generalMessage = "generalMessage";
    public static final String systemMessage = "systemMessage";
    public static final String requestAttention = "requestAttention";
    public static final String watchGameInfo = "watchGameInfo";
    public static final String pingRequest = "pingRequest";

    public void grantAdminStatus();

    public void didEnroll(String gameId, String username);

    public void didUnenroll(String gameId, String username);

    public void userInfo(int loggedin, int enrolled, int playing, int dead,
        long ago, String text);

    public void gameInfo(GameInfo gi);

    public void gameStartsNow(String gameId, int port, String hostingHost,
        int inactivityCheckInterval, int inactivityWarningInterval,
        int inactivityTimeout);

    public void gameStartsSoon(String gameId, String startUser);

    public void gameCancelled(String gameId, String byUser);

    public void chatDeliver(String chatId, long when, String sender,
        String message, boolean resent);

    public void connectionReset(boolean forcedLogout);

    public int getClientVersion();

    public void deliverGeneralMessage(long when, boolean error, String title,
        String message);

    public void systemMessage(long when, String message);

    public void requestAttention(long when, String byUser, boolean byAdmin,
        String message, int beepCount, long beepInterval, boolean windows);

    public void watchGameInfo(String gameId, String host, int port);

    public void tellOwnInfo(String email);
}
