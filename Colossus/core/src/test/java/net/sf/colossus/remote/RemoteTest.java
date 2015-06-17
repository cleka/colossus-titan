package net.sf.colossus.remote;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.server.ManagedGame;
import net.sf.colossus.webcommon.IManagedGame;
import net.sf.colossus.webserver.ColossusRegistry;


/**
 * JUnit tests for Remote calls between Game and Game manager.
 *
 * @author Clemens Katzer
 */

public class RemoteTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(RemoteTest.class
        .getName());

    private final int TEST_PORT = 1295;

    private final ColossusRegistry registry;

    public RemoteTest(String name)
    {
        super(name);
        registry = new ColossusRegistry(TEST_PORT);
        int p = registry.getPort();
        LOGGER.info("RemoteTest: registry running on port " + p);
    }

    private ManagedGame g;
    private IManagedGame stub;

    public void registerGame()
    {
        try
        {
            g = new ManagedGame("1234");
            stub = (IManagedGame)UnicastRemoteObject.exportObject(g,
                0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(TEST_PORT);
            registry.rebind("IManagedGame", stub);

            LOGGER.info("Server ready");
        }
        catch (RemoteException e)
        {
            LOGGER.log(Level.WARNING, "RemoteText exception: ", e);
        }
    }

    public void findGame()
    {
        String message = null;
        IManagedGame stub;
        try
        {
            Registry registry = LocateRegistry.getRegistry(TEST_PORT);
            stub = (IManagedGame) registry.lookup("IManagedGame");
            message = stub.tellStatus();
            LOGGER.info("Got status " + message);
        }
        catch (Exception e)
        {
            System.err.println("RemoteTest exception: " + e.toString());
            e.printStackTrace();
        }
        assertTrue(message != null);

    }

    public void testRegisterAndFindGame()
    {
        registerGame();
        findGame();
    }
}

