/**
 * Class Creature represents a Titan Character
 * @version $Id$
 * @author David Ripton
 */

public class Creature
{
    protected String name;
    protected String pluralName;
    protected int power;
    protected int skill;
    protected boolean rangeStrikes;
    protected boolean flies;
    protected boolean nativeBramble;
    protected boolean nativeDrift;
    protected boolean nativeBog;
    protected boolean nativeSandDune;
    protected boolean nativeSlope;
    protected boolean lord;
    protected boolean demilord;
    protected int count;


    // Add various Creature archetypes as class members
    public static Creature angel = new Creature("Angel", 6, 4, 
        false, true, false, false, false, false, false, true, false, 
        18, "Angels");
    public static Creature archangel = new Creature("Archangel", 9, 4, 
        false, true, false, false, false, false, false, true, false, 
        6, "Archngels");
    public static Creature behemoth = new Creature("Behemoth", 8, 3, 
        false, false, true, false, false, false, false, false, false, 
        18, "Behemoths");
    public static Creature centaur = new Creature("Centaur", 3, 4, 
        false, false, false, false, false, false, false, false, false, 
        25, "Centaurs");
    public static Creature colossus = new Creature("Colossus", 10, 4, 
        false, false, false, true, false, false, true, false, false, 
        10, "Colossi");
    public static Creature cyclops = new Creature("Cyclops", 9, 2, 
        false, false, true, false, false, false, false, false, false, 
        28, "Cyclopes");
    public static Creature dragon = new Creature("Dragon", 9, 3, 
        true, true, false, false, false, false, true, false, false, 
        18, "Dragons");
    public static Creature gargoyle = new Creature("Gargoyle", 4, 3, 
        false, true, true, false, false, false, false, false, false, 
        21, "Gargoyles");
    public static Creature giant = new Creature("Giant", 7, 4, 
        true, false, false, true, false, false, false, false, false, 
        18, "Giants");
    public static Creature gorgon = new Creature("Gorgon", 6, 3, 
        true, true, true, false, false, false, false, false, false, 
        25, "Gorgons");
    public static Creature griffon = new Creature("Griffon", 5, 4, 
        false, true, false, false, false, true, false, false, false, 
        18, "Griffons");
    public static Creature guardian = new Creature("Guardian", 12, 2, 
        false, true, false, false, false, false, false, false, true, 
        6, "Guardians");
    public static Creature hydra = new Creature("Hydra", 10, 3, 
        true, false, false, false, true, true, false, false, false, 
        10, "Hydras");
    public static Creature lion = new Creature("Lion", 5, 3, 
        false, false, false, false, false, true, true, false, false, 
        28, "Lions");
    public static Creature minotaur = new Creature("Minotaur", 4, 4, 
        true, false, false, false, false, false, true, false, false, 
        21, "Minotaurs");
    public static Creature ogre = new Creature("Ogre", 6, 2, 
        false, false, false, false, true, false, true, false, false, 
        25, "Ogres");
    public static Creature ranger = new Creature("Ranger", 4, 4, 
        true, true, false, false, true, false, false, false, false, 
        28, "Rangers");
    public static Creature serpent = new Creature("Serpent", 18, 2, 
        false, false, true, false, false, false, false, false, false, 
        10, "Serpents");
    public static Creature titan = new Creature("Titan", -1, 4, 
        false, false, false, false, false, false, false, true, false, 
        6, "Titans");
    public static Creature troll = new Creature("Troll", 8, 2, 
        false, false, false, true, true, false, false, false, false, 
        28, "Trolls");
    public static Creature unicorn = new Creature("Unicorn", 6, 4, 
        false, false, false, false, false, false, true, false, false, 
        12, "Unicorns");
    public static Creature warbear = new Creature("Warbear", 6, 3, 
        false, false, false, true, false, false, false, false, false, 
        21, "Warbears");
    public static Creature warlock = new Creature("Warlock", 5, 4, 
        true, false, false, false, false, false, false, false, true, 
        6, "Warlocks");
    public static Creature wyvern = new Creature("Wyvern", 7, 3, 
        false, true, false, false, true, false, false, false, false, 
        18, "Wyverns");

    // Sometimes we need to iterate through all creature types.
    public static Creature [] creatures = {angel, archangel, behemoth,
        centaur, colossus, cyclops, dragon, gargoyle, giant, gorgon,
        griffon, guardian, hydra, lion, minotaur, ogre, ranger, 
        serpent, titan, troll, unicorn, warbear, warlock, wyvern};


    public Creature(String name, int power, int skill, boolean rangeStrikes,
        boolean flies, boolean nativeBramble, boolean nativeDrift,
        boolean nativeBog, boolean nativeSandDune, boolean nativeSlope,
        boolean lord, boolean demilord, int count, String pluralName)
    {
        this.name = name;
        this.power = power;
        this.skill = skill;
        this.rangeStrikes = rangeStrikes;
        this.flies = flies;
        this.nativeBramble = nativeBramble;
        this.nativeDrift = nativeDrift;
        this.nativeBog = nativeBog;
        this.nativeSandDune = nativeSandDune;
        this.nativeSlope = nativeSlope;
        this.lord = lord;
        this.demilord = demilord;
        this.count = count;
        this.pluralName = pluralName;
    }


    public int getCount()
    {
        return count;
    }


    public void setCount(int count)
    {
        this.count = count;
    }


    public void takeOne()
    {
        count--;
    }


    public void putOneBack()
    {
        count++;
    }


    public boolean isLord()
    {
        return lord;
    }
    
    
    public boolean isDemiLord()
    {
        return demilord;
    }


    public boolean isImmortal()
    {
        return (lord || demilord);
    }


    public String getName()
    {
        return name;
    }
    
    
    public String getPluralName()
    {
        return pluralName;
    }


    // File.separator does not work right in jar files.  A hardcoded 
    // forward-slash does, and works in *x and Windows.  I have
    // no idea if it works on the Mac, etc.
    public String getImageName(boolean inverted)
    {
        if (inverted)
        {
            return "images/i_" + name + ".gif";
        }
        else
        {
            return "images/" + name + ".gif";
        }
    }
    
    
    public String getImageName()
    {
        return getImageName(false);
    }


    public int getPower()
    {
        return power;
    }


    protected void setPower(int power) 
    {
        this.power = power;
    }


    public int getSkill()
    {
        return skill;
    }


    public boolean rangeStrikes()
    {
        return rangeStrikes;
    }


    public boolean flies()
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


    public static Creature getCreatureFromName(String name)
    {
        for (int i = 0; i < creatures.length; i++)
        {
            if (name.equals(creatures[i].getName()))
            {
                return creatures[i];
            }
        }

        System.out.println("There is no creature called " + name);
        return null;
    }
}
