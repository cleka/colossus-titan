package net.sf.colossus.webcommon;


import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IGameManager extends Remote
{
    public void tellEvent(String description) throws RemoteException;

    public void registerGame(String gameId) throws RemoteException,
        NotBoundException;

    public void unregisterGame(String gameId) throws AccessException,
        NotBoundException, RemoteException;
}
