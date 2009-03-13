package net.sf.colossus.variant;


import java.util.logging.Logger;


/** 
 * Superclass for BattleMap hazards: Terrain & Hexsides, 
 */
public abstract class Hazards implements HazardConstants
{
    private static final Logger LOGGER = Logger.getLogger(Hazards.class
        .getName());
    /**
     * The name used for serialization.
     */
    private final String name;
    private final char code;

    /**
     * Properties - 
     */
    /** Effect to apply to land-bound creature, and to flyer at the
     * end of their movement (when they, presumably, land.).
     */
    public final EffectOnMovement effectOnGroundMovement;
    /** Effect to apply to flying creature, except at the
     * end of their movement (when they, presumably, land.).
     */
    public final EffectOnMovement effectOnFlyerMovement;

    /** WiP : instead of a bunch of variables, group them by
     * category so it's easier to recognize them / deal with them.
     */
    @SuppressWarnings("unused")
    private class CombatEffect
    {
        final EffectOnStrike effect;
        final ScopeOfEffectOnStrike scope;
        final int adjustement;

        CombatEffect(EffectOnStrike effect, ScopeOfEffectOnStrike scope,
            int adjustement)
        {
            this.effect = effect;
            this.scope = scope;
            this.adjustement = adjustement;
        }
    }

    /** Effect to apply when a creature is struck in this terrain.
     */
    public final EffectOnStrike effectforDefendingInTerrain;
    /** Scope of {@link effectforDefendingInTerrain}. Note that
     * the first nativity (first vs. second) aply to the defending
     * creature in this case.
     */
    public final ScopeOfEffectOnStrike scopeForDefenceEffect;
    /** Amount of {@link effectforDefendingInTerrain} */
    public final int defenceEffectAdjustment;

    /** Effect to apply when a creature strike out from this terrain.
     */
    public final EffectOnStrike effectforAttackingFromTerrain;
    /** Scope of {@link effectforAttackingFromTerrain}. Note that
     * the first nativity (first vs. second) aply to the attacking
     * creature in this case.
     */
    public final ScopeOfEffectOnStrike scopeForAttackEffect;
    /** Amount of {@link effectforAttackingFromTerrain} */
    public final int attackEffectAdjustment;

    public final EffectOnStrike effectForBeingRangeStruckInTerrain;
    public final ScopeOfEffectOnStrike scopeForRangeStruckEffect;
    public final int rangeStruckEffectAdjustment;

    public final EffectOnStrike effectforRangeStrikeFromTerrain;
    public final ScopeOfEffectOnStrike scopeForRangeStrikeEffect;
    public final int rangeStrikeEffectAdjustment;

    public final RangeStrikeSpecialEffect rangeStrikeSpecial;
    public final SpecialEffect terrainSpecial;

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
        this.attackEffectAdjustment = attackEffectAdjustment;
        this.effectForBeingRangeStruckInTerrain = effectForBeingRangeSruckInTerrain;
        this.scopeForRangeStruckEffect = scopeForRangeStruckEffect;
        this.rangeStruckEffectAdjustment = RangeStruckEffectAdjustment;
        this.effectforRangeStrikeFromTerrain = effectforRangeStrikeFromTerrain;
        this.scopeForRangeStrikeEffect = scopeForRangeStrikeEffect;
        this.rangeStrikeEffectAdjustment = RangeStrikeEffectAdjustment;
        this.rangeStrikeSpecial = RangeStrikeSpecial;
        this.terrainSpecial = terrainSpecial;

        LOGGER.finest("Create Hazards: " + name);
    }

    public String getName()
    {
        return name;
    }

    public char getCode()
    {
        return code;
    }

    @Override
    public abstract String toString();
}
