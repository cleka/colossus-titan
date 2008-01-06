package net.sf.colossus.server;


import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * Class Caretaker represents the caretaker's stacks.
 * It also contains the (preliminary) Graveyard.
 *
 * @version $Id$
 * @author Bruce Sherrod
 * @author David Ripton
 * @author Tom Fruchterman
 * @author Romain Dolbeau
 */

public final class Caretaker implements Cloneable
{
    private static final Logger LOGGER = Logger.getLogger(Caretaker.class
        .getName());

    /** Mapping from String creature name to Integer count. If the
     *  creature is not found, assume that we have a full count (equal
     *  to Creature.getMaxCount()) */
    private HashMap<String, Integer> map = new HashMap<String, Integer>();

    /** Mapping from String creature name to Integer count. If the
     *  creature is not found, assume that we have a 0 count */
    private HashMap<String, Integer> deadMap = new HashMap<String, Integer>();

    private final Game game;

    Caretaker(Game game)
    {
        this.game = game;
    }

    public int getCount(String creatureName)
    {
        Integer count = map.get(creatureName);
        if (count == null)
        {
            return (Creature.getCreatureByName(creatureName).getMaxCount());
        }
        return count.intValue();
    }

    public int getCount(Creature creature)
    {
        return getCount(creature.getName());
    }

    public int getDeadCount(String creatureName)
    {
        Integer count = deadMap.get(creatureName);
        if (count == null)
        {
            return (0);
        }
        return count.intValue();
    }

    public int getDeadCount(Creature creature)
    {
        return getDeadCount(creature.getName());
    }

    public void setCount(String creatureName, int count)
    {
        map.put(creatureName, new Integer(count));
        updateDisplays(creatureName);
    }

    public void setDeadCount(String creatureName, int count)
    {
        deadMap.put(creatureName, new Integer(count));
        updateDisplays(creatureName);
    }

    void setCount(Creature creature, int count)
    {
        setCount(creature.getName(), count);
    }

    void setDeadCount(Creature creature, int count)
    {
        setDeadCount(creature.getName(), count);
    }

    void resetAllCounts()
    {
        map.clear();
        deadMap.clear();
        fullySyncDisplays();
    }

    void takeOne(Creature creature)
    {
        Integer count = map.remove(creature.getName());
        if (count == null)
        {
            LOGGER.log(Level.INFO, "First " + creature.getName()
                + " recruited");
            map.put(creature.getName(),
                new Integer(creature.getMaxCount() - 1));
        }
        else
        {
            if (count.intValue() == creature.getMaxCount())
            {
                // Not quite right for immortals.
                LOGGER.log(Level.INFO, "First " + creature.getName()
                    + " recruited");
            }
            if (count.intValue() == 1)
            {
                // Not quite right for immortals.
                LOGGER.log(Level.INFO, "Last " + creature.getName()
                    + " recruited");
            }
            if (count.intValue() <= 0)
            {
                LOGGER.log(Level.SEVERE, "Too many " + creature.getName()
                    + " recruited, only " + count.intValue() + " left ?!?");
            }
            map.put(creature.getName(), new Integer(count.intValue() - 1));
        }
        updateDisplays(creature.getName());
    }

    void putOneBack(Creature creature)
    {
        Integer count = map.get(creature.getName());
        // count can be null if we're testing a battle.
        if (count == null)
        {
            count = new Integer(creature.getMaxCount() - 1);
        }
        map.put(creature.getName(), new Integer(count.intValue() + 1));
        updateDisplays(creature.getName());
    }

    void putDeadOne(Creature creature)
    {
        String name = creature.getName();
        Integer deadCount = deadMap.get(name);
        if (deadCount == null)
        {
            deadCount = new Integer(0);
        }
        deadMap.put(name, new Integer(deadCount.intValue() + 1));

        // safety check
        Integer count = map.get(name);
        if (count == null)
        {
            LOGGER.log(Level.WARNING, "A Creature by the name of " + name
                + " died before any was taken.");
        }
        updateDisplays(creature.getName());
    }

    /** Update creatureName's count on all clients. */
    private void updateDisplays(String creatureName)
    {
        Server server = game.getServer();
        if (server != null)
        {
            if (creatureName != null)
            {
                server.allUpdateCreatureCount(creatureName,
                    getCount(creatureName), getDeadCount(creatureName));
            }
        }
    }

    /** Update ALL creatures' counts on all clients. */
    void fullySyncDisplays()
    {
        // Do *all* creatures, not just the ones in the map.
        Iterator<Creature> it = Creature.getCreatures().iterator();
        while (it.hasNext())
        {
            Creature creature = it.next();
            updateDisplays(creature.getName());
        }
    }

    // unchecked conversions since retVal of HashMap.clone() not covariant
    @SuppressWarnings("unchecked")
    @Override
    public Object clone()
    {
        Caretaker newCaretaker = new Caretaker(game);
        // because String and Integer are both immutable, a shallow copy is
        // the same as a deep copy
        newCaretaker.map = (HashMap<String, Integer>)map.clone();
        newCaretaker.deadMap = (HashMap<String, Integer>)deadMap.clone();
        return newCaretaker;
    }

    /** Move dead non-Titan immortals back to stacks. */
    void resurrectImmortals()
    {
        Iterator<Creature> it = Creature.getCreatures().iterator();
        while (it.hasNext())
        {
            Creature creature = it.next();
            if (creature.isImmortal())
            {
                String name = creature.getName();
                int dead = getDeadCount(name);
                if (dead > 0)
                {
                    int live = getCount(name);
                    // Don't use setCount() / setDeadCount(), because we 
                    // want to update displays only at the end. 
                    map.put(name, new Integer(live + dead));
                    deadMap.put(name, new Integer(0));
                }
            }
        }
        fullySyncDisplays();
    }
}
