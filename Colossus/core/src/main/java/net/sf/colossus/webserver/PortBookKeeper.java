package net.sf.colossus.webserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IPortProvider;


/**
 *  This class keeps track of which ports are currently occupied
 *  by ongoing games and which are free for new games.
 *
 *  @author Clemens Katzer
 */
public class PortBookKeeper implements IPortProvider
{
    private static final Logger LOGGER = Logger.getLogger(PortBookKeeper.class
        .getName());

    private final int portRangeFrom;

    /**
     * total nr of ports we are allowed to use according to options file;
     * but only every 2nd is used as a game port
     */
    private final int totalPorts;

    /** Nr of ports that are actually available for game serving
     *  (so, this value == 5 means there can be 5 games)
     */
    private final int gamePorts;

    /** Bookkeeping which (game) ports are currently in use
     */
    private final ArrayList<GameInfo> portInUse;

    /**
     * A placeholder for the bookkeping table, if it's somehow used but we
     * don't know by what or whom
     */
    private final GameInfo NOT_A_REAL_GAME = new GameInfo("00000", true);

    public PortBookKeeper(int portRangeStart, int availablePorts)
    {
        this.portRangeFrom = portRangeStart;
        this.totalPorts = availablePorts;
        portInUse = new ArrayList<GameInfo>(totalPorts);
        for (int i = 0; i < availablePorts; i++)
        {
            portInUse.add(i, null);
        }

        int freePorts = 0;

        for (int i = 0; i < availablePorts; i += 2)
        {
            int port = realPortForIndex(i);
            boolean free = testWhetherPortFree(port);
            if (free)
            {
                markPortFree(port);
                freePorts++;
            }
            else
            {
                LOGGER.warning("Free port table initialization: Port " + port
                    + " seems to be in use! Marking it as in use.");
                markPortUsed(port, NOT_A_REAL_GAME);
            }
        }

        this.gamePorts = freePorts;
    }

    private int realPortForIndex(int portIndex)
    {
        return portRangeFrom + portIndex;
    }

    private int indexForRealPort(int portNumber)
    {
        return portNumber - portRangeFrom;
    }

    private void markPortUsed(int portNr, GameInfo gi)
    {
        portInUse.set(indexForRealPort(portNr), gi);
    }

    private void markPortFree(int portNr)
    {
        portInUse.set(indexForRealPort(portNr), null);
    }

    private GameInfo getGameAtPort(int portNr)
    {
        return portInUse.get(indexForRealPort(portNr));
    }

    private boolean isPortInUse(int portNr)
    {
        return getGameAtPort(portNr) != null;
    }

    /**
     * Get a free port number, chosen randomly; to reduce the risk
     * that a resumed game gets same port => clients from suspended
     * game trying to connect to this new one.
     */
    public int getFreePort(GameInfo gi)
    {
        Random rand = new Random();
        int offset = rand.nextInt(totalPorts) * 2;

        String purpose = "game " + gi.getGameId();
        int port = -1;
        synchronized (portInUse)
        {
            for (int i = 0; i < totalPorts && port == -1; i += 2)
            {
                int j = (i + offset) % totalPorts;
                int tryPort = realPortForIndex(j);
                if (!isPortInUse(tryPort))
                {
                    boolean ok = testThatPortReallyFree(tryPort);
                    if (ok)
                    {
                        markPortUsed(tryPort, gi);
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

    public int calculateUsedPorts()
    {
        return totalPorts / 2 - countFreePorts();
    }

    public int countFreePorts()
    {
        int free = 0;
        synchronized (portInUse)
        {
            for (int i = 0; i < totalPorts; i += 2)
            {
                int tryPort = realPortForIndex(i);
                if (!isPortInUse(tryPort))
                {
                    boolean ok = testThatPortReallyFree(tryPort);
                    if (ok)
                    {
                        free++;
                    }
                    else
                    {
                        LOGGER.log(Level.SEVERE, "countFreePorts: port "
                            + tryPort + " is supposed to be free "
                            + "but test shows it is in use?");
                    }
                }
            }
        }
        return free;
    }

    /** Check that it's really free, as expected, log a warning if not */
    private boolean testThatPortReallyFree(int port)
    {
        if (!testWhetherPortFree(port))
        {
            LOGGER.warning("Port " + port
                + " is supposed to be free but it is not!");
            markPortUsed(port, NOT_A_REAL_GAME);
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
        synchronized (portInUse)
        {
            for (int i = 0; i < totalPorts; i += 2)
            {
                if (!isPortInUse(realPortForIndex(i)))
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
        for (int i = 0; i < totalPorts; i += 2)
        {
            int port = realPortForIndex(i);
            boolean shouldBeFree = isPortInUse(port);
            boolean free = testWhetherPortFree(port);
            if (free != shouldBeFree)
            {
                LOGGER.warning("Port " + port + " was marked as "
                    + (shouldBeFree ? "free" : "not free")
                    + " but actually it is " + (free ? "free" : "not free")
                    + "! Updating table.");
                if (free)
                {
                    markPortFree(port);
                }
                else
                {
                    markPortUsed(port, NOT_A_REAL_GAME);
                }
            }
        }

    }

    public void releasePort(GameInfo gi)
    {
        int port = gi.getPort();
        String purpose = "game " + gi.getGameId();

        synchronized (portInUse)
        {
            GameInfo supposedGI = portInUse.get(indexForRealPort(port));

            int index = indexForRealPort(port);
            if (index < 0 || index > totalPorts)
            {
                LOGGER.log(Level.WARNING, "attempt to release invalid port "
                    + port + " (index = " + index + ")!");
            }
            else if (supposedGI == null)
            {
                LOGGER.log(Level.WARNING, "attempt to release port " + port
                    + " (" + purpose
                    + ") but port book keeper has not marked port as used!");
            }
            else if (supposedGI != gi)
            {
                LOGGER.log(Level.WARNING, "attempt to release port " + port
                    + " (" + purpose
                    + ") but port book keeper thinks it's used by "
                    + "a different game: " + supposedGI.getGameId());
            }
            else if (!testWhetherPortFree(port))
            {
                LOGGER.log(Level.WARNING, "attempt to release port " + port
                    + " (" + purpose
                    + ") but test indicates that it is still in use!");
            }
            else
            {
                markPortFree(port);
                LOGGER.info("Released port " + port + " (" + purpose + ")");
            }
        }
    }

    private String buildPortTableReport()
    {
        StringBuilder sb = new StringBuilder("");
        synchronized (portInUse)
        {
            for (int i = 0; i < totalPorts; i += 2)
            {
                int tryPort = realPortForIndex(i);
                if (sb.length() != 0)
                {
                    sb.append(", ");
                }
                sb.append(tryPort + ": ");
                GameInfo gi = getGameAtPort(tryPort);
                if (gi == null)
                {
                    sb.append("free");
                }
                else
                {
                    sb.append(gi.getGameId());
                }
            }
        }
        return sb.toString();
    }

    public String getStatus()
    {
        StringBuilder st = new StringBuilder();
        st.append("Ports configured/available for games: " + totalPorts + "/"
            + gamePorts + "; still free for games: " + countFreePorts() + "\n");
        st.append("Port usage: " + buildPortTableReport());
        st.append("\n");

        return st.toString();
    }
}
