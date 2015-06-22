package net.sf.colossus.ai.objectives;


import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.ValueRecorder;


/** The tactical objective of killing a certain number of a specific
 * CreatureType.
 *
 * The evaluation function currently return the highest amount of point
 * gang-banging any one of the creaturetype in the opposing legion, multiplied
 * by the priority. Rangestriker count for half. This is capped to 0 if the
 * objective is already attained.
 *
 * The objective is attained whenever the number of creature are missing
 * in the opposing legion.
 *
 * @author Romain Dolbeau
 */
class DestroyCreatureTacticalObjective extends AbstractTacticalObjective
{
    private static final Logger LOGGER = Logger
        .getLogger(DestroyCreatureTacticalObjective.class.getName());
    private final Creature critter;
    private final Legion killlegion;
    private final Client client;
    private final int count;
    private final int number;

    DestroyCreatureTacticalObjective(float priority, Client client,
        Legion killlegion, Creature critter, int number)
    {
        super(priority);
        this.number = number;
        this.critter = critter;
        this.killlegion = killlegion;
        this.client = client;
        count = killlegion.numCreature(critter.getType());
        if (count < number)
        {
            LOGGER.warning("Trying to kill + number + " + critter.getName()
                + " but there is only " + count + " in "
                + killlegion.getMarkerId());
        }
    }

    public boolean objectiveAttained()
    {
        if (killlegion.numCreature(critter.getType()) + number <= count)
        {
            return true;
        }
        return false;
    }

    public ValueRecorder situationContributeToTheObjective()
    {
        ValueRecorder value = new ValueRecorder(getDescription());
        if (objectiveAttained())
        {
            return value;
        }
        for (BattleCritter dCritter : client.getInactiveBattleUnits())
        {
            if (dCritter.getType().equals(critter.getType()))
            {
                ValueRecorder lvalue = new ValueRecorder(getDescription());
                for (BattleCritter aCritter : client.getActiveBattleUnits())
                {
                    int range = Battle.getRange(dCritter.getCurrentHex(),
                        aCritter.getCurrentHex(), false);
                    if (range == 2)
                    {
                        lvalue.add(aCritter.getPointValue(), "Attacker"
                            + aCritter.getType().getName() + "CanStrike"
                            + critter.getType().getName());
                    }
                    else if (aCritter.isRangestriker()
                        && (range <= aCritter.getSkill())
                        && (aCritter.useMagicMissile() || (!dCritter.isLord())))
                    {
                        lvalue.add(aCritter.getPointValue() / 2, "Attacker"
                            + aCritter.getType().getName() + "CanRangeStrike"
                            + critter.getType().getName());
                    }
                }
                if (lvalue.getValue() > value.getValue())
                {
                    value = lvalue;
                }
            }
        }
        return value;
    }

    public String getDescription()
    {
        return "Destroying " + number + " " + critter.getName() + " ("
            + getPriority() + ")";
    }
}
