package net.sf.colossus.webserver;


import java.io.IOException;
import java.net.ServerSocket;


/** This class keeps track of which ports are currently occupied
 *  by ongoing games and which are free for new games.
 *
 *  @version $Id$
 *  @author Clemens Katzer
 */

public class PortBookKeeper
{
    private int portRangeFrom;
    private int availablePorts;

    private boolean[] portUsed;

    public PortBookKeeper(int portRangeFrom, int availablePorts)
    {
        this.portRangeFrom = portRangeFrom;
        this.availablePorts = availablePorts;

        portUsed = new boolean[availablePorts];

        for (int i = 0; i < availablePorts; i++)
        {
            portUsed[i] = false;
        }
    }

    public int getFreePort()
    {
        // System.out.println("searching a free port...");

        int port = -1;
        synchronized (portUsed)
        {
            for (int i = 0; i < availablePorts && port == -1; i += 2)
            {
                if (!portUsed[i])
                {
                    int tryPort = portRangeFrom + i;
                    boolean ok = testThatPortReallyFree(tryPort);
                    if (ok)
                    {
                        portUsed[i] = true;
                        port = tryPort;
                    }
                    else
                    {
                        System.out
                            .println("SEVERE: port "
                                + tryPort
                                + " is supposed to be free but test shows it is in use...");
                    }
                }
            }
        }
        System.out.println("reserving port " + port);

        return port;
    }

    private boolean testThatPortReallyFree(int port)
    {
        boolean ok = false;
        ServerSocket serverSocket = null;
        try
        {
            serverSocket = new ServerSocket(port, 1);
            serverSocket.setReuseAddress(true);
            ok = true;
        }
        catch (IOException ex)
        {
            System.out.println("testThatPortReallyFree: IOException!");

        }

        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        }
        catch (IOException ex)
        {
            System.out
                .println("testThatPortReallyFree: anyway trying to close, "
                    + "got: IOException!" + ex.toString());

        }
        return ok;
    }

    public void releasePort(int port)
    {
        int index = port - portRangeFrom;
        if (index < 0 || index > availablePorts)
        {
            System.out.println("ERROR: attempt to release invalid port "
                + port + " (index = " + index + ")!");
        }
        else
        {
            // System.out.println("Releasing port " + port);
            portUsed[index] = false;
        }
    }

    public void finalize()
    {
        // System.out.println("finalize(): " + this.getClass().getName());
    }
}
