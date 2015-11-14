package net.sf.colossus.webserver;


import java.nio.charset.Charset;


/**
 *  Class WebServerConstants holds constants related to
 *  the Web server, so far only for the config file.
 *
 *  @author Clemens Katzer
 */
public final class WebServerConstants
{

    public final static String defaultOptionsFilename = "WebServer.cf";

    /** for now, only used for chat messages storage file(s) */
    public final static String optDataDirectory = "DataDirectory";

    // keys for the options inside the Web Server options file:
    public final static String optServerPort = "ServerPort";
    public final static String optSocketQueueLen = "SocketQueueLen";
    public final static String optMaxUsers = "MaxUsers";
    public final static String optMaxClients = "MaxClients";
    public final static String optPortRangeFrom = "PortRangeFrom";
    public final static String optAvailablePorts = "AvailablePorts";

    public final static String optLoginMessageFile = "LoginMessageFile";
    public final static String optUsersFile = "UsersFile";
    public final static String optGamesFile = "GamesFile";
    public final static String DEFAULT_GAMES_FILE = "games.dat";

    public final static String optJavaCommand = "JavaCommand";
    public final static String optColossusJar = "ColossusJar";
    public final static String optLogPropTemplate = "LogPropTemplate";
    public final static String optWorkFilesBaseDir = "WorkFilesBaseDir";
    public final static String optStatisticsBaseDir = "StatisticsBaseDir";

    // Mail sending related options / cf file entries:
    public final static String optMailServer = "MailServer";
    public final static String optMailFromAddress = "MailFromAddress";
    public final static String optMailFromName = "MailFromName";
    public final static String optMailThisServer = "ThisServer";
    public final static String optMailContactEmail = "ContactEmail";
    public final static String optMailContactWWW = "ContactWWW";
    public final static String optMailReallyMail = "MailReallyMail";
    public final static String optMailToFile = "MailToFile";

    public final static String optContactAdminFromName = "ContactAdminFromName";
    public final static String optContactAdminFromMail = "ContactAdminFromMail";
    public final static String optContactAdminToName = "ContactAdminToName";
    public final static String optContactAdminToMail = "ContactAdminToMail";

    /**
     *  How many messages does chat server cache,
     *  for redisplay to those who just log in
     */
    public final static int keepLastNMessages = 50;

    // TODO also defined in webclient.WebClientSocketThread!
    public final static Charset charset = Charset.forName("UTF-8");
}
