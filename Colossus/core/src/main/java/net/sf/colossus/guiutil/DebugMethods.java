package net.sf.colossus.guiutil;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.sf.colossus.util.InstanceTracker;


/**
 *  some small methods helpful during development,
 *  to wait before program ends to see object instance
 *  statistics, once just until return is pressed,
 *  once a loop in which one can force GC, see object statistics,
 *  etc. until one enters "x" to make the loop exit.
 */

public class DebugMethods
{
    /**
     * It seems this is the sequence of things needed to achieve
     * full garbage collection/cleanup of JFrame stuff (Java 1.4.2):
     * GC and finalization, 2 dummy frames, do the SwingCleanup hack,
     * and once again GC and finalization.
     * 
     * @param doSwingCleanup Whether to call the swingCleanup() method
     */
    public static void doCleanupStuff(boolean doSwingCleanup)
    {
        System.gc();
        System.runFinalization();

        DummyFrameWithMenu.doOneDummyFrame("exitDummy");
        DummyFrameWithMenu.doOneDummyFrame("exitDummy2");

        if (doSwingCleanup)
        {
            // Should be only done once, the very last time in main()
            DummyFrameWithMenu.swingCleanup();
        }

        System.gc();
        System.runFinalization();
    }

    /**
     * prints out "PRESS RETURN TO CONTINUE",
     * and after return is pressed, 
     * prints then "OK, continuing" and returns.
     */
    public static void waitReturn()
    {
        System.out.println("\nPRESS RETURN TO CONTINUE!");
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                System.in));
            in.readLine();
        }
        catch (IOException e)
        {
            //  
        }
        System.out.println("OK, continuing.");
    }

    /**
     * Repeatedly checks whether now all tracked objects are gone.
     * Each round, does GC + finalize, and asks for input.
     * For input "s", prints statistics.
     * For input "x", exits the loop.
     * Exits the loop also if all are gone. 
     * If from beginning on all are gone, does not even go into the loop.
     * 
     * @param force Go into loop even if allGone already returns true at begin
     */
    public static void waitReturnLoop(boolean force)
    {
        System.gc();
        System.runFinalization();
        InstanceTracker.printStatistics();

        if (InstanceTracker.allGone())
        {
            if (!force)
            {
                return;
            }
        }

        System.out
            .println("Enter s, g, f, h or x. x exits the loop.\n----------\n");
        int cnt = 2;
        String line = "";
        boolean done = false;
        while (!done)
        {
            System.gc();
            System.runFinalization();

            try
            {
                System.out.print("> ");
                BufferedReader in = new BufferedReader(new InputStreamReader(
                    System.in));
                line = in.readLine();

                if (line == null)
                {
                    line = "x";
                }

                if (line.equals("h"))
                {
                    SwingReferenceCleanupHacks.cleanupJPopupMenuGlobals(true);
                    SwingReferenceCleanupHacks.cleanupJMenuBarGlobals();
                }

                if (line.equals("f"))
                {
                    DummyFrameWithMenu f1 = new DummyFrameWithMenu("" + cnt);
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

                    InstanceTracker.printStatistics();
                }

                if (line.equals("x"))
                {
                    done = true;
                }

                if (InstanceTracker.allGone())
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
        System.out
            .println("ok, list empty or x entered... finishing shutdown...");
    }
}
