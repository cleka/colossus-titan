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
import Default.DefaultHint;


public class PantheonHint extends AbstractHintProvider
{
    private final DevRandom rnd = new DevRandom();

    public String getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = DefaultHint.creaturesToStrings(recruits);
        if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Lion") && !legion.contains("Griffon")
                && legion.getHeight() != 6 && legion.numCreature("Lion") == 2
                && oracle.creatureAvailable("Griffon") >= 2
                && oracle.canReach("Desert"))
            {
                return "Lion";
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Troll") && !legion.contains("Wyvern")
                && legion.getHeight() != 6 && legion.numCreature("Troll") == 2
                && oracle.creatureAvailable("Wyvern") >= 2
                && oracle.canReach("Swamp"))
            {
                return "Troll";
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
                return "Griffon";
            }
        }
        else if (terrainId.equals("Hills"))
        {
            if (recruitNames.contains("Wyvern") && !legion.contains("Mammoth")
                && legion.numCreature("Wyvern") == 2
                && oracle.creatureAvailable("Mammoth") >= 2
                && oracle.canReach("Swamp"))
            {
                return "Griffon";
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Wraith"))
            {
                return "Wraith";
            }
            if (recruitNames.contains("Warlock"))
            {
                return "Warlock";
            }
            if (recruitNames.contains("Guardian"))
            {
                return "Guardian";
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 1
                && oracle.creatureAvailable("Cyclops") >= 3)
            {
                return "Gargoyle";
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 1
                && oracle.creatureAvailable("Troll") >= 2)
            {
                return "Ogre";
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return "Centaur";
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 0
                && oracle.creatureAvailable("Cyclops") >= 6)
            {
                return "Gargoyle";
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 0
                && oracle.creatureAvailable("Troll") >= 6)
            {
                return "Ogre";
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 0
                && oracle.creatureAvailable("Lion") >= 6)
            {
                return "Centaur";
            }
        }

        return recruitNames.get(recruitNames.size() - 1);
    }

    public List<String> getInitialSplitHint(MasterHex hex,
        List<AIStyle> aiStyles)
    {
        List<String> li = new ArrayList<String>();
        if (hex.getLabel().equals("100"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Centaur");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Ogre");
            }
        }
        else if (hex.getLabel().equals("200"))
        {
            li.add("Titan");
            li.add("Gargoyle");
            li.add("Ogre");
            li.add("Ogre");
        }
        else if (hex.getLabel().equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Ogre");
            }
            else
            {
                li.add("Titan");
                li.add("Centaur");
                li.add("Centaur");
                li.add("Ogre");
            }
        }
        else if (hex.getLabel().equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Ogre");
                li.add("Ogre");
            }
            else
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Centaur");
            }
        }
        else if (hex.getLabel().equals("500"))
        {
            li.add("Titan");
            li.add("Gargoyle");
            li.add("Ogre");
            li.add("Ogre");
        }
        else if (hex.getLabel().equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Ogre");
                li.add("Ogre");
                li.add("Centaur");
            }
        }
        else if (hex.getLabel().equals("700"))
        {
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (rnd.nextFloat() < 0.4)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Centaur");
                }
                else if (rnd.nextFloat() < 0.66)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Centaur");
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Ogre");
                    li.add("Centaur");
                }
                else
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Centaur");
                    li.add("Centaur");
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (rnd.nextFloat() < 0.25)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Centaur");
                }
                else if (rnd.nextFloat() < 0.33)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Ogre");
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Ogre");
                    li.add("Centaur");
                }
                else
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Centaur");
                    li.add("Centaur");
                }
            }
            else
            {
                if (rnd.nextFloat() < 0.5)
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Ogre");
                    li.add("Centaur");
                }
                else
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Centaur");
                    li.add("Centaur");
                }
            }
        }
        else if (hex.getLabel().equals("800"))
        {
            if (aiStyles.contains(AIStyle.Defensive))
            {
                if (rnd.nextFloat() < 0.4)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Ogre");
                }
                else if (rnd.nextFloat() < 0.66)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Ogre");
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Ogre");
                    li.add("Centaur");
                }
                else
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Centaur");
                    li.add("Centaur");
                }
            }
            else if (aiStyles.contains(AIStyle.Offensive))
            {
                if (rnd.nextFloat() < 0.25)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Centaur");
                }
                else if (rnd.nextFloat() < 0.33)
                {
                    li.add("Titan");
                    li.add("Gargoyle");
                    li.add("Gargoyle");
                    li.add("Ogre");
                }
                else if (rnd.nextFloat() < 0.5)
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Ogre");
                    li.add("Centaur");
                }
                else
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Centaur");
                    li.add("Centaur");
                }
            }
            else
            {
                if (rnd.nextFloat() < 0.5)
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Ogre");
                    li.add("Centaur");
                }
                else
                {
                    li.add("Titan");
                    li.add("Ogre");
                    li.add("Centaur");
                    li.add("Centaur");
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
