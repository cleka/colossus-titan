package Badlands;


import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.DevRandom;
import java.util.*;


public class BadlandsHint implements net.sf.colossus.server.HintInterface
{
    private DevRandom rnd = new DevRandom();

    public String getRecruitHint(String terrain,
            LegionInfo legion,
            List recruits,
            HintOracleInterface oracle,
            String[] section)
    {
        List sect = Arrays.asList(section);

        if (terrain.equals("Brush") || terrain.equals("BrushAlt"))
        {
            if (recruits.contains("Cyclops") &&
                    !legion.contains("Behemoth") &&
                    legion.numCreature("Cyclops") == 2 &&
                    oracle.creatureAvailable("Behemoth") >= 2)
            {
                return "Cyclops";
            }
        }
        else if (terrain.equals("Plains") || terrain.equals("Fort") ||
                terrain.equals("Town") || terrain.equals("Waterhole"))
        {
            if (recruits.contains("Lion") &&
                    !legion.contains("Griffon") &&
                    legion.numCreature("Lion") == 2 &&
                    (oracle.canReach("Desert") ||
                    oracle.canReach("DesertAlt")) &&
                    oracle.creatureAvailable("Griffon") >= 2)
            {
                return "Lion";
            }
            if (sect.contains(Constants.sectionDefensiveAI))
            {
                if (recruits.contains("Centaur") &&
                        legion.numCreature("Centaur") == 2 &&
                        !legion.contains("Warbear") &&
                        legion.getHeight() < 6 &&
                        oracle.biggestAttackerHeight() == 0 &&
                        (oracle.canReach("Woods") ||
                        oracle.canReach("WoodsAlt")) &&
                        !oracle.hexLabel().equals("1") &&
                        !oracle.hexLabel().equals("15") &&
                        !oracle.hexLabel().equals("29"))
                {
                    return "Centaur";
                }
            }
            else if (sect.contains(Constants.sectionOffensiveAI))
            {
                if (recruits.contains("Centaur") &&
                        legion.numCreature("Centaur") == 2 &&
                        !legion.contains("Warbear") &&
                        legion.getHeight() <= 2 &&
                        oracle.biggestAttackerHeight() == 0 &&
                        (oracle.canReach("Woods") ||
                        oracle.canReach("WoodsAlt")))
                {
                    return "Centaur";
                }
            }
        }
        else if (terrain.equals("Marsh") || terrain.equals("MarshAlt"))
        {
            if (recruits.contains("Troll") &&
                    !legion.contains("Wyvern") &&
                    legion.numCreature("Troll") == 2 &&
                    (oracle.canReach("Swamp") ||
                    oracle.canReach("SwampAlt")) &&
                    oracle.creatureAvailable("Wyvern") >= 2)
            {
                return "Troll";
            }
            if (sect.contains(Constants.sectionDefensiveAI))
            {
                if (recruits.contains("Ogre") &&
                        legion.numCreature("Ogre") == 2 &&
                        !legion.contains("Minotaur") &&
                        legion.getHeight() < 6 &&
                        oracle.biggestAttackerHeight() == 0 &&
                        oracle.canReach("Hills") &&
                        !oracle.hexLabel().equals("8") &&
                        !oracle.hexLabel().equals("22") &&
                        !oracle.hexLabel().equals("36"))
                {
                    return "Ogre";
                }
            }
            else if (sect.contains(Constants.sectionOffensiveAI))
            {
                if (recruits.contains("Ogre") &&
                        legion.numCreature("Ogre") == 2 &&
                        !legion.contains("Minotaur") &&
                        legion.getHeight() <= 2 &&
                        oracle.biggestAttackerHeight() == 0 &&
                        oracle.canReach("Hills"))
                {
                    return "Ogre";
                }
            }
        }
        else if (terrain.equals("Tower") || terrain.equals("TowerAlt"))
        // TowerAlt is in Badlands-JDG
        {
            if (recruits.contains("Warlock"))
            {
                return "Warlock";
            }
            if (recruits.contains("Guardian"))
            {
                return "Guardian";
            }
            if (recruits.contains("Ogre") &&
                    legion.numCreature("Ogre") == 2)
            {
                return "Ogre";
            }
            if (recruits.contains("Centaur") &&
                    legion.numCreature("Centaur") == 2)
            {
                return "Centaur";
            }
            if (recruits.contains("Gargoyle") &&
                    legion.numCreature("Gargoyle") == 1 &&
                    oracle.creatureAvailable("Cyclops") >= 3)
            {
                return "Gargoyle";
            }
            if (recruits.contains("Ogre") &&
                    legion.numCreature("Ogre") == 1 &&
                    oracle.creatureAvailable("Troll") >= 2)
            {
                return "Ogre";
            }
            if (recruits.contains("Centaur") &&
                    legion.numCreature("Centaur") == 1 &&
                    oracle.creatureAvailable("Lion") >= 2)
            {
                return "Centaur";
            }
            if (recruits.contains("Gargoyle") &&
                    legion.numCreature("Gargoyle") == 0 &&
                    oracle.creatureAvailable("Cyclops") >= 6)
            {
                return "Gargoyle";
            }
            if (recruits.contains("Ogre") &&
                    legion.numCreature("Ogre") == 0 &&
                    oracle.creatureAvailable("Troll") >= 6)
            {
                return "Ogre";
            }
            if (recruits.contains("Centaur") &&
                    legion.numCreature("Centaur") == 0 &&
                    oracle.creatureAvailable("Lion") >= 6)
            {
                return "Centaur";
            }
        }

        return ((Creature)recruits.get(recruits.size() - 1)).getName();
    }

    public List getInitialSplitHint(String label,
            String[] section)
    {
        List li = new ArrayList();
        if (label.equals("100"))
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
        else if (label.equals("200"))
        {
            li.add("Titan");
            li.add("Gargoyle");
            li.add("Gargoyle");
            li.add("Ogre");
        }
        else if (label.equals("300"))
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
        else if (label.equals("400"))
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
        else if (label.equals("500"))
        {
            li.add("Titan");
            li.add("Gargoyle");
            li.add("Gargoyle");
            li.add("Centaur");
        }
        else if (label.equals("600"))
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
            throw new RuntimeException("Bad hex label " + label);
        }
        return li;
    }

    public int getHintedRecruitmentValueOffset(String name,
            String[] section)
    {
        return 0;
    }
}
