package net.sf.colossus.variant;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/** 
 * This is a typesafe enumeration of all hazard terrains, i.e. the
 * terrains used in the battle maps.
 */
public class HazardTerrain implements HazardConstants
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
    private final static Map<String, HazardTerrain> TERRAIN_MAP = new HashMap<String, HazardTerrain>();

    public HazardTerrain(String name, String effectOnGroundMovement,
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

        TERRAIN_MAP.put(name, this);
    }

    public String getName()
    {
        return name;
    }

    public boolean isNativeBonusTerrain()
    {
        return (scopeForAttackEffect.equals(SCOPENATIVE) || scopeForDefenceEffect
            .equals(SCOPENATIVE))
            && (effectforAttackingFromTerrain.equals(SKILLBONUS)
                || effectforAttackingFromTerrain.equals(POWERBONUS)
                || effectforDefendingInTerrain.equals(SKILLBONUS) || effectforDefendingInTerrain
                .equals(POWERBONUS));
    }

    public boolean isNonNativePenaltyTerrain()
    {
        return (scopeForAttackEffect.equals(SCOPEFOREIGNER) || scopeForDefenceEffect
            .equals(SCOPEFOREIGNER))
            && (effectforAttackingFromTerrain.equals(SKILLPENALTY)
                || effectforAttackingFromTerrain.equals(POWERPENALTY)
                || effectforDefendingInTerrain.equals(SKILLPENALTY) || effectforDefendingInTerrain
                .equals(POWERPENALTY));
    }

    public boolean isNativeOnly()
    {
        return effectOnGroundMovement.equals(BLOCKFOREIGNER);
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
        FREEMOVE, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0,
        NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    public static final HazardTerrain TREE = new HazardTerrain("Tree",
        BLOCKFOREIGNER, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL,
        0, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    public static final HazardTerrain BRAMBLES = new HazardTerrain("Brambles",
        SLOWFOREIGNER, SLOWFOREIGNER, SKILLBONUS, SCOPENATIVE, 1,
        SKILLPENALTY, SCOPEFOREIGNER, -1, SKILLBONUS, SCOPENATIVE, 1,
        SKILLPENALTY, SCOPEFOREIGNER, -1, RANGESTRIKESKILLPENALTY);

    public static final HazardTerrain DRIFT = new HazardTerrain("Drift",
        SLOWFOREIGNER, SLOWFOREIGNER, HEALTHDRAIN, SCOPEFOREIGNER, -1,
        HEALTHDRAIN, SCOPEFOREIGNER, -1, HEALTHDRAIN, SCOPEFOREIGNER, -1,
        HEALTHDRAIN, SCOPEFOREIGNER, -1, RANGESTRIKEFREE);

    public static final HazardTerrain VOLCANO = new HazardTerrain("Volcano",
        BLOCKFOREIGNER, BLOCKFOREIGNER, NOEFFECT, SCOPENULL, 0, POWERBONUS,
        SCOPENATIVE, 2, SKILLBONUS, SCOPENATIVE, 1, POWERBONUS, SCOPENATIVE,
        0, RANGESTRIKEFREE);

    public static final HazardTerrain BOG = new HazardTerrain("Bog",
        BLOCKFOREIGNER, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL,
        0, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    public static final HazardTerrain SAND = new HazardTerrain("Sand",
        SLOWFOREIGNER, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL,
        0, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    public static final HazardTerrain TOWER = new HazardTerrain("Tower",
        FREEMOVE, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0,
        NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    public static final HazardTerrain LAKE = new HazardTerrain("Lake",
        BLOCKFOREIGNER, FREEMOVE, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL,
        0, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL, 0, RANGESTRIKEFREE);

    public static final HazardTerrain STONE = new HazardTerrain("Stone",
        BLOCKFOREIGNER, BLOCKALL, NOEFFECT, SCOPENULL, 0, NOEFFECT, SCOPENULL,
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
}
