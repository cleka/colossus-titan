package net.sf.colossus.variant;


import java.util.Collection;
import java.util.LinkedList;


/** 
 * This is a typesafe enumeration of all hazard Hexsides, i.e. the
 * Hexsides used in the battle maps.
 */
public class HazardHexside extends Hazards
{
    public HazardHexside(String name, char code,
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
        super(name, code, effectOnGroundMovement, effectOnFlyerMovement,
            effectforDefendingInTerrain, scopeForDefenceEffect,
            defenceEffectAdjustment, effectforAttackingFromTerrain,
            scopeForAttackEffect, attackEffectAdjustment,
            effectForBeingRangeSruckInTerrain, scopeForRangeStruckEffect,
            RangeStruckEffectAdjustment, effectforRangeStrikeFromTerrain,
            scopeForRangeStrikeEffect, RangeStrikeEffectAdjustment,
            RangeStrikeSpecial, terrainSpecial);
    }

    public static HazardHexside getTerrainByName(String name)
    {
        return (HazardHexside)getByName(name);
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
        Collection<Hazards> hazards = getAllHazards();
        Collection<HazardHexside> hexsides = new LinkedList<HazardHexside>();
        for (Hazards hazard : hazards)
        {
            if (hazard instanceof HazardHexside)
            {
                hexsides.add((HazardHexside)hazard);
            }
        }
        return hexsides;
    }

    public static final HazardHexside NOTHING = new HazardHexside("Nothing",
        ' ', EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardHexside DUNE = new HazardHexside("Dune", 'd',
        EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.POWERPENALTY, ScopeOfEffectOnStrike.FOREIGNERS, 1,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 2,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED, SpecialEffect.NOSPECIAL);

    public static final HazardHexside CLIFF = new HazardHexside("Cliff", 'c',
        EffectOnMovement.BLOCKALL, EffectOnMovement.FREEMOVE,
        EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED, SpecialEffect.NOSPECIAL);

    public static final HazardHexside SLOPE = new HazardHexside("Slope", 's',
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.FOREIGNERS, 1,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 1,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEOCCUPIED, SpecialEffect.NOSPECIAL);

    public static final HazardHexside TOWER = new HazardHexside("Tower", 'w',
        EffectOnMovement.SLOWALL, EffectOnMovement.FREEMOVE,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.ALL, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.ALL, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.ALL, 1,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEWALL, SpecialEffect.NOSPECIAL);

    // TODO Verify
    public static final HazardHexside RIVER = new HazardHexside("River", 'r',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
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
}
