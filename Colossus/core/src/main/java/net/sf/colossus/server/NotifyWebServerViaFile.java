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
            out.println("Client (type " + (remote ? "remote" : "local")
                + ") connected: " + playerName);
            out.flush();
        }
    }

    public void allClientsConnected()
    {
        if (active)
        {
            out.println("All clients connected");
            out.flush();
        }
    }

    public void gameStartupCompleted()
    {
        if (active)
        {
            out.println("Game Startup Completed");
            out.flush();
        }
    }

    public void serverStoppedRunning()
    {
        if (active)
        {
            removeFlagfile();
        }
    }

    private void createFlagfile()
    {
        if (active)
        {
            flagFile = new File(flagFilename);
            try
            {
                // flagFile.createNewFile();
                out = new PrintWriter(new FileWriter(flagFile));
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
        out.close();
        if (active)
        {
            try
            {
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
} // END Class NotifyWebServerViaFile
