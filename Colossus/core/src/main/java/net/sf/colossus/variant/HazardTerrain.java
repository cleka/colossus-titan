package net.sf.colossus.variant;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/** 
 * This is a typesafe enumeration of all hazard terrains, i.e. the
 * terrains used in the battle maps.
 */
public class HazardTerrain
{
    /**
     * The name used for serialization.
     */
    private final String name;

    /**
     * Determines if native characters get a bonus in this terrain.
     */
    private final boolean isNativeBonusTerrain;

    /**
     * Determines if non-native characters get a penalty in this terrain.
     */
    private final boolean isNonNativePenaltyTerrain;

    /**
     * Determines if only native characters can enter the terrain.
     */
    private final boolean isNativeOnly;

    /**
     * Determines if only flyers can enter the terrain.
     */
    private final boolean isFlyersOnly;

    /**
     * Determines if only native flyers can enter the terrain.
     */
    private final boolean isNativeFlyersOnly;

    /**
     * Determines if a non-native gets slowed in the terrain.
     */
    private final boolean slowsNonNative;

    /**
     * Determines if flyers are not allowed to pass either.
     */
    private final boolean blocksFlying;

    /**
     * A map from the serialization string of a terrain to the instances.
     */
    private final static Map<String, HazardTerrain> TERRAIN_MAP = new HashMap<String, HazardTerrain>();

    public HazardTerrain(String name, boolean nativeBonus,
        boolean nonNativePenalty, boolean nativeOnly, boolean flyerOnly,
        boolean nativeFlyerOnly, boolean slowsNonNative, boolean blocksFlying)
    {
        this.name = name;
        this.isNativeBonusTerrain = nativeBonus;
        this.isNonNativePenaltyTerrain = nonNativePenalty;
        this.isNativeOnly = nativeOnly;
        this.isFlyersOnly = flyerOnly;
        this.isNativeFlyersOnly = nativeFlyerOnly;
        this.slowsNonNative = slowsNonNative;
        this.blocksFlying = blocksFlying;
        TERRAIN_MAP.put(name, this);
    }

    public String getName()
    {
        return name;
    }

    public boolean isNativeBonusTerrain()
    {
        return isNativeBonusTerrain;
    }

    public boolean isNonNativePenaltyTerrain()
    {
        return isNonNativePenaltyTerrain;
    }

    public boolean isNativeOnly()
    {
        return isNativeOnly;
    }

    public boolean isNativeFlyersOnly()
    {
        return isNativeFlyersOnly;
    }

    public boolean isFlyersOnly()
    {
        return isFlyersOnly;
    }

    public boolean slowsNonNative()
    {
        return slowsNonNative;
    }

    public boolean blocksFlying()
    {
        return blocksFlying;
    }

    public static HazardTerrain getTerrainByName(String name)
    {
        return TERRAIN_MAP.get(name);
    }

    /**
     * Returns all available hazard terrains.
     * 
     * This is not variant-specific, any terrain known to the program is listed even
     * if it is not available in the current variant.
     * 
     * TODO this should really be a question to ask a variant instance
     */
    public static final Collection<HazardTerrain> getAllHazardTerrains()
    {
        return TERRAIN_MAP.values();
    }

    public static final HazardTerrain PLAINS = new HazardTerrain("Plains",
        false, false, false, false, false, false, false);

    public static final HazardTerrain BRAMBLES = new HazardTerrain("Brambles",
        true, true, false, false, false, false, false);

    public static final HazardTerrain SAND = new HazardTerrain("Sand", false,
        false, false, false, false, false, false);

    public static final HazardTerrain TREE = new HazardTerrain("Tree", false,
        false, true, false, false, false, false);

    public static final HazardTerrain BOG = new HazardTerrain("Bog", false,
        false, true, false, false, false, false);

    public static final HazardTerrain VOLCANO = new HazardTerrain("Volcano",
        true, false, true, false, true, false, false);

    public static final HazardTerrain DRIFT = new HazardTerrain("Drift",
        false, true, false, false, false, false, false);

    public static final HazardTerrain TOWER = new HazardTerrain("Tower",
        false, false, false, false, false, false, false);

    public static final HazardTerrain LAKE = new HazardTerrain("Lake", false,
        false, true, false, false, false, false);

    public static final HazardTerrain STONE = new HazardTerrain("Stone",
        false, false, true, false, false, false, true);

    public static void main(String[] args)
    {
        for (HazardTerrain terrain : getAllHazardTerrains())
        {
            StringBuilder builder = new StringBuilder();
            builder.append(terrain.getName());
            builder.append(":\n");
            builder.append("+++++++++++++++++++++++++++\n");
            builder.append("blocks flying creatures");
            if (terrain.blocksFlying())
            {
                builder.append(": yes\n");
            }
            else
            {
                builder.append(": no\n");
            }
            builder.append("blocks non-flyers");
            if (terrain.isFlyersOnly())
            {
                builder.append(": yes\n");
            }
            else
            {
                builder.append(": no\n");
            }
            builder.append("gives bonus to natives");
            if (terrain.isNativeBonusTerrain())
            {
                builder.append(": yes\n");
            }
            else
            {
                builder.append(": no\n");
            }
            builder.append("is native flyers only");
            if (terrain.isNativeFlyersOnly())
            {
                builder.append(": yes\n");
            }
            else
            {
                builder.append(": no\n");
            }
            builder.append("is natives only");
            if (terrain.isNativeOnly())
            {
                builder.append(": yes\n");
            }
            else
            {
                builder.append(": no\n");
            }
            builder.append("gives penalty to non-natives");
            if (terrain.isNonNativePenaltyTerrain())
            {
                builder.append(": yes\n");
            }
            else
            {
                builder.append(": no\n");
            }
            builder.append("slows non-natives");
            if (terrain.slowsNonNative())
            {
                builder.append(": yes\n");
            }
            else
            {
                builder.append(": no\n");
            }
            System.out.print(builder.toString());
            System.out.println("+++++++++++++++++++++++++++");
        }
    }
}
