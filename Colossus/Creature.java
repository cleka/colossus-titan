import java.awt.*;
import java.util.*;

/**
 * Class Creature represents a Titan Character
 * @version $Id$
 * @author David Ripton
 */

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
    static final Creature angel = new Creature("Angel", 6, 4, 
        false, true, false, false, false, false, false, true);
    static final Creature archangel = new Creature("Archangel", 9, 4, 
        false, true, false, false, false, false, false, true);
    static final Creature behemoth = new Creature("Behemoth", 8, 3, 
        false, false, true, false, false, false, false, false);
    static final Creature centaur = new Creature("Centaur", 3, 4, 
        false, false, false, false, false, false, false, false);
    static final Creature colossus = new Creature("Colossus", 10, 4, 
        false, false, false, true, false, false, true, false);
    static final Creature cyclops = new Creature("Cyclops", 9, 2, 
        false, false, true, false, false, false, false, false);
    static final Creature dragon = new Creature("Dragon", 9, 3, 
        true, true, false, false, false, false, true, false);
    static final Creature gargoyle = new Creature("Gargoyle", 4, 3, 
        false, true, true, false, false, false, false, false);
    static final Creature giant = new Creature("Giant", 7, 4, 
        true, false, false, true, false, false, false, false);
    static final Creature gorgon = new Creature("Gorgon", 6, 3, 
        true, true, true, false, false, false, false, false);
    static final Creature griffon = new Creature("Griffon", 5, 4, 
        false, true, false, false, false, true, false, false);
    static final Creature guardian = new Creature("Guardian", 12, 2, 
        false, true, false, false, false, false, false, true);
    static final Creature hydra = new Creature("Hydra", 10, 3, 
        true, false, false, false, true, true, false, false);
    static final Creature lion = new Creature("Lion", 5, 3, 
        false, false, false, false, false, true, true, false);
    static final Creature minotaur = new Creature("Minotaur", 4, 4, 
        true, false, false, false, false, false, true, false);
    static final Creature ogre = new Creature("Ogre", 6, 2, 
        false, false, false, false, true, false, true, false);
    static final Creature ranger = new Creature("Ranger", 4, 4, 
        true, true, false, false, true, false, false, false);
    static final Creature serpent = new Creature("Serpent", 18, 2, 
        false, false, true, false, false, false, false, false);
    static final Creature titan = new Creature("Titan", 6, 4, 
        false, false, false, false, false, false, false, true);
    static final Creature troll = new Creature("Troll", 8, 2, 
        false, false, false, true, true, false, false, false);
    static final Creature unicorn = new Creature("Unicorn", 6, 4, 
        false, false, false, false, false, false, true, false);
    static final Creature warbear = new Creature("Warbear", 6, 3, 
        false, false, false, true, false, false, false, false);
    static final Creature warlock = new Creature("Warlock", 5, 4, 
        true, false, false, false, false, false, false, true);
    static final Creature wyvern = new Creature("Wyvern", 7, 3, 
        false, true, false, false, true, false, false, false);
    

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
