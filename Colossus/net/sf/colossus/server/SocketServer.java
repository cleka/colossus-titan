package net.sf.colossus.server;


import java.util.*;
import java.net.*;
import java.io.*;

import net.sf.colossus.util.Log;


/**
 *  Server-side socket handler
 *  @version $Id$
 *  @author David Ripton
 */


final class SocketServer
{
    private Server server;
    private ServerSocket serverSocket;
    private Socket [] clientSockets = new Socket[Constants.MAX_PLAYERS];
    private int numClients = 0;
    private int maxClients;

    SocketServer(Server server, int port, int maxClients)
    {
Log.debug("new SocketServer maxClients = " + maxClients);
        this.server = server;
        this.maxClients = maxClients;

        try
        {
            serverSocket = new ServerSocket(port);
        }
        catch (IOException ex)
        {
            Log.error(ex.toString());
            return;
        }

        server.addLocalClients();

        while (numClients < maxClients)
        {
            waitForConnection();
        }
    }

    private void waitForConnection()
    {
        Socket clientSocket = null;
        try
        {
            clientSocket = serverSocket.accept();
Log.debug("Got client connection");
            clientSockets[numClients] = clientSocket;
            numClients++;
        }
        catch (IOException ex)
        {
            Log.error(ex.toString());
            return;
        }

        new SocketServerThread(server, clientSocket).start();
    }
}
