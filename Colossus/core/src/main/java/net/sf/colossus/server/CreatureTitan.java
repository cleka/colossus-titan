package net.sf.colossus.server;


import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class CreatureTitan represent the CONSTANT information about a
 * Titan (the game) Titan (the creature).
 * 
 * Game related info is in Critter.  Counts of
 * recruited/available/dead are in Caretaker.
 *
 * @version $Id$
 * @author Romain Dolbeau
 */

public class CreatureTitan extends Creature
{
    private static final Logger LOGGER = Logger.getLogger(CreatureTitan.class.getName());

    public CreatureTitan(String name,
        int power,
        int skill,
        boolean rangestrikes,
        boolean flies,
        boolean nativeBramble,
        boolean nativeDrift,
        boolean nativeBog,
        boolean nativeSandDune,
        boolean nativeSlope,
        boolean nativeVolcano,
        boolean nativeRiver,
        boolean nativeStone,
        boolean nativeTree,
        boolean waterDwelling,
        boolean magicMissile,
        boolean summonable,
        boolean lord,
        boolean demilord,
        int maxCount,
        String pluralName,
        String baseColor)
    {
        super(name,
            power,
            skill,
            rangestrikes,
            flies,
            nativeBramble,
            nativeDrift,
            nativeBog,
            nativeSandDune,
            nativeSlope,
            nativeVolcano,
            nativeRiver,
            nativeStone,
            nativeTree,
            waterDwelling,
            magicMissile,
            summonable,
            lord,
            demilord,
            maxCount,
            pluralName,
            baseColor);

        if (!name.equals(Constants.titan))
        {
            LOGGER.log(Level.SEVERE,
                "Creating a CreatureTitan but the name is not Titan !");
        }
    }

    public boolean isImmortal()
    { // Titan aren't immortal
        return false;
    }

    public boolean isTitan()
    {
        return true;
    }

    public String[] getImageNames()
    {
        LOGGER.log(Level.WARNING, "Calling getImageNames() for Titan");
        return super.getImageNames();
    }

    public int getPointValue()
    {
        // Log.warn("Calling getPointValue() on Titan Creature"); 
        // XXX This is wrong, but 24 is better than -4.
        int val = 6 * getSkill();
        return val;
    }

    public int getHintedRecruitmentValue()
    {
        int val = super.getHintedRecruitmentValue();
        LOGGER.log(Level.FINEST, "getHintedRecruitmentValue() is " + val);
        return val;
    }

    public int getHintedRecruitmentValue(String[] section)
    {
        int val = super.getHintedRecruitmentValue(section);
        LOGGER.log(Level.FINEST, "getHintedRecruitmentValue() is " + val);
        return val;
    }
}
