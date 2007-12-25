package net.sf.colossus.util;


import java.util.WeakHashMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


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
 *  Now, each of those tells the SystemExitManager "I'm done",
 *  and if the last one says so, THEN the System.exit() is 
 *  actually executed.
 *  
 *  @version $Id$
 *  @author Clemens Katzer
 */

public class SystemExitManager
{

    private static WeakHashMap viableEntities = new WeakHashMap();

    public static int waiting = 0;

    private static int exitCode = 0;

    public SystemExitManager()
    {
        // nothing to do
    }

    public static synchronized void register(Object viableEntity, String name)
    {
        viableEntities.put(viableEntity, name);
        // System.out.println("SystemExitManager: now " + viableEntities.size() + " entities registered.");
    }

    public static synchronized void doSystemExitMaybe(Object viableEntity, int exitCode)
    {
        SystemExitManager.exitCode = exitCode;

        if (viableEntities.containsKey(viableEntity))
        {
            viableEntities.remove(viableEntity);
            if (viableEntities.isEmpty())
            {
                // System.out.println("\n\nSystemExitManager: " + 
                //      "last viable entitity is gone!!");

                boolean debug = true;

                if (debug)
                {
                    System.gc();
                    System.runFinalization();
                }

                notifyThatAllGone();
            }
        }

        // int count = viableEntities.size();
        // String list = viableEntities.values().toString();
        // System.out.println("SystemExitManager: now " + count + " entities registered: " + list);
    }

    public int getLastExitCode()
    {
        return SystemExitManager.exitCode;
    }

    private static Object mutex = new Object();

    private static void notifyThatAllGone()
    {
        synchronized (mutex)
        {
            mutex.notify();
        }
    }

    public static void waitUntilAllGone()
    {
        synchronized (viableEntities)
        {
            if (viableEntities.isEmpty())
            {
                // System.out.println("waitUntilAllGone: viableEntities already empty! - returning.");
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
                System.out.println("SystemExitManager.waitUntilAllGone(): InterruptedException!");
            }
        }
    }

    public static int getWaitingCnt()
    {
        return waiting;
    }

    // ===================================================================
    //                         D E B U G   S T U F F

    /*
     * Those below (InterruptAWTThreads() and  waitReturn*() )
     * are for debugging/development purposes.
     * Due to a bug in AWT, an application that ever did
     * getDefaultToolkit(), will never return to the prompt
     * by itself (i.e. without System.exit()) because
     * the AWT threads are not demons and don't end by themselves.
     * ==> non-demon threads still running => JVM does not shutdown.
     * 
     *  With this here, can try whether the application otherwise
     *  has cleaned up all, stopped all other (own) socket threads,
     *  see all finalizers, etc.
     */


    /* Actually, since I use the SwingCleanup stuff,
     * I did not need to do this interrupt thing any more...
     */

    /*
     public static void InterruptAWTThreads()
     {
     System.out.println("@@@ start interrupting the AWT's");
     ThreadGroup tg = Thread.currentThread().getThreadGroup(); 
     
     Thread[] list = new Thread[20];
     int cnt = tg.enumerate(list);
     
     int i;
     for ( i=0 ; i < cnt ; i++)
     {
     Thread t = list[i];
     String name = t.getName();
     if (name.startsWith("AWT-Shutdow")
     || name.startsWith("AWT-EventQueue") )
     {
     System.out.println("@@@ Interrupting " + name);
     t.interrupt();
     }
     }
     System.out.println("@@@ after interrupting the AWT's");
     }
     */

    public static void waitReturn()
    {
        System.out.println("\nPRESS RETURN TO CONTINUE!");
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
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

        System.out.println("SystemExitManager.waitReturn(), run by thread " +
            Thread.currentThread().getName());
        System.out.println("\n----------\nStart.main() has done it's job\n");

        System.gc();
        System.runFinalization();
        net.sf.colossus.webcommon.FinalizeManager.printStatistics(false);

        System.out.println("Press return to proceed with cleanup...\n----------\n");
        int cnt = 2;
        String line = "";
        boolean done = false;
        while (!done)
        {
            System.gc();
            System.runFinalization();

            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                line = in.readLine();

                if (line.equals("h"))
                {
                    SwingReferenceCleanupHacks.CleanupJPopupMenuGlobals(true);
                    SwingReferenceCleanupHacks.CleanupJMenuBarGlobals();
                }

                if (line.equals("f"))
                {
                    net.sf.colossus.util.DummyFrameWithMenu f1 =
                        new net.sf.colossus.util.DummyFrameWithMenu(""+cnt);
                    cnt++;

                    try { Thread.sleep(500);
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

                    net.sf.colossus.webcommon.FinalizeManager.printStatistics(false);
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
        System.out.println("ok, list empty or x entered... finishing shutdown...");
    }

    //                         E N D   D E B U G    S T U F F
    // ===================================================================

}
