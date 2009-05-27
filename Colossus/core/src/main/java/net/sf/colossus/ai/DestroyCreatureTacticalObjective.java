package net.sf.colossus.ai;

import java.util.logging.Logger;
import net.sf.colossus.client.Client;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;

/**
 *
 * @author Romain Dolbeau
 */
class DestroyCreatureTacticalObjective extends AbstractTacticalObjective
{
    private static final Logger LOGGER = Logger.getLogger(
            DestroyCreatureTacticalObjective.class.getName());
    private final Creature critter;
    private final Legion killlegion;
    private final Client client;
    private final int count;
    private final int number;

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
            Creature critter, int number)
    {
        super(priority);
        this.number = number;
        this.critter = critter;
        this.killlegion = killlegion;
        this.client = client;
        count = countCreatureType(killlegion);
        if (count < number)
        {
            LOGGER.warning("Trying to kill + number + "  + critter.getName()
                    + " but there is only " + count + " in "
                    + killlegion.getMarkerId());
        }
    }

    public boolean objectiveAttained()
    {
        if (countCreatureType(killlegion) + number <= count)
        {
            return true;
        }
        return false;
    }

    public int situationContributeToTheObjective()
    {
        int mcount = 0;
        if (objectiveAttained())
        {
            return 0;
        }
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
                    else if (aCritter.isRangestriker()
                          && (range <= aCritter.getSkill())
                          && (aCritter.useMagicMissile()
                           || (!dCritter.isLord())))
                    {
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

    public String getDescription()
    {
        return "Destroying " + number + " " + critter.getName();
    }
}
