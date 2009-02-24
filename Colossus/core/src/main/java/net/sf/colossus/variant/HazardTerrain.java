package net.sf.colossus.variant;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/** 
 * This is a typesafe enumeration of all hazard terrains, i.e. the
 * terrains used in the battle maps.
 */
public class HazardTerrain extends Hazards
{
    private static final Logger LOGGER = Logger.getLogger(HazardTerrain.class.getName());
    /**
     * A map from the serialization string of a terrain to the instances.
     */
    private final static Map<String, HazardTerrain> TERRAIN_MAP = new HashMap<String, HazardTerrain>();

    public HazardTerrain(String name, char code,
        EffectOnMovement effectOnGroundMovement,
        EffectOnMovement effectOnFlyerMovement,
        EffectOnStrike effectforDefendingInTerrain,
        ScopeOfEffectOnStrike scopeForDefenceEffect,
        int defenceEffectAdjustment,
        EffectOnStrike effectforAttackingFromTerrain,
        ScopeOfEffectOnStrike scopeForAttackEffect,
        int attackEffectAdjustment,
        EffectOnStrike effectForBeingRangeStruckInTerrain,
        ScopeOfEffectOnStrike scopeForRangeStruckEffect,
        int RangeStruckEffectAdjustment,
        EffectOnStrike effectforRangeStrikeFromTerrain,
        ScopeOfEffectOnStrike scopeForRangeStrikeEffect,
        int RangeStrikeEffectAdjustment,
        RangeStrikeSpecialEffect RangeStrikeSpecial,
        SpecialEffect terrainSpecial)
    {
        super(name, code, effectOnGroundMovement, effectOnFlyerMovement,
            effectforDefendingInTerrain, scopeForDefenceEffect,
            defenceEffectAdjustment, effectforAttackingFromTerrain,
            scopeForAttackEffect, attackEffectAdjustment,
            effectForBeingRangeStruckInTerrain, scopeForRangeStruckEffect,
            RangeStruckEffectAdjustment, effectforRangeStrikeFromTerrain,
            scopeForRangeStrikeEffect, RangeStrikeEffectAdjustment,
            RangeStrikeSpecial, terrainSpecial);
        TERRAIN_MAP.put(name, this);
        LOGGER.finest("Terrain " + name + " with code " + code + " is giving out:" +
                "\n\tgetSkillPenaltyStrikeFrom(true) = " + getSkillPenaltyStrikeFrom(true) +
                "\n\tgetSkillPenaltyStrikeFrom(false) = " + getSkillPenaltyStrikeFrom(false));
        LOGGER.finest("Terrain " + name + " with code " + code + " is giving out:" +
                "\n\tgetSkillBonusStruckFrom(true, true) = " + getSkillBonusStruckIn(true, true) +
                "\n\tgetSkillBonusStruckFrom(true, false) = " + getSkillBonusStruckIn(true, false) +
                "\n\tgetSkillBonusStruckFrom(false, true) = " + getSkillBonusStruckIn(false, true) +
                "\n\tgetSkillBonusStruckFrom(false, false) = " + getSkillBonusStruckIn(false, false));
    }
    
    public boolean isNativeBonusTerrain()
    {
        return (scopeForAttackEffect.equals(ScopeOfEffectOnStrike.NATIVES)
            || scopeForDefenceEffect.equals(ScopeOfEffectOnStrike.NATIVES)
            || scopeForAttackEffect.equals(ScopeOfEffectOnStrike.PATRIOTS) || scopeForDefenceEffect
            .equals(ScopeOfEffectOnStrike.PATRIOTS))
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
        boolean effectsAttack = 
            scopeForAttackEffect. equals(ScopeOfEffectOnStrike.FOREIGNERS)
        && (   effectforAttackingFromTerrain.equals(EffectOnStrike.SKILLPENALTY)
            || effectforAttackingFromTerrain.equals(EffectOnStrike.POWERPENALTY)
           );
        boolean effectsDefence = 
            scopeForDefenceEffect.equals(ScopeOfEffectOnStrike.FOREIGNERS)
        && (    effectforDefendingInTerrain.equals(EffectOnStrike.SKILLPENALTY)
             || effectforDefendingInTerrain.equals(EffectOnStrike.POWERPENALTY)
           );
        return (effectsAttack || effectsDefence);
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

    /* genuine Titan Hazard */
    public static final HazardTerrain PLAINS = new HazardTerrain("Plains",
        ' ', EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    /** Tree is genuine Titan hazard, but the effect on anybody inside is
     * custom, as noone can enter a tree in Titan.
     * For Colossus, native defending in a Tree against a non-native
     * gains 1 Skill. It blocks rangestrike through it, but not to it.
     */
    public static final HazardTerrain TREE = new HazardTerrain("Tree", 't',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.PATRIOTS, 1,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.IMPERIALS, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain BRAMBLES = new HazardTerrain("Brambles",
        'b', EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.SLOWFOREIGNER,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.PATRIOTS, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.FOREIGNERS, 1,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.PATRIOTS, 1,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKESKILLPENALTY,
        SpecialEffect.NOSPECIAL);

    public static final HazardTerrain DRIFT = new HazardTerrain("Drift", 'd',
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.SLOWFOREIGNER,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.HEALTHDRAIN);

    public static final HazardTerrain VOLCANO = new HazardTerrain("Volcano",
        'v', EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.BLOCKFOREIGNER,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 2,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.NATIVES, 1,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain BOG = new HazardTerrain("Bog", 'o',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain SAND = new HazardTerrain("Sand", 's',
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain TOWER = new HazardTerrain("Tower", 'w',
        EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    /* extra standard Colossus hazard */
    public static final HazardTerrain LAKE = new HazardTerrain("Lake", 'l',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    /** Stone native gains 1 skill when defending against non-native ;
     * Stone cannot be entered by Flyer or non-native.
     */
    public static final HazardTerrain STONE = new HazardTerrain("Stone", 'n',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.BLOCKALL,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.PATRIOTS, 1,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED, SpecialEffect.NOSPECIAL);

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

    /** Return the penalty to apply to the Strike Factor of a creature
     * striking out from that terrain on a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @return The amount of penalty to apply.
     */
    public int getSkillPenaltyStrikeFrom(boolean attackerIsNative) {
        if (effectforAttackingFromTerrain == EffectOnStrike.NOEFFECT) {
            return 0;
        }
        if (effectforAttackingFromTerrain == EffectOnStrike.SKILLPENALTY) {
            if (scopeForAttackEffect == ScopeOfEffectOnStrike.ALL) {
                return attackEffectAdjustment;
            }
            if (attackerIsNative &&
                    scopeForAttackEffect == ScopeOfEffectOnStrike.NATIVES) {
                return attackEffectAdjustment;
            }
            if (!attackerIsNative &&
                    scopeForAttackEffect == ScopeOfEffectOnStrike.FOREIGNERS) {
                return attackEffectAdjustment;
            }
            if ((scopeForAttackEffect == ScopeOfEffectOnStrike.PATRIOTS) ||
                (scopeForAttackEffect == ScopeOfEffectOnStrike.IMPERIALS)) {
                /* not enough information to decide */
                LOGGER.warning("Called without the native status of the defender," +
                        "and effect " + scopeForAttackEffect + " requires it.");
                return 0;
            }
            return 0;
        }
        if (effectforAttackingFromTerrain == EffectOnStrike.SKILLBONUS) {
            LOGGER.warning("Called with an unsupported effect " +
                    effectforAttackingFromTerrain);
            return 0;
        }
        if (effectforAttackingFromTerrain == EffectOnStrike.BLOCKED) {
            LOGGER.warning("Called with an unlikely effect " +
                    effectforAttackingFromTerrain);
            return 0;
        }
        return 0;
    }
    public int getSkillBonusStruckIn(boolean attackerIsNative, boolean defenderIsNative) {
        if (effectforDefendingInTerrain == EffectOnStrike.SKILLBONUS) {
            if (scopeForDefenceEffect == ScopeOfEffectOnStrike.ALL) {
                return defenceEffectAdjustment;
            }
            if (defenderIsNative &&
                    scopeForDefenceEffect == ScopeOfEffectOnStrike.NATIVES) {
                return defenceEffectAdjustment;
            }
            if (!defenderIsNative &&
                    scopeForDefenceEffect == ScopeOfEffectOnStrike.FOREIGNERS) {
                return defenceEffectAdjustment;
            }
            if (defenderIsNative &&
                    !attackerIsNative &&
                    scopeForDefenceEffect == ScopeOfEffectOnStrike.PATRIOTS) {
                return defenceEffectAdjustment;
            }
            if (!defenderIsNative &&
                    attackerIsNative &&
                    scopeForDefenceEffect == ScopeOfEffectOnStrike.IMPERIALS) {
                return defenceEffectAdjustment;
            }
            return 0;
        }
        if (effectforDefendingInTerrain == EffectOnStrike.SKILLPENALTY) {
            LOGGER.warning("Called with an unsupported effect " +
                    effectforDefendingInTerrain);
            return 0;
        }
        if (effectforDefendingInTerrain == EffectOnStrike.BLOCKED) {
            LOGGER.warning("Called with an unlikely effect " +
                    effectforDefendingInTerrain);
            return 0;
        }
        return 0;
    }
        /*
     * Scope Constants -
     * All - is everyone
     * Natives means Natives vs anyone
     * Patriots means Natives vs Non-Natives
     * Foreigners are Non-Natives vs anyone
     * Imperials means Non-Natives vs Natives
     */
    /** Whether this terrain blocks rangestrike.
     * @return Whether this terrain blocks rangestrike.
     */
    public boolean blocksLineOfSight() {
        return rangeStrikeSpecial == RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED;
    }
}
