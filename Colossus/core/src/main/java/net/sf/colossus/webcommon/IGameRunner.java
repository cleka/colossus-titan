package net.sf.colossus.webcommon;


/**
 *  Interface for classes that can run/supervise a WebServer Game.
 *  Implemented by RunGameInOwnJVM and RunGameInSameJVM.
 *
 *  @author Clemens Katzer
 */
public interface IGameRunner
{
    public void start();

    boolean makeRunningGame();

    int getHostingPort();

    String getHostingHost();

    public boolean tryToStart();

    /**
     *  Waits until socket is up, i.e. game is ready to accept clients.
     */
    boolean waitUntilReadyToAcceptClients(int timeout);

    boolean waitUntilGameStartedSuccessfully(int timeout);
}