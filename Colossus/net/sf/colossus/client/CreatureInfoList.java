package net.sf.colossus.client;

import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.Constants;

/**
 *  A specialized list of creature info, for split prediction.
 *  @version $Id$
 *  @author David Ripton
 */
class CreatureInfoList extends ArrayList
{
    int numCreature(String creatureName)
    {
        int count = 0;
        Iterator it = iterator();
        while (it.hasNext())
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
        Iterator it = iterator();
        while (it.hasNext())
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
        Log.error("No uncertain creatures");
    }

    boolean isSupersetOf(CreatureInfoList other)
    {
        Iterator it = other.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (numCreature(ci.getName()) < 
                other.numCreature(ci.getName()))
            {
                return false;
            }
        }
        return true;
    }

    /** Remove the first element matching name.  Return true if found. */
    boolean removeCreatureByName(String name)
    {
        if (name.startsWith("Titan"))
        {
            name = "Titan";
        }
        Iterator it = iterator();
        while (it.hasNext())
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
        Iterator it = iterator();
        while (it.hasNext())
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
        Iterator it = iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            dupe.add(ci.clone());
        }
        return dupe;
    }
}
