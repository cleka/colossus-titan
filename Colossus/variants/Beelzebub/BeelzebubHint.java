package Beelzebub;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import Default.DefaultHint;


public class BeelzebubHint implements net.sf.colossus.server.HintInterface
{
    private DevRandom rnd = new DevRandom();

    public String getRecruitHint(String terrain, LegionInfo legion,
        List recruits, HintOracleInterface oracle, String[] section)
    {
        recruits = DefaultHint.creaturesToStrings(recruits);

        if (terrain.equals("Plains"))
        {
            if (recruits.contains("Centaur") && !legion.contains("Warbear")
                && !legion.contains("Ent") && !legion.contains("Unicorn")
                && !legion.contains("Ranger") && !legion.contains("Knight")
                && !oracle.canRecruit("Ranger") && legion.getHeight() != 6
                && legion.numCreature("Centaur") == 2
                && oracle.canReach("Woods")
                && oracle.creatureAvailable("Warbear") >= 2)
            {
                return "Centaur";
            }
            if (recruits.contains("Ranger") && !legion.contains("Basilisk")
                && legion.getHeight() != 6
                && legion.numCreature("Ranger") == 2
                && oracle.canReach("Marsh")
                && oracle.creatureAvailable("Basilisk") >= 1)
            {
                return "Ranger";
            }

        }
        else if (terrain.equals("Marsh"))
        {
            if (recruits.contains("Goblin") && !legion.contains("Minotaur")
                && !legion.contains("Djinn") && !legion.contains("Manticore")
                && !legion.contains("Ranger") && !legion.contains("Basilisk")
                && !oracle.canRecruit("Ranger")
                && legion.numCreature("Goblin") == 2
                && legion.getHeight() != 6 && oracle.canReach("Hills")
                && oracle.creatureAvailable("Minotaur") >= 2)
            {
                return "Goblin";
            }
        }
        else if (terrain.equals("Ruins"))
        {
            if (recruits.contains("Skeleton") && !legion.contains("Zombie")
                && !legion.contains("Ghost") && !legion.contains("Vampire")
                && !oracle.canRecruit("Vampire") && !legion.contains("Lich")
                && !legion.contains("Death") && !legion.contains("Mummy")
                && legion.numCreature("Skeleton") == 2
                && legion.getHeight() != 6 && oracle.canReach("Tombs")
                && oracle.creatureAvailable("Zombie") >= 2)
            {
                return "Skeleton";
            }
            if (recruits.contains("Shade") && !legion.contains("Ghost")
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
        else if (terrain.equals("Tombs"))
        {
            if (recruits.contains("Ghost") && !legion.contains("Wraith")
                && !legion.contains("Beelzebub") && !legion.contains("Death")
                && !oracle.canRecruit("Death")
                && legion.numCreature("Ghost") == 2 && legion.getHeight() != 6
                && oracle.canReach("Circle")
                && oracle.creatureAvailable("Wraith") >= 2)
            {
                return "Ghost";
            }
        }
        else if (terrain.equals("Desert"))
        {
            if (recruits.contains("Djinn") && !legion.contains("Manticore")
                && legion.numCreature("Djinn") == 2 && legion.getHeight() != 6
                && oracle.canReach("Hills")
                && oracle.creatureAvailable("Manticore") >= 1)
            {
                return "Djinn";
            }
        }
        else if (terrain.equals("Swamp"))
        {
            if (recruits.contains("Ent") && !legion.contains("Unicorn")
                && legion.numCreature("Ent") == 2 && legion.getHeight() != 6
                && oracle.canReach("Woods")
                && oracle.creatureAvailable("Unicorn") >= 1)
            {
                return "Ent";
            }
        }
        else if (terrain.equals("Tower"))
        {
            if (recruits.contains("Skeleton")
                && legion.contains("Skeleton")
                && legion.numCreature("Skeleton") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Tombs")
                    && !legion.contains("Zombie") && oracle
                    .creatureAvailable("Zombie") >= 2)))
            {
                return "Skeleton";
            }
            if (recruits.contains("Skeleton") && legion.contains("Skeleton")
                && legion.numCreature("Skeleton") == 1
                && !legion.contains("Shade") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Shade") >= 2)
            {
                return "Skeleton";
            }
            if (recruits.contains("Skeleton") && !legion.contains("Skeleton")
                && !legion.contains("Shade") && legion.getHeight() != 6
                && oracle.creatureAvailable("Shade") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return "Skeleton";
            }
            if (recruits.contains("Goblin")
                && legion.contains("Goblin")
                && legion.numCreature("Goblin") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Hills")
                    && !legion.contains("Minotaur") && oracle
                    .creatureAvailable("Minotaur") >= 2)))
            {
                return "Goblin";
            }
            if (recruits.contains("Goblin") && legion.contains("Goblin")
                && legion.numCreature("Goblin") == 1
                && !legion.contains("Naga") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Naga") >= 2)
            {
                return "Goblin";
            }
            if (recruits.contains("Goblin") && !legion.contains("Goblin")
                && !legion.contains("Naga") && legion.getHeight() != 6
                && oracle.creatureAvailable("Naga") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return "Goblin";
            }
            if (recruits.contains("Centaur")
                && legion.contains("Centaur")
                && legion.numCreature("Centaur") == 2
                && !oracle.canRecruit("Golem")
                && (legion.getHeight() != 6 || (oracle.canReach("Woods")
                    && !legion.contains("Warbear") && oracle
                    .creatureAvailable("Warbear") >= 2)))
            {
                return "Centaur";
            }
            if (recruits.contains("Centaur") && legion.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && !legion.contains("Lion") && !oracle.canRecruit("Golem")
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return "Centaur";
            }
            if (recruits.contains("Centaur") && !legion.contains("Centaur")
                && !legion.contains("Lion") && legion.getHeight() != 6
                && oracle.creatureAvailable("Lion") >= 6
                && !oracle.canRecruit("Golem"))
            {
                return "Centaur";
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
                li.add("Skeleton");
                li.add("Centaur");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Skeleton");
                li.add("Skeleton");
                li.add("Goblin");
            }
        }
        else if (label.equals("200"))
        {
            li.add("Titan");
            li.add("Goblin");
            li.add("Goblin");
            li.add("Skeleton");
        }
        else if (label.equals("300"))
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
                li.add("Skeleton");
                li.add("Goblin");
            }
        }
        else if (label.equals("400"))
        {
            li.add("Titan");
            li.add("Goblin");
            li.add("Goblin");
            li.add("Skeleton");

        }
        else if (label.equals("500"))
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
                li.add("Skeleton");
                li.add("Goblin");
            }
        }
        else if (label.equals("600"))
        {
            li.add("Titan");
            li.add("Goblin");
            li.add("Goblin");
            li.add("Skeleton");
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
