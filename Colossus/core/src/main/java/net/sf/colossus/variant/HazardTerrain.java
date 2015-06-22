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
    private static final Logger LOGGER = Logger.getLogger(HazardTerrain.class
        .getName());
    /**
     * A map from the serialization string of a terrain to the instances.
     */
    private final static Map<String, HazardTerrain> TERRAIN_MAP = new HashMap<String, HazardTerrain>();

    public HazardTerrain(String name, char code,
        EffectOnMovement effectOnGroundMovement,
        EffectOnMovement effectOnFlyerMovement, CombatEffect defenseEffect,
        CombatEffect attackEffect, CombatEffect rangedDefenseEffect,
        CombatEffect rangedAttackEffect,
        RangeStrikeSpecialEffect RangeStrikeSpecial,
        SpecialEffect terrainSpecial)
    {
        super(name, code, effectOnGroundMovement, effectOnFlyerMovement,
            defenseEffect, attackEffect, rangedDefenseEffect,
            rangedAttackEffect, RangeStrikeSpecial, terrainSpecial);
        TERRAIN_MAP.put(name, this);
        /*
        LOGGER.finest("Terrain " + name + " with code " + code + " is giving out:" +
                "\n\tgetSkillPenaltyStrikeFrom(true) = " + getSkillPenaltyStrikeFrom(true) +
                "\n\tgetSkillPenaltyStrikeFrom(false) = " + getSkillPenaltyStrikeFrom(false));
        LOGGER.finest(
                "\n\tgetSkillBonusStruckFrom(true, true) = " + getSkillBonusStruckIn(true, true) +
                "\n\tgetSkillBonusStruckFrom(true, false) = " + getSkillBonusStruckIn(true, false) +
                "\n\tgetSkillBonusStruckFrom(false, true) = " + getSkillBonusStruckIn(false, true) +
                "\n\tgetSkillBonusStruckFrom(false, false) = " + getSkillBonusStruckIn(false, false));
        LOGGER.finest(
                "\n\tslowsGround(true) = " + slowsGround(true) +
                "\n\tslowsGround(false) = " + slowsGround(false));
        LOGGER.finest(
                "\n\tslowsFlyer(true) = " + slowsFlyer(true) +
                "\n\tslowsFlyer(false) = " + slowsFlyer(false));
         */
        if ((effectOnGroundMovement == EffectOnMovement.BLOCKFOREIGNER)
            && (effectOnFlyerMovement == EffectOnMovement.BLOCKALL))
        {
            LOGGER.warning("Flyers are all blocked, but ground only block"
                + " non-natives in Hazardterrain " + name
                + ". This combinations might cause trouble.");
        }
    }

    public boolean isNativeBonusTerrain()
    {
        return (attackEffect.scope.equals(ScopeOfEffectOnStrike.NATIVES)
            || defenseEffect.scope.equals(ScopeOfEffectOnStrike.NATIVES)
            || attackEffect.scope.equals(ScopeOfEffectOnStrike.PATRIOTS) || defenseEffect.scope
                .equals(ScopeOfEffectOnStrike.PATRIOTS))
            && (attackEffect.effect.equals(EffectOnStrike.SKILLBONUS)
                || attackEffect.effect.equals(EffectOnStrike.POWERBONUS)
                || defenseEffect.effect.equals(EffectOnStrike.SKILLBONUS) || defenseEffect.effect
                    .equals(EffectOnStrike.POWERBONUS));
    }

    public boolean isNonNativePenaltyTerrain()
    {
        boolean effectsAttack = attackEffect.scope
            .equals(ScopeOfEffectOnStrike.FOREIGNERS)
            && (attackEffect.effect.equals(EffectOnStrike.SKILLPENALTY) || attackEffect.effect
                .equals(EffectOnStrike.POWERPENALTY));
        boolean effectsDefence = defenseEffect.scope
            .equals(ScopeOfEffectOnStrike.FOREIGNERS)
            && (defenseEffect.effect.equals(EffectOnStrike.SKILLPENALTY) || defenseEffect.effect
                .equals(EffectOnStrike.POWERPENALTY));
        return (effectsAttack || effectsDefence);
    }

    /** Get the HazardTerrain by its name.
     * Ideally, this shouldn't be used anywhere but in the Variant code
     * at load-time, thus becoming package private.
     * @param name The name of the terrain to access.
     * @return The terrain of the requested name.
     */
    public static HazardTerrain getTerrainByName(String name)
    {
        return TERRAIN_MAP.get(name);
    }

    public static HazardTerrain getDefaultTerrain()
    {
        return getTerrainByName("Plains");
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

    /* ALL the static objects here should be package private (or even protected),
     * no-one should do direct access. That's why the static accessor
     * are for.
     * The main problems before this becomes possible are:
     * 1) XMLparser. We should fix the attributes to use the proper name and
     *    simplify the parser.
     * 2) The Color in BattleHex. it should be moved here.
     */

    /* genuine Titan Hazard, with no effect whatsoever */
    static final HazardTerrain PLAINS = new HazardTerrain(
        "Plains",
        ' ',
        EffectOnMovement.FREEMOVE,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    /** Tree is a genuine Titan hazard, but the effect on anybody inside is
     * custom, as noone can enter a tree in Titan.
     * For Colossus, native defending in a Tree against a non-native
     * gains 1 Skill. It blocks rangestrike through it, but not to it.
     */
    static final HazardTerrain TREE = new HazardTerrain("Tree", 't',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.SKILLBONUS,
            ScopeOfEffectOnStrike.PATRIOTS, 1), new CombatEffect(
            EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.BLOCKED,
            ScopeOfEffectOnStrike.IMPERIALS, 0), new CombatEffect(
            EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain BRAMBLES = new HazardTerrain("Brambles",
        'b', EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.SLOWFOREIGNER,
        new CombatEffect(EffectOnStrike.SKILLBONUS,
            ScopeOfEffectOnStrike.PATRIOTS, 1), new CombatEffect(
            EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.FOREIGNERS, 1),
        new CombatEffect(EffectOnStrike.SKILLBONUS,
            ScopeOfEffectOnStrike.PATRIOTS, 1), new CombatEffect(
            EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKESKILLPENALTY,
        SpecialEffect.NOSPECIAL);

    static final HazardTerrain DRIFT = new HazardTerrain(
        "Drift",
        'd',
        EffectOnMovement.SLOWFOREIGNER,
        EffectOnMovement.SLOWFOREIGNER,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.HEALTHDRAIN);

    public static final HazardTerrain VOLCANO = new HazardTerrain(
        "Volcano",
        'v',
        EffectOnMovement.BLOCKFOREIGNER,
        EffectOnMovement.BLOCKFOREIGNER,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.POWERBONUS,
            ScopeOfEffectOnStrike.NATIVES, 2), new CombatEffect(
            EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.NATIVES, 1),
        new CombatEffect(EffectOnStrike.POWERBONUS,
            ScopeOfEffectOnStrike.NATIVES, 2),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    static final HazardTerrain BOG = new HazardTerrain(
        "Bog",
        'o',
        EffectOnMovement.BLOCKFOREIGNER,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    static final HazardTerrain SAND = new HazardTerrain(
        "Sand",
        's',
        EffectOnMovement.SLOWFOREIGNER,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE,
        SpecialEffect.HEALTHDRAIN_WATERDWELLER);

    static final HazardTerrain TOWER = new HazardTerrain(
        "Tower",
        'w',
        EffectOnMovement.FREEMOVE,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    /* extra standard Colossus hazard */
    /** Only waterdweller can enter a lake, but anybody can fly over it.
     * No effect on combat.
     */
    static final HazardTerrain LAKE = new HazardTerrain(
        "Lake",
        'l',
        EffectOnMovement.BLOCKFOREIGNER,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    /* extra standard Colossus hazard */
    /** Stone native gains 1 skill when defending against non-native ;
     * Stone cannot be entered by non-native. No rangestrike can traverse a
     * Stone.
     */
    public static final HazardTerrain STONE = new HazardTerrain(
        "Stone",
        'n',
        EffectOnMovement.BLOCKFOREIGNER,
        EffectOnMovement.BLOCKFOREIGNER,
        new CombatEffect(EffectOnStrike.SKILLBONUS,
            ScopeOfEffectOnStrike.PATRIOTS, 1),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED, SpecialEffect.NOSPECIAL);

    static final HazardTerrain SPRING = new HazardTerrain(
        "Spring",
        'g',
        EffectOnMovement.SLOWFOREIGNER,
        EffectOnMovement.SLOWFOREIGNER,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.HEALTHGAIN);

    static final HazardTerrain TARPIT = new HazardTerrain(
        "TarPit",
        'a',
        EffectOnMovement.SLOWALL,
        EffectOnMovement.SLOWALL,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.PERMSLOW);

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("+++++++++++++++++++++++++++\n");
        builder.append(getName());
        builder.append(":\n");
        return builder.toString();
    }

    public boolean blocksFlyers()
    {
        return effectOnFlyerMovement.equals(EffectOnMovement.BLOCKALL);
    }

    public boolean blocksGround()
    {
        return effectOnGroundMovement.equals(EffectOnMovement.BLOCKALL);
    }

    public boolean isFlyersNativeOnly()
    {
        return effectOnFlyerMovement.equals(EffectOnMovement.BLOCKFOREIGNER);
    }

    public boolean isGroundNativeOnly()
    {
        return effectOnGroundMovement.equals(EffectOnMovement.BLOCKFOREIGNER);
    }

    public boolean slowsGround(boolean isNative)
    {
        if (effectOnGroundMovement == EffectOnMovement.SLOWALL)
        {
            return true;
        }
        if (effectOnGroundMovement == EffectOnMovement.SLOWFOREIGNER)
        {
            return !isNative;
        }
        if (effectOnGroundMovement == EffectOnMovement.BLOCKALL)
        {
            LOGGER.warning("Can't be slowed if everybody is blocked.");
            return false;
        }
        if (effectOnGroundMovement == EffectOnMovement.BLOCKFOREIGNER)
        {
            if (!isNative)
            {
                LOGGER.warning("Can't be slowed if foreigner is blocked.");
            }
            return false;
        }
        return false;
    }

    public boolean slowsFlyer(boolean isNative)
    {
        if (effectOnFlyerMovement == EffectOnMovement.SLOWALL)
        {
            return true;
        }
        if (effectOnFlyerMovement == EffectOnMovement.SLOWFOREIGNER)
        {
            return !isNative;
        }
        if (effectOnFlyerMovement == EffectOnMovement.BLOCKALL)
        {
            LOGGER.warning("Flyers can't be slowed if flyers are blocked.");
            return false;
        }
        if (effectOnFlyerMovement == EffectOnMovement.BLOCKFOREIGNER)
        {
            if (!isNative)
            {
                LOGGER
                    .warning("Foreign flyers can't be slowed if foreign flyers are blocked.");
            }
            return false;
        }
        return false;
    }

    public boolean slows(boolean isNative, boolean isFlyer)
    {
        if (isFlyer && !slowsFlyer(isNative))
        {
            return false;
        }
        if (!slowsGround(isNative))
        {
            return false;
        }
        return true;
    }

    /** Do the real computation of the bonus (negative if penalty).
     * @param firstIsNative Whether the first creature (attacker for attack skill/power, defender for defense skill/power) is native here
     * @param secondIsNative Whether the second creature is native here
     * @param effect The effect to use
     * @param scope The scope to use
     * @param whichIsBonus Which effect is a bonus (power || skill)
     * @param whichIsPenalty Which effect is a penalty (power || skill)
     * @param ovalue The original adjustment of the effect
     * @return The final attacking or defending skill or power
     */
    private int computeSkillOrPowerBonus(final boolean firstIsNative,
        final boolean secondIsNative, final CombatEffect effect,
        final EffectOnStrike whichIsBonus, final EffectOnStrike whichIsPenalty)
    {
        /*
         * Scope Constants -
         * All - is everyone
         * Natives means Natives vs anyone
         * Patriots means Natives vs Non-Natives
         * Foreigners are Non-Natives vs anyone
         * Imperials means Non-Natives vs Natives
         */
        if (effect.effect == EffectOnStrike.NOEFFECT)
        {
            return 0;
        }
        int value = effect.adjustment;
        if ((effect.effect == whichIsPenalty)
            || (effect.effect == whichIsBonus))
        {
            if (effect.effect == whichIsPenalty)
            {
                value = -effect.adjustment;
            }
            if (effect.scope == ScopeOfEffectOnStrike.ALL)
            {
                return value;
            }
            if (firstIsNative && effect.scope == ScopeOfEffectOnStrike.NATIVES)
            {
                return value;
            }
            if (!firstIsNative
                && effect.scope == ScopeOfEffectOnStrike.FOREIGNERS)
            {
                return value;
            }
            if (firstIsNative && !secondIsNative
                && effect.scope == ScopeOfEffectOnStrike.PATRIOTS)
            {
                return value;
            }
            if (!firstIsNative && secondIsNative
                && effect.scope == ScopeOfEffectOnStrike.IMPERIALS)
            {
                return value;
            }
            return 0;
        }
        if (effect.effect == EffectOnStrike.BLOCKED)
        {
            LOGGER.warning("Called with an unlikely effect " + effect);
            return 0;
        }
        return 0;
    }

    /** Return the bonus to apply to the Strike Factor of a creature
     * striking out from that terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getSkillBonusStrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(attackerIsNative, defenderIsNative,
            attackEffect, EffectOnStrike.SKILLBONUS,
            EffectOnStrike.SKILLPENALTY);
    }

    /** Return the penalty to apply to the Strike Factor of a creature
     * striking out from that terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     * @see #getSkillBonusStrikeFrom(boolean, boolean)
     *      #getPowerBonusStrikeFrom(boolean, boolean)
     *      #getPowerPenaltyStrikeFrom(boolean, boolean)
     */
    public int getSkillPenaltyStrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -getSkillBonusStrikeFrom(attackerIsNative, defenderIsNative);
    }

    /** Return the bonus to apply to the Strike Factor of a creature struck
     * in this terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getSkillBonusStruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(defenderIsNative, attackerIsNative,
            defenseEffect, EffectOnStrike.SKILLBONUS,
            EffectOnStrike.SKILLPENALTY);
    }

    /** Return the penalty to apply to the Strike Factor of a creature struck
     * in this terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     */
    public int getSkillPenaltyStruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -(this
            .getSkillBonusStruckIn(attackerIsNative, defenderIsNative));
    }

    /** Return the bonus to apply to the Strike Factor of a creature
     * striking out from that terrain on a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getPowerBonusStrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(attackerIsNative, defenderIsNative,
            attackEffect, EffectOnStrike.POWERBONUS,
            EffectOnStrike.POWERPENALTY);
    }

    /** Return the penalty to apply to the Power Factor of a creature
     * striking out from that terrain on a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     */
    public int getPowerPenaltyStrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -(this.getPowerBonusStrikeFrom(attackerIsNative,
            defenderIsNative));
    }

    /** Return the bonus to apply to the Strike Factor of a creature struck
     * in this terrain by a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getPowerBonusStruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(defenderIsNative, attackerIsNative,
            defenseEffect, EffectOnStrike.POWERBONUS,
            EffectOnStrike.POWERPENALTY);
    }

    /** Return the penalty to apply to the Strike Factor of a creature struck
     * in this terrain by a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     */
    public int getPowerPenaltyStruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -(this
            .getPowerBonusStruckIn(attackerIsNative, defenderIsNative));
    }

    /** Return the bonus to apply to the Strike Factor of a creature
     * rangestriking out from that terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getSkillBonusRangestrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(attackerIsNative, defenderIsNative,
            rangedAttackEffect, EffectOnStrike.SKILLBONUS,
            EffectOnStrike.SKILLPENALTY);
    }

    /** Return the penalty to apply to the Strike Factor of a creature
     * rangestriking out from that terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     * @see #getSkillBonusRangestrikeFrom(boolean, boolean)
     *      #getPowerBonusRangestrikeFrom(boolean, boolean)
     *      #getPowerPenaltyRangestrikeFrom(boolean, boolean)
     */
    public int getSkillPenaltyRangestrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -getSkillBonusRangestrikeFrom(attackerIsNative,
            defenderIsNative);
    }

    /** Return the bonus to apply to the Strike Factor of a creature Rangestruck
     * in this terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getSkillBonusRangestruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(defenderIsNative, attackerIsNative,
            rangedDefenseEffect, EffectOnStrike.SKILLBONUS,
            EffectOnStrike.SKILLPENALTY);
    }

    /** Return the penalty to apply to the Strike Factor of a creature Rangestruck
     * in this terrain.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     */
    public int getSkillPenaltyRangestruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -(this.getSkillBonusRangestruckIn(attackerIsNative,
            defenderIsNative));
    }

    /** Return the bonus to apply to the Strike Factor of a creature
     * rangestriking out from that terrain on a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getPowerBonusRangestrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(attackerIsNative, defenderIsNative,
            rangedAttackEffect, EffectOnStrike.POWERBONUS,
            EffectOnStrike.POWERPENALTY);
    }

    /** Return the penalty to apply to the Power Factor of a creature
     * rangestriking out from that terrain on a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     */
    public int getPowerPenaltyRangestrikeFrom(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -(this.getPowerBonusRangestrikeFrom(attackerIsNative,
            defenderIsNative));
    }

    /** Return the bonus to apply to the Strike Factor of a creature Rangestruck
     * in this terrain by a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of bonus to apply (negative if it's a penalty).
     */
    public int getPowerBonusRangestruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return computeSkillOrPowerBonus(defenderIsNative, attackerIsNative,
            rangedDefenseEffect, EffectOnStrike.POWERBONUS,
            EffectOnStrike.POWERPENALTY);
    }

    /** Return the penalty to apply to the Strike Factor of a creature Rangestruck
     * in this terrain by a unspecified creature.
     * @param attackerIsNative Whether the attacker is native from this HazardTerrain
     * @param defenderIsNative Whether the defender is native from this HazardTerrain
     * @return The amount of penalty to apply (negative if it's a bonus).
     */
    public int getPowerPenaltyRangestruckIn(boolean attackerIsNative,
        boolean defenderIsNative)
    {
        return -(this.getPowerBonusRangestruckIn(attackerIsNative,
            defenderIsNative));
    }

    /** Whether this terrain blocks rangestrike.
     * @return Whether this terrain blocks rangestrike.
     */
    public boolean blocksLineOfSight()
    {
        return rangeStrikeSpecial == RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED;
    }

    /** Whether this terrain is healing
     * @return Whether this terrain is healing
     */
    public boolean isHealing()
    {
        return this.terrainSpecial == SpecialEffect.HEALTHGAIN;
    }

    /** Whether this terrain slows for the duration of the battle
     * @return Whether this terrain slows for the duration of the battle
     */
    public boolean isSlowingToNonNative()
    {
        return this.terrainSpecial == SpecialEffect.PERMSLOW;
    }

    /** Whether this terrain is damaging to non-native.
     * @return Whether this terrain is damaging to non-native.
     */
    public boolean isDamagingToNonNative()
    {
        return this.terrainSpecial == SpecialEffect.HEALTHDRAIN;
    }

    /** Whether this terrain is damaging to water dweller.
     * @return Whether this terrain is damaging water dweller.
     */
    public boolean isDamagingToWaterDweller()
    {
        return this.terrainSpecial == SpecialEffect.HEALTHDRAIN_WATERDWELLER;
    }

    /** Return the bonus to apply to the Strike Factor of a Creature whose
     * line-of-fire cross this hex.
     * TODO there should be an effect variable (instead of 1), and we also
     * might add the other variants (skillbonus, powerpenalty, powerbonus)
     *
     * @return The bonus to apply to the Strike Factor,
     *         negative if it's a penalty.
     */
    public int getSkillBonusRangestrikeThrough(boolean rangestrikerIsNative)
    {
        if ((!rangestrikerIsNative)
            && (this.rangeStrikeSpecial == RangeStrikeSpecialEffect.RANGESTRIKESKILLPENALTY))
        {
            return -1;
        }
        return 0;
    }

    /** Return the penalty to apply to the Strike Factor of a Creature whose
     * line-of-fire cross this hex.
     * @return The penalty to apply to the Strike Factor, negative if it's a bonus.
     */
    public int getSkillPenaltyRangestrikeThrough(boolean rangestrikerIsNative)
    {
        return -(this.getSkillBonusRangestrikeThrough(rangestrikerIsNative));
    }

    /** USE ONLY FOR BATTLELANDBUILDER! */
    public boolean isSand()
    {
        return this == SAND;
    }

    /** USE ONLY FOR BATTLELANDBUILDER! */
    public boolean isPlains()
    {
        return this == PLAINS;
    }
}
