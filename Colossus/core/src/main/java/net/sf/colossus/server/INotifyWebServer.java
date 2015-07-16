package net.sf.colossus.server;


public interface INotifyWebServer
{

    public final String ALL_CLIENTS_CONNECTED = "All clients connected";

    public final String GAME_STARTUP_COMPLETED = "Game Startup Completed";

    public final String GAME_STARTUP_FAILED = "Game Startup Failed! Reason: ";

    public abstract boolean isActive();

    public abstract void readyToAcceptClients();

    public abstract void gotClient(String playerName, boolean remote);

    public abstract void allClientsConnected();

    public abstract void gameStartupCompleted();

    public abstract void gameStartupFailed(String reason);

    public abstract void serverStoppedRunning();

    public abstract void gameIsSuspended();

}