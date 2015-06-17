package net.sf.colossus.webserver;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.webcommon.IGameManager;


/**
 * JUnit tests for Remote calls between Game and Game manager.
 *
 * @author Clemens Katzer
 */

public class GameManagerTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(GameManagerTest.class
        .getName());


    private GameManager gmRealObject;

    private Registry registry;


    public GameManagerTest(String name)
    {
        super(name);

        // this.cr = new ColossusRegistry(1099);

        LOGGER.info("GameManagerTest object instantiated.");
    }

    public void testCreateAndClearOneGM()
    {

        LOGGER.info("Creating new GameManager");
        gmRealObject = new GameManager();
        assertTrue(gmRealObject != null);

        LOGGER.info("Looking up GameManager from remote");
        IGameManager gotGm = getRemoteGm();
        assertNotNull(gotGm);

        LOGGER.info("\n\n-----\nCleaning up again...");
        clearRemoteGm();

    }


    public IGameManager getRemoteGm()
    {
        IGameManager gotGm = null;
        try
        {
            registry = LocateRegistry.getRegistry();
            gotGm = (IGameManager)registry.lookup(GameManager.OBJ_ID);
        }
        catch (NotBoundException e)
        {
            System.err.println("NotBound exception: " + e.toString());
            // e.printStackTrace();
        }

        catch (RemoteException e)
        {
            System.err.println("RemoteText exception: " + e.toString());
            // e.printStackTrace();
        }
        return gotGm;
    }

    public void clearRemoteGm()
    {
        try
        {
            registry.unbind(GameManager.OBJ_ID);
        }
        catch (RemoteException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NotBoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

