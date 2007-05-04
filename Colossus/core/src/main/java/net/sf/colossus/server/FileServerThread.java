package net.sf.colossus.server;


import java.util.*;
import java.net.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.ResourceLoader;


/**
 * Thread handling the distribution of files to clients.
 * @version $Id$
 * @author Romain Dolbeau
 */

final class FileServerThread extends Thread
{
    private ServerSocket fileServer;
    private List activeSocketList;
    private static final String sep = Constants.protocolTermSeparator;

    private boolean keepGoingOn = true;

    FileServerThread(java.util.List activeSocketList, int port)
    {
        super();
        setDaemon(true);
        this.activeSocketList = activeSocketList;
        try
        {
            fileServer =
                    new ServerSocket(port,
                    Constants.MAX_MAX_PLAYERS);
        }
        catch (Exception e)
        {
            Log.error("FileServerThread : " + e);
            System.exit(1);
        }
    }

    public void stopGoingOn()
    {
        keepGoingOn = false;
    }

    public void run()
    {
        while (keepGoingOn)
        {
            try
            {
                Socket fileClient = fileServer.accept();

                InetAddress requester = fileClient.getInetAddress();

                boolean knownIP = false;

                synchronized (activeSocketList)
                {
                    Iterator it = activeSocketList.iterator();
                    while (it.hasNext() && !knownIP)
                    {
                        InetAddress cIP = ((Socket)it.next()).getInetAddress();
                        knownIP = requester.equals(cIP);
                    }
                }

                if (knownIP)
                {
                    InputStream is = fileClient.getInputStream();

                    BufferedReader in =
                            new BufferedReader(new InputStreamReader(is));

                    String request = in.readLine();

                    OutputStream os = fileClient.getOutputStream();

                    Log.debug("Serving request " + request +
                            " from " + fileClient);
                    
                    boolean ignoreFail = false;

                    List li = Split.split(sep, request);

                    String filename = (String)li.remove(0);
                     
                    // right now (05/2007) clients should not send this -
                    // take into use somewhat later.
                    if (filename.equals(Constants.fileServerIgnoreFailSignal))
                    {
                        ignoreFail = true;
                        filename = (String)li.remove(0);
                    }

                    // Meanwhile, suppress the warnings at least for the README
                    // and the markersFile...
                    // we know that they usually print warnings for other
                    // than "Default" dir.

                    // @TODO: e.g. when the overnext public build is out
                    // (the one right now (20.4.2007) does not contain this yet), 
                    // make the client one day submit the ignorefail signal, 
                    // and remove this markersFileName/README temporary hack.
                    if (filename.startsWith(Constants.markersNameFile) ||
                        filename.startsWith("README") )
                    {
                        ignoreFail = true;
                    }
                    
                    byte[] data = ResourceLoader.getBytesFromFile(filename, li,
                            true, ignoreFail);

                    if (data != null)
                    {
                        os.write(data);
                    }
                    // else we just write nothing.
                }
                else
                {
                    Log.warn("SOMEBODY NOT A CLIENT "
                        +"IS TRYING TO ACCESS A FILE !");
                    Log.warn("Request was from " + fileClient);
                }
                fileClient.close();
            }
            catch (Exception e)
            {
                Log.warn("FileServerThread : " + e);
            }
        }

        Log.debug("FileServerThread is done");

        try
        {
            fileServer.close();
        }
        catch (Exception e)
        {
            Log.warn("FileServerThread : " + e + " while closing socket");
        }
    }
}
