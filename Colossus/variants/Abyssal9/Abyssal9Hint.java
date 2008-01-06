package Abyssal9;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;


public class Abyssal9Hint implements net.sf.colossus.server.HintInterface
{
    private final DevRandom rnd = new DevRandom();

    // Convert list of recruits from Creature to String for easier compares.
    public static List<String> creaturesToStrings(List<Creature> creatures)
    {
        List<String> recruits = new ArrayList<String>();
        for (Iterator<Creature> it = creatures.iterator(); it.hasNext();)
        {
            Object ob = it.next();
            String str = ob.toString();
            recruits.add(str);
        }
        return recruits;
    }

    public String getRecruitHint(String terrain, LegionInfo legion,
        List<Creature> recruits, HintOracleInterface oracle, String[] section)
    {
        List<String> recruitNames = creaturesToStrings(recruits);
        List<String> sect = Arrays.asList(section);

        if (terrain.equals("Brush"))
        {
            if (recruitNames.contains("Cyclops")
                && !legion.contains("Behemoth") && legion.getHeight() != 6
                && legion.numCreature("Cyclops") == 2
                && oracle.creatureAvailable("Behemoth") >= 2)
            {
                return "Cyclops";
            }
        }
        else if (terrain.equals("Plains"))
        {
            if (recruitNames.contains("Lion") && !legion.contains("Griffon")
                && legion.getHeight() != 6 && legion.numCreature("Lion") == 2
                && oracle.creatureAvailable("Griffon") >= 2)
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
        else if (terrain.equals("Marsh"))
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
        else if (terrain.equals("Woods"))
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
        else if (terrain.equals("Desert"))
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
        else if (terrain.equals("Tundra"))
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
        else if (terrain.equals("Tower"))
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

    public List<String> getInitialSplitHint(String label, String[] section)
    {
        // This variant favors the ogre track over the centaur track at 
        // the start, so keep either ogres or gargoyles with the centaur
        // except for a rare change-up.
        List<String> li = new ArrayList<String>();
        {
            float f = rnd.nextFloat();
            if (f < 0.01)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Centaur");
                li.add("Centaur");
            }
            else if (f < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Ogre");
                li.add("Ogre");
            }
            else if (f < 0.75)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
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
        return li;
    }

    public int getHintedRecruitmentValueOffset(String name, String[] section)
    {
        return 0;
    }
}
