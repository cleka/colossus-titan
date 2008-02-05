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
    public final EffectOnMovement effectOnGroundMovement; // Also flyers at end of move
    public final EffectOnMovement effectOnFlyerMovement;

    public final EffectOnStrike effectforDefendingInTerrain;
    public final ScopeOfEffectOnStrike scopeForDefenceEffect;
    public final int defenceEffectAdjustment;

    public final EffectOnStrike effectforAttackingFromTerrain;
    public final ScopeOfEffectOnStrike scopeForAttackEffect;
    public final int attackEffectAdjustment;

    public final EffectOnStrike effectForBeingRangeSruckInTerrain;
    public final ScopeOfEffectOnStrike scopeForRangeStruckEffect;
    public final int rangeStruckEffectAdjustment;

    public final EffectOnStrike effectforRangeStrikeFromTerrain;
    public final ScopeOfEffectOnStrike scopeForRangeStrikeEffect;
    public final int rangeStrikeEffectAdjustment;

    public final RangeStrikeSpecialEffect rangeStrikeSpecial;
    public final TerrainSpecial terrainSpecial;

    /**
     * A map from the serialization string of a terrain to the instances.
     */
    private final static Map<String, HazardTerrain> TERRAIN_MAP = new HashMap<String, HazardTerrain>();

    public HazardTerrain(String name, EffectOnMovement effectOnGroundMovement,
        EffectOnMovement effectOnFlyerMovement,
        EffectOnStrike effectforDefendingInTerrain,
        ScopeOfEffectOnStrike scopeForDefenceEffect,
        int defenceEffectAdjustment,
        EffectOnStrike effectforAttackingFromTerrain,
        ScopeOfEffectOnStrike scopeForAttackEffect,
        int attackEffectAdjustment,
        EffectOnStrike effectForBeingRangeSruckInTerrain,
        ScopeOfEffectOnStrike scopeForRangeStruckEffect,
        int RangeStruckEffectAdjustment,
        EffectOnStrike effectforRangeStrikeFromTerrain,
        ScopeOfEffectOnStrike scopeForRangeStrikeEffect,
        int RangeStrikeEffectAdjustment,
        RangeStrikeSpecialEffect RangeStrikeSpecial,
        TerrainSpecial terrainSpecial)
    {
        this.name = name;
        this.effectOnGroundMovement = effectOnGroundMovement;
        this.effectOnFlyerMovement = effectOnFlyerMovement;

        this.effectforDefendingInTerrain = effectforDefendingInTerrain;
        this.scopeForDefenceEffect = scopeForDefenceEffect;
        this.defenceEffectAdjustment = defenceEffectAdjustment;

        this.effectforAttackingFromTerrain = effectforAttackingFromTerrain;
        this.scopeForAttackEffect = scopeForAttackEffect;
        this.attackEffectAdjustment = attackEffectAdjustment;
        this.effectForBeingRangeSruckInTerrain = effectForBeingRangeSruckInTerrain;
        this.scopeForRangeStruckEffect = scopeForRangeStruckEffect;
        this.rangeStruckEffectAdjustment = RangeStruckEffectAdjustment;
        this.effectforRangeStrikeFromTerrain = effectforRangeStrikeFromTerrain;
        this.scopeForRangeStrikeEffect = scopeForRangeStrikeEffect;
        this.rangeStrikeEffectAdjustment = RangeStrikeEffectAdjustment;
        this.rangeStrikeSpecial = RangeStrikeSpecial;
        this.terrainSpecial = terrainSpecial;

        TERRAIN_MAP.put(name, this);
    }

    public String getName()
    {
        return name;
    }

    public boolean isNativeBonusTerrain()
    {
        return (scopeForAttackEffect.equals(ScopeOfEffectOnStrike.NATIVES) || scopeForDefenceEffect
            .equals(ScopeOfEffectOnStrike.NATIVES))
            && (effectforAttackingFromTerrain
                .equals(EffectOnStrike.SKILLBONUS)
                || effectforAttackingFromTerrain
                    .equals(EffectOnStrike.POWERBONUS)
                || effectforDefendingInTerrain
                    .equals(EffectOnStrike.SKILLBONUS) || effectforDefendingInTerrain
                .equals(EffectOnStrike.POWERBONUS));
    }

    public boolean isNonNativePenaltyTerrain()
    {
        return (scopeForAttackEffect.equals(ScopeOfEffectOnStrike.FOREIGNERS) || scopeForDefenceEffect
            .equals(ScopeOfEffectOnStrike.FOREIGNERS))
            && (effectforAttackingFromTerrain
                .equals(EffectOnStrike.SKILLPENALTY)
                || effectforAttackingFromTerrain
                    .equals(EffectOnStrike.POWERPENALTY)
                || effectforDefendingInTerrain
                    .equals(EffectOnStrike.SKILLPENALTY) || effectforDefendingInTerrain
                .equals(EffectOnStrike.POWERPENALTY));
    }

    public boolean isNativeOnly()
    {
        return effectOnGroundMovement.equals(EffectOnMovement.BLOCKFOREIGNER);
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
        EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain TREE = new HazardTerrain("Tree",
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain BRAMBLES = new HazardTerrain("Brambles",
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.SLOWFOREIGNER,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.NATIVES, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.FOREIGNERS, -1,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.NATIVES, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.FOREIGNERS, -1,
        RangeStrikeSpecialEffect.RANGESTRIKESKILLPENALTY,
        TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain DRIFT = new HazardTerrain("Drift",
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.SLOWFOREIGNER,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.HEALTHDRAIN);

    public static final HazardTerrain VOLCANO = new HazardTerrain("Volcano",
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.BLOCKFOREIGNER,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 2,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.NATIVES, 1,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain BOG = new HazardTerrain("Bog",
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain SAND = new HazardTerrain("Sand",
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain TOWER = new HazardTerrain("Tower",
        EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain LAKE = new HazardTerrain("Lake",
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

    public static final HazardTerrain STONE = new HazardTerrain("Stone",
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.BLOCKALL,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, TerrainSpecial.NOSPECIAL);

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
        return effectOnFlyerMovement.equals(EffectOnMovement.BLOCKALL);
    }

    public boolean isFlyersOnly()
    {
        return effectOnGroundMovement.equals(EffectOnMovement.BLOCKALL);
    }

    public boolean isNativeFlyersOnly()
    {
        return effectOnFlyerMovement.equals(EffectOnMovement.BLOCKFOREIGNER);
    }

    public boolean slowsNonNative()
    {
        return effectOnFlyerMovement.equals(EffectOnMovement.SLOWFOREIGNER)
            || effectOnGroundMovement.equals(EffectOnMovement.SLOWFOREIGNER);
    }
}
