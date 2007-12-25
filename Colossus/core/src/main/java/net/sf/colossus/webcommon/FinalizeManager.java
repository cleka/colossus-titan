package net.sf.colossus.webcommon;


import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;


/**
 *  Objects can register here during their constructor and
 *  and unregister when their finalizer is run. This class 
 *  keeps then track which instances are still running.
 *   
 *  One can configure for which classes one is interested
 *  in seeing the created/finalized/delta.
 *   
 *  This is meant for debug/development purposes, to verify
 *  that cleanup is usually done properly, and not threads
 *  or objects stay unintentionally referenced and never
 *  get garbagecollected. 
 *  So while editing/testing, one sets the "which one to see" 
 *  to the ones one want to monitor, but in productive use, 
 *  i.e. when checked in to svn, this should be set to
 *  "don't show anything".
 *  (perhaps with Java 1.5 or 1.6, or a good debugger one can
 *  achieve the same effect, but I don't know how ;-)
 *   
 *  @version $Id$
 *  @author Clemens Katzer
 *    
 */

public class FinalizeManager
{
    private static WeakHashMap instanceGroups = new WeakHashMap();

    private static HashSet interestedIn = new HashSet();
    // private static String prefix = "net.sf.colossus.";

    private static boolean interestedInAll = true;

    // if interestedInAll is false, then it registers only those
    // put into the HashSet with the block below:
    static
    {
        interestedIn.add("net.sf.colossus.server.Player");

        interestedIn.add("net.sf.colossus.client.Client");
        interestedIn.add("net.sf.colossus.client.SocketClientThread");
        interestedIn.add("net.sf.colossus.client.MasterBoard");
        interestedIn.add("net.sf.colossus.client.SimpleAI");

        interestedIn.add("net.sf.colossus.util.KFrame");
        interestedIn.add("net.sf.colossus.client.BattleMap");
        interestedIn.add("net.sf.colossus.client.ShowReadme");
        interestedIn.add("net.sf.colossus.client.ShowHelpDoc");

        interestedIn.add("net.sf.colossus.server.Start");
        interestedIn.add("net.sf.colossus.server.GetPlayers");
        interestedIn.add("net.sf.colossus.server.Game");
        interestedIn.add("net.sf.colossus.server.Server");
        interestedIn.add("net.sf.colossus.server.SocketServerThread");

        interestedIn.add("net.sf.colossus.util.KDialog");
        interestedIn.add("net.sf.colossus.client.StatusScreen");

        interestedIn.add("net.sf.colossus.client.WebClient");
    }

    public static synchronized void register(Object o, String id)
    {
        String type = o.getClass().getName();
        if ((interestedIn.contains(type) || interestedInAll)
        //              &&  !type.equals("net.sf.colossus.util.DummyFrameWithMenu") 
        )
        {
            // System.out.println("Registering object of type " + type + " with id " + id);
            if (instanceGroups.containsKey(type))
            {
                // System.out.println("Adding to existing group " + type);
                FinalizeClassGroup group = (FinalizeClassGroup)instanceGroups
                    .get(type);
                group.addInstance(o, id);
            }
            else
            {
                // System.out.println("Creating new group for " + type);
                FinalizeClassGroup group = new FinalizeClassGroup(type);
                group.addInstance(o, id);
                instanceGroups.put(type, group);
            }
        }
        else
        {
            // System.out.println("NOT registering object of type " + type + " with id " + id);
        }
    }

    public static synchronized void setId(Object o, String id)
    {
        String type = o.getClass().getName();
        // String shortType = FinalizeClassGroup.shortType(type);

        if (interestedIn.contains(type))
        {
            // System.out.println("FinalizeManager.setId(): One object of type " + 
            //        shortType + " changes ID to '" + id + "'");
        }

        // nothing needs to be actually done, it will disappear by itself
        // from the WeakHashMap...

        if (instanceGroups.containsKey(type))
        {
            FinalizeClassGroup group = (FinalizeClassGroup)instanceGroups
                .get(type);
            FinalizeClassGroup.typeInstance i = group.getInstance(o);
            if (i != null)
            {
                i.setId(id);
            }
        }
    }

    public static synchronized void unregister(Object o)
    {
        String type = o.getClass().getName();
        //        String shortType = FinalizeClassGroup.shortType(type);

        //        System.out.println("finalize(): One object of type " + shortType + " unregisters...");

        // nothing needs to be actually done, it will disappear by itself
        // from the WeakHashMap...

        if (instanceGroups.containsKey(type))
        {
            //            FinalizeClassGroup group = (FinalizeClassGroup) instanceGroups.get(type);
            //            group.removeInstance(o);
            //            group.printStatistics(false);
        }
    }

    public static synchronized void printStatistics(boolean detailed)
    {
        System.out.println("==========\nObject instances statistics:");
        Iterator it = instanceGroups.keySet().iterator();
        while (it.hasNext())
        {
            String type = (String)it.next();
            FinalizeClassGroup group = (FinalizeClassGroup)instanceGroups
                .get(type);
            // System.out.println(group.toString());
            group.printStatistics(detailed);
        }
        System.out.println("\n");
    }

    public static synchronized boolean allGone()
    {
        Iterator it = instanceGroups.keySet().iterator();
        while (it.hasNext())
        {
            String type = (String)it.next();
            FinalizeClassGroup group = (FinalizeClassGroup)instanceGroups
                .get(type);
            if (group.amountLeft() != 0)
            {
                return false;
            }
        }
        return true;
    }

}
