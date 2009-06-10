package TitanPlus;


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


public class TitanPlusHint extends AbstractHintProvider
{
    public TitanPlusHint(Variant variant)
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
            if (recruitNames.contains("Cyclops")
                && !legion.contains("Behemoth")
                && legion.numCreature("Cyclops") == 2
                && oracle.creatureAvailable("Behemoth") >= 2)
            {
                return getCreatureType("Cyclops");
            }
        }
        else if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Lion") && !legion.contains("Griffon")
                && legion.numCreature("Lion") == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffon") >= 2)
            {
                return getCreatureType("Lion");
            }
        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Troll") && !legion.contains("Wyvern")
                && legion.numCreature("Troll") == 2
                && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return getCreatureType("Troll");
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Warlock"))
            {
                return getCreatureType("Warlock");
            }
            if (recruitNames.contains("Guardian"))
            {
                return getCreatureType("Guardian");
            }
            if (recruitNames.contains("Cyclops")
                && legion.numCreature("Cyclops") == 2)
            {
                return getCreatureType("Cyclops");
            }
            if (recruitNames.contains("Troll")
                && legion.numCreature("Troll") == 2)
            {
                return getCreatureType("Troll");
            }
            if (recruitNames.contains("Lion")
                && legion.numCreature("Lion") == 2)
            {
                return getCreatureType("Lion");
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
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Lion"));
                li.add(getCreatureType("Lion"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Troll"));
            }
        }
        else if (hex.getLabel().equals("200"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Cyclops"));
            li.add(getCreatureType("Troll"));
            li.add(getCreatureType("Troll"));
        }
        else if (hex.getLabel().equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Troll"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Lion"));
                li.add(getCreatureType("Lion"));
                li.add(getCreatureType("Troll"));
            }
        }
        else if (hex.getLabel().equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Troll"));
                li.add(getCreatureType("Troll"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Lion"));
            }
        }
        else if (hex.getLabel().equals("500"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Cyclops"));
            li.add(getCreatureType("Troll"));
            li.add(getCreatureType("Troll"));
        }
        else if (hex.getLabel().equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Cyclops"));
                li.add(getCreatureType("Lion"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Troll"));
                li.add(getCreatureType("Troll"));
                li.add(getCreatureType("Lion"));
            }
        }
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }
}
