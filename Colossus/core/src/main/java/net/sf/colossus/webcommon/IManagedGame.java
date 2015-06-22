package net.sf.colossus.webcommon;


import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IManagedGame extends Remote
{
    public String tellStatus() throws RemoteException;

}
