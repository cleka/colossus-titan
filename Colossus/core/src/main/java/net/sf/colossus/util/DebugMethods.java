package net.sf.colossus.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 *  two small methods helpful during development,
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
}
