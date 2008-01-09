package net.sf.colossus.client;


import java.util.HashMap;
import java.util.Map;

import net.sf.colossus.game.Game;
import net.sf.colossus.server.Creature;
import net.sf.colossus.variant.CreatureType;


/**
 *  Client-side cache of Caretaker.
 *  @version $Id$
 *  @author David Ripton
 */

public final class CaretakerInfo
{

    /** Map of creature name to Integer count.  As in Caretaker, if an entry
     *  is missing then we assume it is set to the maximum. */
    private final Map<String, Integer> creatureCounts = new HashMap<String, Integer>();

    /** Map of creature name to Integer count.  As in Caretaker, if an entry
     *  is missing then we assume it is set to 0. */
    private final Map<String, Integer> creatureDeadCounts = new HashMap<String, Integer>();

    /**
     * The game of which we manage the creatures.
     */
    private final Game game;

    public CaretakerInfo(Game game)
    {
        this.game = game;
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
        Integer count = creatureCounts.get(creatureName);
        if (count == null)
        {
            Creature cre = (Creature)game.getVariant().getCreatureByName(
                creatureName);
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

    public int getCount(CreatureType creature)
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
        Integer count = creatureDeadCounts.get(creatureName);
        if (count == null)
        {
            return 0;
        }
        return count.intValue();
    }

    public int getDeadCount(CreatureType creature)
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
        return getMaxCount(game.getVariant().getCreatureByName(creatureName));
    }

    public int getMaxCount(CreatureType creature)
    {
        if (creature != null)
        {
            return ((Creature)creature).getMaxCount();
        }
        else
        {
            return 0;
        }
    }
}
