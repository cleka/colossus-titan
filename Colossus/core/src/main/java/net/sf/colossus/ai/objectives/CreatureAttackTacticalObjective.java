/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.colossus.ai.objectives;


import java.util.Map;
import java.util.Set;

import net.sf.colossus.ai.AbstractAI;
import net.sf.colossus.ai.helper.BattleEvalConstants;
import net.sf.colossus.client.Client;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.Probs;
import net.sf.colossus.util.ValueRecorder;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;


/** The objective of sending all of a CreatureType into battle, presumably
 * because we don't really need them for anything else.
 *
 * @author Romain Dolbeau
 */
class CreatureAttackTacticalObjective extends AbstractTacticalObjective
{
    private final Creature creature;
    private final Legion liveLegion;
    private final Client client;
    private final AbstractAI ai;
    private final BattleEvalConstants bec;

    CreatureAttackTacticalObjective(float priority, Client client,
        Legion liveLegion, Creature creature, AbstractAI ai,
        BattleEvalConstants bec)
    {
        super(priority);
        this.creature = creature;
        this.liveLegion = liveLegion;
        this.client = client;
        this.ai = ai;
        this.bec = bec;
    }

    public boolean objectiveAttained()
    {
        return getCount() == 0;
    }

    public int getCount()
    {
        return liveLegion.numCreature(creature.getType());
    }

    /** This is mostly a copy/paste from the EvaluateCritterMove_Strike
     * and EvaluateCritterMove_Rangestrike  functions in SimpleAI. This is
     * known. The goal is indeed to replace the big hardwired functions in
     * SimpleAI by a bunch of objectives, so we can tweak what critter does
     * what in an easier way.
     */
    public ValueRecorder situationContributeToTheObjective()
    {
        ValueRecorder value = new ValueRecorder(getDescription());
        final MasterBoardTerrain terrain = client.getBattleSite().getTerrain();
        final int turn = client.getBattleTurnNumber();
        int which = 0;
        Map<BattleHex, Integer> strikeMap = ai.findStrikeMap();
        for (BattleCritter critter : client.getActiveBattleUnits())
        {
            if (critter.getCurrentHex().isEntrance())
            {
                continue;
            }
            if (critter.getType().equals(creature.getType()))
            {
                final int skill = critter.getSkill();
                final int power = critter.getPower();
                Set<BattleHex> targetHexes = client.findStrikes(critter
                    .getTag());
                String desc = creature.getName() + " #" + which;
                which++;
                int numTargets = targetHexes.size();
                if (targetHexes.size() == 0)
                {
                    continue;
                }
                if (client.isInContact(critter, true))
                {
                    value.add(bec.ATTACKER_ADJACENT_TO_ENEMY, desc
                        + ": AttackerAdjacentToEnemy");

                    int killValue = 0;
                    int numKillableTargets = 0;
                    int hitsExpected = 0;

                    for (BattleHex targetHex : targetHexes)
                    {
                        BattleCritter target = client.getBattleCS()
                            .getBattleUnit(targetHex);

                        // Reward being next to enemy titans.  (Banzai!)
                        if (target.isTitan())
                        {
                            value.add(bec.ADJACENT_TO_ENEMY_TITAN, desc
                                + ": AdjacentToEnemyTitan");
                        }

                        // Reward being next to a rangestriker, so it can't hang
                        // back and plink us.
                        if (target.isRangestriker()
                            && !critter.isRangestriker())
                        {
                            value.add(bec.ADJACENT_TO_RANGESTRIKER, desc
                                + ": AdjacenttoRangestriker");
                        }

                        // Attack Warlocks so they don't get Titan
                        if (target.getType().useMagicMissile())
                        {
                            value.add(bec.ADJACENT_TO_BUDDY_TITAN, desc
                                + ": AdjacentToBuddyTitan");
                        }

                        // Reward being next to an enemy that we can probably
                        // kill this turn.
                        int dice = ai.getBattleStrike().getDice(critter,
                            target);
                        int strikeNum = ai.getBattleStrike().getStrikeNumber(
                            critter, target);
                        double meanHits = Probs.meanHits(dice, strikeNum);
                        if (meanHits + target.getHits() >= target.getPower())
                        {
                            numKillableTargets++;
                            int targetValue = ai.getKillValue(target, terrain);
                            killValue = Math.max(targetValue, killValue);
                        }
                        else
                        {
                            // reward doing damage to target - esp. titan.
                            int targetValue = ai.getKillValue(target, terrain);
                            killValue = (int)(0.5 * (meanHits / target
                                .getPower()) * Math
                                .max(targetValue, killValue));
                        }

                        // Reward ganging up on enemies.
                        if (strikeMap != null)
                        {
                            int numAttackingThisTarget = strikeMap.get(
                                targetHex).intValue();
                            if (numAttackingThisTarget > 1)
                            {
                                value.add(bec.GANG_UP_ON_CREATURE, desc
                                    + ": GangUpOnCreature Strike");
                            }
                        }

                        // Penalize damage that we can take this turn,
                        {
                            dice = ai.getBattleStrike().getDice(target,
                                critter);
                            strikeNum = ai.getBattleStrike().getStrikeNumber(
                                target, critter);
                            hitsExpected += Probs.meanHits(dice, strikeNum);
                        }
                    }
                    if (liveLegion.equals(client.getAttacker()))
                    {
                        value.add(bec.ATTACKER_KILL_SCALE_FACTOR * killValue,
                            desc + ": AttackerKillValueScaled");
                        value.add(bec.KILLABLE_TARGETS_SCALE_FACTOR
                            * numKillableTargets, desc
                            + ": AttackerNumKillable");
                    }
                    else
                    {
                        value.add(bec.DEFENDER_KILL_SCALE_FACTOR * killValue,
                            desc + ": DefenderKillValueScaled");
                        value.add(bec.KILLABLE_TARGETS_SCALE_FACTOR
                            * numKillableTargets, desc
                            + ": DefenderNumKillable");
                    }

                    int hits = critter.getHits();

                    // XXX Attacking legions late in battle ignore damage.
                    // the isTitan() here should be moved to _Titan function above ?
                    if (liveLegion.equals(client.getDefender())
                        || critter.isTitan() || turn <= 4)
                    {
                        if (hitsExpected + hits >= power)
                        {
                            if (liveLegion.equals(client.getAttacker()))
                            {
                                value.add(bec.ATTACKER_GET_KILLED_SCALE_FACTOR
                                    * ai.getKillValue(critter, terrain), desc
                                    + ": AttackerGetKilled");
                            }
                            else
                            {
                                value.add(bec.DEFENDER_GET_KILLED_SCALE_FACTOR
                                    * ai.getKillValue(critter, terrain), desc
                                    + ": DefenderGetKilled");
                            }
                        }
                        else
                        {
                            if (liveLegion.equals(client.getAttacker()))
                            {
                                value.add(bec.ATTACKER_GET_HIT_SCALE_FACTOR
                                    * ai.getKillValue(critter, terrain), desc
                                    + ": AttackerGetHit");
                            }
                            else
                            {
                                value.add(bec.DEFENDER_GET_HIT_SCALE_FACTOR
                                    * ai.getKillValue(critter, terrain), desc
                                    + ": DefendergetHit");
                            }
                        }
                    }
                }
                else
                {
                    // Rangestrikes.
                    value.add(bec.FIRST_RANGESTRIKE_TARGET, desc
                        + ": FirstRangestrikeTarget");

                    // Having multiple targets is good, in case someone else
                    // kills one.
                    if (numTargets >= 2)
                    {
                        value.add(bec.EXTRA_RANGESTRIKE_TARGET, desc
                            + ": ExtraRangestrikeTarget");
                    }

                    // Non-warlock skill 4 rangestrikers should slightly prefer
                    // range 3 to range 4.  Non-brush rangestrikers should
                    // prefer strikes not through bramble.  Warlocks should
                    // try to rangestrike titans.
                    boolean penalty = true;
                    for (BattleHex targetHex : targetHexes)
                    {
                        BattleCritter target = client.getBattleCS()
                            .getBattleUnit(targetHex);
                        if (target.isTitan())
                        {
                            value.add(bec.RANGESTRIKE_TITAN, desc
                                + ": RangestrikeTitan");
                        }
                        int strikeNum = ai.getBattleStrike().getStrikeNumber(
                            critter, target);
                        if (strikeNum <= 4 - skill + target.getSkill())
                        {
                            penalty = false;
                        }

                        // Reward ganging up on enemies.
                        if (strikeMap != null)
                        {
                            int numAttackingThisTarget = strikeMap.get(
                                targetHex).intValue();
                            if (numAttackingThisTarget > 1)
                            {
                                value.add(bec.GANG_UP_ON_CREATURE, desc
                                    + ": GangUpOnCreature RangeStrike");
                            }
                        }
                    }
                    if (!penalty)
                    {
                        value.add(bec.RANGESTRIKE_WITHOUT_PENALTY, desc
                            + ": RangestrikeWithoutPenalty");
                    }
                }
            }
        }
        return value;
    }

    public String getDescription()
    {
        return "Using " + creature.getName() + " to attack (" + getPriority()
            + ")";
    }
}
