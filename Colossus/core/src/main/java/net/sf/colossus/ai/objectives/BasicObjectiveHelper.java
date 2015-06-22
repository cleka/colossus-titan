package net.sf.colossus.ai.objectives;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.colossus.ai.AbstractAI;
import net.sf.colossus.client.Client;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.RecruitingSubTree;
import net.sf.colossus.variant.Variant;


/**
 * A naive (basic!) implementation of @IObjectiveHelper.
 * This is still mostly for testing the code.
 * @author Romain Dolbeau
 */
public class BasicObjectiveHelper extends AbstractObjectiveHelper
{
    private static final Logger LOGGER = Logger
        .getLogger(BasicObjectiveHelper.class.getName());

    public BasicObjectiveHelper(Client client, AbstractAI ai, Variant variant)
    {
        super(client, ai, variant);
    }

    /** really stupid heuristic */
    private AllThereIsToKnowAboutYourCreature findCreatureToDestroyInAttacker()
    {
        Creature creature = null;
        int mcount = 0;
        Legion attacker = client.getAttacker();

        for (Creature lcritter : attacker.getCreatures())
        {
            int count = ai.countCreatureAccrossAllLegionFromPlayer(lcritter);
            if (count == 0)
            {
                LOGGER.warning("Strange, we have at least a "
                    + lcritter.getName() + " in legion "
                    + attacker.getMarkerId() + " yet 0 in total...");
            }
            LOGGER.finest("Found " + count + " " + lcritter.getName()
                + " in all ; we have "
                + attacker.numCreature(lcritter.getType()) + " in here");
            if (!lcritter.isLord() && !lcritter.isDemiLord()
                && ((creature == null) || (count < mcount)))
            {
                creature = lcritter;
                mcount = count;
            }
        }

        LOGGER.finest("Less Common choice: "
            + (creature != null ? creature.getName() : "(NOBODY)"));

        List<AllThereIsToKnowAboutYourCreature> overkill = new ArrayList<AllThereIsToKnowAboutYourCreature>();

        overkill.addAll(attackerToKnowledge.values());
        Collections.sort(overkill, HEURISTIC_ORDER);

        StringBuffer buf = new StringBuffer();
        for (AllThereIsToKnowAboutYourCreature atitkayc : overkill)
        {
            buf.append("\t" + atitkayc.toString() + "\n");
        }
        LOGGER.finest("AllThereIsToKnowAboutYourCreature order:\n"
            + buf.toString());

        if (overkill.size() > 0)
        {
            return overkill.get(overkill.size() - 1);
        }

        if (creature != null)
        {
            return new AllThereIsToKnowAboutYourCreature(ai, creature,
                attacker);
        }

        return null;
    }

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
            if (!lcritter.isLord()
                && RecruitingSubTree.isADeadEnd(variant, lcritter.getType()))
            {
                LOGGER.info("CommonObjective: " + lcritter.getName()
                    + " is a dead end and non-lord, aka Cannon Fodder");
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
                LOGGER.info("CommonObjective: " + lcritter.getName()
                    + " matters (a bit)...");
                lListObjectives.add(new CreatureAttackTacticalObjective(
                    oec.SECOND_WAVE_ATTACK_PRIORITY, client, myself, lcritter,
                    ai, ai.bec));
            }
        }

        return lListObjectives;
    }

    /** Currently attackerObjective is very dumb:
     * try and kill the Titan (if there) and the biggest creature
     */
    public List<TacticalObjective> attackerObjective()
    {
        List<TacticalObjective> lListObjectives = new ArrayList<TacticalObjective>();
        lListObjectives.addAll(commonObjective(client.getAttacker()));
        Creature toKill = null;
        for (Creature lcritter : client.getDefender().getCreatures())
        {
            if (lcritter.isTitan())
            {
                lListObjectives.add(new DestroyCreatureTacticalObjective(
                    oec.DESTROY_TITAN_PRIORITY, client, client.getDefender(),
                    lcritter, 1));
            }
            else
            {
                if (toKill == null)
                {
                    toKill = lcritter;
                }
                else
                {
                    if (toKill.getPointValue() < lcritter.getPointValue())
                    {
                        toKill = lcritter;
                    }
                }
            }
        }
        for (Creature lcritter : client.getAttacker().getCreatures())
        {
            if (lcritter.isTitan())
            {
                lListObjectives.add(new PreserveCreatureTacticalObjective(
                    oec.ATTACKER_PRESERVE_TITAN_PRIORITY, client, client
                        .getAttacker(), lcritter));
            }
        }
        if (toKill != null)
        {
            lListObjectives.add(new DestroyCreatureTacticalObjective(
                oec.DESTROY_IMPORTANT_CRITTER_PRIORITY, client, client
                    .getDefender(), toKill, 1));
        }
        for (TacticalObjective to : lListObjectives)
        {
            LOGGER.info("Attacker Objective: " + to.getDescription());
        }
        return lListObjectives;
    }

    public List<TacticalObjective> defenderObjective()
    {
        List<TacticalObjective> lListObjectives = new ArrayList<TacticalObjective>();
        lListObjectives.addAll(commonObjective(client.getDefender()));
        for (Creature lcritter : client.getAttacker().getCreatures())
        {
            if (lcritter.isTitan())
            {
                lListObjectives.add(new DestroyCreatureTacticalObjective(
                    oec.DESTROY_TITAN_PRIORITY, client, client.getAttacker(),
                    lcritter, 1));
            }
        }
        for (Creature lcritter : client.getDefender().getCreatures())
        {
            if (lcritter.isTitan())
            {
                lListObjectives.add(new PreserveCreatureTacticalObjective(
                    oec.DEFENDER_PRESERVE_TITAN_PRIORITY, client, client
                        .getDefender(), lcritter));
            }
        }
        AllThereIsToKnowAboutYourCreature toKill = findCreatureToDestroyInAttacker();
        if (toKill != null)
        {
            lListObjectives.add(new DestroyCreatureTacticalObjective(
                oec.DESTROY_IMPORTANT_CRITTER_PRIORITY, client, client
                    .getAttacker(), toKill.creature, Math.min(
                    toKill.stackNumber, toKill.numberNeededHere)));
        }
        for (TacticalObjective to : lListObjectives)
        {
            LOGGER.info("Defender Objective: " + to.getDescription());
        }
        return lListObjectives;
    }

    private static final Comparator<AllThereIsToKnowAboutYourCreature> HEURISTIC_ORDER = new Comparator<AllThereIsToKnowAboutYourCreature>()
    {
        private int avoidNullPointerException(
            AllThereIsToKnowAboutYourCreature c1,
            AllThereIsToKnowAboutYourCreature c2)
        {
            if ((c1.bestRecruit != null) && (c2.bestRecruit == null))
            {
                return 1;
            }
            if ((c1.bestRecruit == null) && (c2.bestRecruit != null))
            {
                return -1;
            }
            if ((c1.bestRecruit != null) && (c2.bestRecruit != null))
            {
                if (c1.bestRecruit.getPointValue() > c2.bestRecruit
                    .getPointValue())
                {
                    return 1;
                }
                if (c1.bestRecruit.getPointValue() < c2.bestRecruit
                    .getPointValue())
                {
                    return -1;
                }
            }
            if (c1.creature.getPointValue() > c2.creature.getPointValue())
            {
                return 1;
            }
            if (c1.creature.getPointValue() < c2.creature.getPointValue())
            {
                return -1;
            }
            return 0;
        }

        public int compare(AllThereIsToKnowAboutYourCreature c1,
            AllThereIsToKnowAboutYourCreature c2)
        {
            if (!c1.thisStackHasBetter && c2.thisStackHasBetter)
            {
                return 1;
            }
            if (c1.thisStackHasBetter && !c2.thisStackHasBetter)
            {
                return -1;
            }
            if (c1.thisStackHasBetter && c2.thisStackHasBetter)
            {
                int result = avoidNullPointerException(c1, c2);
                if (result != 0)
                {
                    return result;
                }
            }
            if (c1.isImmediatelyUsefulKilling
                && !c2.isImmediatelyUsefulKilling)
            {
                return 1;
            }
            if (!c1.isImmediatelyUsefulKilling
                && c2.isImmediatelyUsefulKilling)
            {
                return -1;
            }
            if (c1.isImmediatelyUsefulKilling && c2.isImmediatelyUsefulKilling)
            {
                int result = avoidNullPointerException(c1, c2);
                if (result != 0)
                {
                    return result;
                }
            }
            if (c1.onlyThisStackHasIt && !c2.onlyThisStackHasIt)
            {
                return 1;
            }
            if (!c1.onlyThisStackHasIt && c2.onlyThisStackHasIt)
            {
                return -1;
            }
            if (c1.onlyThisStackHasIt && c2.onlyThisStackHasIt)
            {
                int result = avoidNullPointerException(c1, c2);
                if (result != 0)
                {
                    return result;
                }
            }
            if (c1.numberNeededHere > c2.numberNeededHere)
            {
                return 1;
            }
            if (c1.numberNeededHere < c2.numberNeededHere)
            {
                return -1;
            }
            return c1.creature.getName().compareTo(c2.creature.getName());
        }
    };

}
