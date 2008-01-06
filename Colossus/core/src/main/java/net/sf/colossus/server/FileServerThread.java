package net.sf.colossus.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.ChildThreadManager;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Split;


/**
 * Thread handling the distribution of files to clients.
 * @version $Id$
 * @author Romain Dolbeau
 */

final class FileServerThread extends Thread
{
    private static final Logger LOGGER = Logger
        .getLogger(FileServerThread.class.getName());

    private ServerSocket fileServer;
    private List<Socket> activeSocketList;
    private ChildThreadManager threadMgr;

    private static final String sep = Constants.protocolTermSeparator;

    private int port;
    private boolean keepGoingOn = true;

    FileServerThread(java.util.List<Socket> activeSocketList, int port,
        ChildThreadManager mgr)
    {
        super();
        setDaemon(true);
        this.activeSocketList = activeSocketList;
        this.port = port;
        this.threadMgr = mgr;
        try
        {
            fileServer = new ServerSocket(port, Constants.MAX_MAX_PLAYERS);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Can not open server socket", e);
            System.exit(1);
        }
    }

    /*
     * Set the flag goingOn to false, and makes a dmumy connection to get
     * the fileserverthread out of the accept() call.
     */
    public void stopGoingOn()
    {
        keepGoingOn = false;
        makeDummyConnection();
    }

    public void makeDummyConnection()
    {
        // make a dummy connection, to get the thread out of the
        // accept().

        try
        {
            Socket socket = new Socket("localhost", port);
            socket.close();
        }
        // UnknownHostException, IOException, IllegalBlockingModeException
        catch (Exception e)
        {
            LOGGER
                .log(Level.SEVERE, "FileServerThread: " + e, (Throwable)null);
        }
    }

    public void run()
    {
        threadMgr.registerToThreadManager(this);
        net.sf.colossus.webcommon.InstanceTracker.register(this, "only 1");
        try
        {
            while (keepGoingOn)
            {
                try
                {
                    Socket fileClient = fileServer.accept();
                    if (!keepGoingOn)
                    {
                        break;
                    }

                    InetAddress requester = fileClient.getInetAddress();

                    boolean knownIP = false;

                    synchronized (activeSocketList)
                    {
                        Iterator<Socket> it = activeSocketList.iterator();
                        while (it.hasNext() && !knownIP)
                        {
                            InetAddress cIP = it.next()
                                .getInetAddress();
                            knownIP = requester.equals(cIP);
                        }
                    }

                    if (knownIP)
                    {
                        InputStream is = fileClient.getInputStream();

                        BufferedReader in = new BufferedReader(
                            new InputStreamReader(is));

                        String request = in.readLine();

                        OutputStream os = fileClient.getOutputStream();

                        LOGGER.log(Level.FINEST, "Serving request " + request
                            + " from " + fileClient);

                        boolean ignoreFail = false;

                        List li = Split.split(sep, request);

                        String filename = (String)li.remove(0);

                        // right now (05/2007) clients should not send this -
                        // take into use somewhat later.
                        if (filename
                            .equals(Constants.fileServerIgnoreFailSignal))
                        {
                            ignoreFail = true;
                            filename = (String)li.remove(0);
                        }

                        // Meanwhile, suppress the warnings at least for the
                        // README and the markersFile...
                        // we know that they usually print warnings for other
                        // than "Default" dir.

                        // @TODO: e.g. when the overnext public build is out
                        // (the one right now (20.4.2007) does not contain
                        // this yet), make the client one day submit the 
                        // ignorefail signal, and remove this
                        //  markersFileName/README temporary hack.
                        if (filename.startsWith(Constants.markersNameFile)
                            || filename.startsWith("README"))
                        {
                            ignoreFail = true;
                        }

                        byte[] data = ResourceLoader.getBytesFromFile(
                            filename, li, true, ignoreFail);

                        if (data != null)
                        {
                            os.write(data);
                        }
                        // else we just write nothing.
                    }
                    else
                    {
                        LOGGER.log(Level.WARNING, "SOMEBODY NOT A CLIENT "
                            + "IS TRYING TO ACCESS A FILE !");
                        LOGGER.log(Level.WARNING, "Request was from "
                            + fileClient);
                    }
                    fileClient.close();
                }
                catch (Exception e)
                {
                    LOGGER.log(Level.WARNING, "FileServerThread : "
                        + e.toString());
                }
            }

            LOGGER.log(Level.FINEST, "FileServerThread is done");

            try
            {
                fileServer.close();
            }
            catch (IOException e)
            {
                LOGGER.log(Level.WARNING, "FileServerThread : " + e
                    + " while closing socket");
            }
        }

        // catch whatever it be, to make sure the unregister is done
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "FileServerThread : " + e
                + " outer try/catch block");
        }

        threadMgr.unregisterFromThreadManager(this);
        threadMgr = null;
    }
}
