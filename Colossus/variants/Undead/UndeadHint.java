package Undead;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.ai.AbstractHintProvider;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


public class UndeadHint extends AbstractHintProvider
{
    public UndeadHint(Variant variant)
    {
        super(variant);
    }

    private final DevRandom rnd = new DevRandom();

    public CreatureType getRecruitHint(MasterBoardTerrain terrain,
        IOracleLegion legion, List<CreatureType> recruits,
        IHintOracle oracle,
        List<AIStyle> aiStyles)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = AbstractHintProvider.creaturesToStrings(recruits);

        if (terrainId.equals("Brush"))
        {
            if (recruitNames.contains("Zombie") && !legion.contains("Wraith")
                && legion.numCreature("Zombie") == 2
                && oracle.creatureAvailable("Wraith") >= 2)
            {
                return getCreatureType("Zombie");
            }
        }
        else if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Naga") && !legion.contains("Griffin")
                && legion.numCreature("Naga") == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffin") >= 2)
            {
                return getCreatureType("Naga");
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Orc") && !legion.contains("Wyvern")
                && legion.numCreature("Orc") == 2 && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return getCreatureType("Orc");
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Beholder"))
            {
                return getCreatureType("Beholder");
            }
            if (recruitNames.contains("Harpy"))
            {
                return getCreatureType("Harpy");
            }
            if (recruitNames.contains("Golem"))
            {
                return getCreatureType("Golem");
            }
            if (recruitNames.contains("Wizard"))
            {
                return getCreatureType("Wizard");
            }
            if (recruitNames.contains("Skeleton")
                && legion.numCreature("Skeleton") == 1
                && oracle.creatureAvailable("Zombie") >= 3)
            {
                return getCreatureType("Skeleton");
            }
            if (recruitNames.contains("Troglodyte")
                && legion.numCreature("Troglodyte") == 1
                && oracle.creatureAvailable("Orc") >= 2)
            {
                return getCreatureType("Troglodyte");
            }
            if (recruitNames.contains("Paladin")
                && legion.numCreature("Paladin") == 1
                && oracle.creatureAvailable("Naga") >= 2)
            {
                return getCreatureType("Paladin");
            }
            if (recruitNames.contains("Skeleton")
                && legion.numCreature("Skeleton") == 0
                && oracle.creatureAvailable("Zombie") >= 6)
            {
                return getCreatureType("Skeleton");
            }
            if (recruitNames.contains("Troglodyte")
                && legion.numCreature("Troglodyte") == 0
                && oracle.creatureAvailable("Orc") >= 6)
            {
                return getCreatureType("Troglodyte");
            }
            if (recruitNames.contains("Paladin")
                && legion.numCreature("Paladin") == 0
                && oracle.creatureAvailable("Naga") >= 6)
            {
                return getCreatureType("Paladin");
            }
        }

        return recruits.get(recruits.size() - 1);
    }

    public List<CreatureType> getInitialSplitHint(MasterHex hex,
        List<AIStyle> aiStyles)
    {
        List<CreatureType> li = new ArrayList<CreatureType>();
        if (hex.getLabel().equals("100"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Paladin"));
                li.add(getCreatureType("Paladin"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Troglodyte"));
            }
        }
        else if (hex.getLabel().equals("200"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Skeleton"));
            li.add(getCreatureType("Skeleton"));
            li.add(getCreatureType("Troglodyte"));
        }
        else if (hex.getLabel().equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Troglodyte"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Paladin"));
                li.add(getCreatureType("Paladin"));
                li.add(getCreatureType("Troglodyte"));
            }
        }
        else if (hex.getLabel().equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Troglodyte"));
                li.add(getCreatureType("Troglodyte"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Paladin"));
            }
        }
        else if (hex.getLabel().equals("500"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Skeleton"));
            li.add(getCreatureType("Skeleton"));
            li.add(getCreatureType("Paladin"));
        }
        else if (hex.getLabel().equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Paladin"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Troglodyte"));
                li.add(getCreatureType("Troglodyte"));
                li.add(getCreatureType("Paladin"));
            }
        }
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }
}
