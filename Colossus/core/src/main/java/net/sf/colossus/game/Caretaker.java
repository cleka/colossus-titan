package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.colossus.variant.CreatureType;


/**
 *  The caretaker tracks the number of creatures still available and those dead.
 *  
 *  For each creature type the number of creatures still available for mustering and
 *  the number of creatures already dead is stored. The latter function means this
 *  version of a caretaker integrates what is called a 'graveyard' in a normal Titan
 *  game.
 */
public class Caretaker
{
    /**
     * Callback interface for listening to changes to the numbers.
     * 
     * TODO this is not final since we use the access to the outer class in {@link #fullUpdate()}
     *      Maybe it would be better to just use an interface and forget about this little
     *      convenience base implementation -- the obj.new Class() syntax is not exactly
     *      common and thus probably rather confusing
     */
    public abstract class ChangeListener
    {
        /**
         * Called whenever a change to the availability of a single creature type occurs.
         * 
         * This is not called by the {@link Caretaker} on a full update, but the default 
         * implementation of {@link #fullUpdate()} in this class does it. By overriding
         * the latter you can avoid getting a call to this method for each creature type.
         * 
         * @param type The creature type for which the count is changed.
         * @param availableCount The new number of available creatures of this type.
         */
        public void creatureTypeAvailabilityUpdated(CreatureType type,
            int availableCount)
        {
            // base implementation does nothing
        }

        /**
         * Called whenever a change to the number of dead creatures of a single type occurs.
         * 
         * This is not called by the {@link Caretaker} on a full update, but the default 
         * implementation of {@link #fullUpdate()} in this class does it. By overriding
         * the latter you can avoid getting a call to this method for each creature type.
         * 
         * @param type The creature type for which the count is changed.
         * @param deadCount The new number of dead creatures of this type.
         */
        public void creatureTypeDeadCountUpdated(CreatureType type,
            int deadCount)
        {
            // base implementation does nothing
        }

        public void fullUpdate()
        {
            for (CreatureType type : game.getVariant().getCreatureTypes())
            {
                creatureTypeAvailabilityUpdated(type, getCount(type));
                creatureTypeDeadCountUpdated(type, getDeadCount(type));
            }
        }
    }

    /** 
     * Map of creature types to the number of available creatures.
     */
    private final Map<CreatureType, Integer> creatureAvailableCounts = new HashMap<CreatureType, Integer>();

    /** 
     * Map of creature types to the number of dead creatures.
     */
    private final Map<CreatureType, Integer> creatureDeadCounts = new HashMap<CreatureType, Integer>();

    /**
     * The game of which we manage the creatures.
     */
    private final Game game;

    /**
     * All parties interested in changes to our numbers.
     */
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    public Caretaker(Game game)
    {
        this.game = game;
        resetAllCounts();
    }

    public void resetAllCounts()
    {
        if (game.getVariant() == null)
        {
            // TODO for some reason this can happen during stresstesting -> fix game initialization
            return;
        }
        for (CreatureType type : game.getVariant().getCreatureTypes())
        {
            creatureAvailableCounts.put(type, Integer.valueOf(type
                .getMaxCount()));
            creatureDeadCounts.put(type, Integer.valueOf(0));
        }
        triggerFullUpdate();
    }

    public void setCount(CreatureType type, int availableCount)
    {
        assert type != null : "Can not update counts unless creature type given";
        creatureAvailableCounts.put(type, Integer.valueOf(availableCount));
        triggerOneAvailabilityCount(type, availableCount);
    }

    public void setDeadCount(CreatureType type, int deadCount)
    {
        assert type != null : "Can not update counts unless creature type given";
        creatureDeadCounts.put(type, Integer.valueOf(deadCount));
        triggerOneDeadCount(type, deadCount);
    }

    public int getCount(CreatureType type)
    {
        return creatureAvailableCounts.get(type).intValue();
    }

    public int getDeadCount(CreatureType type)
    {
        return creatureDeadCounts.get(type).intValue();
    }

    protected Game getGame()
    {
        return game;
    }

    public void addListener(ChangeListener listener)
    {
        this.listeners.add(listener);
    }

    public void removeListener(ChangeListener listener)
    {
        this.listeners.remove(listener);
    }

    private void triggerOneAvailabilityCount(CreatureType type, int count)
    {
        for (ChangeListener listener : listeners)
        {
            listener.creatureTypeAvailabilityUpdated(type, count);
        }
    }

    private void triggerOneDeadCount(CreatureType type, int count)
    {
        for (ChangeListener listener : listeners)
        {
            listener.creatureTypeDeadCountUpdated(type, count);
        }
    }

    private void triggerFullUpdate()
    {
        for (ChangeListener listener : listeners)
        {
            listener.fullUpdate();
        }
    }

    public void takeOne(CreatureType type)
    {
        assert type != null : "Can not take null creature type";
        int count = getCount(type);
        assert count > 0 : "Trying to take creature that doesn't exist anymore";
        setCount(type, count - 1);
    }

    public void putOneBack(CreatureType type)
    {
        assert type != null : "Can not put null creature type back";
        setCount(type, getCount(type) + 1);
    }

    public void putDeadOne(CreatureType type)
    {
        assert type != null : "Can not put null creature type into graveyard";
        setDeadCount(type, getDeadCount(type) + 1);
    }

    /** 
     * Move dead non-Titan immortals back to stacks. 
     */
    public void resurrectImmortals()
    {
        for (CreatureType type : getGame().getVariant().getCreatureTypes())
        {
            if (type.isImmortal())
            {
                int dead = getDeadCount(type);
                if (dead > 0)
                {
                    int live = getCount(type);
                    // Don't use setCount() / setDeadCount(), because we 
                    // want to update displays only at the end. 
                    creatureAvailableCounts.put(type, Integer.valueOf(live
                        + dead));
                    creatureDeadCounts.put(type, Integer.valueOf(0));
                }
            }
        }
        triggerFullUpdate();
    }
}
