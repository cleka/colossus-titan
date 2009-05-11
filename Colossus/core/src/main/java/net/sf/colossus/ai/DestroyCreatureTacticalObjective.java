package net.sf.colossus.ai;

import java.util.logging.Logger;
import net.sf.colossus.client.Client;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;

/**
 *
 * @authorRomain Dolbeau
 */
class DestroyCreatureTacticalObjective implements TacticalObjective
{
    private static final Logger LOGGER = Logger.getLogger(
            DestroyCreatureTacticalObjective.class.getName());
    private final Creature critter;
    private final Legion killlegion;
    private final Client client;
    private final int count;
    private final int priority;

    private int countCreatureType(Legion legion)
    {
        int lcount = 0;
        for (Creature lcreature : legion.getCreatures())
        {
            if (lcreature.getType().equals(critter.getType()))
            {
                lcount++;
            }
        }
        return lcount;
    }

    DestroyCreatureTacticalObjective(int priority, Client client, Legion killlegion,
            Creature critter)
    {
        this.priority = priority;
        this.critter = critter;
        this.killlegion = killlegion;
        this.client = client;
        count = countCreatureType(killlegion);
        if (count <= 0)
        {
            LOGGER.warning("Trying to kill a " + critter.getName() +
                    " but there isn't any in " + killlegion.getMarkerId());
        }
    }

    public boolean objectiveAttained()
    {
        if (countCreatureType(killlegion) < count)
        {
            return true;
        }
        return false;
    }

    public int situationContributeToTheObjective()
    {
        int mcount = 0;
        for (BattleCritter dCritter : client.getInactiveBattleUnits())
        {
            if (dCritter.getCreatureType().equals(critter.getType()))
            {
                int lcount = 0;
                for (BattleCritter aCritter : client.getActiveBattleUnits())
                {
                    int range = Battle.getRange(dCritter.getCurrentHex(),
                            aCritter.getCurrentHex(), false);
                    if (range == 2)
                    {
                        lcount += aCritter.getPointValue();
                    }
                    else if (range <= aCritter.getSkill())
                    { /* TODO: lord are immnue to non-magical rangestrike */
                        lcount += aCritter.getPointValue() / 2;
                    }
                }
                if (lcount > mcount)
                {
                    mcount = lcount;
                }
            }
        }
        return mcount * getPriority();
    }

    public int getPriority()
    {
        return priority;
    }

    public String getDescription()
    {
        return "Destroying a " + critter.getName();
    }
}
