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
     * Scope Constants -
     * All - is everyone
     * Natives means Natives vs anyone
     * Patriots means Natives vs Foreigners
     * Foreigners are Non-Natives vs anyone
     * Imperials means Foreigners vs Natives
     */
    public enum ScopeOfEffectOnStrike
    {
        NATIVES, PATRIOTS, FOREIGNERS, IMPERIALS, ALL
    }

    /**
     * Strike/RangeStrike Constants
     */
    public enum EffectOnStrike
    {
        SKILLBONUS, SKILLPENALTY, POWERBONUS, POWERPENALTY, BLOCKED, NOEFFECT
    }

    /** Special effects.
     */
    public enum SpecialEffect
    {
        /** No special effect */
        NOSPECIAL,
        /** Drain health from non-native */
        HEALTHDRAIN,
        /** Drain health from water dweller */
        HEALTHDRAIN_WATERDWELLER,
        /** Heal */
        HEALTHGAIN,
        /** Persistent slow of creatures */
        PERMSLOW
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

    public String toString();
}