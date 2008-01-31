package net.sf.colossus.variant;


/** 
 * This holds constants used in Hazard Terrain and Hazard Hexside 
 */
public interface HazardConstants
{
    /**
     * Movement Constants
     */
    public enum EffectOnMovement
    {
        BLOCKALL, BLOCKFOREIGNER, SLOWALL, SLOWFOREIGNER, FREEMOVE
    }

    /**
     * Scope Constants
     */
    public enum ScopeOfEffectOnStrike
    {
        ALL, NATIVES, FOREIGNERS, NOBODY
    }

    /**
     * Strike/RangeStrike Constants
     */
    public enum EffectOnStrike
    {
        SKILLBONUS, SKILLPENALTY, POWERBONUS, POWERPENALTY, HEALTHDRAIN, BLOCKED, NOEFFECT
    }

    /**
    RANGESTRIKEFREE = "No effect on RangeStrike";
    RANGESTRIKEBLOCKED = "Blocks RangeStrike";
    RANGESTRIKEOCCUPIED = "Blocks RangeStrike unless Occupied";
    RANGESTRIKEWALL = "Blocks RangeStrike unless Occupied - 1 Skill";
    RANGESTRIKESKILLPENALTY = "Non-Native RangeStriker loses skill for each";
    */
    public enum RangeStrikeSpecialEffect
    {
        RANGESTRIKEFREE, RANGESTRIKEBLOCKED, RANGESTRIKEOCCUPIED, RANGESTRIKEWALL, RANGESTRIKESKILLPENALTY
    }

    public String getName();

    public boolean isNativeBonusTerrain();

    public boolean isNonNativePenaltyTerrain();

    public boolean isNativeOnly();

    public String toString();
}