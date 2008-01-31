package net.sf.colossus.variant;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.colossus.game.Game;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;


/**
 * Hub for all variant-specific information.
 * 
 * This class is meant to give access to all the information about a Colossus
 * game in the static sense: the master board layout, the battle board layouts,
 * available creatures, rules, etc. The information about a game in progress is
 * in the {@link Game} class.
 * 
 * Instances of this class are immutable.
 * 
 * TODO add access to the markers by having a class for them
 * TODO same thing for the colors/markersets
 */
public class Variant
{
    private final List<CreatureType> creatureTypes;
    private final List<CreatureType> summonableCreatureTypes;
    private final List<MasterBoardTerrain> battleLands;
    private final MasterBoard masterBoard;

    /** 
     * A map for fast lookup of creatures by their name.
     * 
     * This is a cache to find creatures by their case-insensitive name quickly.
     */
    private final Map<String, CreatureType> creatureTypeByNameCache = new HashMap<String, CreatureType>();

    public Variant(List<CreatureType> creatureTypes,
        List<MasterBoardTerrain> battleLands, MasterBoard masterBoard)
    {
        // defensive copies to ensure immutability
        this.creatureTypes = new ArrayList<CreatureType>(creatureTypes);

        // create some caches for faster lookups -- by name and by the "summonable" attribute
        initCreatureNameCache();
        this.summonableCreatureTypes = new ArrayList<CreatureType>();
        CollectionHelper.copySelective(this.creatureTypes,
            this.summonableCreatureTypes, new Predicate<CreatureType>()
            {
                public boolean matches(CreatureType creatureType)
                {
                    return creatureType.isSummonable();
                }
            });

        this.battleLands = new ArrayList<MasterBoardTerrain>(battleLands);
        this.masterBoard = masterBoard;
    }

    public List<CreatureType> getCreatureTypes()
    {
        return Collections.unmodifiableList(this.creatureTypes);
    }

    public List<MasterBoardTerrain> getBattleLands()
    {
        return Collections.unmodifiableList(this.battleLands);
    }

    public MasterBoard getMasterBoard()
    {
        return masterBoard;
    }

    /** 
     * Look up a creature type by its name.
     * 
     * The lookup is case-insensitive at the moment (TODO: check if that makes
     * sense at all).
     * 
     * TODO in the long run noone should really need this since the names shouldn't
     * be passed around by themselves
     * 
     * @param name Name of a creature type. Not null.
     * @return CreatureType with the given name, null no such creature type.
     */
    public CreatureType getCreatureByName(final String name)
    {
        String lowerCaseName = name.toLowerCase();
        return creatureTypeByNameCache.get(lowerCaseName);
    }

    private void initCreatureNameCache()
    {
        // find it the slow way and add to cache.
        Iterator<CreatureType> it = this.creatureTypes.iterator();
        while (it.hasNext())
        {
            CreatureType creatureType = it.next();
            creatureTypeByNameCache.put(creatureType.getName().toLowerCase(),
                creatureType);
        }

        // "null" (not a null pointer...) is used for recruiter
        // when it is anonymous, so it is known and legal,
        // mapped to null (a null pointer, this time).
        // TODO avoid using nulls altogether
        creatureTypeByNameCache.put("null", null);
    }

    /**
     * Checks if a creature with the given name exists.
     *  
     * @param name (case insensitive) name of a creature, must not be null.
     * @return true if this names represents a creature
     */
    public boolean isCreature(final String name)
    {
        return creatureTypeByNameCache.containsKey(name.toLowerCase());
    }

    public List<CreatureType> getSummonableCreatureTypes()
    {
        return summonableCreatureTypes;
    }
}
