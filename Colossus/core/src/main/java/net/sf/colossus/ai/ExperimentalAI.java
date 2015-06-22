package net.sf.colossus.ai;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.ai.helper.LegionMove;
import net.sf.colossus.ai.helper.OnTheFlyLegionMove;
import net.sf.colossus.ai.objectives.IObjectiveHelper;
import net.sf.colossus.ai.objectives.SecondObjectiveHelper;
import net.sf.colossus.ai.objectives.TacticalObjective;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Legion;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ValueRecorder;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * Yet Another AI, to test some stuff.
 *
 * @author Romain Dolbeau
 */
public class ExperimentalAI extends SimpleAI // NO_UCD
{

    private static final Logger LOGGER = Logger.getLogger(ExperimentalAI.class
        .getName());
    private final static long MAX_EXHAUSTIVE_SEARCH_MOVES = 15000;

    public ExperimentalAI(Client client)
    {
        super(client);

        /* ExperimentalAI doesn't like to loose critter. */
        bec.OFFBOARD_DEATH_SCALE_FACTOR = -2000;
        /* ExperimentalAI doesn't like to get out unprotected */
        bec.DEFENDER_BY_EDGE_OR_BLOCKINGHAZARD_BONUS = 40;
        /* And it's a sadist, too. */
        bec.DEFENDER_BY_DAMAGINGHAZARD_BONUS = 60;

        variant = VariantSupport.getCurrentVariant();
    }

    @Override
    Collection<LegionMove> findLegionMoves(
        final List<List<CritterMove>> allCritterMoves)
    {
        long realcount = 1;
        for (List<CritterMove> lcm : allCritterMoves)
        {
            realcount *= lcm.size();
        }
        if (realcount < MAX_EXHAUSTIVE_SEARCH_MOVES)
        {
            LOGGER.finer("Less than " + MAX_EXHAUSTIVE_SEARCH_MOVES
                + ", using exhaustive search (" + realcount + ")");
            return generateLegionMoves(allCritterMoves, true);
        }
        LOGGER.finer("More than " + MAX_EXHAUSTIVE_SEARCH_MOVES
            + ", using on-the-fly search (" + realcount + ")");
        return new OnTheFlyLegionMove(allCritterMoves);
    }

    @Override
    public List<CritterMove> battleMove()
    {
        List<CritterMove> r = super.battleMove();
        /* force the GC, so we have a chance to call the finalize() from
         * the OnTheFlyLegionMove::Iterator used in findBattleMoves.
         */
        Runtime.getRuntime().gc();
        return r;
    }

    /** this computes the special case of the Titan critter */
    @Override
    protected void evaluateCritterMove_Titan(final BattleCritter critter,
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final Legion legion, final int turn)
    {
        if (hex.isEntrance())
        {
            return;
        }
        // Reward titans sticking to the edges of the back row
        // surrounded by allies.  We need to relax this in the
        // last few turns of the battle, so that attacking titans
        // don't just sit back and wait for a time loss.
        if (!critter.isTitan())
        {
            LOGGER
                .warning("evaluateCritterMove_Titan called on non-Titan critter");
            return;
        }
        if (terrain.isTower() && legion.equals(client.getDefender()))
        {
            // Stick to the center of the tower.
            value.add(bec.TITAN_TOWER_HEIGHT_BONUS * hex.getElevation(),
                "TitanTowerHeightBonus");
        }
        else
        {
            if (legion.equals(client.getDefender()))
            {
                // defending titan is a coward.
                value.add(bec.TITAN_FORWARD_EARLY_PENALTY * 6
                    - rangeToClosestOpponent(hex),
                    "Defending TitanForwardEarlyPenalty");
                for (int i = 0; i < 6; i++)
                {
                    BattleHex neighbor = hex.getNeighbor(i);
                    if (neighbor == null /* Edge of the map */
                        || neighbor.getTerrain().blocksGround()
                        || (neighbor.getTerrain().isGroundNativeOnly() && !hasOpponentNativeCreature(neighbor
                            .getTerrain())))
                    {
                        value.add(bec.TITAN_BY_EDGE_OR_BLOCKINGHAZARD_BONUS,
                            "Defending TitanByEdgeOrBlockingHazard (" + i
                                + ")");
                    }
                }
            }
            else
            {
                // attacking titan should progressively involve itself
                value.add(
                    Math.round((((float)4. - turn) / (float)3.)
                        * bec.TITAN_FORWARD_EARLY_PENALTY
                        * ((float)6. - rangeToClosestOpponent(hex))),
                    "Progressive TitanForwardEarlyPenalty");
                for (int i = 0; i < 6; i++)
                {
                    BattleHex neighbor = hex.getNeighbor(i);
                    if (neighbor == null /* Edge of the map */
                        || neighbor.getTerrain().blocksGround()
                        || (neighbor.getTerrain().isGroundNativeOnly() && !hasOpponentNativeCreature(neighbor
                            .getTerrain())))
                    {
                        // being close to the edge is not a disavantage, even late
                        // in the battle, so min is 0 point.
                        value
                            .add(
                                Math.round((Math.max(((float)4. - turn)
                                    / (float)3., (float)0.) * bec.TITAN_BY_EDGE_OR_BLOCKINGHAZARD_BONUS)),
                                "Progressive TitanByEdgeOrBlockingHazard ("
                                    + i + ")");
                    }
                }
            }
        }
    }

    /** this compute for non-titan defending critter */
    @Override
    protected void evaluateCritterMove_Defender(final BattleCritter critter,
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final LegionClientSide legion, final int turn)
    {
        if (hex.isEntrance())
        {
            return;
        }
        // Encourage defending critters to hang back.
        BattleHex entrance = terrain.getEntrance(legion.getEntrySide());
        if (terrain.isTower())
        {
            // Stick to the center of the tower.
            value.add(bec.DEFENDER_TOWER_HEIGHT_BONUS * hex.getElevation(),
                "DefenderTowerHeightBonus");
        }
        else
        {
            int range = Battle.getRange(hex, entrance, true);

            // To ensure that defending legions completely enter
            // the board, prefer the second row to the first.  The
            // exception is small legions early in the battle,
            // when trying to survive long enough to recruit.
            int preferredRange = 3;
            if (legion.getHeight() <= 3 && turn < 4)
            {
                preferredRange = 2;
            }
            if (range != preferredRange)
            {
                value.add(
                    bec.DEFENDER_FORWARD_EARLY_PENALTY
                        * Math.abs(range - preferredRange),
                    "DefenderForwardEarlyPenalty");
            }
            for (int i = 0; i < 6; i++)
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor == null /* Edge of the map */
                    || neighbor.getTerrain().blocksGround()
                    || (neighbor.getTerrain().isGroundNativeOnly() && !hasOpponentNativeCreature(neighbor
                        .getTerrain())))
                {
                    value.add(bec.DEFENDER_BY_EDGE_OR_BLOCKINGHAZARD_BONUS,
                        "DefenderByEdgeOrBlockingHazard (" + i + ")");
                }
                else if (neighbor.getTerrain().isDamagingToNonNative()
                    && !hasOpponentNativeCreature(neighbor.getTerrain()))
                {
                    value.add(bec.DEFENDER_BY_DAMAGINGHAZARD_BONUS,
                        "DefenderByDamagingHazard (" + i + ")");
                }
            }
        }
    }

    /**
     * "Does nothing" override of evaluateCritterMove_Strike in @SimpleAI.
     * The job of that one is handled (supposedly better... I wish) by
     * the objectives code.
     */
    @Override
    protected void evaluateCritterMove_Strike(final BattleCritter critter,
        final Map<BattleHex, Integer> strikeMap, ValueRecorder value,
        final MasterBoardTerrain terrain, final BattleHex hex,
        final int power, final int skill, final LegionClientSide legion,
        final int turn, final Set<BattleHex> targetHexes)
    {
        return;
    }

    /**
     * "Does nothing" override of evaluateCritterMove_Rangestrike in @SimpleAI.
     * The job of that one is handled (supposedly better... I wish) by
     * the objectives code.
     */
    @Override
    protected void evaluateCritterMove_Rangestrike(
        final BattleCritter critter, final Map<BattleHex, Integer> strikeMap,
        ValueRecorder value, final MasterBoardTerrain terrain,
        final BattleHex hex, final int power, final int skill,
        final LegionClientSide legion, final int turn,
        final Set<BattleHex> targetHexes)
    {
        return;
    }

    @Override
    protected int evaluateLegionBattleMoveAsAWhole(LegionMove lm,
        Map<BattleHex, Integer> strikeMap, ValueRecorder value)
    {
        final Legion legion = client.getMyEngagedLegion();
        if (legion.equals(client.getAttacker()))
        {
            // TODO, something
        }
        else
        {
            boolean nobodyGetsHurt = true;
            int numCanBeReached = 0;
            int maxThatCanReach = 0;
            for (BattleCritter critter : client.getActiveBattleUnits())
            {
                int canReachMe = 0;
                BattleHex myHex = critter.getCurrentHex();
                for (BattleCritter foe : client.getInactiveBattleUnits())
                {
                    BattleHex foeHex = foe.getCurrentHex();
                    int range = Battle.getRange(foeHex, myHex, true);
                    if ((range != Constants.OUT_OF_RANGE)
                        && ((range - 2) <= foe.getSkill()))
                    {
                        canReachMe++;
                    }
                }
                if (canReachMe > 0)
                {
                    nobodyGetsHurt = false;
                    numCanBeReached++;
                    if (maxThatCanReach < canReachMe)
                    {
                        maxThatCanReach = canReachMe;
                    }
                }
            }
            if (numCanBeReached == 1) // TODO: Rangestriker
            {
                value.add(bec.DEF__AT_MOST_ONE_IS_REACHABLE,
                    "Def_AtMostOneIsReachable");
            }
            if (maxThatCanReach == 1) // TODO: Rangestriker
            {
                value.add(bec.DEF__NOONE_IS_GANGBANGED,
                    "Def_NoOneIsGangbanged");
            }
            if (nobodyGetsHurt) // TODO: Rangestriker
            {
                value.add(bec.DEF__NOBODY_GETS_HURT, "Def_NobodyGetsHurt");
            }
        }
        if (listObjectives != null)
        {
            for (TacticalObjective to : listObjectives)
            {
                ValueRecorder temp = to.situationContributeToTheObjective();
                temp.setScale(to.getPriority());
                value.add(temp);
            }
        }
        return value.getValue();
    }

    private List<TacticalObjective> listObjectives = null;

    @Override
    public void initBattle()
    {
        super.initBattle();
        if (client.getMyEngagedLegion() != null)
        {
            /* TODO: Objectives are never uupdated during Battle, which is
             * inherently broken. For instance, both defender recruit at turn 4
             * and attacker's summoned angel won't have specific objective,
             * when it's likely they should attack.
             */
            IObjectiveHelper helper = new SecondObjectiveHelper(client, this,
                variant);
            if (client.getMyEngagedLegion().equals(client.getDefender()))
            {
                listObjectives = helper.defenderObjective();
            }
            else
            {
                listObjectives = helper.attackerObjective();
            }
        }
    }

    @Override
    public void cleanupBattle()
    {
        super.cleanupBattle();
        if (listObjectives != null)
        {
            for (TacticalObjective to : listObjectives)
            {
                LOGGER.info("Objective:" + to.getDescription() + " -> "
                    + to.objectiveAttained());
            }
            listObjectives = null;
        }
    }

}
