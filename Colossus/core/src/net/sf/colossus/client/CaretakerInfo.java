package net.sf.colossus.client;


import java.util.HashMap;
import java.util.Map;

import net.sf.colossus.server.Creature;


/**
 *  Client-side cache of Caretaker.
 *  @version $Id$
 *  @author David Ripton
 */

public final class CaretakerInfo
{

    /** Map of creature name to Integer count.  As in Caretaker, if an entry
     *  is missing then we assume it is set to the maximum. */
    private Map creatureCounts = new HashMap();

    /** Map of creature name to Integer count.  As in Caretaker, if an entry
     *  is missing then we assume it is set to 0. */
    private Map creatureDeadCounts = new HashMap();

    public CaretakerInfo()
    {
    }

    public void updateCount(String creatureName, int count, int deadCount)
    {
        if (creatureName != null)
        {
            creatureCounts.put(creatureName, new Integer(count));
            creatureDeadCounts.put(creatureName, new Integer(deadCount));
        }
    }

    public int getCount(String creatureName)
    {
        Integer count = (Integer)creatureCounts.get(creatureName);
        if (count == null)
        {
            Creature cre = Creature.getCreatureByName(creatureName);
            if (cre != null)
            {
                return cre.getMaxCount();
            }
            else
            { // Creature doesn't exist
                return -1;
            }
        }
        return count.intValue();
    }

    public int getCount(Creature creature)
    {
        if (creature != null)
        {
            return getCount(creature.getName());
        }
        else
        {
            return -1;
        }
    }

    public int getDeadCount(String creatureName)
    {
        Integer count = (Integer)creatureDeadCounts.get(creatureName);
        if (count == null)
        {
            return 0;
        }
        return count.intValue();
    }

    public int getDeadCount(Creature creature)
    {
        if (creature != null)
        {
            return getDeadCount(creature.getName());
        }
        else
        {
            return 0;
        }
    }

    public int getMaxCount(String creatureName)
    {
        return getMaxCount(Creature.getCreatureByName(creatureName));
    }

    public int getMaxCount(Creature creature)
    {
        if (creature != null)
        {
            return creature.getMaxCount();
        }
        else
        {
            return 0;
        }
    }
}
