import java.util.*;

/**
 *
 * Class Caretaker represents the caretaker's stacks
 *
 * <P><B>TODO: </B>We should also have a graveyard -- it serves
 * no game purpose, but it is cool. Unfortunately there are several
 * ways that characters can die, not all are accounted for
 *
 * @version $Id$
 * @author Bruce Sherrod
 */

import net.sf.colossus.*;

public final class Caretaker implements Cloneable
{
    /**
     * mapping from String creature name to Integer count.  If the
     * creature is not found, assume that we have a full count (equal
     * to Creature.getMaxCount())
     */
    private HashMap map = new HashMap();

    // XXX Temporary?  We need a Game reference for callback events.
    private Game game;

    public Caretaker(Game game)
    {
        this.game = game;
    }


    /**
     * This is an adapter so that we can use the CreatureCollectionView
     * in the main program
     */
    class CaretakerCollection implements ICreatureCollection
    {
        public String getName()
        {
            return "Caretaker's Stacks";
        }
        public void setCount(String strCharacterName, int nCount)
        {
            Caretaker.this.setCount(strCharacterName, nCount);
        }
        public int getCount(String strCharacterName)
        {
            return Caretaker.this.getCount(strCharacterName);
        }
    }

    public ICreatureCollection getCollectionInterface()
    {
        return new CaretakerCollection();
    }

    protected int getCount(String strCreatureName)
    {
        Integer count = (Integer) map.get(strCreatureName);
        if (count == null)
        {
            return CharacterArchetype.getMaxCount(strCreatureName);
        }
        return count.intValue();
    }

    public int getCount(Creature creature)
    {
        return getCount(creature.getName());
    }

    protected void setCount(String strCreatureName, int count)
    {
        map.put(strCreatureName, new Integer(count)); 
        updateDisplays();
    }

    public void setCount(Creature creature, int count)
    {
        setCount(creature.getName(), count);
    }

    public void resetAllCounts()
    {
        map = new HashMap();
        updateDisplays();
    }

    public void takeOne(Creature creature)
    {
        Integer count = (Integer) map.get(creature.getName());
        if (count == null)
        {
            Log.event("First " + creature.getName() + " recruited");
            map.put(creature.getName(), new Integer(creature.getMaxCount() - 
                1));
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
        updateDisplays();
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
        updateDisplays();
    }

    public void updateDisplays()
    {
        if (game != null)
        {
            Server server = game.getServer();
            if (server != null)
            {
                server.allUpdateCaretakerDisplay();
            }
        }
    }

    /**
     * deep copy for AI
     */
    public Caretaker AICopy()
    {
        return (Caretaker) clone();
    }

    // ---------------------------------
    // Override from Object 

    public Object clone()
    {
        Caretaker newCaretaker = new Caretaker(game);
        // because String and Integer are both immutable, a shallow copy is
        // the same as a deep copy
        newCaretaker.map = (HashMap) map.clone();
        return newCaretaker;
    }
}
