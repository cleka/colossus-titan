package net.sf.colossus.server;


import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;


/**
 * Class CreatureTitan represent the CONSTANT information about a
 * Titan (the game) Titan (the creature).
 * 
 * Game related info is in Critter.  Counts of
 * recruited/available/dead are in Caretaker.
 * 
 * TODO this class doesn't really fulfill the whole {@link CreatureType} interface
 * since it can't handle getPower() at the moment (and thus some other things don't
 * work). The solution could be to consider each Titan a type of creature of his
 * own, distinguished by the Player owning them, which then could be stored as
 * member in the class, delegating {@link #getPower()} to {@link PlayerServerSide#getTitanPower()}.
 * 
 * TODO this should probably be in the variant package
 *
 * @version $Id$
 * @author Romain Dolbeau
 */

public class CreatureTitan extends CreatureType
{
    private static final Logger LOGGER = Logger.getLogger(CreatureTitan.class
        .getName());

    public CreatureTitan(String name, int power, int skill,
        boolean rangestrikes, boolean flies,
        Set<HazardTerrain> nativeTerrrains, boolean nativeSlope,
        boolean nativeRiver, boolean waterDwelling, boolean magicMissile,
        boolean summonable, boolean lord, boolean demilord, int maxCount,
        String pluralName, String baseColor)
    {
        super(name, power, skill, rangestrikes, flies, nativeTerrrains,
            nativeSlope, nativeRiver, waterDwelling, magicMissile, summonable,
            lord, demilord, maxCount, pluralName, baseColor);

        if (!name.equals(Constants.titan))
        {
            LOGGER.log(Level.SEVERE,
                "Creating a CreatureTitan but the name is not Titan !");
        }
    }

    @Override
    public boolean isImmortal()
    { // Titan aren't immortal
        return false;
    }

    @Override
    public boolean isTitan()
    {
        return true;
    }

    @Override
    public int getPointValue()
    {
        // Log.warn("Calling getPointValue() on Titan Creature"); 
        // XXX This is wrong, but 24 is better than -4.
        int val = 6 * getSkill();
        return val;
    }
}
