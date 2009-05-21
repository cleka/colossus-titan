package net.sf.colossus.webcommon;


/**
 *  Interface for classes that can run/supervise a WebServer Game.
 *  E.g. RunGameInOwnJVM and there should be also RunGameInSameJVM (tbd.)
 */

public interface IGameRunner
{
    public void start();

    boolean makeRunningGame();

    int getHostingPort();

    String getHostingHost();

    void setServerNull();

    /* Waits until socket is up, i.e. game is ready to accept clients.
     */
    boolean waitUntilReadyToAcceptClients(int timeout);

    boolean waitUntilGameStartedSuccessfully(int timeout);
}