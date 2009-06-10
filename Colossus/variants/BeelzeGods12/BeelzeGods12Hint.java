package BeelzeGods12;


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


public class BeelzeGods12Hint extends AbstractHintProvider
{
    public BeelzeGods12Hint(Variant variant)
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

        if (terrainId.equals("Plains"))
        {
            if (recruitNames.contains("Centaur")
                && !legion.contains("Warbear") && !legion.contains("Ent")
                && !legion.contains("Unicorn") && !legion.contains("Ranger")
                && !legion.contains("Knight") && !oracle.canRecruit("Ranger")
                && legion.getHeight() != 6
                && legion.numCreature("Centaur") == 2
                && oracle.canReach("Woods")
                && oracle.creatureAvailable("Warbear") >= 2)
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Ranger")
                && !legion.contains("Basilisk") && legion.getHeight() != 6
                && legion.numCreature("Ranger") == 2
                && oracle.canReach("Marsh")
                && oracle.creatureAvailable("Basilisk") >= 1)
            {
                return getCreatureType("Ranger");
            }

        }
        else if (terrainId.equals("Marsh"))
        {
            if (recruitNames.contains("Goblin")
                && !legion.contains("Minotaur") && !legion.contains("Djinn")
                && !legion.contains("Manticore") && !legion.contains("Ranger")
                && !legion.contains("Basilisk")
                && !oracle.canRecruit("Ranger")
                && legion.numCreature("Goblin") == 2
                && legion.getHeight() != 6 && oracle.canReach("Hills")
                && oracle.creatureAvailable("Minotaur") >= 2)
            {
                return getCreatureType("Goblin");
            }
        }
        else if (terrainId.equals("Ruins"))
        {
            if (recruitNames.contains("Skeleton")
                && !legion.contains("Zombie") && !legion.contains("Ghost")
                && !legion.contains("Vampire")
                && !oracle.canRecruit("Vampire") && !legion.contains("Lich")
                && !legion.contains("Death") && !legion.contains("Mummy")
                && legion.numCreature("Skeleton") == 2
                && legion.getHeight() != 6 && oracle.canReach("Tombs")
                && oracle.creatureAvailable("Zombie") >= 2)
            {
                return getCreatureType("Skeleton");
            }
            if (recruitNames.contains("Shade") && !legion.contains("Ghost")
                && !legion.contains("Wraith") && !legion.contains("Beelzebub")
                && !legion.contains("Lich") && !oracle.canRecruit("Lich")
                && !legion.contains("Death")
                && legion.numCreature("Shade") == 2 && legion.getHeight() != 6
                && oracle.canReach("Circle")
                && oracle.creatureAvailable("Ghost") >= 2)
            {
                return getCreatureType("Shade");
            }
        }
        else if (terrainId.equals("Tombs"))
        {
            if (recruitNames.contains("Ghost") && !legion.contains("Wraith")
                && !legion.contains("Beelzebub") && !legion.contains("Death")
                && !oracle.canRecruit("Death")
                && legion.numCreature("Ghost") == 2 && legion.getHeight() != 6
                && oracle.canReach("Circle")
                && oracle.creatureAvailable("Wraith") >= 2)
            {
                return getCreatureType("Ghost");
            }
        }
        else if (terrainId.equals("Desert"))
        {
            if (recruitNames.contains("Djinn")
                && !legion.contains("Manticore")
                && legion.numCreature("Djinn") == 2 && legion.getHeight() != 6
                && oracle.canReach("Hills")
                && oracle.creatureAvailable("Manticore") >= 1)
            {
                return getCreatureType("Djinn");
            }
        }
        else if (terrainId.equals("Swamp"))
        {
            if (recruitNames.contains("Ent") && !legion.contains("Unicorn")
                && legion.numCreature("Ent") == 2 && legion.getHeight() != 6
                && oracle.canReach("Woods")
                && oracle.creatureAvailable("Unicorn") >= 1)
            {
                return getCreatureType("Ent");
            }
        }
        else if (terrainId.equals("Tower"))
        {
            if (recruitNames.contains("Skeleton")
                && legion.contains("Skeleton")
                && legion.numCreature("Skeleton") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Tombs")
                    && !legion.contains("Zombie") && oracle
                    .creatureAvailable("Zombie") >= 2)))
            {
                return getCreatureType("Skeleton");
            }
            if (recruitNames.contains("Skeleton")
                && legion.contains("Skeleton")
                && legion.numCreature("Skeleton") == 1
                && !legion.contains("Shade") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Shade") >= 2)
            {
                return getCreatureType("Skeleton");
            }
            if (recruitNames.contains("Skeleton")
                && !legion.contains("Skeleton") && !legion.contains("Shade")
                && legion.getHeight() != 6
                && oracle.creatureAvailable("Shade") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return getCreatureType("Skeleton");
            }
            if (recruitNames.contains("Goblin")
                && legion.contains("Goblin")
                && legion.numCreature("Goblin") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Hills")
                    && !legion.contains("Minotaur") && oracle
                    .creatureAvailable("Minotaur") >= 2)))
            {
                return getCreatureType("Goblin");
            }
            if (recruitNames.contains("Goblin") && legion.contains("Goblin")
                && legion.numCreature("Goblin") == 1
                && !legion.contains("Naga") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Naga") >= 2)
            {
                return getCreatureType("Goblin");
            }
            if (recruitNames.contains("Goblin") && !legion.contains("Goblin")
                && !legion.contains("Naga") && legion.getHeight() != 6
                && oracle.creatureAvailable("Naga") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return getCreatureType("Goblin");
            }
            if (recruitNames.contains("Centaur")
                && legion.contains("Centaur")
                && legion.numCreature("Centaur") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Woods")
                    && !legion.contains("Warbear") && oracle
                    .creatureAvailable("Warbear") >= 2)))
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Centaur") && legion.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && !legion.contains("Lion") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return getCreatureType("Centaur");
            }
            if (recruitNames.contains("Centaur")
                && !legion.contains("Centaur") && !legion.contains("Lion")
                && legion.getHeight() != 6
                && oracle.creatureAvailable("Lion") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return getCreatureType("Centaur");
            }
        }
        return recruits.get(recruits.size() - 1);
    }

    public List<CreatureType> getInitialSplitHint(MasterHex hex,
        List<AIStyle> aiStyles)
    {
        List<CreatureType> li = new ArrayList<CreatureType>();
        if (hex.getLabel().equals("501"))
        {
            float nextFloat = rnd.nextFloat();
            if (nextFloat < 0.3333)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Goblin"));
            }
            else if (nextFloat < 0.6667)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Skeleton"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Skeleton"));
            }
        }
        else if (hex.getLabel().equals("502"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Skeleton"));
        }
        else if (hex.getLabel().equals("503"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Skeleton"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Skeleton"));
            }
        }
        else if (hex.getLabel().equals("504"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Skeleton"));
            li.add(getCreatureType("Skeleton"));
            li.add(getCreatureType("Goblin"));
        }
        else if (hex.getLabel().equals("505"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Skeleton"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Goblin"));
            }
        }
        else if (hex.getLabel().equals("506"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Skeleton"));
        }
        else if (hex.getLabel().equals("507"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Goblin"));
            }
        }
        else if (hex.getLabel().equals("508"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Centaur"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Goblin"));
            }
        }
        else if (hex.getLabel().equals("509"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Centaur"));
                li.add(getCreatureType("Skeleton"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Skeleton"));
            }
        }
        else if (hex.getLabel().equals("510"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Skeleton"));
        }
        else if (hex.getLabel().equals("511"))
        {
            li.add(getCreatureType("Titan"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Centaur"));
            li.add(getCreatureType("Skeleton"));
        }
        else if (hex.getLabel().equals("512"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Centaur"));
            }
            else
            {
                li.add(getCreatureType("Titan"));
                li.add(getCreatureType("Skeleton"));
                li.add(getCreatureType("Goblin"));
                li.add(getCreatureType("Goblin"));
            }
        }
        else
        {
            throw new RuntimeException("Bad hex: " + hex);
        }
        return li;
    }

    @Override
    public int getHintedRecruitmentValueOffset(CreatureType creature,
        List<AIStyle> aiStyles)
    {
        if (creature.getName().equals("Imp"))
        {
            return -5;
        }
        else if (creature.getName().equals("Devil"))
        {
            return 10;
        }
        return 0;
    }
}
