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

    /** The effect on a combat category (strike|rangestrike, defense|attack),
     * including the effect (what), the scope (who), and the adjustement
     * (how much).
     */
    public static class CombatEffect
    {
        public final EffectOnStrike effect;
        public final ScopeOfEffectOnStrike scope;
        public final int adjustment;

        CombatEffect(EffectOnStrike effect, ScopeOfEffectOnStrike scope,
            int adjustment)
        {
            this.effect = effect;
            this.scope = scope;
            this.adjustment = adjustment;
        }
    }

    /** CombatEffect to apply when a creature is struck in this terrain.
     */
    public final CombatEffect defenseEffect;
    /** CombatEffect to apply when a creature strike out from this terrain.
     */
    public final CombatEffect attackEffect;
    /** CombatEffect to apply when a creature is rangestruck in this terrain.
     */
    public final CombatEffect rangedDefenseEffect;
    /** CombatEffect to apply when a creature rangestrike out from this terrain.
     */
    public final CombatEffect rangedAttackEffect;

    public final RangeStrikeSpecialEffect rangeStrikeSpecial;
    public final SpecialEffect terrainSpecial;

    public Hazards(String name, char code,
        EffectOnMovement effectOnGroundMovement,
        EffectOnMovement effectOnFlyerMovement, CombatEffect defenseEffect,
        CombatEffect attackEffect, CombatEffect rangedDefenseEffect,
        CombatEffect rangedAttackEffect,
        RangeStrikeSpecialEffect RangeStrikeSpecial,
        SpecialEffect terrainSpecial)
    {
        this.name = name;
        this.code = code;
        this.effectOnGroundMovement = effectOnGroundMovement;
        this.effectOnFlyerMovement = effectOnFlyerMovement;

        this.defenseEffect = defenseEffect;
        this.attackEffect = attackEffect;
        this.rangedDefenseEffect = rangedDefenseEffect;
        this.rangedAttackEffect = rangedAttackEffect;

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
