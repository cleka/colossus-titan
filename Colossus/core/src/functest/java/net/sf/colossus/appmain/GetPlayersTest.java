package net.sf.colossus.appmain;


import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.server.VariantSupport;


public class GetPlayersTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(GetPlayersTest.class
        .getName());

    private final long MUTEX_WAIT_TIMEOUT = 2000;

    private GetPlayers gp;
    private Object mutex;
    private Options options;

    public GetPlayersTest(String testName)
    {
        super(testName);
    }

    public void testGetPlayersSetOneHuman()
    {
        LOGGER.info("Setting player type 0 to human");
        System.err.println("Setting player type 0 to human");

        gp.setPlayerType(0, Constants.human);

        synchronized (mutex)
        {
            Runnable virtualUser = new Runnable()
            {
                public void run()
                {
                    /*
                     Yeah, that is dirty. During developing the test
                     I use the below stuff, so I can modify the GetPlayers
                     window (e.g. put something else in a name or type field)
                     to verify that the test case would really fail.
                    */
                    /*
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e)
                    {
                        //
                    }
                    */
                    // this simulates "the user presses New Game button"
                    runInEDT();
                }
            };

            // Start a runnable that clicks the "New Game" button
            // and by that (hopefully) does dispose, which again
            // will notify us in the wait below.
            // Note that it can't notify us before we are in wait()
            // below because we still own the mutex monitor :)

            new Thread(virtualUser).start();
            try
            {
                mutex.wait(MUTEX_WAIT_TIMEOUT);
            }
            catch (InterruptedException e)
            {
                //
            }
        }

        // Only true if notify was done; would be false if wait() in our
        // Runnable came back because of the timeout.
        //
        boolean wasProperlyNotified = gp.getMutexNotified();
        System.err.println("wasProperlyNotified " + wasProperlyNotified);

        assert wasProperlyNotified : "GetPlayers did not notify mutex, wait() timed out!";

        // Get back data from options object and verify they are as expected
        int i = 0;
        String name = options.getStringOption(Options.playerName + i);
        String type = options.getStringOption(Options.playerType + i);

        String shouldBeName = System.getProperty("user.name",
            Constants.byColor);

        LOGGER.info("Got player name 0 as " + name + "; expected would be "
            + shouldBeName);

        assertTrue(type != null && type.equals(Constants.human));
        assertTrue(name != null && !name.equals(Constants.none));
        assertTrue(name != null && name.equals(shouldBeName));
    }

    public void runInEDT()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            clickNewGame();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        clickNewGame();
                    }
                });
            }
            catch (InvocationTargetException e)
            {
                LOGGER.log(Level.SEVERE,
                    "Failed to run setupClientGUI with invokeAndWait(): ", e);
            }
            catch (InterruptedException e2)
            {
                LOGGER.log(Level.SEVERE,
                    "Failed to run setupClientGUI with invokeAndWait(): ", e2);
            }
        }
    }

    public void clickNewGame()
    {
        gp.doNewGame();
    }

    @Override
    protected void setUp()
    {
        VariantSupport.loadVariantByName("Default", true);

        Options startOptions = new Options(Constants.OPTIONS_START);
        WhatNextManager whatNextMgr = new WhatNextManager(startOptions);

        options = new Options("UnitTest", true);
        mutex = new Object();

        gp = new GetPlayers(options, mutex, whatNextMgr, false);
    }
}
