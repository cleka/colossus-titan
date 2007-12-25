package net.sf.colossus.webserver;


/** Class WebServerConstants holds constants related to 
 *  the Web server, so far only for the config file.
 *  
 *  @version $Id$
 *  @author Clemens Katzer
 */

final class WebServerConstants
{

    public final static String defaultOptionsFilename = "WebServer.cf";

    // keys for the options inside the Web Server options file:
    public final static String optServerPort = "ServerPort";
    public final static String optSocketQueueLen = "SocketQueueLen";
    public final static String optMaxUsers = "MaxUsers";
    public final static String optMaxClients = "MaxClients";
    public final static String optPortRangeFrom = "PortRangeFrom";
    public final static String optAvailablePorts = "AvailablePorts";

    public final static String optUsersFile = "UsersFile";

    public final static String optJavaCommand = "JavaCommand";
    public final static String optColossusJar = "ColossusJar";
    public final static String optLogPropTemplate = "LogPropTemplate";

    public final static String optWorkFilesBaseDir = "WorkFilesBaseDir";

    // how many messages does chat server cache, for redisplay to
    // those who just log in:
    public final static int keepLastNMessages = 10;
}
