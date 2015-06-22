package net.sf.colossus.server;


import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.IGameManager;
import net.sf.colossus.webcommon.IManagedGame;
import net.sf.colossus.webserver.GameManager;


public class ManagedGame implements IManagedGame
{
    private static final Logger LOGGER = Logger.getLogger(ManagedGame.class
        .getName());

    private final String id;

    private Registry registry;

    private IGameManager gm;

    /** True if registration to registry was successful */
    private boolean bound = false;

    public ManagedGame(String id) throws RemoteException
    {
        this.id = id;
        LOGGER.info("ManagedGame for id " + id + " instantiated.");

        try
        {
            initRegistry();
        }
        catch (RemoteException e)
        {
            LOGGER.log(Level.SEVERE, "Can't locate registry!", e);
            throw e;
        }
    }

    public String tellStatus() throws RemoteException
    {
        return "It's real!";

    }

    public String getRegistryId()
    {
        return "ManagedGame-" + id;
    }

    private void initRegistry() throws RemoteException
    {
        registry = LocateRegistry.getRegistry();
    }

    public boolean getBound()
    {
        return bound;
    }

    /** Register this managed game to rmi registry so that GameManager can
     *  can find it from there via it's game id.
     *
     * @return An exception indicating a failure, null if all is ok.
     */
    Exception registerToRegistry()
    {
        Exception gotException = null;
        try
        {
            // NOTE: !!!
            // When I use the exportObject without port argument, it returns
            // something different and/or the object can NOT be unexported!
            // In the API for UnicastRemoteObject constructor is defined,
            // port zero means anonymous ports; this is NOT specified at the
            // exportObject methods, but seems to be the case nevertheless.
            // ==> in future, use with port 0 or perhaps in future some
            // own defined port?
            Remote stub = UnicastRemoteObject.exportObject(this, 0);
            registry.bind(getRegistryId(), stub);

            bound = true;
            LOGGER.info("OK: Registered to registry...");
        }
        catch (AlreadyBoundException e)
        {
            gotException = e;
            LOGGER.log(Level.SEVERE, "Id " + getRegistryId()
                + "Already bound?", e);
        }
        catch (RemoteException e)
        {
            gotException = e;
            LOGGER.log(Level.SEVERE, "Id " + getRegistryId()
                + "Remote exception.", e);
        }
        return gotException;
    }

    void unregisterFromRegistry() throws AccessException, NotBoundException,
        RemoteException
    {
        registry.unbind(getRegistryId());
        UnicastRemoteObject.unexportObject(this, false);
        bound = false;
    }

    /** Register with own GameId-based RegistryId to the GameManager.
     *
     */
    void registerToGameManager() throws AccessException, NotBoundException,
        RemoteException
    {
        gm = (IGameManager)registry.lookup(GameManager.OBJ_ID);
        gm.registerGame(getRegistryId());
    }

    void unregisterFromGameManager() throws AccessException, RemoteException,
        NotBoundException
    {
        gm.unregisterGame(getRegistryId());
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        Exception gotException = null;

        try
        {
            ManagedGame mg = new ManagedGame("1234");
            mg.registerToRegistry();

            LOGGER.info("Now registering game to manager...");
            mg.registerToGameManager();

            LOGGER.info("Sleeping 7 seconds...");
            sleepFor(7000);

            LOGGER.info("Now UNregistering from game manager...");
            mg.unregisterFromGameManager();

            LOGGER.info("Now UNregistering from registry...");
            mg.unregisterFromRegistry();
        }

        catch (AccessException e)
        {
            gotException = e;
        }
        catch (NotBoundException e)
        {
            gotException = e;
        }
        catch (RemoteException e)
        {
            gotException = e;
        }

        if (gotException != null)
        {
            LOGGER.log(Level.SEVERE, "main: exception " + gotException);
            System.exit(1);
        }

        System.out.println("OK, main() ends now.");

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

}
