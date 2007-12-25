package TitanPlus;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import Default.DefaultHint;


public class TitanPlusHint implements net.sf.colossus.server.HintInterface
{
    private DevRandom rnd = new DevRandom();

    public String getRecruitHint(String terrain, LegionInfo legion,
        List recruits, HintOracleInterface oracle, String[] section)
    {
        recruits = DefaultHint.creaturesToStrings(recruits);

        if (terrain.equals("Brush"))
        {
            if (recruits.contains("Cyclops") && !legion.contains("Behemoth")
                && legion.numCreature("Cyclops") == 2
                && oracle.creatureAvailable("Behemoth") >= 2)
            {
                return "Cyclops";
            }
        }
        else if (terrain.equals("Plains"))
        {
            if (recruits.contains("Lion") && !legion.contains("Griffon")
                && legion.numCreature("Lion") == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffon") >= 2)
            {
                return "Lion";
            }
        }
        else if (terrain.equals("Marsh"))
        {
            if (recruits.contains("Troll") && !legion.contains("Wyvern")
                && legion.numCreature("Troll") == 2
                && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return "Troll";
            }
        }
        else if (terrain.equals("Tower"))
        {
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
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Cyclops");
                li.add("Lion");
                li.add("Lion");
            }
            else
            {
                li.add("Titan");
                li.add("Cyclops");
                li.add("Cyclops");
                li.add("Troll");
            }
        }
        else if (label.equals("200"))
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
                li.add("Cyclops");
                li.add("Troll");
            }
            else
            {
                li.add("Titan");
                li.add("Lion");
                li.add("Lion");
                li.add("Troll");
            }
        }
        else if (label.equals("400"))
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
            li.add("Titan");
            li.add("Cyclops");
            li.add("Troll");
            li.add("Troll");
        }
        else if (label.equals("600"))
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
                li.add("Troll");
                li.add("Troll");
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
