import java.util.*;

/**
 *
 * Class Careture represents the caretaker's stacks
 *
 * @version $Id$
 * @author Bruce Sherrod
 */

public final class Caretaker
{
    /**
     * mapping from String creature name to Integer count.  If the
     * creature is not found, assume that we have a full count (equal
     * to Creature.getMaxCount())
     */
    private HashMap map = new HashMap();

    public int getCount(Creature creature)
    {
        Integer count = (Integer) map.get(creature.getName());
        if (count == null)
        {
            return creature.getMaxCount();
        }
        return count.intValue();
    }

    public void setCount(Creature creature, int count)
    {
        map.put(creature.getName(), new Integer(count));
    }

    public void resetAllCounts()
    {
        map = new HashMap();
    }

    public void takeOne(Creature creature)
    {
        Integer count = (Integer) map.get(creature.getName());
        if (count == null)
        {
            Log.event("First " + creature.getName() + " recruited");
            map.put(creature.getName(), new Integer(
		creature.getMaxCount() - 1));
        }
        else
        {
            if (count.intValue() == creature.getMaxCount())
            {
                // Not quite right for demi-lords.
                Log.event("First " + creature.getName() + " recruited");
            }
            if (count.intValue() == 1)
            {
                Log.event("Last " + creature.getName() + " recruited");
            }
            map.put(creature.getName(), new Integer(count.intValue()-1));
        }
    }

    public void putOneBack(Creature creature)
    {
        Integer count = (Integer)map.get(creature.getName());
        // count can be null if we're testing a battle.
        if (count == null)
        {
            count = new Integer(creature.getMaxCount() - 1);
        }
        map.put(creature.getName(), new Integer(count.intValue()+1));
    }

    /**
     * deep copy for AI
     */
    public Caretaker AICopy()
    {
        Caretaker newCaretaker = new Caretaker();
        // because String and Integer are both immutable, a shallow copy is
        // the same as a deep copy
        newCaretaker.map = (HashMap) map.clone();
        return newCaretaker;
    }
}
