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
    private java.util.List activeSocketList;
    private static final String sep = Constants.protocolTermSeparator;

    FileServerThread(java.util.List activeSocketList)
    {
        super();
        setDaemon(true);
        this.activeSocketList = activeSocketList;
        try
        {
            fileServer =
                new ServerSocket(Constants.defaultFileServerPort,
                                 Constants.MAX_MAX_PLAYERS);
        }
        catch (Exception e)
        {
            Log.error("FileServerThread : " + e);
            System.exit(1);
        }
    }
    
    public void run()
    {
        while (true)
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

                    List li = Split.split(sep, request);
                
                    String filename = (String)li.remove(0);

                    byte[] data = ResourceLoader.getBytesFromFile(filename, li, true);

                    os.write(data);
                }
                else
                {
                    Log.warn("SOMEBODY NOT A CLIENT IS TRYING TO ACCESS A FILE !");
                    Log.warn("Request was from " + fileClient);
                }
                fileClient.close();
            }
            catch (Exception e)
            {
                Log.warn("FileServerThread : " + e);
            } 
        }
    }
}
