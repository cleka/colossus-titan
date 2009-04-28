package net.sf.colossus.client;


import net.sf.colossus.server.IServer;


/**
 * Generic type of connection to the server. Right now we have only
 * Socket-based connection (SocketClientThread); but the ClientThread
 * should become unaware of the type of connection, only do the
 * messageString-to-method-call and vice-versa translation,
 * and the connection specific parts (read/write to socket or via e.g.
 * a queue) to the ServerConnection class(es).
 */
public interface IServerConnection
{
    public String getReasonFail();

    public void startThread();

    public void updateThreadName(String playerName);

    public IServer getIServer();

    public boolean isAlreadyDown();

    public void stopSocketClientThread();

}