package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.IGameManager;
import net.sf.colossus.webcommon.IManagedGame;


/** This "manager" is accessible via RMI (served by the ColossusRegistry
 *  object) for the games started in own processes.
 *  Once a game comes up, it can register to here, telling it's game Id,
 *  and reporting anything that happens which is of interest to the server;
 *  for example, this could be things like player died etc.
 *
 *  Additionally the server can communicate with the games, because it
 *  gets their game Id, and looks up the ManagedGame remote object for it.
 *
 *  TODO: work just started (2011-04-24)
 *
 *
 *  @author Clemens Katzer
 */

public class GameManager implements IGameManager
{
    private static final Logger LOGGER = Logger.getLogger(GameManager.class
        .getName());

    public final static String OBJ_ID = "IGameManager";

    private final HashMap<String, IManagedGame> games;

    private ColossusRegistry r;

    private Registry registry;

    public GameManager()
    {
        games = new HashMap<String, IManagedGame>();

        LOGGER.info("List games created.");

        initRegistryIfNeeded();
        LOGGER.info("Registry created.");

        doGetRegistry();

        registerManager();
        LOGGER.info("Game Manager <init>: registration to registry done.");

        listRegistryContents();

    }

    private void initRegistryIfNeeded()
    {
        r = new ColossusRegistry(ColossusRegistry.DEFAULT_PORT);
        int p = r.getPort();
        LOGGER.info("main: registry now running on port " + p);
    }

    private void doGetRegistry()
    {
        try
        {
            registry = LocateRegistry.getRegistry();
        }
        catch (RemoteException e)
        {
            LOGGER.log(Level.SEVERE, "registerManager: " + e);
        }
    }

    public void listRegistryContents()
    {
        try
        {
            String[] list = registry.list();
            System.out.println(Arrays.asList(list));
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "list() failed: ", e);
        }
    }

    private void registerManager()
    {
        try
        {
            IGameManager stub = (IGameManager)UnicastRemoteObject
                .exportObject(this, 0);
            registry.bind(OBJ_ID, stub);
            LOGGER.info("Manager registered to registry.");
        }
        catch (RemoteException e)
        {
            System.err.println("registerManager: " + e.toString());
            e.printStackTrace();
        }
        catch (AlreadyBoundException e)
        {
            System.err.println("registerManager: " + e.toString());
            e.printStackTrace();
        }
    }

    public void unregisterManager()
    {
        try
        {
            registry.unbind(OBJ_ID);
            LOGGER.info("GameManager unregistered.");
        }
        catch (RemoteException e)
        {
            LOGGER.log(Level.SEVERE, "unregisterManager: RemoteException ", e);
        }
        catch (NotBoundException e)
        {
            LOGGER.log(Level.SEVERE, "unregisterManager: NotBound ", e);
        }
    }

    // Mostly useful for main()
    public int getGameCount()
    {
        return games.size();
    }

    public void tellEvent(String description) throws RemoteException
    {
        System.out.println("Game told us: " + description);

    }

    public void registerGame(String gameId) throws RemoteException,
        NotBoundException
    {
        System.out.println("Game " + gameId + " registered on server.");
        IManagedGame gameStub = lookupGameByRegid(gameId);
        games.put(gameId, gameStub);
    }

    public void unregisterGame(String gameId) throws AccessException,
        NotBoundException, RemoteException
    {

        lookupGameByRegid(gameId);
        games.remove(gameId);
    }

    private IManagedGame lookupGameByRegid(String gameId)
        throws AccessException, NotBoundException, RemoteException
    {
        IManagedGame game = null;

        game = (IManagedGame)registry.lookup(gameId);
        String status = game.tellStatus();
        System.out.println("Got Status from game " + gameId + ": '" + status
            + "'");

        return game;
    }

    /**
     *  This is only for testing purposes.
     *  Normally the GameManager object is created by WebServer object.
     */
    public static void main(String[] args)
    {

        final GameManager gm = new GameManager();

        Runnable runny = new Runnable()
        {
            public void run()
            {
                exitOnReturn(gm);
            }
        };
        new Thread(runny).start();

        while (true)
        {
            int count = gm.getGameCount();
            System.out.println("Game count: " + count);
            sleepFor(5000);
        }
    }

    // Helper for main()
    private static void sleepFor(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            LOGGER.log(Level.FINEST,
                "InterruptException caught... ignoring it...");
        }
    }

    // Helper for main()
    private static void exitOnReturn(GameManager gm)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
            String textLine = reader.readLine();
            System.out.println("Got line: " + textLine);
            gm.unregisterManager();
            System.exit(0);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
