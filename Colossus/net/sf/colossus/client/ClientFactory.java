package net.sf.colossus.client;


import net.sf.colossus.server.IServer;


/**
 *  public Client factory.  
 *  @version $Id$
 *  @author David Ripton
 */


public class ClientFactory
{
    public static IClient createClient(IServer server, String playerName, 
        boolean primary)
    {
        Client client = new Client(server, playerName, primary);
        return (IClient)client;
    }
}
