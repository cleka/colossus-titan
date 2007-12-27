package net.sf.colossus.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  Keeps track of "viable entities", i.e. parts of the program
 *  for which it makes sense to have them living on their own.
 *  Viable entities so far are :
 *  - the game server part with it's subthreads,
 *  - the user Client with MasterBoard (e.g. as remote client)
 *  - the web server client
 *  The reason is, earlier each of them may at some point do
 *  System.exit(), even if one would have liked to keep one
 *  other part open. (for example, 3 local players -- if one 
 *  was dead and you closed that MasterBoard, the whole application
 *  did exit.
 *  
 *  Now, each of those tells the ViableEntityManager "I'm done",
 *  and if the last one says so, THEN the System.exit() is 
 *  actually executed. Or rather, nowadays, the main() thread
 *  can go on, come up with a menu again or something.
 *  
 *  @version $Id$
 *  @author Clemens Katzer
 */

public class ViableEntityManager
{
    private static final Logger LOGGER =
        Logger.getLogger(ViableEntityManager.class.getName());

    private static boolean debug = false;

    private static WeakHashMap viableEntities = new WeakHashMap();
    private static int waiting = 0;
    private static Object mutex = new Object();
    
    public static synchronized void register(Object viableEntity, String name)
    {
        viableEntities.put(viableEntity, name);
        LOGGER.log(Level.FINEST, "ViableEntityManager: now " +
            viableEntities.size() + " entities registered.");
    }

    public static synchronized void unregister(Object viableEntity)
    {
        if (viableEntities.containsKey(viableEntity))
        {
            viableEntities.remove(viableEntity);
            if (viableEntities.isEmpty())
            {
                LOGGER.log(Level.FINEST, 
                    "\n\nSystemExitManager: last viable entity is gone!!");

                // notify that all gone:
                synchronized (mutex)
                {
                    mutex.notify();
                }
            }
        }

        if (debug)
        {
            int count = viableEntities.size();
            String list = viableEntities.values().toString();
            LOGGER.log(Level.FINEST, "ViableEntityManager: now " + count +
                " entities registered: " + list);
        }
    }

    public static int getWaitingCnt()
    {
        return waiting;
    }
    
    public static void waitUntilAllGone()
    {
        synchronized (viableEntities)
        {
            if (viableEntities.isEmpty())
            {
                LOGGER.log(Level.FINEST,
                    "waitUntilAllGone: viableEntities already empty! " + 
                    "- returning.");
                return;
            }
        }

        synchronized (mutex)
        {
            try
            {
                waiting++;
                mutex.wait();
                waiting--;
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.WARNING,
                    "waitUntilAllGone(): interrupted!", e);
            }
        }
    }


    // ===================================================================
    //                   D E B U G    S T U F F

    public static void waitReturn()
    {
        System.out.println("\nPRESS RETURN TO CONTINUE!");
        try
        {
            BufferedReader in =
                new BufferedReader(new InputStreamReader(System.in));
            in.readLine();
        }
        catch (IOException e)
        {
            //  
        }
        System.out.println("OK, continuing.");
    }

    public static void waitReturnLoop(boolean force)
    {
        if (net.sf.colossus.webcommon.FinalizeManager.allGone())
        {
            if (!force)
            {
                return;
            }
        }

        System.out.println("ViableEntityManager.waitReturn(), run by thread " +
            Thread.currentThread().getName());
        System.out.println("\n----------\nStart.main() has done it's job\n");

        System.gc();
        System.runFinalization();
        net.sf.colossus.webcommon.FinalizeManager.printStatistics();

        System.out.println(
            "Press return to proceed with cleanup...\n----------\n");
        int cnt = 2;
        String line = "";
        boolean done = false;
        while (!done)
        {
            System.gc();
            System.runFinalization();

            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                    System.in));
                line = in.readLine();

                if (line.equals("h"))
                {
                    SwingReferenceCleanupHacks.CleanupJPopupMenuGlobals(true);
                    SwingReferenceCleanupHacks.CleanupJMenuBarGlobals();
                }

                if (line.equals("f"))
                {
                    net.sf.colossus.util.DummyFrameWithMenu f1 = new net.sf.colossus.util.DummyFrameWithMenu(
                        "" + cnt);
                    cnt++;

                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException ex)
                    {
                        //
                    }

                    f1.dispose();
                    f1 = null;
                }

                if (line.equals("g"))
                {
                    System.gc();
                    System.runFinalization();
                }

                if (line.equals("s"))
                {
                    System.gc();
                    System.runFinalization();

                    net.sf.colossus.webcommon.FinalizeManager
                        .printStatistics();
                }

                if (line.equals("x"))
                {
                    done = true;
                }

                if (net.sf.colossus.webcommon.FinalizeManager.allGone())
                {
                    System.out.println("OK, allGone now true, we can stop.");
                    done = true;
                }
            }
            catch (IOException e)
            {
                //  
            }
            System.gc();
            System.runFinalization();
        }
        System.out.println(
            "ok, list empty or x entered... finishing shutdown...");
    }

    //                         E N D   D E B U G    S T U F F
    // ===================================================================
}
