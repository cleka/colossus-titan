package net.sf.colossus.variant;


import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import Balrog.CreatureBalrog;


/**
 * A type of creature in a variant.
 * 
 * This class models a generic creature type, i.e. all features that are common
 * through all creatures of a specific type.
 */
public class CreatureType
{
    /**
     * A comparator sorting creature types by name.
     */
    public static final Comparator<CreatureType> NAME_ORDER = new Comparator<CreatureType>()
    {
        public int compare(CreatureType type1, CreatureType type2)
        {
            return type1.getName().compareTo(type2.getName());
        }
    };

    private final String name;

    private final String pluralName;

    private final Set<HazardTerrain> nativeTerrains = new HashSet<HazardTerrain>();

    private final boolean isSummonable;

    public CreatureType(String name, String pluralName,
        Set<HazardTerrain> nativeTerrains, boolean isSummonable)
    {
        this.name = name;
        this.pluralName = pluralName;

        // defensive, but shallow copy of terrains
        this.nativeTerrains.addAll(nativeTerrains);

        this.isSummonable = isSummonable;
    }

    /**
     * The name used for creatures of this type.
     */
    public String getName()
    {
        return name;
    }

    /**
     * A display name.
     * 
     * This is overridden by {@link CreatureBalrog} at the moment -- not sure why this 
     * is necessary. TODO check need for this method
     */
    public String getDisplayName()
    {
        return getName();
    }

    /**
     * The name used for multiple creatures of this type.
     */
    public String getPluralName()
    {
        return pluralName;
    }

    /**
     * Checks if the type of creature is native in a terrain type.
     * 
     * @param terrain The terrain to check. Not null.
     * @return true iff creatures of this type are native in the terrain.
     */
    public boolean isNativeIn(HazardTerrain terrain)
    {
        assert terrain != null;
        return nativeTerrains.contains(terrain);
    }

    public boolean isSummonable()
    {
        return isSummonable;
    }
}
