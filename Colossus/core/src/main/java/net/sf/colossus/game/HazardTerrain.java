package net.sf.colossus.game;


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
     * A token used to represent the terrain.
     * 
     * Must be unique over all hazard terrains.
     * 
     * TODO: do we need this?
     */
    private final Character token;

    /**
     * Determines if native characters get a bonus in this terrain.
     */
    private final boolean isNativeBonusTerrain;

    /**
     * Determines if non-native characters get a penalty in this terrain.
     */
    private final boolean isNonNativePenaltyTerrain;

    /**
     * A map from the serialization string of a terrain to the instances.
     * 
     * This is a Map<String,HazardTerrain>.
     */
    private final static Map TERRAIN_MAP = new HashMap();

    public HazardTerrain(String name, char token, boolean nativeBonus,
        boolean nonNativePenalty)
    {
        this.name = name;
        this.token = Character.valueOf(token);
        this.isNativeBonusTerrain = nativeBonus;
        this.isNonNativePenaltyTerrain = nonNativePenalty;
        assert TERRAIN_MAP.get(this.token) == null : "Duplicate terrain token not allowed";
        TERRAIN_MAP.put(name, this);
    }

    public String getName()
    {
        return name;
    }

    public char getToken()
    {
        return token.charValue();
    }

    public boolean isNativeBonusTerrain()
    {
        return isNativeBonusTerrain;
    }

    public boolean isNonNativePenaltyTerrain()
    {
        return isNonNativePenaltyTerrain;
    }

    public static HazardTerrain getTerrainByName(String name)
    {
        return (HazardTerrain)TERRAIN_MAP.get(name);
    }

    public static final Collection getAllHazardTerrains()
    {
        return TERRAIN_MAP.values();
    }

    public static final HazardTerrain PLAINS = new HazardTerrain("Plains",
        'p', false, false);

    public static final HazardTerrain BRAMBLES = new HazardTerrain("Brambles",
        'r', true, true);

    public static final HazardTerrain SAND = new HazardTerrain("Sand", 's',
        false, false);

    public static final HazardTerrain TREE = new HazardTerrain("Tree", 't',
        false, false);

    public static final HazardTerrain BOG = new HazardTerrain("Bog", 'o',
        false, false);

    public static final HazardTerrain VOLCANO = new HazardTerrain("Volcano",
        'v', true, false);

    public static final HazardTerrain DRIFT = new HazardTerrain("Drift", 'd',
        false, true);

    public static final HazardTerrain TOWER = new HazardTerrain("Tower", 'w',
        false, false);

    public static final HazardTerrain LAKE = new HazardTerrain("Lake", 'l',
        false, false);

    public static final HazardTerrain STONE = new HazardTerrain("Stone", 'n',
        false, false);
}
