package net.sf.colossus.client;

import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.Constants;

/** Sort creatures in decreasing order of importance.  Keep identical 
 *  creatures together with a secondary sort by creature name. */
final class CreatureInfoComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        CreatureInfo info1 = (CreatureInfo)o1;
        CreatureInfo info2 = (CreatureInfo)o2;
        Creature creature1 = Creature.getCreatureByName(info1.getName());
        Creature creature2 = Creature.getCreatureByName(info2.getName());
        int diff = SimpleAI.getKillValue(creature2) - 
            SimpleAI.getKillValue(creature1);
        if (diff != 0)
        {
            return diff;
        }
        else
        {
            return creature1.getName().compareTo(creature2.getName());
        }
    }
}
