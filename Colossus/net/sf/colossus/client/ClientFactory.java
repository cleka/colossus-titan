package net.sf.colossus.client;


import java.rmi.*;
import net.sf.colossus.server.IRMIServer;
import net.sf.colossus.util.Log;


/**
 *  public Client factory.  
 *  @version $Id$
 *  @author David Ripton
 */


public class ClientFactory
{
    public static IRMIClient createClient(IRMIServer server, String playerName,
        boolean primary)
    {
        try
        {
            Client client = new Client(server, playerName, primary);
            return (IRMIClient)client;
        }
        catch (RemoteException e)
        {
            Log.error("ClientFactory().createClient()");
            e.printStackTrace();
        }
        return null;
    }
}
