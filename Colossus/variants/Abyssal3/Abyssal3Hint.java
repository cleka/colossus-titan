package Abyssal3;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import Default.DefaultHint;


public class Abyssal3Hint implements net.sf.colossus.server.HintInterface
{
    private final DevRandom rnd = new DevRandom();

    public String getRecruitHint(String terrain, LegionInfo legion,
        List recruits, HintOracleInterface oracle, String[] section)
    {
        recruits = DefaultHint.creaturesToStrings(recruits);

        if (terrain.equals("Brush"))
        {
            if (recruits.contains("Cyclops") && !legion.contains("Behemoth")
                && legion.getHeight() != 6
                && legion.numCreature("Cyclops") == 2
                && oracle.creatureAvailable("Behemoth") >= 2)
            {
                return "Cyclops";
            }
        }
        else if (terrain.equals("Plains"))
        {
            if (recruits.contains("Lion") && !legion.contains("Griffon")
                && legion.getHeight() != 6 && legion.numCreature("Lion") == 2
                && oracle.creatureAvailable("Griffon") >= 2)
            {
                return "Lion";
            }
        }
        else if (terrain.equals("Marsh"))
        {
            if (recruits.contains("Troll") && !legion.contains("Wyvern")
                && legion.getHeight() != 6 && legion.numCreature("Troll") == 2
                && oracle.creatureAvailable("Wyvern") >= 2
                && oracle.canReach("Swamp"))
            {
                return "Troll";
            }
            if (recruits.contains("Ranger")
                && !legion.contains("AirElemental") && legion.getHeight() != 6
                && legion.numCreature("Ranger") == 2
                && oracle.creatureAvailable("AirElemental") >= 3
                && oracle.canReach("Plains"))
            {
                return "Ranger";
            }
        }
        else if (terrain.equals("Woods"))
        {
            if (recruits.contains("Unicorn")
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
            if (recruits.contains("Hydra")
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
            if (recruits.contains("Colossus")
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
            if (recruits.contains("Knight"))
            {
                return "Knight";
            }
            if (recruits.contains("Warlock"))
            {
                return "Warlock";
            }
            if (recruits.contains("Guardian"))
            {
                return "Guardian";
            }
            if (recruits.contains("Cyclops")
                && legion.numCreature("Cyclops") == 2)
            {
                return "Cyclops";
            }
            if (recruits.contains("Troll") && legion.numCreature("Troll") == 2)
            {
                return "Troll";
            }
            if (recruits.contains("Lion") && legion.numCreature("Lion") == 2)
            {
                return "Lion";
            }
        }

        return (String)recruits.get(recruits.size() - 1);
    }

    public List getInitialSplitHint(String label, String[] section)
    {
        List li = new ArrayList();
        if (label.equals("100"))
        {
            li.add("Titan");
            li.add("Cyclops");
            li.add("Troll");
            li.add("Troll");
        }
        else if (label.equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Cyclops");
                li.add("Troll");
                li.add("Troll");
            }
            else
            {
                li.add("Titan");
                li.add("Cyclops");
                li.add("Cyclops");
                li.add("Lion");
            }
        }
        else if (label.equals("500"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Cyclops");
                li.add("Cyclops");
                li.add("Lion");
            }
            else
            {
                li.add("Titan");
                li.add("Cyclops");
                li.add("Lion");
                li.add("Lion");
            }
        }
        else
        {
            throw new RuntimeException("Bad hex label " + label);
        }
        return li;
    }

    public int getHintedRecruitmentValueOffset(String name, String[] section)
    {
        return 0;
    }
}
