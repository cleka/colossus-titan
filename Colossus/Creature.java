import java.util.*;

/**
 *
 * Class Creature represents the CONSTANT information about a Titan Creature.
 *
 * Game related info is in Critter.  Counts of recruited/available/dead are in Caretaker.
 *
 * @version $Id$
 * @author David Ripton, Bruce Sherrod
 */

public class Creature implements Comparable
{
    private final String name;
    private final String pluralName;
    private final int power;
    private final int skill;
    private final boolean rangestrikes;
    private final boolean flies;
    private final boolean nativeBramble;
    private final boolean nativeDrift;
    private final boolean nativeBog;
    private final boolean nativeSandDune;
    private final boolean nativeSlope;
    private final boolean lord;
    private final boolean demilord;
    private final int maxCount;

    // Add various Creature archetypes as class members
    public static final Creature angel = new Creature("Angel", 6, 4,
        false, true, false, false, false, false, false, true, false,
        18, "Angels");
    public static final Creature archangel = new Creature("Archangel", 9, 4,
        false, true, false, false, false, false, false, true, false,
        6, "Archngels");
    public static final Creature behemoth = new Creature("Behemoth", 8, 3,
        false, false, true, false, false, false, false, false, false,
        18, "Behemoths");
    public static final Creature centaur = new Creature("Centaur", 3, 4,
        false, false, false, false, false, false, false, false, false,
        25, "Centaurs");
    public static final Creature colossus = new Creature("Colossus", 10, 4,
        false, false, false, true, false, false, true, false, false,
        10, "Colossi");
    public static final Creature cyclops = new Creature("Cyclops", 9, 2,
        false, false, true, false, false, false, false, false, false,
        28, "Cyclopes");
    public static final Creature dragon = new Creature("Dragon", 9, 3,
        true, true, false, false, false, false, true, false, false,
        18, "Dragons");
    public static final Creature gargoyle = new Creature("Gargoyle", 4, 3,
        false, true, true, false, false, false, false, false, false,
        21, "Gargoyles");
    public static final Creature giant = new Creature("Giant", 7, 4,
        true, false, false, true, false, false, false, false, false,
        18, "Giants");
    public static final Creature gorgon = new Creature("Gorgon", 6, 3,
        true, true, true, false, false, false, false, false, false,
        25, "Gorgons");
    public static final Creature griffon = new Creature("Griffon", 5, 4,
        false, true, false, false, false, true, false, false, false,
        18, "Griffons");
    public static final Creature guardian = new Creature("Guardian", 12, 2,
        false, true, false, false, false, false, false, false, true,
        6, "Guardians");
    public static final Creature hydra = new Creature("Hydra", 10, 3,
        true, false, false, false, true, true, false, false, false,
        10, "Hydras");
    public static final Creature lion = new Creature("Lion", 5, 3,
        false, false, false, false, false, true, true, false, false,
        28, "Lions");
    public static final Creature minotaur = new Creature("Minotaur", 4, 4,
        true, false, false, false, false, false, true, false, false,
        21, "Minotaurs");
    public static final Creature ogre = new Creature("Ogre", 6, 2,
        false, false, false, false, true, false, true, false, false,
        25, "Ogres");
    public static final Creature ranger = new Creature("Ranger", 4, 4,
        true, true, false, false, true, false, false, false, false,
        28, "Rangers");
    public static final Creature serpent = new Creature("Serpent", 18, 2,
        false, false, true, false, false, false, false, false, false,
        10, "Serpents");
    public static final Creature titan = new Creature("Titan", -1, 4,
        false, false, false, false, false, false, false, true, false,
        6, "Titans");
    public static final Creature troll = new Creature("Troll", 8, 2,
        false, false, false, true, true, false, false, false, false,
        28, "Trolls");
    public static final Creature unicorn = new Creature("Unicorn", 6, 4,
        false, false, false, false, false, false, true, false, false,
        12, "Unicorns");
    public static final Creature warbear = new Creature("Warbear", 6, 3,
        false, false, false, true, false, false, false, false, false,
        21, "Warbears");
    public static final Creature warlock = new Creature("Warlock", 5, 4,
        true, false, false, false, false, false, false, false, true,
        6, "Warlocks");
    public static final Creature wyvern = new Creature("Wyvern", 7, 3,
        false, true, false, false, true, false, false, false, false,
        18, "Wyverns");

    /** For marking unknown enemy creatures when tracking PBEM games. */
    public static final Creature unknown = new Creature("Unknown", 1, 1,
        false, false, false, false, false, false, false, false, false,
        1, "Unknown");

    /** Sometimes we need to iterate through all creature types. */
    private static final Creature [] creaturesArray = {angel, archangel,
        behemoth, centaur, colossus, cyclops, dragon, gargoyle, giant,
        gorgon, griffon, guardian, hydra, lion, minotaur, ogre, ranger,
        serpent, titan, troll, unicorn, warbear, warlock, wyvern};

    private static final List creatures = Arrays.asList(creaturesArray);


    private Creature(String name, int power, int skill, boolean rangestrikes,
        boolean flies, boolean nativeBramble, boolean nativeDrift,
        boolean nativeBog, boolean nativeSandDune, boolean nativeSlope,
        boolean lord, boolean demilord, int maxCount, String pluralName)
    {
        this.name = name;
        this.power = power;
        this.skill = skill;
        this.rangestrikes = rangestrikes;
        this.flies = flies;
        this.nativeBramble = nativeBramble;
        this.nativeDrift = nativeDrift;
        this.nativeBog = nativeBog;
        this.nativeSandDune = nativeSandDune;
        this.nativeSlope = nativeSlope;
        this.lord = lord;
        this.demilord = demilord;
        this.maxCount = maxCount;
        this.pluralName = pluralName;
    }


    protected Creature(Creature creature)
    {
        this.name = creature.name;
        this.power = creature.power;
        this.skill = creature.skill;
        this.rangestrikes = creature.rangestrikes;
        this.flies = creature.flies;
        this.nativeBramble = creature.nativeBramble;
        this.nativeDrift = creature.nativeDrift;
        this.nativeBog = creature.nativeBog;
        this.nativeSandDune = creature.nativeSandDune;
        this.nativeSlope = creature.nativeSlope;
        this.lord = creature.lord;
        this.demilord = creature.demilord;
        this.maxCount = creature.maxCount;
        this.pluralName = creature.pluralName;
    }


    public static List getCreatures()
    {
        return creatures;
    }

    public int getMaxCount()
    {
        return maxCount;
    }

    public boolean isLord()
    {
        return lord;
    }


    public boolean isImmortal()
    {
        return (lord || demilord);
    }


    public boolean isTitan()
    {
        return name.equals("Titan");
    }

    public boolean isAngel()
    {
        return name.equals("Angel") || name.equals("Archangel");
    }


    public String getName()
    {
        return name;
    }


    public String getPluralName()
    {
        return pluralName;
    }


    public String getImageName(boolean inverted)
    {
        StringBuffer basename = new StringBuffer();
        if (inverted)
        {
            basename.append(Chit.invertedPrefix);
        }
        basename.append(name);
        return basename.toString();
    }


    public String getImageName()
    {
        return getImageName(false);
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
    {
        return getPower() * getSkill();
    }

    public boolean isRangestriker()
    {
        return rangestrikes;
    }


    public boolean isFlier()
    {
        return flies;
    }


    public boolean isNativeBramble()
    {
        return nativeBramble;
    }


    public boolean isNativeDrift()
    {
        return nativeDrift;
    }


    public boolean isNativeBog()
    {
        return nativeBog;
    }


    public boolean isNativeSandDune()
    {
        return nativeSandDune;
    }


    public boolean isNativeSlope()
    {
        return nativeSlope;
    }


    public static Creature getCreatureByName(String name)
    {
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            if (name.equals(creature.getName()))
            {
                return creature;
            }
        }

        Game.logError("There is no creature called " + name);
        return null;
    }


    public String toString()
    {
        return name;
    }


    /** Compare by name.  Inconsistent with equals. */
    public int compareTo(Object object)
    {
        if (object instanceof Creature)
        {
            Creature other = (Creature)object;
            return (name.compareTo(other.name));
        }
        else
        {
            throw new ClassCastException();
        }
    }
}
