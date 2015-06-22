package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(Caretaker.class
        .getName());

    /**
     * Callback interface for listening to changes to the numbers.
     */
    public static interface ChangeListener
    {
        /**
         * Called whenever a change to the availability of a single creature type occurs.
         *
         * This is not called by the {@link Caretaker} on a full update but only on smaller
         * changes.
         *
         * @param type The creature type for which the count is changed.
         * @param availableCount The new number of available creatures of this type.
         */
        public void creatureTypeAvailabilityUpdated(CreatureType type,
            int availableCount);

        /**
         * Called whenever a change to the number of dead creatures of a single type occurs.
         *
         * This is not called by the {@link Caretaker} on a full update but only on smaller
         * changes.
         *
         * @param type The creature type for which the count is changed.
         * @param deadCount The new number of dead creatures of this type.
         */
        public void creatureTypeDeadCountUpdated(CreatureType type,
            int deadCount);

        /** Called when a change was done on either avail or dead, or both.
         *
         * @param type The creature type for which the count is changed.
         */
        public void creatureTypeCountsUpdated(CreatureType type);

        /**
         * Called after large changes when listeners should perform an update of all
         * inferred information and/or displays.
         */
        public void fullUpdate();
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
            // We don't need synchronized access here, because this happens
            // only at game start, not during game
            creatureAvailableCounts.put(type,
                Integer.valueOf(type.getMaxCount()));
            creatureDeadCounts.put(type, Integer.valueOf(0));
        }
        triggerFullUpdate();
    }

    public void setAvailableCount(CreatureType type, int availableCount)
    {
        assert type != null : "Can not update counts unless creature type given";
        // TODO get rid of synchronized access
        // Synchronized access is needed only because of Balrog.
        // If Balrog count update would be done together with score change,
        // instead of only "primarily during turn change",
        // (so it's needed additionally after engagement as part of lookup
        //  for special possible recruits, "because in engagement phase one
        //  or Balrogs might have been created")
        // and/or every if client has it's own caretaker, recruit graph,
        // and custom recruit base,
        // then this whole synchronized access could be dropped.
        synchronized (creatureAvailableCounts)
        {
            creatureAvailableCounts.put(type, Integer.valueOf(availableCount));
        }
        triggerOneAvailabilityCount(type, availableCount);
    }

    public void setDeadCount(CreatureType type, int deadCount)
    {
        assert type != null : "Can not update counts unless creature type given";
        creatureDeadCounts.put(type, Integer.valueOf(deadCount));
        triggerOneDeadCount(type, deadCount);
    }

    public int getAvailableCount(CreatureType type)
    {
        synchronized (creatureAvailableCounts)
        {
            return creatureAvailableCounts.get(type).intValue();
        }
    }

    public void adjustAvailableCount(CreatureType type)
    {
        int total = type.getMaxCount();
        int dead = getDeadCount(type);
        int alive = game.getNumLivingCreatures(type);

        int avail = total - (dead + alive);

        if (avail < 0)
        {
            LOGGER.warning("available for " + type.getName() + " can't be "
                + avail + "! Setting it to 0.");
            avail = 0;
        }

        // We don't need synchronized access here, because get and set
        // do that
        if (getAvailableCount(type) != avail)
        {
            LOGGER.info("Caretaker counts for " + type.getName()
                + ": adjusting avail to " + avail + " dead=" + dead
                + " inGame=" + alive + " total=" + total);
            setAvailableCount(type, avail);
        }
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

    private void triggerOneCountUpdate(CreatureType type)
    {
        for (ChangeListener listener : listeners)
        {
            listener.creatureTypeCountsUpdated(type);
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
        int count = getAvailableCount(type);
        assert count > 0 : "Trying to take creature that doesn't exist anymore";
        setAvailableCount(type, count - 1);
    }

    public void putOneBack(CreatureType type)
    {
        assert type != null : "Can not put null creature type back";
        setAvailableCount(type, getAvailableCount(type) + 1);
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
                    int live = getAvailableCount(type);
                    // Don't use setCount() / setDeadCount(), because we
                    // want to update displays only at the end.

                    // We don't need synchronized access here, because Balrog
                    // is not immortal
                    creatureAvailableCounts.put(type,
                        Integer.valueOf(live + dead));
                    creatureDeadCounts.put(type, Integer.valueOf(0));
                    triggerOneCountUpdate(type);
                }
            }
        }
        // triggerFullUpdate();
    }
}
