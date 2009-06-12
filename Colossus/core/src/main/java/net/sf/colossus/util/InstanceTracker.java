package net.sf.colossus.util;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  Objects can register here when they are created.
 *
 *  This class notices when it an object is garbage collected
 *  and keeps then track which instances are still running/alive.
 *
 *  One can configure for which classes one is interested
 *  in seeing the created/removed delta.
 *
 *  This is meant for debug/development purposes, to verify
 *  that cleanup is usually done properly, and not threads
 *  or objects stay unintentionally referenced and never
 *  get garbage collected.
 *
 *  So while editing/testing, one sets the "which one to see"
 *  to the ones one want to monitor, but in productive use,
 *  i.e. when checked in to svn, this should be set to
 *  "don't show anything".
 *  (perhaps with Java 1.5 or 1.6, or a good debugger one can
 *  achieve the same effect, but I don't know how ;-)
 *
 *  @author Clemens Katzer
 */
public class InstanceTracker
{
    private static final Logger LOGGER = Logger
        .getLogger(InstanceTracker.class.getName());

    private static Map<String, InstanceGroup> instanceGroups = new WeakHashMap<String, InstanceGroup>();

    private static Set<String> interestedIn = new HashSet<String>();
    // private static String prefix = "net.sf.colossus.";

    private static boolean interestedInAll = true;
    private static boolean ignoreDummyFrame = true;

    // if interestedInAll is false, then it registers only those
    // put into the HashSet with the block below:
    static
    {
        interestedIn.add("net.sf.colossus.client.Client");
        interestedIn.add("net.sf.colossus.client.SocketClientThread");
        interestedIn.add("net.sf.colossus.client.MasterBoard");
        interestedIn.add("net.sf.colossus.ai.SimpleAI");

        interestedIn.add("net.sf.colossus.util.KFrame");
        interestedIn.add("net.sf.colossus.util.KDialog");
        interestedIn.add("net.sf.colossus.client.BattleMap");
        interestedIn.add("net.sf.colossus.client.ShowReadme");
        interestedIn.add("net.sf.colossus.client.ShowHelpDoc");

        interestedIn.add("net.sf.colossus.server.PlayerServerSide");
        interestedIn.add("net.sf.colossus.server.Start");
        interestedIn.add("net.sf.colossus.server.GetPlayers");
        interestedIn.add("net.sf.colossus.server.GameServerSide");
        interestedIn.add("net.sf.colossus.server.Server");
        interestedIn.add("net.sf.colossus.server.ClientHandler");

        interestedIn.add("net.sf.colossus.util.KDialog");
        interestedIn.add("net.sf.colossus.client.StatusScreen");

        interestedIn.add("net.sf.colossus.client.WebClient");
    }

    public static synchronized void register(Object o, String id)
    {
        String type = o.getClass().getName();
        if ((interestedIn.contains(type) || interestedInAll)
            && (ignoreDummyFrame || !type
                .equals("net.sf.colossus.util.DummyFrameWithMenu")))
        {
            LOGGER.log(Level.FINEST, "Registering object of type " + type
                + " with id " + id);
            if (instanceGroups.containsKey(type))
            {
                LOGGER.log(Level.FINEST, "Adding to existing group " + type);
                InstanceGroup group = instanceGroups.get(type);
                group.addInstance(o, id);
            }
            else
            {
                LOGGER.log(Level.FINEST, "Creating new group for " + type);
                InstanceGroup group = new InstanceGroup(type);
                group.addInstance(o, id);
                instanceGroups.put(type, group);
            }
        }
        else
        {
            LOGGER.log(Level.FINEST, "NOT registering object of type " + type
                + " with id " + id);
        }
    }

    public static synchronized void setId(Object o, String id)
    {
        String type = o.getClass().getName();
        String shortType = InstanceGroup.shortType(type);

        if (interestedIn.contains(type))
        {
            LOGGER.log(Level.FINEST,
                "InstanceTracker.setId(): One object of type " + shortType
                    + " changes ID to '" + id + "'");
        }

        // nothing needs to be actually done, it will disappear by itself
        // from the WeakHashMap...

        if (instanceGroups.containsKey(type))
        {
            InstanceGroup group = instanceGroups.get(type);
            InstanceGroup.TypeInstance i = group.getInstance(o);
            if (i != null)
            {
                i.setId(id);
            }
        }
    }

    public static synchronized void printStatistics()
    {
        String stat = getPrintStatistics();
        LOGGER.log(Level.INFO, stat);
    }

    private static synchronized String getPrintStatistics()
    {
        StringBuilder stat = new StringBuilder();
        stat.append("==========\nObject instances statistics:");

        Iterator<String> it = instanceGroups.keySet().iterator();
        while (it.hasNext())
        {
            String type = it.next();
            InstanceGroup group = instanceGroups.get(type);
            stat.append(group.getPrintStatistics());
        }
        stat.append("\n");
        return stat.substring(0);
    }

    public static synchronized boolean allGone()
    {
        Iterator<String> it = instanceGroups.keySet().iterator();
        while (it.hasNext())
        {
            String type = it.next();
            InstanceGroup group = instanceGroups.get(type);
            if (group.amountLeft() != 0)
            {
                return false;
            }
        }
        return true;
    }

}
