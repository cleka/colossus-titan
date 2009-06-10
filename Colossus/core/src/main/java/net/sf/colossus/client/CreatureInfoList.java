package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;


/**
 * A specialized list of creature info, for split prediction.
 *
 * @author David Ripton
 */
class CreatureInfoList extends ArrayList<CreatureInfo>
{
    private static final Logger LOGGER = Logger
        .getLogger(CreatureInfoList.class.getName());

    int numCreature(String creatureName)
    {
        int count = 0;
        for (CreatureInfo ci : this)
        {
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
        for (CreatureInfo ci : this)
        {
            if (ci.getName().equals(creatureName))
            {
                return ci;
            }
        }
        return null;
    }

    void removeLastUncertainCreature()
    {
        ListIterator<CreatureInfo> lit = this.listIterator(this.size());
        while (lit.hasPrevious())
        {
            CreatureInfo ci = lit.previous();
            if (!ci.isCertain())
            {
                lit.remove();
                return;
            }
        }
        LOGGER.log(Level.SEVERE, "No uncertain creatures");
    }

    List<String> getCreatureNames()
    {
        List<String> list = new ArrayList<String>();
        for (CreatureInfo ci : this)
        {
            list.add(ci.getName());
        }
        return list;
    }

    List<CreatureType> getCreatureTypes()
    {
        List<CreatureType> list = new ArrayList<CreatureType>();
        for (CreatureInfo ci : this)
        {
            list.add(ci.getCreatureType());
        }
        return list;
    }

    /** Deep copy */
    @Override
    public CreatureInfoList clone()
    {
        CreatureInfoList dupe = new CreatureInfoList();
        for (CreatureInfo ci : this)
        {
            dupe.add(ci.clone());
        }
        return dupe;
    }
}
