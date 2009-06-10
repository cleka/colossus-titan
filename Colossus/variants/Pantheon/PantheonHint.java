package Pantheon;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.ai.AbstractHintProvider;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


public class PantheonHint extends AbstractHintProvider
{
    public PantheonHint(Variant variant)
    {
        super(variant);
    }

    private final DevRandom rnd = new DevRandom();

    public CreatureType getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = AbstractHintProvider.creaturesToStrings(recruits);
        if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Lion") && !legion.contains("Griffon")
                && legion.getHeight() != 6 && legion.numCreature("Lion") == 2
                && oracle.creatureAvailable("Griffon") >= 2
                && oracle.canReach("Desert"))
            {
                return getCreatureType("Lion");
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Troll") && !legion.contains("Wyvern")
                && legion.getHeight() != 6 && legion.numCreature("Troll") == 2
                && oracle.creatureAvailable("Wyvern") >= 2
                && oracle.canReach("Swamp"))
            {
                return getCreatureType("Troll");
            }
        }
        else if (terrainId.equals("Woods"))
        {
            if (recruitNames.contains("Griffon")
                && !legion.contains("Salamander")
                && legion.numCreature("Griffon") == 2
                && oracle.creatureAvailable("Salamander") >= 2
                && oracle.canReach("Desert"))
            {
                return getCreatureType("Griffon");
            }
        }
        else if (terrainId.equals("Hills"))
        {
            if (recruitNames.contains("Wyvern") && !legion.contains("Mammoth")
                && legion.numCreature("Wyvern") == 2
                && oracle.creatureAvailable("Mammoth") >= 2
                && oracle.canReach("Swamp"))
            {
                return getCreatureType("Griffon");
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Wraith"))
            {
                return getCreatureType("Wraith");
            }
            if (recruitNames.contains("Warlock"))
            {
                return getCreatureType("Warlock");
            }
            if (recruitNames.contains("Guardian"))
            {
                return getCreatureType("Guardian");
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 1
                && oracle.creatureAvailable("Cyclops") >= 3)
            {
                return getCreatureType("Gargoyle");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 1
                && oracle.creatureAvailable("Troll") >= 2)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 0
                && oracle.creatureAvailable("Cyclops") >= 6)
            {
                return getCreatureType("Gargoyle");
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 0
                && oracle.creatureAvailable("Troll") >= 6)
            {
                return getCreatureType("Ogre");
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 0
                && oracle.creatureAvailable("Lion") >= 6)
            {
                return getCreatureType("Centaur");
            }
        }

        return recruits.get(recruits.size() - 1);
    }

    public List<CreatureType> getInitialSplitHint(MasterHex hex,
        List<AIStyle> aiStyles)
    {
        List<CreatureType> li = new ArrayList<CreatureType>();
        if (hex.getLabel().equals("100"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Ogre"));
            }
        }
        else if (hex.getLabel().equals("200"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Gargoyle"));
            li.add(getCreatureType("Ogre"));
            li.add(getCreatureType("Ogre"));
        }
        else if (hex.getLabel().equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Ogre"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Ogre"));
            }
        }
        else if (hex.getLabel().equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Ogre"));
                li.add(getCreatureType("Ogre"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Centaur"));
            }
        }
        else if (hex.getLabel().equals("500"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Gargoyle"));
            li.add(getCreatureType("Ogre"));
            li.add(getCreatureType("Ogre"));
        }
        else if (hex.getLabel().equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Gargoyle"));
                li.add(getCreatureType("Centaur"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Ogre"));
                li.add(getCreatureType("Ogre"));
                li.add(getCreatureType("Centaur"));
            }
        }
        else if (hex.getLabel().equals("700"))
        {
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (rnd.nextFloat() < 0.4)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Centaur"));
                }
                else if (rnd.nextFloat() < 0.66)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Centaur"));
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                }
                else
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                    li.add(getCreatureType("Centaur"));
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (rnd.nextFloat() < 0.25)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Centaur"));
                }
                else if (rnd.nextFloat() < 0.33)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Ogre"));
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                }
                else
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                    li.add(getCreatureType("Centaur"));
                }
            }
            else
            {
                if (rnd.nextFloat() < 0.5)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                }
                else
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                    li.add(getCreatureType("Centaur"));
                }
            }
        }
        else if (hex.getLabel().equals("800"))
        {
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (rnd.nextFloat() < 0.4)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Ogre"));
                }
                else if (rnd.nextFloat() < 0.66)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Ogre"));
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                }
                else
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                    li.add(getCreatureType("Centaur"));
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (rnd.nextFloat() < 0.25)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Centaur"));
                }
                else if (rnd.nextFloat() < 0.33)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Gargoyle"));
                    li.add(getCreatureType("Ogre"));
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                }
                else
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                    li.add(getCreatureType("Centaur"));
                }
            }
            else
            {
                if (rnd.nextFloat() < 0.5)
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                }
                else
                {
                    li.add(getCreatureType("Titan"));
                    li.add(getCreatureType("Ogre"));
                    li.add(getCreatureType("Centaur"));
                    li.add(getCreatureType("Centaur"));
                }
            }
        }
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }
}
