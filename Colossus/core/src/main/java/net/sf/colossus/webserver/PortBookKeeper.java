package net.sf.colossus.webserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  This class keeps track of which ports are currently occupied
 *  by ongoing games and which are free for new games.
 *
 *  @author Clemens Katzer
 */
public class PortBookKeeper
{
    private static final Logger LOGGER = Logger.getLogger(PortBookKeeper.class
        .getName());

    private final int portRangeFrom;

    /** total nr of free ports according to options file;
     * but only every 2nd is used as a game port
     */
    private final int availablePorts;

    /** Nr of ports that are actually currently available for game serving
     *  (so, this value == 5 means there can be 5 games)
     */
    private final int freeGamePorts;

    /** Bookkeeping which (game) ports are currently in use
     */
    private final boolean[] portUsed;

    public PortBookKeeper(int portRangeFrom, int availablePorts)
    {
        this.portRangeFrom = portRangeFrom;
        this.availablePorts = availablePorts;

        portUsed = new boolean[availablePorts];
        int freePorts = 0;

        for (int i = 0; i < availablePorts; i += 2)
        {
            int port = portRangeFrom + i;
            boolean free = testWhetherPortFree(port);
            if (free)
            {
                portUsed[i] = false;
                freePorts++;
            }
            else
            {
                LOGGER.warning("Free port table initialization: Port " + port
                    + " seems to be in use! Marking it as in use.");
                portUsed[i] = true;
            }
        }

        this.freeGamePorts = freePorts;
    }

    public int getFreeGamePortsCount()
    {
        return freeGamePorts;
    }

    public int getFreePort(String purpose)
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

        if (port > 0)
        {
            LOGGER
                .log(Level.INFO, "Reserved port " + port + " for " + purpose);
        }

        ensureSomeFreePortsRemain();

        return port;
    }

    /** Check that it's really free, as expected, log a warning if not */
    private boolean testThatPortReallyFree(int port)
    {
        if (!testWhetherPortFree(port))
        {
            LOGGER.warning("Port " + port
                + " is supposed to be free but it is not!");
            portUsed[port] = true;
            return false;
        }
        else
        {
            return true;
        }
    }

    /** Just check it, whether it's free or not */
    private boolean testWhetherPortFree(int port)
    {
        boolean ok = false;
        ServerSocket serverSocket = null;
        try
        {
            serverSocket = new ServerSocket(port, 1);
            serverSocket.setReuseAddress(true);
            serverSocket.close();
            ok = true;
        }
        catch (IOException e)
        {
            LOGGER.info("Caught IOException "
                + "while attempting to creating socket on port " + port);

            String msg = e.getMessage();
            if (msg == null || !msg.equals("Address already in use: JVM_Bind"))
            {
                LOGGER.log(Level.WARNING,
                    "Unrecognized exception while checking port " + port
                        + " whether it is free: ", e);
            }
        }

        return ok;
    }

    private void ensureSomeFreePortsRemain()
    {
        int seemsFree = 0;
        synchronized (portUsed)
        {
            for (int i = 0; i < availablePorts; i += 2)
            {
                if (!portUsed[i])
                {
                    seemsFree++;
                }
            }
        }
        if (seemsFree < 3)
        {
            LOGGER.info("Only " + seemsFree
                + " ports are registered as free. Rechecking...");

            reCheckPorts();
        }
    }

    private void reCheckPorts()
    {
        for (int i = 0; i < availablePorts; i += 2)
        {
            int port = portRangeFrom + i;
            boolean shouldBeFree = portUsed[i];
            boolean free = testWhetherPortFree(port);
            if (free != shouldBeFree)
            {
                LOGGER.warning("Port " + port + " was marked as "
                    + (shouldBeFree ? "free" : "not free")
                    + " but actually it is " + (free ? "free" : "not free")
                    + "! Updating table.");
                portUsed[i] = free;
            }
        }

    }

    public void releasePort(int port, String purpose)
    {
        int index = port - portRangeFrom;
        if (index < 0 || index > availablePorts)
        {
            LOGGER.log(Level.WARNING, "attempt to release invalid port "
                + port + " (index = " + index + ")!");
        }
        else if (!testWhetherPortFree(port))
        {
            LOGGER.log(Level.WARNING, "attempt to release port " + port + " ("
                + purpose + ") but test indicates that it is still in use!");
        }
        else
        {
            portUsed[index] = false;
            LOGGER.info("Released port " + port + " (" + purpose + ")");
        }
    }
}
