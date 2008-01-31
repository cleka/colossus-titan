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
    public final EffectOnMovement effectOnGroundMovement; // Also flyers at end of move
    public final EffectOnMovement effectOnFlyerMovement;

    public final EffectOnStrike effectforDefendingInTerrain;
    public final ScopeOfEffectOnStrike scopeForDefenceEffect;
    public final int defenceEffectAdjustment;

    public final EffectOnStrike effectforAttackingFromTerrain;
    public final ScopeOfEffectOnStrike scopeForAttackEffect;
    public final int AttackEffectAdjustment;

    public final EffectOnStrike effectForBeingRangeSruckInTerrain;
    public final ScopeOfEffectOnStrike scopeForRangeStruckEffect;
    public final int RangeStruckEffectAdjustment;

    public final EffectOnStrike effectforRangeStrikeFromTerrain;
    public final ScopeOfEffectOnStrike scopeForRangeStrikeEffect;
    public final int RangeStrikeEffectAdjustment;

    public final RangeStrikeSpecialEffect RangeStrikeSpecial;

    /**
     * A map from the serialization string of a terrain to the instances.
     */
    private final static Map<String, HazardHexside> HEXSIDE_MAP = new HashMap<String, HazardHexside>();

    public HazardHexside(String name, EffectOnMovement effectOnGroundMovement,
        EffectOnMovement effectOnFlyerMovement, EffectOnStrike effectforDefendingInTerrain,
        ScopeOfEffectOnStrike scopeForDefenceEffect, int defenceEffectAdjustment,
        EffectOnStrike effectforAttackingFromTerrain, ScopeOfEffectOnStrike scopeForAttackEffect,
        int attackEffectAdjustment, EffectOnStrike effectForBeingRangeSruckInTerrain,
        ScopeOfEffectOnStrike scopeForRangeStruckEffect, int RangeStruckEffectAdjustment,
        EffectOnStrike effectforRangeStrikeFromTerrain,
        ScopeOfEffectOnStrike scopeForRangeStrikeEffect, int RangeStrikeEffectAdjustment,
        RangeStrikeSpecialEffect RangeStrikeSpecial)
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
        EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY,
        0, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE);

    public static final HazardHexside DUNE = new HazardHexside("Dune",
        EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE, EffectOnStrike.POWERPENALTY,
        ScopeOfEffectOnStrike.FOREIGNERS, 1, EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 2,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED);

    public static final HazardHexside CLIFF = new HazardHexside("Cliff",
        EffectOnMovement.BLOCKALL, EffectOnMovement.FREEMOVE, EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.ALL, 0, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED);

    public static final HazardHexside SLOPE = new HazardHexside("Slope",
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.FREEMOVE, EffectOnStrike.SKILLPENALTY,
        ScopeOfEffectOnStrike.FOREIGNERS, -1, EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 1,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED);

    public static final HazardHexside TOWER = new HazardHexside("Tower",
        EffectOnMovement.SLOWALL, EffectOnMovement.FREEMOVE, EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.ALL, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.ALL, 1, EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.ALL, 1,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEWALL);

    // TODO Verify
    public static final HazardHexside RIVER = new HazardHexside("River",
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE, EffectOnStrike.NOEFFECT,
        ScopeOfEffectOnStrike.NOBODY, 0, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0, EffectOnStrike.NOEFFECT,
        ScopeOfEffectOnStrike.NOBODY, 0, EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.NOBODY, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE);

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
