package net.sf.colossus.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 *  some small methods helpful during development,
 *  to wait before program ends to see object instance
 *  statistics, once just until return is pressed,
 *  once a loop in which one can force GC, see object statistics,
 *  etc. until one enters "x" to make the loop exit.
 */

public class DebugMethods
{

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

    private static void sleepFor(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException ex)
        {
            // Nothing to do... 
        }
    }

    /*
     * It seems this is the sequence of things needed to achieve
     * full garbage collection/cleanup of JFrame stuff (Java 1.4.2).
     */
    public static void doCleanupStuff(boolean doSwingCleanup)
    {
        System.gc();
        System.runFinalization();

        net.sf.colossus.util.DummyFrameWithMenu.doOneDummyFrame("exitDummy");
        net.sf.colossus.util.DummyFrameWithMenu.doOneDummyFrame("exitDummy2");

        if (doSwingCleanup)
        {
            net.sf.colossus.util.DummyFrameWithMenu.swingCleanup();
        }

        System.gc();
        System.runFinalization();
    }

    /*
     * I used this when it seemed that it needs several rounds of 
     * Finalize, GC, sleep before all was away.
     * Probaby the real point was that the timer needed the 30 secs
     * before it ended...
     * so most likely this here can be totally removed.
     */
    public static void waitLoopUntilAllIsCleanedUp()
    {
        boolean debugWhetherAllIsCleanedUp = false;

        if (!debugWhetherAllIsCleanedUp)
        {
            return;
        }

        // All the code in this method is not necessary in normal use.
        // I use it during development, to track down which objects
        // were not released by the garbage collection, does the JVM 
        // end by itself (even without System.exit(), just because
        // "all non-demon threads are gone" -- as the theory says,
        // unfortunately it's not the reality ;-))

        System.gc();
        System.runFinalization();

        // create one dummy Frame, so that the static stuff in Swing
        // "forgets" the last JFrame used before (usually a MasterBoard)...
        net.sf.colossus.util.DummyFrameWithMenu.doOneDummyFrame("waitLoop");

        System.gc();
        System.runFinalization();

        sleepFor(1000);

        // Do a couple of rounds of GC, FInalization, sleep, check whether
        // now all objects gone, each time waiting little longer.
        // Often they were finally gone when i=5 or 7 or so...

        boolean allGone = net.sf.colossus.webcommon.FinalizeManager.allGone();

        if (!allGone)
        {
            System.out.println("\n\n################\n");
        }

        int i = 0;
        while (!allGone && i < 5)
        {
            System.out.println("Cleanup waiting, round " + i);
            int j;
            for (j = 0; j <= i; j++)
            {
                System.gc();
                System.runFinalization();
            }
            sleepFor((long)(i + 1) * 500);

            System.gc();
            System.runFinalization();

            allGone = net.sf.colossus.webcommon.FinalizeManager.allGone();
            if (!allGone)
            {
                net.sf.colossus.webcommon.FinalizeManager
                    .printStatistics();
            }
            i++;
        }

        if (allGone)
        {
            System.out
                .println("\n=====\nOK, all registered instances are gone!\n");
        }
        else
        {
            System.out
                .println("\n=====\nGiving up, some instances remained!\n");

            if (false)
            {
                DebugMethods.waitReturnLoop(true);
            }

            net.sf.colossus.webcommon.FinalizeManager.printStatistics();
        }

        int cnt = ViableEntityManager.getWaitingCnt();
        if (cnt > 0)
        {
            System.out
                .println("\n!!!!\nNOTE: ViableEntityManager waiting count != 0: "
                    + cnt);
        }

        // right now this InterruptAWTThreads() is necessary to do, 
        // when StartClient GUI was used - without, it will just never 
        // exit by itself -- I haven't found out yet why.
        //    .. Actually: it is, if there is some JFrames still have
        //       listeners, or something like that. At least since I
        //       use everywhere KFrames / the Swing Cleanup hack, this
        //       AWT interrupting hasn't been necessary. 
        // When StartClient GUI was not instantiated,
        // e.g. a normal game with own server, or -c with -g,
        // then this isn't necessary either.

        // Actually it's nowadays not necessary in any case
        // - at least in Java 1.4.2.
        // ViableEntityManager.InterruptAWTThreads();
    }



    
    
}
