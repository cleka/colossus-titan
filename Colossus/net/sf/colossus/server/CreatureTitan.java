package net.sf.colossus.server;

import net.sf.colossus.util.Log;

/**
 * Class CreatureTitan represent the CONSTANT information about a
 * Titan (the game) Titan (the creature).
 * 
 * Game related info is in Critter.  Counts of
 * recruited/available/dead are in Caretaker.
 *
 * @author Romain Dolbeau
 */

public class CreatureTitan extends Creature
{
    public CreatureTitan(String name,
                         Integer power,
                         Integer skill,
                         Boolean rangestrikes,
                         Boolean flies,
                         Boolean nativeBramble,
                         Boolean nativeDrift,
                         Boolean nativeBog,
                         Boolean nativeSandDune,
                         Boolean nativeSlope,
                         Boolean nativeVolcano,
                         Boolean nativeRiver,
                         Boolean nativeStone,
                         Boolean nativeTree,
                         Boolean waterDwelling,
                         Boolean magicMissile,
                         Boolean summonable,
                         Boolean lord,
                         Boolean demilord,
                         Integer maxCount,
                         String pluralName,
                         String baseColor)
    {
        super(name,
              power.intValue(),
              skill.intValue(),
              rangestrikes.booleanValue(),
              flies.booleanValue(),
              nativeBramble.booleanValue(),
              nativeDrift.booleanValue(),
              nativeBog.booleanValue(),
              nativeSandDune.booleanValue(),
              nativeSlope.booleanValue(),
              nativeVolcano.booleanValue(),
              nativeRiver.booleanValue(),
              nativeStone.booleanValue(),
              nativeTree.booleanValue(),
              waterDwelling.booleanValue(),
              magicMissile.booleanValue(),
              summonable.booleanValue(),
              lord.booleanValue(),
              demilord.booleanValue(),
              maxCount.intValue(),
              pluralName,
              baseColor);

        if (!name.equals(Constants.titan))
        {
            Log.error("Creating a CreatureTitan but the name is not Titan !");
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
        Log.warn("Calling getImageNames() for Titan");
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
        Log.warn("Calling getHintedRecruitmentValue() on CreatureTitan"); 
        int val = super.getHintedRecruitmentValue();
        Log.debug("getHintedRecruitmentValue() is " + val);
        return val;
    }

    public int getHintedRecruitmentValue(String[] section)
    {
        Log.warn("Calling getHintedRecruitmentValue([]) on CreatureTitan");
        int val = super.getHintedRecruitmentValue(section);
        Log.debug("getHintedRecruitmentValue() is " + val);
        return val;
    }
}
