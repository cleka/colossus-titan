package net.sf.colossus.server;


public interface INotifyWebServer
{

    public abstract boolean isActive();

    public abstract void readyToAcceptClients();

    public abstract void gotClient(String playerName, boolean remote);

    public abstract void allClientsConnected();

    public abstract void gameStartupCompleted();

    public abstract void serverStoppedRunning();

}