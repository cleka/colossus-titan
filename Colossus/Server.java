import java.util.*;
import java.net.*;

/**
 *  Class Server lives on the server side and handles all communcation with
 *  the clients.  It talks to the server classes locally, and to the Clients
 *  via the network protocol.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Server
{
    public static final int port = 1969;
    private Game game;

    // XXX How do we keep track of which client goes with which player?
    // Sort by tower number when the game starts?

    /** For now we'll keep a list of client refs locally rather than using
     *  the network protocol.  We will eventually instead keep a list of the
     *  existing socket connections to use. Maybe also save things like
     *  the originating IP, in case a connection breaks and we need to
     *  authenticate reconnects.  Do not share these references. */
    private ArrayList clients = new ArrayList();


    public Server(Game game)
    {
        this.game = game;
    }


    /** Temporary.  We will not use direct client refs later. */
    public void addClient(Client client)
    {
        clients.add(client);
    }

    // TODO Set up one thread per client?

    // TODO Need a scheme to broadcast a message to all clients, versus
    // sending one to one client.  Use a list of client numbers for each
    // message, so that we can send a message to two players, not just
    // one or all?

    // TODO Event handling scheme to catch messages from clients.  Parsing
    // scheme to turn them into method calls.

    /** Temporary convenience method until all client-side classes no longer
     *  need game refs. */
    public Game getGame()
    {
        return game;
    }


    public void allUpdateStatusScreen()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.updateStatusScreen();
        }
    }

    public void allShowMovementRoll(int roll)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.showMovementRoll(roll);
        }
    }

    public void allClearAllCarries()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.clearAllCarries();
        }
    }

    // XXX temp
    public void loadOptions()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.loadOptions();
        }
    }

    /** XXX do not use */
    public Client getClient(String playerName)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            if (playerName.equals(client.getPlayerName()))
            {
                return client;
            }
        }
        return null;
    }

    /** Don't use this. */
    public Client getClient(int playerNum)
    {
        return (Client)clients.get(playerNum);
    }


    public boolean getClientOption(String playerName, String optname)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            return client.getOption(optname);
        }
        return false;
    }

    public boolean getClientOption(String optname)
    {
        Client client = getClient(0);
        if (client != null)
        {
            return client.getOption(optname);
        }
        return false;
    }

    public String getClientStringOption(String playerName, String optname)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            return client.getStringOption(optname);
        }
        return "false";
    }

    /** XXX temp */
    public void setClientOption(String playerName, String optname,
        boolean value)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            client.setOption(optname, value);
        }
    }


    public static void main(String [] args)
    {
        System.out.println("Not implemented yet");
    }
}
