package net.sf.colossus.webcommon;


/** Interface for what WebClient sends to WebServer 
 *
 *  @version $Id$
 *  @author Clemens Katzer
 */

public interface IWebServer
{
    public static String Login = "login";
    public static String Logout = "logout";
    public static String RegisterUser = "register";
    public static String ConfirmRegistration = "confirm";
    public static String ChangePassword = "changepassword";
    public static String Propose = "propose";
    public static String Enroll = "enroll";
    public static String Unenroll = "unenroll";
    public static String Start = "start";
    public static String Cancel = "cancel";
    public static String ChatSubmit = "chatsubmit";
    public static String ShutdownServer = "shutdownserver";

    public static String generalChatName = "#general";

    // login() is treated specially

    // register is treated specially, too:
    // public String registerUser(String username, String password, String email);

    // does not really return a GI object over socket, but method needs
    // that return value (internally) when used in server side.
    public GameInfo proposeGame(String initiator, String variant,
        String viewmode, long startAt, int duration, String summary,
        String expire, boolean unlimMulli, boolean balTowers,
        int min, int target, int max);

    public void enrollUserToGame(String gameId, String username);

    // the next 3 would not really need to send the user over network,
    // since at server side the socketthread would know it; but the
    // interface to between socket and server needs it...
    // so we handle it "properly" also on client side, even if
    // functionally unnecessary.
    public void unenrollUserFromGame(String gameId, String username);

    public void cancelGame(String gameId, String byUser);

    public void startGame(String gameId);

    public void chatSubmit(String chatId, String sender, String message);

    // public void submitAnyText(String text);

    public void shutdownServer();

    public String changeProperties(String username, String oldPW,
        String newPW, String email, Boolean isAdminObj);

    public void logout();

}
