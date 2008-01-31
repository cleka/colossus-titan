package net.sf.colossus.variant;


/** 
 * This holds constants used in Hazard Terrain and Hazard Hexside 
 */
public interface HazardConstants
{
    /**
     * Movement Constants
     */
    public static final String BLOCKALL = "Blocks all Movement";
    public static final String BLOCKFOREIGNER = "Blocks Non-Native Movement";
    public static final String SLOWALL = "Slows Everyone";
    public static final String SLOWFOREIGNER = "Slows Non-Natives";
    public static final String FREEMOVE = "Does Not Impact Movement";

    /**
     * Scope Constants
     */
    public static final String SCOPEALL = "Everyone";
    public static final String SCOPENATIVE = "Natives Only";
    public static final String SCOPEFOREIGNER = "Non-Natives Only";
    public static final String SCOPENULL = "Nobody";
    /**
     * Strike/RangeStrike Constants
     */
    public static final String SKILLBONUS = "Skill increase";
    public static final String SKILLPENALTY = "Skill decrease";
    public static final String POWERBONUS = "Power increase";
    public static final String POWERPENALTY = "Power decrease";
    public static final String HEALTHDRAIN = "Lose health every strike phase";
    public static final String NOEFFECT = "No Terrain Effect";

    public static final String RANGESTRIKEFREE = "No effect on RangeStrike";
    public static final String RANGESTRIKEBLOCKED = "Blocks RangeStrike";
    public static final String RANGESTRIKEOCCUPIED = "Blocks RangeStrike unless Occupied";
    public static final String RANGESTRIKEWALL = "Blocks RangeStrike unless Occupied - 1 Skill";
    public static final String RANGESTRIKESKILLPENALTY = "Non-Native RangeStriker loses skill for each";

    public String getName();

    public boolean isNativeBonusTerrain();

    public boolean isNonNativePenaltyTerrain();

    public boolean isNativeOnly();

    public String toString();
}