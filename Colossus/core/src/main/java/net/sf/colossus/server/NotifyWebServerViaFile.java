package net.sf.colossus.server;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * For communication between Game/Server and WebServer.
 *
 * Perhaps later replaced with a two-way socket connection?
 * Class is always created, no matter whether we have a web
 * server ( => active == true) or not ( => active == false);
 * but this way, we can have all the
 *    "if (we have a web server) { } "
 * checking done inside this class and do not clutter the
 * main server code.
 */

public class NotifyWebServerViaFile implements INotifyWebServer
{
    private static final Logger LOGGER = Logger
        .getLogger(NotifyWebServerViaFile.class.getName());

    private final String flagFilename;
    private PrintWriter out;
    private File flagFile = null;
    // Do we even have a web server to notify at all?
    private final boolean active;
    private boolean suspended = false;

    public NotifyWebServerViaFile(String name)
    {
        if (name != null && !name.equals(""))
        {
            flagFilename = name;
            active = true;
        }
        else
        {
            flagFilename = null;
            active = false;
        }
    }

    public boolean isActive()
    {
        return active;
    }

    public void readyToAcceptClients()
    {
        if (active)
        {
            createFlagfile();
        }
    }

    public void gotClient(String playerName, boolean remote)
    {
        if (active)
        {
            out.println((remote ? "Remote" : "Local") + " client connected: "
                + playerName);
        }
    }

    public void allClientsConnected()
    {
        if (active)
        {
            out.println(ALL_CLIENTS_CONNECTED);
        }
    }

    public void gameStartupCompleted()
    {
        if (active)
        {
            out.println(GAME_STARTUP_COMPLETED);
        }
    }

    public void gameStartupFailed(String reason)
    {
        if (active)
        {
            out.println(GAME_STARTUP_FAILED + reason);
            gameIsSuspended();
        }
    }

    public void serverStoppedRunning()
    {
        if (active)
        {
            if (suspended)
            {
                LOGGER.finest("Server stopped running and suspended "
                    + "set; no need to remove a file.");
            }
            else
            {
                removeFlagfile();
            }
        }
    }

    private void createFlagfile()
    {
        if (active)
        {
            flagFile = new File(flagFilename);
            try
            {
                out = new PrintWriter(new FileWriter(flagFile), true);
            }
            catch (IOException e)
            {
                LOGGER.log(Level.SEVERE,
                    "Could not create web server flag file " + flagFilename
                        + "!!", (Throwable)null);
            }
        }
    }

    private void removeFlagfile()
    {
        LOGGER.info("removeFlagFile called??");
        if (active)
        {
            try
            {
                out.close();
                flagFile.delete();
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE,
                    "Could not delete web server flag file " + flagFilename
                        + "!!" + e.toString(), (Throwable)null);
            }
        }
    }

    public void gameIsSuspended()
    {
        suspended = true;
        if (active)
        {
            renameFlagfile(flagFilename + ".suspended");
        }
    }

    private void renameFlagfile(String suspendedFilename)
    {
        try
        {
            out.close();
            flagFile.renameTo(new File(suspendedFilename));
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Could not rename web server flag file "
                + flagFilename + " to new name " + suspendedFilename + "!!"
                + e.toString(), (Throwable)null);
        }
    }

} // END Class NotifyWebServerViaFile
