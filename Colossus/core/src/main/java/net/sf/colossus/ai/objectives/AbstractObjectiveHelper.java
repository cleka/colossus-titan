package net.sf.colossus.ai.objectives;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sf.colossus.ai.AbstractAI;
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
 * @author dolbeau
 */
public abstract class AbstractObjectiveHelper implements IObjectiveHelper
{
    protected final ObjectiveEvalConstants oec;
    protected final Client client;
    protected final AbstractAI ai;
    protected final Variant variant;

    protected final Map<Creature, AllThereIsToKnowAboutYourCreature> attackerToKnowledge;
    protected final Map<Creature, AllThereIsToKnowAboutYourCreature> defenderToKnowledge;

    protected AbstractObjectiveHelper(Client client, AbstractAI ai,
        Variant variant)
    {

        this.client = client;
        this.ai = ai;
        this.variant = variant;
        oec = new ObjectiveEvalConstants();

        attackerToKnowledge = new HashMap<Creature, AllThereIsToKnowAboutYourCreature>();
        defenderToKnowledge = new HashMap<Creature, AllThereIsToKnowAboutYourCreature>();

        Legion attacker = client.getAttacker();
        for (Creature lcritter : attacker.getCreatures())
        {
            if (!lcritter.isTitan())
            {
                attackerToKnowledge.put(lcritter,
                    new AllThereIsToKnowAboutYourCreature(ai, lcritter,
                        attacker));
            }
        }

        Legion defender = client.getDefender();
        for (Creature lcritter : defender.getCreatures())
        {
            if (!lcritter.isTitan())
            {
                defenderToKnowledge.put(lcritter,
                    new AllThereIsToKnowAboutYourCreature(ai, lcritter,
                        defender));
            }
        }
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

    /**
     * Helper class holding some knowledge about a given creature.
     */
    protected class AllThereIsToKnowAboutYourCreature
    {
        /** The creature this knowledged is about */
        final Creature creature;
        /** How many creature of the same type the player owns (in all its Legion) */
        final int playerNumber;
        /** How many creature of the same type are in this legion (including this one) */
        final int stackNumber;
        /** What can this creature recruits */
        final Set<CreatureType> recruits;
        /** The best possible recruit (by points value) this creature could someday recruit (anywhere) */
        final CreatureType bestRecruit;
        /** How many we need in the current terrain to recruit (BIGNUM if we can't recruit) */
        final int numberNeededHere;
        /** Whether the current Legion already has something better in the recruit tree (of this terrain) */
        final boolean thisStackHasBetter;
        /** Whether it's immediately useful to kill, i.e. we already have just enough to
         * recruit and nothing better in this terrain.
         */
        final boolean isImmediatelyUsefulKilling;
        /** Whether this creature type appears in this stack, and in this stack only */
        final boolean onlyThisStackHasIt;
        /** How many are left in the Caretaker's stack */
        final int numberLeftToRecruit;
        /** Whether we can still recruit here or we are already out of luck (always true if we can't recruit here...) */
        final boolean enoughLeftToRecruitHere;
        /** Whether we can still recruit here  with no room to spare (always true if we can't recruit here...) */
        final boolean justEnoughLeftToRecruitHere;

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
            buf.append(" numberLeftToRecruit=" + numberLeftToRecruit);
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

            numberLeftToRecruit = ai.getCaretaker().getAvailableCount(
                creature.getType());

            if (numberNeededHere < Constants.BIGNUM)
            {
                enoughLeftToRecruitHere = (stackNumber + numberLeftToRecruit) >= numberNeededHere;
                justEnoughLeftToRecruitHere = (stackNumber + numberLeftToRecruit) == numberNeededHere;
            }
            else
            {
                enoughLeftToRecruitHere = true;
                justEnoughLeftToRecruitHere = true;
            }
        }
    }
}
