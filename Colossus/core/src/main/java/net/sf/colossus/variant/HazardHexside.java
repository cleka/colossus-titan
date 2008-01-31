package net.sf.colossus.variant;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/** 
 * This is a typesafe enumeration of all hazard Hexsides, i.e. the
 * Hexsides used in the battle maps.
 */
public class HazardHexside implements HazardConstants
{
    /**
     * The name used for serialization.
     */
    private final String name;

    /**
     * Properties - 
     */
    public final String effectOnGroundMovement; // Also flyers at end of move
    public final String effectOnFlyerMovement;

    public final String effectforDefendingInTerrain;
    public final String scopeForDefenceEffect;
    public final int defenceEffectAdjustment;

    public final String effectforAttackingFromTerrain;
    public final String scopeForAttackEffect;
    public final int AttackEffectAdjustment;

    public final String effectForBeingRangeSruckInTerrain;
    public final String scopeForRangeStruckEffect;
    public final int RangeStruckEffectAdjustment;

    public final String effectforRangeStrikeFromTerrain;
    public final String scopeForRangeStrikeEffect;
    public final int RangeStrikeEffectAdjustment;

    public final String RangeStrikeSpecial;

    /**
     * A map from the serialization string of a terrain to the instances.
     */
    private final static Map<String, HazardHexside> HEXSIDE_MAP = new HashMap<String, HazardHexside>();

    public HazardHexside(String name, String effectOnGroundMovement,
        String effectOnFlyerMovement, String effectforDefendingInTerrain,
        String scopeForDefenceEffect, int defenceEffectAdjustment,
        String effectforAttackingFromTerrain, String scopeForAttackEffect,
        int attackEffectAdjustment, String effectForBeingRangeSruckInTerrain,
        String scopeForRangeStruckEffect, int RangeStruckEffectAdjustment,
        String effectforRangeStrikeFromTerrain,
        String scopeForRangeStrikeEffect, int RangeStrikeEffectAdjustment,
        String RangeStrikeSpecial)
    {
        this.name = name;
        this.effectOnGroundMovement = effectOnGroundMovement;
        this.effectOnFlyerMovement = effectOnFlyerMovement;

        this.effectforDefendingInTerrain = effectforDefendingInTerrain;
        this.scopeForDefenceEffect = scopeForDefenceEffect;
        this.defenceEffectAdjustment = defenceEffectAdjustment;

        this.effectforAttackingFromTerrain = effectforAttackingFromTerrain;
        this.scopeForAttackEffect = scopeForAttackEffect;
        this.AttackEffectAdjustment = attackEffectAdjustment;
        this.effectForBeingRangeSruckInTerrain = effectForBeingRangeSruckInTerrain;
        this.scopeForRangeStruckEffect = scopeForRangeStruckEffect;
        this.RangeStruckEffectAdjustment = RangeStruckEffectAdjustment;
        this.effectforRangeStrikeFromTerrain = effectforRangeStrikeFromTerrain;
        this.scopeForRangeStrikeEffect = scopeForRangeStrikeEffect;
        this.RangeStrikeEffectAdjustment = RangeStrikeEffectAdjustment;
        this.RangeStrikeSpecial = RangeStrikeSpecial;

        HEXSIDE_MAP.put(name, this);
    }

    public String getName()
    {
        return name;
    }

    public static HazardHexside getTerrainByName(String name)
    {
        return HEXSIDE_MAP.get(name);
    }

    /**
     * Returns all available hazard terrains.
     * 
     * This is not variant-specific, any terrain known to the program is listed even
     * if it is not available in the current variant.
     * 
     * TODO this should really be a question to ask a variant instance
     */
    public static final Collection<HazardHexside> getAllHazardHexsides()
    {
        return HEXSIDE_MAP.values();
    }

    public static final HazardHexside NOTHING = new HazardHexside("Nothing",
        FREEMOVE, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0,
        NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    public static final HazardHexside DUNE = new HazardHexside("Dune",
        FREEMOVE, FREEMOVE, POWERPENALTY, SCOPEFOREIGNER, 1, POWERBONUS,
        SCOPENATIVE, 2, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0,
        RANGESTRIKEOCCUPIED);

    public static final HazardHexside CLIFF = new HazardHexside("Cliff",
        BLOCKALL, FREEMOVE, BLOCKED, SCOPEALL, 0, BLOCKED, SCOPEALL, 0,
        NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEOCCUPIED);

    public static final HazardHexside SLOPE = new HazardHexside("Slope",
        SLOWFOREIGNER, FREEMOVE, SKILLPENALTY, SCOPEFOREIGNER, -1, POWERBONUS,
        SCOPENATIVE, 1, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0,
        RANGESTRIKEOCCUPIED);

    public static final HazardHexside TOWER = new HazardHexside("Tower",
        SLOWALL, FREEMOVE, SKILLBONUS, SCOPEALL, 1, SKILLPENALTY, SCOPEALL, 1,
        SKILLPENALTY, SCOPEALL, 1, NOEFFECT, SCOPENULL, 0, RANGESTRIKEWALL);

    // TODO Verify
    public static final HazardHexside RIVER = new HazardHexside("River",
        BLOCKFOREIGNER, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL,
        0, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("+++++++++++++++++++++++++++\n");
        builder.append(getName());
        builder.append(":\n");
        return builder.toString();
    }

    public boolean blocksFlying()
    {
        return effectOnFlyerMovement.equals(BLOCKALL);
    }

    public boolean isFlyersOnly()
    {
        return effectOnGroundMovement.equals(BLOCKALL);
    }

    public boolean isNativeFlyersOnly()
    {
        return effectOnFlyerMovement.equals(BLOCKFOREIGNER);
    }

    public boolean slowsNonNative()
    {
        return effectOnFlyerMovement.equals(SLOWFOREIGNER)
            || effectOnGroundMovement.equals(SLOWFOREIGNER);
    }

    public boolean isNativeBonusTerrain()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isNativeOnly()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isNonNativePenaltyTerrain()
    {
        // TODO Auto-generated method stub
        return false;
    }
}
