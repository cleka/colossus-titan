package net.sf.colossus.server;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Log;
import net.sf.colossus.parser.CreatureLoader;
import net.sf.colossus.parser.TerrainRecruitLoader;

/**
 * Class Creature represents the CONSTANT information about a Titan
 * Creature.
 *
 * Game related info is in Critter.  Counts of
 * recruited/available/dead are in Caretaker.
 *
 * @version $Id$
 * @author David Ripton, Bruce Sherrod
 * @author Romain Dolbeau
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
    private final boolean nativeVolcano;
    private final boolean nativeRiver;
    private final boolean nativeStone;
    private final boolean nativeTree;
    private final boolean waterDwelling;
    private final boolean magicMissile;
    private final boolean summonable;
    private final boolean lord;
    private final boolean demilord;
    private final int maxCount;
    private final String baseColor;
    private static boolean noBaseColor = false;

    public static final Creature unknown = new Creature("Unknown", 1, 1,
        false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false,
        1, "Unknown", null);

    /** Sometimes we need to iterate through all creature types. */
    private static java.util.List creatures = new ArrayList();
    private static java.util.List summonableCreatures = new ArrayList();


    public Creature(String name, int power, int skill, boolean rangestrikes,
        boolean flies, boolean nativeBramble, boolean nativeDrift,
        boolean nativeBog, boolean nativeSandDune, boolean nativeSlope,
        boolean nativeVolcano, boolean nativeRiver, boolean nativeStone,
        boolean nativeTree, boolean waterDwelling, boolean magicMissile, 
        boolean summonable, boolean lord, boolean demilord, int maxCount, 
        String pluralName, String baseColor)
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
        this.nativeVolcano = nativeVolcano;
        this.nativeRiver = nativeRiver;
        this.nativeStone = nativeStone;
        this.nativeTree = nativeTree;
        this.waterDwelling = waterDwelling;
        this.magicMissile = magicMissile;
        this.summonable = summonable;
        this.lord = lord;
        this.demilord = demilord;
        this.maxCount = maxCount;
        this.pluralName = pluralName;
        this.baseColor = baseColor;

        /* warn about likely inapropriate combinations */
        if (waterDwelling && nativeSandDune)
        {
            Log.warn("Creature " + name + 
                " is both a Water Dweller and native to Sand and Dune.");
        }
    }


    public Creature(Creature creature)
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
        this.nativeVolcano = creature.nativeVolcano;
        this.nativeRiver = creature.nativeRiver;
        this.nativeStone = creature.nativeStone;
        this.nativeTree = creature.nativeTree;
        this.waterDwelling = creature.waterDwelling;
        this.magicMissile = creature.magicMissile;
        this.summonable = creature.summonable;
        this.lord = creature.lord;
        this.demilord = creature.demilord;
        this.maxCount = creature.maxCount;
        this.pluralName = creature.pluralName;
        this.baseColor = creature.baseColor;
    }


    /** Call immediately after loading variant, before using creatures. */
    public static void loadCreatures()
    {
        try 
        {
            creatures.clear();
            java.util.List directories = 
                VariantSupport.getVarDirectoriesList();
            InputStream creIS = ResourceLoader.getInputStream(
                VariantSupport.getCreaturesName(), directories);
            if (creIS == null) 
            {
                throw new FileNotFoundException(
                    VariantSupport.getCreaturesName());
            }
            CreatureLoader creatureLoader = new CreatureLoader(creIS);
            while (creatureLoader.oneCreature(creatures) >= 0) {}
        }
        catch (Exception e) 
        {
            Log.error("Creatures def. loading failed : " + e);
            System.exit(1);
        }
        summonableCreatures.clear();
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature c = (Creature)it.next();
            if (c.isSummonable())
            {
                summonableCreatures.add(c);
            }
        }
        Collections.sort(creatures);
    }


    public static java.util.List getCreatures()
    {
        return java.util.Collections.unmodifiableList(creatures);
    }

    public static java.util.List getSummonableCreatures()
    {
        return java.util.Collections.unmodifiableList(summonableCreatures);
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
        return name.equals(Constants.titan);
    }

    public String getName()
    {
        return name;
    }

    public String getPluralName()
    {
        return pluralName;
    }

    public String getImageName()
    {
        return name;
    }

    public String[] getImagesNames()
    {
        String[] tempNames;
        if (baseColor != null)
        {
            int specialIncrement = ((isFlier() || isRangestriker()) ? 1 : 0);
            tempNames =
                new String[4 + specialIncrement];
            String colorSuffix =  "-" + (noBaseColor ? "black" : baseColor);
            tempNames[0] = name;
            tempNames[1] = "Power-" + getPower() + colorSuffix;
            
            tempNames[2] = "Skill-" + getSkill() + colorSuffix;
            tempNames[3] = name + "-Name" + colorSuffix;
            if (specialIncrement > 0)
            {
                tempNames[4] =
                    (isFlier() ? "Flying" : "") +
                    (isRangestriker() ? "Rangestrike" : "") + colorSuffix;
            }
        }
        else
        {
            tempNames = new String[1];
            tempNames[0] = name;
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
    {
        return getPower() * getSkill();
    }

    public int getHintedRecruitmentValue()
    {
        return getPointValue() +
            VariantSupport.getHintedRecruitmentValueOffset(name);
    }
    
    public boolean isRangestriker()
    {
        return rangestrikes;
    }
    
    public boolean isFlier()
    {
        return flies;
    }

    public boolean isNativeTerrain(char t)
    {
        switch(t)
        {
        case 'p': /* undefined */
            return false;
        case 'w': /* undefined, beneficial for everyone */
            return true;
        case 'r':
            return isNativeBramble();
        case 's':
            return isNativeSandDune();
        case 't':
            return isNativeTree();
        case 'o':
            return isNativeBog();
        case 'v':
            return isNativeVolcano();
        case 'd':
            return isNativeDrift();
        case 'l':
            return isWaterDwelling();
        case 'n':
            return isNativeStone();
        default:
            return false;
        }
    }

    public boolean isNativeHexside(char h)
    {
        switch(h)
        {
        default:
            return false;
        case ' ': /* undefined */
            return false;
        case 'd':
            return isNativeSandDune();
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

    public boolean isNativeVolcano()
    {
        return nativeVolcano;
    }

    public boolean isNativeRiver()
    {
        return nativeRiver;
    }

    public boolean isNativeStone()
    {
        return nativeStone;
    }

    public boolean isNativeTree()
    {
        return nativeTree;
    }

    public boolean isWaterDwelling()
    {
        return waterDwelling;
    }

    public boolean useMagicMissile()
    {
        return magicMissile;
    }

    public boolean isSummonable()
    {
        return summonable;
    }

    public static Creature getCreatureByName(String name)
    {
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            if (name.equalsIgnoreCase(creature.getName()))
            {
                return creature;
            }
        }
        return null;
    }

    public static boolean isCreature(String name)
    {
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            if (name != null && name.equals(creature.getName()))
            {
                return true;
            }
        }
        return false;
    }

    public String toString()
    {
        return name;
    }


    /** Compare by name. */
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


    /** Compare by name. */
    public final boolean equals(Object object)
    {
        if (!(object instanceof Creature))
        {
            return false;
        }
        Creature other = (Creature)object;
        return name.equals(other.getName());
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public static void setNoBaseColor(boolean b)
    {
        noBaseColor = b;
    }
}
