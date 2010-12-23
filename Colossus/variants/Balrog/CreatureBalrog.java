package Balrog;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;


/**
 * Custom class implementing the Balrog Creature.
 *
 * It is a DemiLord yet isn't immortal, and it's Image Name is Balrog no matter
 * what is it's Creature Name.
 *
 * One becomes available in a Player home Tower for every 300 points earned
 * by the Player. This means the maximum number of aailable Balrog changes,
 * which is why we need a custom CreatureType.
 *
 * @author Romain Dolbeau
 */
public class CreatureBalrog extends CreatureType
{
    private static final Logger LOGGER = Logger.getLogger(CreatureBalrog.class
        .getName());
    private final static List<CreatureType> allBalrogs = new ArrayList<CreatureType>();

    /**
     * Constructor to be called via reflection.
     *
     * The signature of the constructor must be exactly what is tried
     * in the Loader, otherwise creation fails (no superclass or interface
     * allowed, only cold, hard implementation).
     */
    public CreatureBalrog(
        String name, Integer power, Integer skill,
        Boolean rangestrikes, Boolean flies,
        HashSet<HazardTerrain> nativeTerrrains, Boolean nativeSlope,
        Boolean nativeRiver, Boolean nativeDune, Boolean waterDwelling,
        Boolean magicMissile, Boolean summonable, Boolean lord,
        Boolean demilord, Integer maxCount, String pluralName,
        String baseColor, Integer poison, Integer slows) // NO_UCD
    {
        super(name, power.intValue(), skill.intValue(), rangestrikes
            .booleanValue(), flies.booleanValue(), nativeTerrrains,
            nativeSlope.booleanValue(), nativeRiver.booleanValue(), nativeDune
                .booleanValue(), waterDwelling.booleanValue(), magicMissile
                .booleanValue(), summonable.booleanValue(), lord
                .booleanValue(), demilord.booleanValue(), maxCount.intValue(),
            pluralName, baseColor, poison.intValue(), slows.intValue());
        LOGGER.finest("Successfully created custom CreatureType " + name
            + " (class " + CreatureBalrog.class.getName() + ")");
        allBalrogs.add(this);
    }

    final static List<CreatureType> getAllBalrogs()
    {
        return allBalrogs;
    }

    @Override
    public boolean isImmortal()
    { // demilord yet not immortal
        return false;
    }

    @Override
    public String getImageName()
    {
        return "Balrog";
    }
}
