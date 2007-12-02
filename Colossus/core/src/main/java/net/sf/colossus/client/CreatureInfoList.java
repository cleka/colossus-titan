package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  A specialized list of creature info, for split prediction.
 *  @version $Id$
 *  @author David Ripton
 */
class CreatureInfoList extends ArrayList
{
    private static final Logger LOGGER = Logger.getLogger(CreatureInfoList.class.getName());

    int numCreature(String creatureName)
    {
        int count = 0;
        for (Iterator it = iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (creatureName.equals(ci.getName()))
            {
                count++;
            }
        }
        return count;
    }

    /** Return the first CreatureInfo that matches the passed name. */
    CreatureInfo getCreatureInfo(String creatureName)
    {
        for (Iterator it = iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.getName().equals(creatureName))
            {
                return ci;
            }
        }
        return null;
    }

    void removeLastUncertainCreature()
    {
        ListIterator lit = this.listIterator(this.size());
        while (lit.hasPrevious())
        {
            CreatureInfo ci = (CreatureInfo)lit.previous();
            if (!ci.isCertain())
            {
                lit.remove();
                return;
            }
        }
        LOGGER.log(Level.SEVERE, "No uncertain creatures", (Throwable)null);
    }

    /** Remove the first element matching name.  Return true if found. */
    boolean removeCreatureByName(String name)
    {
        if (name.startsWith("Titan"))
        {
            name = "Titan";
        }
        for (Iterator it = iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (name.equals(ci.getName()))
            {
                it.remove();
                return true;
            }
        }
        return false;
    }

    List getCreatureNames()
    {
        List list = new ArrayList();
        for (Iterator it = iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            list.add(ci.getName());
        }
        return list;
    }

    /** Deep copy */
    public Object clone()
    {
        CreatureInfoList dupe = new CreatureInfoList();
        for (Iterator it = iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            dupe.add(ci.clone());
        }
        return dupe;
    }
}
