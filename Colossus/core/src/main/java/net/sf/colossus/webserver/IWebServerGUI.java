package net.sf.colossus.webserver;


/**
 *  Interface for the operations the WebServer can do with it's GUI
 *
 *  @author Clemens Katzer
 */
public interface IWebServerGUI
{
    public abstract void setUserInfo(String s);

    public abstract void setScheduledGamesInfo(String s);

    public abstract void setInstantGamesInfo(String s);

    public abstract void setRunningGamesInfo(String s);

    public abstract void setEndingGamesInfo(String s);

    public abstract void setSuspendedGamesInfo(String string);

    public abstract void setUsedPortsInfo(String string);

    public abstract void shutdown();
}
