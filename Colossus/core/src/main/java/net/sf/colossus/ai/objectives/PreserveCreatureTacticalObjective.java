package net.sf.colossus.ai.objectives;


import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.ValueRecorder;


/** The tactical objective of preserving all of a specific CreatureType.
 *
 * The evaluation function currently return the negative of the highest amount
 * of point gang-banging any one of the creaturetype in our legion, multiplied
 * by the priority. Rangestriker count for half. This doesn't take a number
 * into account, so if you try to preserve 2 out of 3 Lions, this will
 * try to protect ALL Lions. This is capped to 0 after the objective has
 * failed.
 *
 * The objective is attained as long as enough creature are alive.
 *
 *
 * @author Romain Dolbeau
 */
class PreserveCreatureTacticalObjective extends AbstractTacticalObjective
{
    private static final Logger LOGGER = Logger
        .getLogger(PreserveCreatureTacticalObjective.class.getName());
    private final Creature critter;
    private final Legion liveLegion;
    private final Client client;
    private final int count;

    PreserveCreatureTacticalObjective(float priority, Client client,
        Legion liveLegion, Creature critter)
    {
        super(priority);
        this.critter = critter;
        this.liveLegion = liveLegion;
        this.client = client;
        count = liveLegion.numCreature(critter.getType());
        if (count <= 0)
        {
            LOGGER.warning("Trying to preserve all " + critter.getName()
                + " but there isn't any in " + liveLegion.getMarkerId());
        }
    }

    public boolean objectiveAttained()
    {
        if (liveLegion.numCreature(critter.getType()) >= count)
        {
            return true;
        }
        return false;
    }

    public ValueRecorder situationContributeToTheObjective()
    {
        ValueRecorder value = new ValueRecorder(getDescription());
        if (!objectiveAttained())
        {
            return value;
        }
        for (BattleCritter dCritter : client.getActiveBattleUnits())
        {
            if (dCritter.getType().equals(critter.getType()))
            {
                ValueRecorder lvalue = new ValueRecorder(getDescription());
                for (BattleCritter aCritter : client.getInactiveBattleUnits())
                {
                    int range = Battle.getRange(dCritter.getCurrentHex(),
                        aCritter.getCurrentHex(), false);
                    if (range == 2)
                    {
                        lvalue.add(-aCritter.getPointValue(), "Attacker"
                            + aCritter.getType().getName() + "CanStrike"
                            + critter.getType().getName());
                    }
                    else if (aCritter.isRangestriker()
                        && (range <= aCritter.getSkill())
                        && (aCritter.useMagicMissile() || (!dCritter.isLord())))
                    {
                        lvalue.add(-aCritter.getPointValue(), "Attacker"
                            + aCritter.getType().getName() + "CanStrike"
                            + critter.getType().getName());
                    }
                }
                if (lvalue.getValue() < value.getValue())
                {
                    value = lvalue;
                }
            }
        }
        return value;
    }

    public String getDescription()
    {
        return "Preserving all " + critter.getName() + " (" + getPriority()
            + ")";
    }
}