package net.sf.colossus.server;


import java.util.*;

import net.sf.colossus.util.Log;


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


final class Caretaker implements Cloneable
{
    /**
     * mapping from String creature name to Integer count.  If the
     * creature is not found, assume that we have a full count (equal
     * to Creature.getMaxCount())
     */
    private HashMap map = new HashMap();

    // XXX Temporary?  We need a Game reference for callback events.
    private Game game;

    Caretaker(Game game)
    {
        this.game = game;
    }


    int getCount(String strCreatureName)
    {
        Integer count = (Integer) map.get(strCreatureName);
        if (count == null)
        {
            return (Creature.getCreatureByName(strCreatureName).getMaxCount());
        }
        return count.intValue();
    }

    int getCount(Creature creature)
    {
        return getCount(creature.getName());
    }

    void setCount(String strCreatureName, int count)
    {
        map.put(strCreatureName, new Integer(count)); 
        updateDisplays();
    }

    void setCount(Creature creature, int count)
    {
        setCount(creature.getName(), count);
    }

    void resetAllCounts()
    {
        map = new HashMap();
        updateDisplays();
    }

    void takeOne(Creature creature)
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

    void putOneBack(Creature creature)
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

    void updateDisplays()
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
    Caretaker AICopy()
    {
        return (Caretaker)clone();
    }

    // ---------------------------------
    // Override from Object 

    public Object clone()
    {
        Caretaker newCaretaker = new Caretaker(game);
        // because String and Integer are both immutable, a shallow copy is
        // the same as a deep copy
        newCaretaker.map = (HashMap)map.clone();
        return newCaretaker;
    }
}
