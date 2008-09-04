package BeelzeGods12;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.server.HintInterface;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import Default.DefaultHint;


public class BeelzeGods12Hint implements HintInterface
{
    private final DevRandom rnd = new DevRandom();

    public String getRecruitHint(MasterBoardTerrain terrain,
        LegionClientSide legion, List<CreatureType> recruits,
        HintOracleInterface oracle, String[] section)
    {
        String terrainId = terrain.getId();
        List<String> recruitNames = DefaultHint.creaturesToStrings(recruits);

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
                return "Centaur";
            }
            if (recruitNames.contains("Ranger")
                && !legion.contains("Basilisk") && legion.getHeight() != 6
                && legion.numCreature("Ranger") == 2
                && oracle.canReach("Marsh")
                && oracle.creatureAvailable("Basilisk") >= 1)
            {
                return "Ranger";
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
                return "Goblin";
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
                return "Skeleton";
            }
            if (recruitNames.contains("Shade") && !legion.contains("Ghost")
                && !legion.contains("Wraith") && !legion.contains("Beelzebub")
                && !legion.contains("Lich") && !oracle.canRecruit("Lich")
                && !legion.contains("Death")
                && legion.numCreature("Shade") == 2 && legion.getHeight() != 6
                && oracle.canReach("Circle")
                && oracle.creatureAvailable("Ghost") >= 2)
            {
                return "Shade";
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
                return "Ghost";
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
                return "Djinn";
            }
        }
        else if (terrainId.equals("Swamp"))
        {
            if (recruitNames.contains("Ent") && !legion.contains("Unicorn")
                && legion.numCreature("Ent") == 2 && legion.getHeight() != 6
                && oracle.canReach("Woods")
                && oracle.creatureAvailable("Unicorn") >= 1)
            {
                return "Ent";
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
                return "Skeleton";
            }
            if (recruitNames.contains("Skeleton")
                && legion.contains("Skeleton")
                && legion.numCreature("Skeleton") == 1
                && !legion.contains("Shade") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Shade") >= 2)
            {
                return "Skeleton";
            }
            if (recruitNames.contains("Skeleton")
                && !legion.contains("Skeleton") && !legion.contains("Shade")
                && legion.getHeight() != 6
                && oracle.creatureAvailable("Shade") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return "Skeleton";
            }
            if (recruitNames.contains("Goblin")
                && legion.contains("Goblin")
                && legion.numCreature("Goblin") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Hills")
                    && !legion.contains("Minotaur") && oracle
                    .creatureAvailable("Minotaur") >= 2)))
            {
                return "Goblin";
            }
            if (recruitNames.contains("Goblin") && legion.contains("Goblin")
                && legion.numCreature("Goblin") == 1
                && !legion.contains("Naga") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Naga") >= 2)
            {
                return "Goblin";
            }
            if (recruitNames.contains("Goblin") && !legion.contains("Goblin")
                && !legion.contains("Naga") && legion.getHeight() != 6
                && oracle.creatureAvailable("Naga") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return "Goblin";
            }
            if (recruitNames.contains("Centaur")
                && legion.contains("Centaur")
                && legion.numCreature("Centaur") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Woods")
                    && !legion.contains("Warbear") && oracle
                    .creatureAvailable("Warbear") >= 2)))
            {
                return "Centaur";
            }
            if (recruitNames.contains("Centaur") && legion.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && !legion.contains("Lion") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return "Centaur";
            }
            if (recruitNames.contains("Centaur")
                && !legion.contains("Centaur") && !legion.contains("Lion")
                && legion.getHeight() != 6
                && oracle.creatureAvailable("Lion") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return "Centaur";
            }
        }
        return recruitNames.get(recruitNames.size() - 1);
    }

    public List<String> getInitialSplitHint(String label, String[] section)
    {
        List<String> li = new ArrayList<String>();
        if (label.equals("501"))
        {
            float nextFloat = rnd.nextFloat();
            if (nextFloat < 0.3333)
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Goblin");
            }
            else if (nextFloat < 0.6667)
            {
                li.add("Titan");
                li.add("Goblin");
                li.add("Goblin");
                li.add("Skeleton");
            }
            else
            {
                li.add("Titan");
                li.add("Centaur");
                li.add("Centaur");
                li.add("Skeleton");
            }
        }
        else if (label.equals("502"))
        {
            li.add("Titan");
            li.add("Centaur");
            li.add("Centaur");
            li.add("Skeleton");
        }
        else if (label.equals("503"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Centaur");
                li.add("Centaur");
                li.add("Skeleton");
            }
            else
            {
                li.add("Titan");
                li.add("Goblin");
                li.add("Goblin");
                li.add("Skeleton");
            }
        }
        else if (label.equals("504"))
        {
            li.add("Titan");
            li.add("Skeleton");
            li.add("Skeleton");
            li.add("Goblin");
        }
        else if (label.equals("505"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Centaur");
                li.add("Centaur");
                li.add("Skeleton");
            }
            else
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Goblin");
            }
        }
        else if (label.equals("506"))
        {
            li.add("Titan");
            li.add("Centaur");
            li.add("Centaur");
            li.add("Skeleton");
        }
        else if (label.equals("507"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Centaur");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Goblin");
                li.add("Goblin");
            }
        }
        else if (label.equals("508"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Goblin");
                li.add("Goblin");
            }
        }
        else if (label.equals("509"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Centaur");
                li.add("Centaur");
                li.add("Skeleton");
            }
            else
            {
                li.add("Titan");
                li.add("Goblin");
                li.add("Goblin");
                li.add("Skeleton");
            }
        }
        else if (label.equals("510"))
        {
            li.add("Titan");
            li.add("Centaur");
            li.add("Centaur");
            li.add("Skeleton");
        }
        else if (label.equals("511"))
        {
            li.add("Titan");
            li.add("Centaur");
            li.add("Centaur");
            li.add("Skeleton");
        }
        else if (label.equals("512"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Goblin");
                li.add("Goblin");
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
        if (name.equals("Imp"))
        {
            return -5;
        }
        else if (name.equals("Devil"))
        {
            return 10;
        }
        return 0;
    }
}
