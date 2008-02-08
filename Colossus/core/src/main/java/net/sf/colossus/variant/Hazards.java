package net.sf.colossus.variant;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/** 
 * Superclass for BattleMap hazards: Terrain & Hexsides, 
 */
public abstract class Hazards implements HazardConstants
{
    /**
     * The name used for serialization.
     */
    private final String name;
    private final char code;

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
    public final SpecialEffect terrainSpecial;

    /**
     * A map from the serialization string of a terrain to the instances.
     */
    private final static Map<String, Hazards> MAP = new HashMap<String, Hazards>();

    public Hazards(String name, char code,
        EffectOnMovement effectOnGroundMovement,
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
        SpecialEffect terrainSpecial)
    {
        this.name = name;
        this.code = code;
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
        this.terrainSpecial = terrainSpecial;

        MAP.put(name, this);
    }

    public String getName()
    {
        return name;
    }

    public char getCode()
    {
        return code;
    }

    static Hazards getByName(String name)
    {
        return MAP.get(name);
    }

    /**
     * Returns all available hazard terrains.
     * 
     * This is not variant-specific, any terrain known to the program is listed even
     * if it is not available in the current variant.
     * 
     * TODO this should really be a question to ask a variant instance
     */
    static final Collection<Hazards> getAllHazards()
    {
        return MAP.values();
    }

    @Override
    public abstract String toString();
}
