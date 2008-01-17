package net.sf.colossus.server;


import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;


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

public final class Caretaker
{
    private static final Logger LOGGER = Logger.getLogger(Caretaker.class
        .getName());

    /** Mapping from String creature name to Integer count. If the
     *  creature is not found, assume that we have a full count (equal
     *  to Creature.getMaxCount()) */
    private final HashMap<String, Integer> map = new HashMap<String, Integer>();

    /** Mapping from String creature name to Integer count. If the
     *  creature is not found, assume that we have a 0 count */
    private final HashMap<String, Integer> deadMap = new HashMap<String, Integer>();

    private final GameServerSide game;

    Caretaker(GameServerSide game)
    {
        this.game = game;
    }

    public int getCount(String creatureName)
    {
        Integer count = map.get(creatureName);
        if (count == null)
        {
            return ((CreatureTypeServerSide)game.getVariant()
                .getCreatureByName(creatureName)).getMaxCount();
        }
        return count.intValue();
    }

    public int getCount(CreatureType creature)
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

    public int getDeadCount(CreatureType creature)
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

    void setCount(CreatureTypeServerSide creature, int count)
    {
        setCount(creature.getName(), count);
    }

    void setDeadCount(CreatureTypeServerSide creature, int count)
    {
        setDeadCount(creature.getName(), count);
    }

    void resetAllCounts()
    {
        map.clear();
        deadMap.clear();
        fullySyncDisplays();
    }

    void takeOne(CreatureTypeServerSide creature)
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

    void putOneBack(CreatureTypeServerSide creature)
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

    void putDeadOne(CreatureTypeServerSide creature)
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
        if (game.getVariant() == null)
        {
            // no variant loaded yet.
            // hack 9.1.2008 to get stresstest working again.
            return;
        }

        // Do *all* creatures, not just the ones in the map.
        Iterator<CreatureType> it = game.getVariant().getCreatureTypes()
            .iterator();
        while (it.hasNext())
        {
            CreatureType creature = it.next();
            updateDisplays(creature.getName());
        }
    }

    /** Move dead non-Titan immortals back to stacks. */
    void resurrectImmortals()
    {
        Iterator<CreatureType> it = game.getVariant().getCreatureTypes()
            .iterator();
        while (it.hasNext())
        {
            CreatureTypeServerSide creature = (CreatureTypeServerSide)it.next();
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
