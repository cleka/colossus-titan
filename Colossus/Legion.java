import java.awt.*;
import java.util.*;

/**
 * Class Legion represents a Titan stack of Characters and its
 * stack marker.
 * @version $Id$
 * @author David Ripton
 */

class Legion
{
    Chit chit;
    int height;
    String markerId;    // Bk03, Rd12, etc.
    Character [] chars = new Character[7];

    Legion(int cx, int cy, int scale, String markerId,
        Container container, int height, Character [] chars)
    {
        String imageFilename = "images/" + markerId + ".gif";
        this.chit = new Chit(cx, cy, scale, imageFilename, container);
        this.height = height;
        // XXX Is this the right way to do this?
        this.chars = chars;
    }


    int getPointValue()
    {
        int pointValue = 0;
        for (int i = 0; i < height; i++)
        {
            pointValue += chars[i].getPointValue();
        }
        return pointValue;
    }


    boolean canFlee()
    {
        for (int i = 0; i < height; i++)
        {
            if (chars[i].lord)
            {
                return false;
            }
        }
        return true;
    }

}


class Character
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


    // Add various character archetypes as class members
    private static final Character[] characterData =
    {
        new Character("Angel", 6, 4, false, true, false, false, false,
            false, false, true),
        new Character("Archangel", 9, 4, false, true, false, false, false,
            false, false, true),
        new Character("Behemoth", 8, 3, false, false, true, false, false, 
            false, false, false),
        new Character("Centaur", 3, 4, false, false, false, false, false,
            false, false, false),
        new Character("Colossus", 10, 4, false, false, false, true, false,
            false, true, false),
        new Character("Cyclops", 9, 2, false, false, true, false, false,
            false, false, false),
        new Character("Dragon", 9, 3, true, true, false, false, false,
            false, true, false),
        new Character("Gargoyle", 4, 3, false, true, true, false, false,
            false, false, false),
        new Character("Giant", 7, 4, true, false, false, true, false,
            false, false, false),
        new Character("Gorgon", 6, 3, true, true, true, false, false, 
            false, false, false),
        new Character("Griffon", 5, 4, false, true, false, false, false,
            true, false, false),
        new Character("Guardian", 12, 2, false, true, false, false, false,
            false, false, true),
        new Character("Hydra", 10, 3, true, false, false, false, true,
            true, false, false),
        new Character("Lion", 5, 3, false, false, false, false, false,
            true, true, false),
        new Character("Minotaur", 4, 4, true, false, false, false, false,
            false, true, false),
        new Character("Ogre", 6, 2, false, false, false, false, true,
            false, true, false),
        new Character("Ranger", 4, 4, true, true, false, false, true,
            false, false, false),
        new Character("Serpent", 18, 2, false, false, true, false, false,
            false, false, false),
        new Character("Titan", 6, 4, false, false, false, false, false,
            false, false, true),
        new Character("Troll", 8, 2, false, false, false, true, true,
            false, false, false),
        new Character("Unicorn", 6, 4, false, false, false, false, false,
            false, true, false),
        new Character("Warbear", 6, 3, false, false, false, true, false, 
            false, false, false),
        new Character("Warlock", 5, 4, true, false, false, false, false,
            false, false, true),
        new Character("Wyvern", 7, 3, false, true, false, false, true,
            false, false, false),
    };
    
    private static Hashtable lookup = new Hashtable(characterData.length);
    static
    {
        for (int i = 0; i < characterData.length; i++)
        {
            lookup.put(characterData[i].name, characterData[i]);
        }
    }

    Character(String name, int power, int skill, boolean rangeStrikes, 
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
