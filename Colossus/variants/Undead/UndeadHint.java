package Undead;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.IVariantHint;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import Default.DefaultHint;


public class UndeadHint implements IVariantHint
{
    private final DevRandom rnd = new DevRandom();

    public String getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle, String[] section)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = DefaultHint.creaturesToStrings(recruits);

        if (terrainId.equals("Brush"))
        {
            if (recruitNames.contains("Zombie") && !legion.contains("Wraith")
                && legion.numCreature("Zombie") == 2
                && oracle.creatureAvailable("Wraith") >= 2)
            {
                return "Zombie";
            }
        }
        else if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Naga") && !legion.contains("Griffin")
                && legion.numCreature("Naga") == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffin") >= 2)
            {
                return "Naga";
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Orc") && !legion.contains("Wyvern")
                && legion.numCreature("Orc") == 2 && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return "Orc";
            }
        }
        else if (terrainId.equals("Tower"))
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

    public List<String> getInitialSplitHint(MasterHex hex, String[] section)
    {
        List<String> li = new ArrayList<String>();
        if (hex.getLabel().equals("100"))
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
        else if (hex.getLabel().equals("200"))
        {
            li.add("Titan");
            li.add("Skeleton");
            li.add("Skeleton");
            li.add("Troglodyte");
        }
        else if (hex.getLabel().equals("300"))
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
        else if (hex.getLabel().equals("400"))
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
        else if (hex.getLabel().equals("500"))
        {
            li.add("Titan");
            li.add("Skeleton");
            li.add("Skeleton");
            li.add("Paladin");
        }
        else if (hex.getLabel().equals("600"))
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
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }

    public int getHintedRecruitmentValueOffset(String name, String[] section)
    {
        return 0;
    }
}
