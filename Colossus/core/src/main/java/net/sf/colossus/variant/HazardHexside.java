package net.sf.colossus.variant;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This is a typesafe enumeration of all hazard Hexsides, i.e. the
 * Hexsides used in the battle maps.
 *
 * TODO turn into proper enum
 */
public class HazardHexside extends Hazards
{
    /**
    * A map from the serialization string of a Hexside to the instances.
    */
    private final static Map<String, HazardHexside> HEXSIDE_MAP = new HashMap<String, HazardHexside>();
    private final static Map<Character, HazardHexside> HEXSIDE_MAP_UGLY = new HashMap<Character, HazardHexside>();

    public HazardHexside(String name, char code,
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
        HEXSIDE_MAP.put(name, this);
        HEXSIDE_MAP_UGLY.put(new Character(code), this);
    }

    public static HazardHexside getHexsideByName(String name)
    {
        return HEXSIDE_MAP.get(name);
    }

    /** deprecated because we want to get rid of the single char stuff */
    @Deprecated
    public static HazardHexside getHexsideByCode(char code)
    {
        return HEXSIDE_MAP_UGLY.get(new Character(code));
    }

    /**
     * Returns all available hazard hexsides.
     *
     * This is not variant-specific, any hexside known to the program is listed even
     * if it is not available in the current variant.
     *
     * TODO this should really be a question to ask a variant instance
     */
    public static final Collection<HazardHexside> getAllHazardHexsides()
    {
        return HEXSIDE_MAP.values();
    }

    public static final HazardHexside NOTHING = new HazardHexside(
        "Nothing",
        ' ',
        EffectOnMovement.FREEMOVE,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardHexside DUNE = new HazardHexside(
        "Dune",
        'd',
        EffectOnMovement.FREEMOVE,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.POWERPENALTY,
            ScopeOfEffectOnStrike.FOREIGNERS, 1),
        new CombatEffect(EffectOnStrike.POWERBONUS,
            ScopeOfEffectOnStrike.NATIVES, 2),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED, SpecialEffect.NOSPECIAL);

    public static final HazardHexside CLIFF = new HazardHexside(
        "Cliff",
        'c',
        EffectOnMovement.BLOCKALL,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED, SpecialEffect.NOSPECIAL);

    public static final HazardHexside SLOPE = new HazardHexside(
        "Slope",
        's',
        EffectOnMovement.SLOWFOREIGNER,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.SKILLPENALTY,
            ScopeOfEffectOnStrike.FOREIGNERS, 1),
        new CombatEffect(EffectOnStrike.POWERBONUS,
            ScopeOfEffectOnStrike.NATIVES, 1),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED, SpecialEffect.NOSPECIAL);

    public static final HazardHexside TOWER = new HazardHexside(
        "Tower",
        'w',
        EffectOnMovement.SLOWALL,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.ALL,
            1),
        new CombatEffect(EffectOnStrike.SKILLPENALTY,
            ScopeOfEffectOnStrike.ALL, 1),
        new CombatEffect(EffectOnStrike.SKILLPENALTY,
            ScopeOfEffectOnStrike.ALL, 1),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEWALL, SpecialEffect.NOSPECIAL);

    // TODO Verify
    public static final HazardHexside RIVER = new HazardHexside(
        "River",
        'r',
        EffectOnMovement.BLOCKFOREIGNER,
        EffectOnMovement.FREEMOVE,
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        new CombatEffect(EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0),
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("+++++++++++++++++++++++++++\n");
        builder.append(getName());
        builder.append(":\n");
        return builder.toString();
    }

    // TODO use combat bonus tables instead of hardcoded those three
    //      (created as result of char-to-HazardHexside refactoring)
    public boolean isNativeBonusHexside()
    {
        if (this == HazardHexside.TOWER || this == HazardHexside.SLOPE
            || this == HazardHexside.DUNE)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    // TODO use combat bonus tables instead of hardcoded those three
    //      (created as result of char-to-HazardHexside refactoring)
    // TODO right now, this is identical behavior as "isNativeBonusHexside()"
    public boolean isNonNativePenaltyHexside()
    {
        if (this == HazardHexside.TOWER || this == HazardHexside.SLOPE
            || this == HazardHexside.DUNE)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
