package net.sf.colossus.variant;


import java.util.HashSet;
import java.util.Set;


/**
 * A type of creature in a variant.
 * 
 * This class models a generic creature type, i.e. all features that are common
 * through all creatures of a specific type.
 */
public class CreatureType
{
    private final Set<HazardTerrain> nativeTerrains = new HashSet<HazardTerrain>();

    public CreatureType(Set<HazardTerrain> nativeTerrains)
    {
        // defensive, but shallow copy
        this.nativeTerrains.addAll(nativeTerrains);
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
}
