package Pantheon;


import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.DevRandom;
import Default.DefaultHint;
import java.util.*;


public class PantheonHint implements net.sf.colossus.server.HintInterface
{
    private DevRandom rnd = new DevRandom();

    public String getRecruitHint(String terrain,
            LegionInfo legion,
            List recruits,
            HintOracleInterface oracle,
            String[] section)
    {
        recruits = DefaultHint.creaturesToStrings(recruits);
        if (terrain.equals("Plains"))
        {
            if (recruits.contains("Lion") &&
                    !legion.contains("Griffon") &&
                    legion.getHeight() != 6 &&
                    legion.numCreature("Lion") == 2 &&
                    oracle.creatureAvailable("Griffon") >= 2 &&
                    oracle.canReach("Desert"))
            {
                return "Lion";
            }
        }
        else if (terrain.equals("Marsh"))
        {
            if (recruits.contains("Troll") &&
                    !legion.contains("Wyvern") &&
                    legion.getHeight() != 6 &&
                    legion.numCreature("Troll") == 2 &&
                    oracle.creatureAvailable("Wyvern") >= 2 &&
                    oracle.canReach("Swamp"))
            {
                return "Troll";
            }
        }
        else if (terrain.equals("Woods"))
        {
            if (recruits.contains("Griffon") &&
                    !legion.contains("Salamander") &&
                    legion.numCreature("Griffon") == 2 &&
                    oracle.creatureAvailable("Salamander") >= 2 &&
                    oracle.canReach("Desert"))
            {
                return "Griffon";
            }
        }
        else if (terrain.equals("Hills"))
        {
            if (recruits.contains("Wyvern") &&
                    !legion.contains("Mammoth") &&
                    legion.numCreature("Wyvern") == 2 &&
                    oracle.creatureAvailable("Mammoth") >= 2 &&
                    oracle.canReach("Swamp"))
            {
                return "Griffon";
            }
        }
        else if (terrain.equals("Tower"))
        {
            if (recruits.contains("Wraith"))
            {
                return "Wraith";
            }
            if (recruits.contains("Warlock"))
            {
                return "Warlock";
            }
            if (recruits.contains("Guardian"))
            {
                return "Guardian";
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

        return (String)recruits.get(recruits.size() - 1);
    }

    public List getInitialSplitHint(String label,
            String[] section)
    {
        List sect = Arrays.asList(section);

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
            li.add("Ogre");
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
            li.add("Ogre");
            li.add("Ogre");
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
        else if (label.equals("700"))
        {
            if (sect.contains(Constants.sectionDefensiveAI))
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
            else if (sect.contains(Constants.sectionOffensiveAI))
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
        else if (label.equals("800"))
        {
            if (sect.contains(Constants.sectionDefensiveAI))
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
            else if (sect.contains(Constants.sectionOffensiveAI))
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
