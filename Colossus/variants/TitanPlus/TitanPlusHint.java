package TitanPlus;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import Default.DefaultHint;


public class TitanPlusHint implements net.sf.colossus.server.HintInterface
{
    private final DevRandom rnd = new DevRandom();

    public String getRecruitHint(String terrain, LegionClientSide legion,
        List<CreatureType> recruits, HintOracleInterface oracle,
        String[] section)
    {
        List<String> recruitNames = DefaultHint.creaturesToStrings(recruits);

        if (terrain.equals("Brush"))
        {
            if (recruitNames.contains("Cyclops")
                && !legion.contains("Behemoth")
                && legion.numCreature("Cyclops") == 2
                && oracle.creatureAvailable("Behemoth") >= 2)
            {
                return "Cyclops";
            }
        }
        else if (terrain.equals("Plains"))
        {
            if (recruitNames.contains("Lion") && !legion.contains("Griffon")
                && legion.numCreature("Lion") == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffon") >= 2)
            {
                return "Lion";
            }
        }
        else if (terrain.equals("Marsh"))
        {
            if (recruitNames.contains("Troll") && !legion.contains("Wyvern")
                && legion.numCreature("Troll") == 2
                && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return "Troll";
            }
        }
        else if (terrain.equals("Tower"))
        {
            if (recruitNames.contains("Warlock"))
            {
                return "Warlock";
            }
            if (recruitNames.contains("Guardian"))
            {
                return "Guardian";
            }
            if (recruitNames.contains("Cyclops")
                && legion.numCreature("Cyclops") == 2)
            {
                return "Cyclops";
            }
            if (recruitNames.contains("Troll")
                && legion.numCreature("Troll") == 2)
            {
                return "Troll";
            }
            if (recruitNames.contains("Lion")
                && legion.numCreature("Lion") == 2)
            {
                return "Lion";
            }
        }

        return recruitNames.get(recruitNames.size() - 1);
    }

    public List<String> getInitialSplitHint(String label, String[] section)
    {
        List<String> li = new ArrayList<String>();
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
