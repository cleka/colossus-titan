import java.util.*;

/**
 *
 * Class Careture represents the caretaker's stacks
 *
 * @author Bruce Sherrod
 */

public class Caretaker
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
	    return creature.getMaxCount();
        return count.intValue();;
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
	    map.put(creature.getName(), new Integer(creature.getMaxCount() - 1));
	}
	else
	{
	    map.put(creature.getName(), new Integer(count.intValue()-1));
	}
    } 

    public void putOneBack(Creature creature)
    {
	Integer count = (Integer) map.get(creature.getName());
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
