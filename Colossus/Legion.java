import java.awt.*;
import java.util.*;

/**
 * Class Legion represents a Titan stack of Creatures and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

class Legion
{
    Chit chit;
    int height;
    String markerId;    // Bk03, Rd12, etc.
    Creature [] creatures = new Creature[7];

    Legion(int cx, int cy, int scale, String markerId,
        Container container, int height, Creature creature0,
        Creature creature1, Creature creature2, Creature creature3, 
        Creature creature4, Creature creature5, 
        Creature creature6)
    {
        String imageFilename = "images/" + markerId + ".gif";
        this.chit = new Chit(cx, cy, scale, imageFilename, container);
        this.height = height;
        creatures[0] = creature0;
        creatures[1] = creature1;
        creatures[2] = creature2;
        creatures[3] = creature3;
        creatures[4] = creature4;
        creatures[5] = creature5;
        creatures[6] = creature6;
    }


    int getPointValue()
    {
        int pointValue = 0;
        for (int i = 0; i < height; i++)
        {
            pointValue += creatures[i].getPointValue();
        }
        return pointValue;
    }


    boolean canFlee()
    {
        for (int i = 0; i < height; i++)
        {
            if (creatures[i].lord)
            {
                return false;
            }
        }
        return true;
    }

}


class Creature
{
    String name;
    int power;
    int skill;
    boolean rangeStrikes; 
    boolean flies;
    boolean nativeBramble;
    boolean nativeDrift;
    boolean nativeBog;
    boolean nativeSandDune;
    boolean nativeSlope;
    boolean lord;


    // Add various Creature archetypes as class members
    private static final Creature[] creatureData =
    {
        new Creature("Angel", 6, 4, false, true, false, false, false,
            false, false, true),
        new Creature("Archangel", 9, 4, false, true, false, false, false,
            false, false, true),
        new Creature("Behemoth", 8, 3, false, false, true, false, false, 
            false, false, false),
        new Creature("Centaur", 3, 4, false, false, false, false, false,
            false, false, false),
        new Creature("Colossus", 10, 4, false, false, false, true, false,
            false, true, false),
        new Creature("Cyclops", 9, 2, false, false, true, false, false,
            false, false, false),
        new Creature("Dragon", 9, 3, true, true, false, false, false,
            false, true, false),
        new Creature("Gargoyle", 4, 3, false, true, true, false, false,
            false, false, false),
        new Creature("Giant", 7, 4, true, false, false, true, false,
            false, false, false),
        new Creature("Gorgon", 6, 3, true, true, true, false, false, 
            false, false, false),
        new Creature("Griffon", 5, 4, false, true, false, false, false,
            true, false, false),
        new Creature("Guardian", 12, 2, false, true, false, false, false,
            false, false, true),
        new Creature("Hydra", 10, 3, true, false, false, false, true,
            true, false, false),
        new Creature("Lion", 5, 3, false, false, false, false, false,
            true, true, false),
        new Creature("Minotaur", 4, 4, true, false, false, false, false,
            false, true, false),
        new Creature("Ogre", 6, 2, false, false, false, false, true,
            false, true, false),
        new Creature("Ranger", 4, 4, true, true, false, false, true,
            false, false, false),
        new Creature("Serpent", 18, 2, false, false, true, false, false,
            false, false, false),
        new Creature("Titan", 6, 4, false, false, false, false, false,
            false, false, true),
        new Creature("Troll", 8, 2, false, false, false, true, true,
            false, false, false),
        new Creature("Unicorn", 6, 4, false, false, false, false, false,
            false, true, false),
        new Creature("Warbear", 6, 3, false, false, false, true, false, 
            false, false, false),
        new Creature("Warlock", 5, 4, true, false, false, false, false,
            false, false, true),
        new Creature("Wyvern", 7, 3, false, true, false, false, true,
            false, false, false),
    };
    
    private static Hashtable lookup = new Hashtable(creatureData.length);
    static
    {
        for (int i = 0; i < creatureData.length; i++)
        {
            lookup.put(creatureData[i].name, creatureData[i]);
        }
    }

    Creature(String name, int power, int skill, boolean rangeStrikes, 
        boolean flies, boolean nativeBramble, boolean nativeDrift, 
        boolean nativeBog, boolean nativeSandDune, boolean nativeSlope, 
        boolean lord)
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
    }

    int getPointValue()
    {
        return power * skill;
    }

}
