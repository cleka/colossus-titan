package net.sf.colossus.ai;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.RecruitingSubTree;
import net.sf.colossus.variant.Variant;


/**
 *
 * @author Romain Dolbeau
 */
public class BasicObjectiveHelper implements IObjectiveHelper
{
    private static final Logger LOGGER = Logger
        .getLogger(BasicObjectiveHelper.class.getName());
    private final ObjectiveEvalConstants oec;
    private final Client client;
    private final AbstractAI ai;
    private final Variant variant;

    BasicObjectiveHelper(Client client, AbstractAI ai, Variant variant)
    {
        this.client = client;
        this.ai = ai;
        this.variant = variant;
        oec = new ObjectiveEvalConstants();
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
        for (Creature lcritter : attacker.getCreatures())
        {
            if (!lcritter.isTitan())
            {
                overkill.add(new AllThereIsToKnowAboutYourCreature(ai,
                    lcritter, attacker));
            }
        }
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

    private List<TacticalObjective> commonObjective(Legion myself)
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
                lListObjectives.add(new CreatureAttackTacticalObjective(
                    oec.FIRST_WAVE_ATTACK_PRIORITY, client, myself, lcritter,
                    ai, ai.bec));
            }
            else if (lcritter.isLord() && !lcritter.isTitan())
            {
                lListObjectives.add(new CreatureAttackTacticalObjective(
                    oec.FIRST_WAVE_ATTACK_PRIORITY, client, myself, lcritter,
                    ai, ai.bec));
            }
            else if (!lcritter.isTitan())
            {
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

    protected class ObjectiveEvalConstants
    {
        final float DESTROY_TITAN_PRIORITY = 5.f;
        final float ATTACKER_PRESERVE_TITAN_PRIORITY = 2.f;
        final float DEFENDER_PRESERVE_TITAN_PRIORITY = 5.f;
        final float DESTROY_IMPORTANT_CRITTER_PRIORITY = 1.f;
        final float FIRST_WAVE_ATTACK_PRIORITY = 1.f;
        final float SECOND_WAVE_ATTACK_PRIORITY = 0.5f;
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

    class AllThereIsToKnowAboutYourCreature
    {
        final Creature creature;
        final int playerNumber;
        final int stackNumber;
        final Set<CreatureType> recruits;
        final CreatureType bestRecruit;
        final int numberNeededHere;
        final boolean thisStackHasBetter;
        final boolean isImmediatelyUsefulKilling;
        final boolean onlyThisStackHasIt;

        @Override
        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append(creature.getName());
            buf.append(" playerNumber=" + playerNumber);
            buf.append(" stackNumber=" + stackNumber);
            buf.append(" bestRecruit="
                + (bestRecruit != null ? bestRecruit.getName() : "(NONE)"));
            buf.append(" numberNeededHere=" + numberNeededHere);
            buf.append(" thisStackHasBetter=" + thisStackHasBetter);
            buf.append(" isImmediatelyUsefulKilling="
                + isImmediatelyUsefulKilling);
            buf.append(" onlyThisStackHasIt=" + onlyThisStackHasIt);
            return buf.toString();
        }

        AllThereIsToKnowAboutYourCreature(AbstractAI ai, Creature creature,
            Legion legion)
        {
            this.creature = creature;
            MasterBoardTerrain terrain = legion.getCurrentHex().getTerrain();
            playerNumber = ai
                .countCreatureAccrossAllLegionFromPlayer(creature);
            int count = 0;
            for (Creature creature2 : legion.getCreatures())
            {
                if (creature.getType().equals(creature2.getType()))
                {
                    count++;
                }
            }
            stackNumber = count;
            recruits = RecruitingSubTree.getAllInAllSubtreesIgnoringSpecials(
                variant, creature.getType());
            CreatureType temp = null;
            for (CreatureType ct : recruits)
            {
                if (temp == null)
                {
                    temp = ct;
                }
                else
                {
                    if (temp.getPointValue() < ct.getPointValue())
                    {
                        temp = ct;
                    }
                }
            }
            bestRecruit = temp;
            int nnh = terrain.getRecruitingSubTree().maximumNumberNeededOf(
                creature.getType(), legion.getCurrentHex());
            if (nnh == -1)
            {
                numberNeededHere = Constants.BIGNUM;
            }
            else
            {
                numberNeededHere = nnh;
            }
            boolean hasBetter = false;
            for (CreatureType recruit : recruits)
            {
                if (recruit.getPointValue() > creature.getPointValue())
                {
                    for (Creature c : legion.getCreatures())
                    {
                        if (c.getType().equals(recruit))
                        {
                            hasBetter = true;
                        }
                    }
                }
            }
            thisStackHasBetter = hasBetter;

            if (!hasBetter && (numberNeededHere == stackNumber))
            {
                isImmediatelyUsefulKilling = true;
            }
            else
            {
                isImmediatelyUsefulKilling = false;
            }
            if (playerNumber == stackNumber)
            {
                onlyThisStackHasIt = true;
            }
            else
            {
                onlyThisStackHasIt = false;
            }
        }
    }
}
