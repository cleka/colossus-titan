package net.sf.colossus.webserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;


/** This class keeps track of which ports are currently occupied
 *  by ongoing games and which are free for new games.
 *
 *  @version $Id$
 *  @author Clemens Katzer
 */

public class PortBookKeeper
{
    private static final Logger LOGGER = Logger.getLogger(PortBookKeeper.class
        .getName());

    private final int portRangeFrom;
    private final int availablePorts;

    private final boolean[] portUsed;

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
                        LOGGER.log(Level.SEVERE, "port " + tryPort
                            + " is supposed to be free "
                            + "but test shows it is in use?");
                    }
                }
            }
        }
        LOGGER.log(Level.FINEST, "reserving port " + port);

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
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, "testThatPortReallyFree IOException "
                + "while attempting to open", e);
        }

        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, "testThatPortReallyFree IOException "
                + "while attempting to close", e);
        }
        return ok;
    }

    public void releasePort(int port)
    {
        int index = port - portRangeFrom;
        if (index < 0 || index > availablePorts)
        {
            LOGGER.log(Level.WARNING, "attempt to release invalid port "
                + port + " (index = " + index + ")!");
        }
        else
        {
            portUsed[index] = false;
        }
    }
}
