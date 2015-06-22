package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Logger;


public class ColossusRegistry
{
    private static final Logger LOGGER = Logger
        .getLogger(ColossusRegistry.class.getName());

    public final static int DEFAULT_PORT = 1099;

    private final int port;

    public ColossusRegistry(int port)
    {
        this.port = port;
        startRegistry();
    }

    private void startRegistry()
    {
        try
        {
            LocateRegistry.createRegistry(port);
        }
        catch (RemoteException e)
        {
            LOGGER.info("ColossusRegistry: createRegistry failed - ignored.");
        }
    }

    public int getPort()
    {
        return this.port;
    }

    public static void main(String[] args)
    {
        ColossusRegistry r = new ColossusRegistry(DEFAULT_PORT);
        int p = r.getPort();
        System.out.println("main: registry now running on port " + p);
        System.out.println("      Press return to make registry terminate.");

        BufferedReader reader = new BufferedReader(new InputStreamReader(
            System.in));
        try
        {
            String textLine = reader.readLine();
            System.out.println("Got line: " + textLine);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
