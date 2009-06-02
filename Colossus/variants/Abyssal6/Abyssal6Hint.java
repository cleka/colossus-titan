package Abyssal6;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.colossus.common.Constants;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import Default.DefaultHint;


public class Abyssal6Hint implements net.sf.colossus.variant.IVariantHint
{
    private final DevRandom rnd = new DevRandom();

    public String getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle, String[] section)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = DefaultHint.creaturesToStrings(recruits);
        List<String> sect = Arrays.asList(section);

        if (terrainId.equals("Brush"))
        {
            if (recruitNames.contains("Cyclops")
                && !legion.contains("Behemoth") && legion.getHeight() != 6
                && legion.numCreature("Cyclops") == 2
                && oracle.creatureAvailable("Behemoth") >= 2
                && oracle.canReach("Jungle"))
            {
                return "Cyclops";
            }
        }
        else if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Lion") && !legion.contains("Griffon")
                && legion.getHeight() != 6 && legion.numCreature("Lion") == 2
                && oracle.creatureAvailable("Griffon") >= 2
                && oracle.canReach("Desert"))
            {
                return "Lion";
            }
            if (sect.contains(Constants.sectionDefensiveAI))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear") && legion.getHeight() < 6
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Woods")
                    && !oracle.hexLabel().equals("1")
                    && !oracle.hexLabel().equals("15")
                    && !oracle.hexLabel().equals("29"))
                {
                    return "Centaur";
                }
            }
            else if (sect.contains(Constants.sectionOffensiveAI))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear") && legion.getHeight() <= 2
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Woods"))
                {
                    return "Centaur";
                }
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
            if (recruitNames.contains("Ranger")
                && !legion.contains("AirElemental") && legion.getHeight() != 6
                && legion.numCreature("Ranger") == 2
                && oracle.creatureAvailable("AirElemental") >= 3
                && oracle.canReach("Plains"))
            {
                return "Ranger";
            }
            if (sect.contains(Constants.sectionDefensiveAI))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur") && legion.getHeight() < 6
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Hills")
                    && !oracle.hexLabel().equals("8")
                    && !oracle.hexLabel().equals("22")
                    && !oracle.hexLabel().equals("36"))
                {
                    return "Ogre";
                }
            }
            else if (sect.contains(Constants.sectionOffensiveAI))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur") && legion.getHeight() <= 2
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Hills"))
                {
                    return "Ogre";
                }
            }
        }
        else if (terrainId.equals("Woods"))
        {
            if (recruitNames.contains("Unicorn")
                && !legion.contains("EarthElemental")
                && legion.getHeight() != 6
                && legion.numCreature("Unicorn") == 2
                && oracle.creatureAvailable("EarthElemental") >= 3
                && oracle.canReach("Hills"))
            {
                return "Unicorn";
            }
        }
        else if (terrainId.equals("Desert"))
        {
            if (recruitNames.contains("Hydra")
                && !legion.contains("WaterElemental")
                && legion.getHeight() != 6 && legion.numCreature("Hydra") == 2
                && oracle.creatureAvailable("WaterElemental") >= 3
                && oracle.canReach("Swamp"))
            {
                return "Hydra";
            }
        }
        else if (terrainId.equals("Tundra"))
        {
            if (recruitNames.contains("Colossus")
                && !legion.contains("FireElemental")
                && legion.getHeight() != 6
                && legion.numCreature("Colossus") == 2
                && oracle.creatureAvailable("FireElemental") >= 3
                && oracle.canReach("Mountains"))
            {
                return "Colossus";
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Knight"))
            {
                return "Knight";
            }
            if (recruitNames.contains("Warlock"))
            {
                return "Warlock";
            }
            if (recruitNames.contains("Guardian"))
            {
                return "Guardian";
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 2)
            {
                return "Ogre";
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 2)
            {
                return "Centaur";
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

    public List<String> getInitialSplitHint(MasterHex hex, String[] section)
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
            li.add("Gargoyle");
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
            li.add("Gargoyle");
            li.add("Centaur");
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
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }

    public int getHintedRecruitmentValueOffset(CreatureType creature,
        String[] section)
    {
        if (creature.getName().equals("Druid"))
        {
            return -10;
        }
        return 0;
    }
}
