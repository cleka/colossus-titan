package Undead;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import Default.DefaultHint;


public class UndeadHint implements net.sf.colossus.server.HintInterface
{
    private final DevRandom rnd = new DevRandom();

    public String getRecruitHint(String terrain, LegionClientSide legion,
        List<CreatureType> recruits, HintOracleInterface oracle,
        String[] section)
    {
        List<String> recruitNames = DefaultHint.creaturesToStrings(recruits);

        if (terrain.equals("Brush"))
        {
            if (recruitNames.contains("Zombie") && !legion.contains("Wraith")
                && legion.numCreature("Zombie") == 2
                && oracle.creatureAvailable("Wraith") >= 2)
            {
                return "Zombie";
            }
        }
        else if (terrain.equals("Plains"))
        {
            if (recruitNames.contains("Naga") && !legion.contains("Griffin")
                && legion.numCreature("Naga") == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffin") >= 2)
            {
                return "Naga";
            }
        }
        else if (terrain.equals("Marsh"))
        {
            if (recruitNames.contains("Orc") && !legion.contains("Wyvern")
                && legion.numCreature("Orc") == 2 && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return "Orc";
            }
        }
        else if (terrain.equals("Tower"))
        {
            if (recruitNames.contains("Beholder"))
            {
                return "Beholder";
            }
            if (recruitNames.contains("Harpy"))
            {
                return "Harpy";
            }
            if (recruitNames.contains("Golem"))
            {
                return "Golem";
            }
            if (recruitNames.contains("Wizard"))
            {
                return "Wizard";
            }
            if (recruitNames.contains("Skeleton")
                && legion.numCreature("Skeleton") == 1
                && oracle.creatureAvailable("Zombie") >= 3)
            {
                return "Skeleton";
            }
            if (recruitNames.contains("Troglodyte")
                && legion.numCreature("Troglodyte") == 1
                && oracle.creatureAvailable("Orc") >= 2)
            {
                return "Troglodyte";
            }
            if (recruitNames.contains("Paladin")
                && legion.numCreature("Paladin") == 1
                && oracle.creatureAvailable("Naga") >= 2)
            {
                return "Paladin";
            }
            if (recruitNames.contains("Skeleton")
                && legion.numCreature("Skeleton") == 0
                && oracle.creatureAvailable("Zombie") >= 6)
            {
                return "Skeleton";
            }
            if (recruitNames.contains("Troglodyte")
                && legion.numCreature("Troglodyte") == 0
                && oracle.creatureAvailable("Orc") >= 6)
            {
                return "Troglodyte";
            }
            if (recruitNames.contains("Paladin")
                && legion.numCreature("Paladin") == 0
                && oracle.creatureAvailable("Naga") >= 6)
            {
                return "Paladin";
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
                li.add("Skeleton");
                li.add("Paladin");
                li.add("Paladin");
            }
            else
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Troglodyte");
            }
        }
        else if (label.equals("200"))
        {
            li.add("Titan");
            li.add("Skeleton");
            li.add("Skeleton");
            li.add("Troglodyte");
        }
        else if (label.equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Troglodyte");
            }
            else
            {
                li.add("Titan");
                li.add("Paladin");
                li.add("Paladin");
                li.add("Troglodyte");
            }
        }
        else if (label.equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Troglodyte");
                li.add("Troglodyte");
            }
            else
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Paladin");
            }
        }
        else if (label.equals("500"))
        {
            li.add("Titan");
            li.add("Skeleton");
            li.add("Skeleton");
            li.add("Paladin");
        }
        else if (label.equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Paladin");
            }
            else
            {
                li.add("Titan");
                li.add("Troglodyte");
                li.add("Troglodyte");
                li.add("Paladin");
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
