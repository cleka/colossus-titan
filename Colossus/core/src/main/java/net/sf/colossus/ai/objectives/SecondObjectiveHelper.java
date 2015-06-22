package net.sf.colossus.ai.objectives;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.colossus.ai.AbstractAI;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.RecruitingSubTree;
import net.sf.colossus.variant.Variant;


/**
 * Extension of @BasicObjectiveHelper.
 * This is still mostly for testing the code.
 * @author Romain Dolbeau
 */
public class SecondObjectiveHelper extends BasicObjectiveHelper
{
    private static final Logger LOGGER = Logger
        .getLogger(SecondObjectiveHelper.class.getName());

    public SecondObjectiveHelper(Client client, AbstractAI ai, Variant variant)
    {
        super(client, ai, variant);
    }

    @Override
    protected List<TacticalObjective> commonObjective(Legion myself)
    {
        List<TacticalObjective> lListObjectives = new ArrayList<TacticalObjective>();
        Map<CreatureType, Creature> toConsider = new TreeMap<CreatureType, Creature>();

        for (Creature lcritter : myself.getCreatures())
        { // at most one entry per CreatureType ...
            toConsider.put(lcritter.getType(), lcritter);
        }

        for (CreatureType creature : toConsider.keySet())
        {
            Creature lcritter = toConsider.get(creature);
            AllThereIsToKnowAboutYourCreature ac = new AllThereIsToKnowAboutYourCreature(
                ai, lcritter, myself);
            if (!lcritter.isLord()
                && (RecruitingSubTree.isADeadEnd(variant, lcritter.getType()) || (!ac.enoughLeftToRecruitHere)))
            {
                if (!ac.enoughLeftToRecruitHere)
                {
                    LOGGER.info("CommonObjective: " + lcritter.getName()
                        + " has no more friends, aka Cannon Fodder");
                }
                else
                {
                    LOGGER.info("CommonObjective: " + lcritter.getName()
                        + " is a dead end and non-lord, aka Cannon Fodder");
                }
                lListObjectives.add(new CreatureAttackTacticalObjective(
                    oec.FIRST_WAVE_ATTACK_PRIORITY, client, myself, lcritter,
                    ai, ai.bec));
            }
            else if (lcritter.isLord() && !lcritter.isTitan())
            {
                LOGGER.info("CommonObjective: " + lcritter.getName()
                    + " is a non-titan lord, aka Cannon Fodder");
                lListObjectives.add(new CreatureAttackTacticalObjective(
                    oec.FIRST_WAVE_ATTACK_PRIORITY, client, myself, lcritter,
                    ai, ai.bec));
            }
            else if (!lcritter.isTitan())
            {
                float priority = oec.FIRST_WAVE_ATTACK_PRIORITY;
                LOGGER
                    .info("CommonObjective: " + lcritter.getName()
                        + " matters (a bit)... doing more eval (" + priority
                        + ")");
                if (ac.numberNeededHere < Constants.BIGNUM)
                {
                    priority = priority / 1.2f;
                    LOGGER.info("CommonObjective: " + lcritter.getName()
                        + " could recruit (" + priority + ")");
                    if (ac.justEnoughLeftToRecruitHere)
                    {
                        priority = priority / 1.2f;
                        LOGGER.info("CommonObjective: " + lcritter.getName()
                            + " not a single one to spare worldwide ("
                            + priority + ")");
                    }
                }
                if (ac.numberNeededHere == ac.stackNumber)
                {
                    priority = priority / 1.2f;
                    LOGGER.info("CommonObjective: " + lcritter.getName()
                        + " can recruit with no spare (" + priority + ")");
                }
                if (ac.onlyThisStackHasIt)
                {
                    priority = priority / 1.2f;
                    LOGGER.info("CommonObjective: " + lcritter.getName()
                        + " only stack with it (" + priority + ")");
                }
                boolean cs = true;
                for (CreatureType creature2 : toConsider.keySet())
                {
                    Creature lcritter2 = toConsider.get(creature2);
                    AllThereIsToKnowAboutYourCreature ac2 = new AllThereIsToKnowAboutYourCreature(
                        ai, lcritter2, myself);
                    if (!creature.equals(creature2))
                    {
                        if ((ac.bestRecruit != null)
                            && ac.bestRecruit.equals(ac2.bestRecruit))
                        {
                            if (RecruitingSubTree
                                .getAllInAllSubtreesIgnoringSpecials(variant,
                                    creature).contains(creature2))
                            {
                                cs = false;
                            }
                        }
                    }
                }
                if (cs)
                {
                    priority = priority / 1.2f;
                    LOGGER.info("CommonObjective: " + lcritter.getName()
                        + " best in class (" + priority + ")");
                }
                lListObjectives.add(new CreatureAttackTacticalObjective(
                    priority, client, myself, lcritter, ai, ai.bec));
            }
        }

        return lListObjectives;
    }
}
