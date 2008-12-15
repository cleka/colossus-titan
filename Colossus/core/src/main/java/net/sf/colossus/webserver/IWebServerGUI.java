package net.sf.colossus.webserver;


public interface IWebServerGUI
{
    public abstract void setUserInfo(String s);

    public abstract void setScheduledGamesInfo(String s);

    public abstract void setInstantGamesInfo(String s);

    public abstract void setRunningGamesInfo(String s);

    public abstract void setEndingGamesInfo(String s);

    public abstract void cleanup();

    public abstract void dispose();
}
