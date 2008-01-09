package net.sf.colossus.server;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;


/**
 * Class Creature represents the CONSTANT information about a
 * Titan (the game) creature. Titan (the creature) use
 * class CreatureTitan.
 *
 * Game related info is in Critter.  Counts of
 * recruited/available/dead are in Caretaker.
 *
 * @version $Id$
 * @author David Ripton, Bruce Sherrod
 * @author Romain Dolbeau
 */

public class Creature extends CreatureType
{
    private static final Logger LOGGER = Logger.getLogger(Creature.class
        .getName());

    private final int power;
    private final int skill;
    private final boolean rangestrikes;
    private final boolean flies;
    private final boolean nativeSlope;
    private final boolean nativeRiver;
    private final boolean waterDwelling;
    private final boolean magicMissile;
    private final boolean lord;
    private final boolean demilord;
    private int maxCount; // Not final because we adjust for titans.
    private final String baseColor;
    private static boolean noBaseColor = false;

    public static final Creature unknown = new Creature("Unknown", 1, 1,
        false, false, new HashSet<HazardTerrain>(), false, false, false,
        false, false, false, false, 1, "Unknown", null);

    public Creature(String name, int power, int skill, boolean rangestrikes,
        boolean flies, Set<HazardTerrain> nativeTerrrains,
        boolean nativeSlope, boolean nativeRiver, boolean waterDwelling,
        boolean magicMissile, boolean summonable, boolean lord,
        boolean demilord, int maxCount, String pluralName, String baseColor)
    {
        super(name, pluralName, nativeTerrrains, summonable);
        this.power = power;
        this.skill = skill;
        this.rangestrikes = rangestrikes;
        this.flies = flies;
        this.nativeSlope = nativeSlope;
        this.nativeRiver = nativeRiver;
        this.waterDwelling = waterDwelling;
        this.magicMissile = magicMissile;
        this.lord = lord;
        this.demilord = demilord;
        this.maxCount = maxCount;
        this.baseColor = baseColor;

        /* warn about likely inappropriate combinations */
        if (waterDwelling && isNativeIn(HazardTerrain.SAND))
        {
            LOGGER.log(Level.WARNING, "Creature " + name
                + " is both a Water Dweller and native to Sand and Dune.");
        }
    }

    public int getMaxCount()
    {
        return maxCount;
    }

    /** Only called on Titans after numPlayers is known. */
    void setMaxCount(int maxCount)
    {
        this.maxCount = maxCount;
    }

    public boolean isLord()
    {
        return lord;
    }

    public boolean isDemiLord()
    {
        return demilord;
    }

    public boolean isLordOrDemiLord()
    {
        return (isLord() || isDemiLord());
    }

    public boolean isImmortal()
    { // might not the same for derived class
        return isLordOrDemiLord();
    }

    public boolean isTitan()
    { // Titan use class CreatureTitan
        return false;
    }

    /** true if any if the values can change during the game returned by:
     * - getPower, getSkill, (and therefore getPointValue)
     * - isRangestriker, isFlier, useMagicMissile
     * - isNativeTerraion(t), for all t
     * - isNativeHexSide(h) for all h
     * In Standard game only the titans change their attributes
     */
    public boolean canChangeValue()
    {
        return isTitan();
    }

    public String getImageName()
    {
        return getName();
    }

    public String[] getImageNames()
    {
        String[] tempNames;
        if (baseColor != null)
        {
            int specialIncrement = ((isFlier() || isRangestriker()) ? 1 : 0);
            tempNames = new String[4 + specialIncrement];
            String colorSuffix = "-" + (noBaseColor ? "black" : baseColor);
            tempNames[0] = getImageName();
            tempNames[1] = "Power-" + getPower() + colorSuffix;

            tempNames[2] = "Skill-" + getSkill() + colorSuffix;
            tempNames[3] = getName() + "-Name" + colorSuffix;
            if (specialIncrement > 0)
            {
                tempNames[4] = (isFlier() ? "Flying" : "")
                    + (isRangestriker() ? "Rangestrike" : "") + colorSuffix;
            }
        }
        else
        {
            tempNames = new String[1];
            tempNames[0] = getImageName();
        }
        return tempNames;
    }

    public int getPower()
    {
        return power;
    }

    public int getSkill()
    {
        return skill;
    }

    public int getPointValue()
    { // this function is replicated in Critter
        return getPower() * getSkill();
    }

    public int getHintedRecruitmentValue()
    { // this function is replicated in Critter
        return getPointValue()
            + VariantSupport.getHintedRecruitmentValueOffset(getName());
    }

    public int getHintedRecruitmentValue(String[] section)
    { // this function is replicated in Critter
        return getPointValue()
            + VariantSupport.getHintedRecruitmentValueOffset(getName(),
                section);
    }

    public boolean isRangestriker()
    {
        return rangestrikes;
    }

    public boolean isFlier()
    {
        return flies;
    }

    public boolean isNativeHexside(char h)
    {
        switch (h)
        {
            default:
                return false;

            case ' ': /* undefined */
                return false;

            case 'd':
                return isNativeIn(HazardTerrain.SAND);

            case 'c': /* undefined */
                return false;

            case 's':
                return isNativeSlope();

            case 'w': /* undefined, beneficial for everyone */
                return true;

            case 'r':
                return isNativeRiver();
        }
    }

    public boolean isNativeSlope()
    {
        return nativeSlope;
    }

    public boolean isNativeRiver()
    {
        return nativeRiver;
    }

    public boolean isWaterDwelling()
    {
        return waterDwelling;
    }

    public boolean useMagicMissile()
    {
        return magicMissile;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    /** Compare by name. */
    @Override
    public final boolean equals(Object object)
    {
        if (object.getClass() != this.getClass())
        {
            return false;
        }
        Creature other = (Creature)object;
        return getName().equals(other.getName());
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }

    public static void setNoBaseColor(boolean b)
    {
        noBaseColor = b;
    }

    public String getBaseColor()
    {
        if (baseColor != null)
        {
            return baseColor;
        }
        else
        {
            return "";
        }
    }

    /** 
     * Get the non-terrainified part of the kill-value.
     * 
     * TODO this is not model, but AI related (but also used in client for
     * sorting creatures -- the client uses the AI for recruit hints, too) 
     */
    public int getKillValue()
    {
        int val = 10 * getPointValue();
        final int skill = getSkill();
        if (skill >= 4)
        {
            val += 2;
        }
        else if (skill <= 2)
        {
            val += 1;
        }
        if (isFlier())
        {
            val += 4;
        }
        if (isRangestriker())
        {
            val += 5;
        }
        if (useMagicMissile())
        {
            val += 4;
        }
        if (isTitan())
        {
            val += 1000;
        }
        return val;
    }
}
