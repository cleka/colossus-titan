package net.sf.colossus.ai;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * Yet Another AI, to test some stuff.
 */
public class ExperimentalAI extends SimpleAI // NO_UCD
{

    private static final Logger LOGGER = Logger.getLogger(ExperimentalAI.class
        .getName());
    private final static long MAX_EXHAUSTIVE_SEARCH_MOVES = 15000;

    private final DefenderFirstMoveRecordSQL dfmr;

    public ExperimentalAI(Client client)
    {
        super(client);

        /* ExperimentalAI doesn't like to loose critter. */
        bec.OFFBOARD_DEATH_SCALE_FACTOR = -2000;
        /* ExperimentalAI doesn't like to get out unprotected */
        bec.DEFENDER_BY_EDGE_OR_BLOCKINGHAZARD_BONUS = 40;
        /* And it's a sadist, too. */
        bec.DEFENDER_BY_DAMAGINGHAZARD_BONUS = 60;

        dfmr = new DefenderFirstMoveRecordSQL("localhost", "colossus",
                "colossus", VariantSupport.getCurrentVariant().getName());
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
        if (client.getBattleTurnNumber() == 1)
        {
            if (dfmr.isUsable())
            {
                if (client.getMyEngagedLegion().equals(client.getDefender()))
                {
                    List<CritterMove> amoves = new ArrayList<CritterMove>();
                    for (BattleCritter critter : client.getActiveBattleUnits())
                    {
                        CritterMove cm = new CritterMove(critter, critter.
                                getStartingHex(), critter.getCurrentHex());
                        amoves.add(cm);
                    }
                    List<BattleCritter> attackers =
                            new ArrayList<BattleCritter>();
                    attackers.addAll(client.getInactiveBattleUnits());
                    Set<List<CritterMove>> unusedYet = dfmr.getPerfectMatches(
                            amoves, attackers, client.getBattleSite().
                            getTerrain(), client.getDefender().getEntrySide());
                    Set<List<CritterMove>> unusedYet2 = dfmr.getImperfectMatches(
                            amoves, attackers, client.getBattleSite().
                            getTerrain(), client.getDefender().getEntrySide());
                    LOGGER.warning("FOUND " + unusedYet.size() + " perfect matches and " + unusedYet2.size() + " imperfect matches.");
                }
                else
                {
                    if (true || isHumanLegion(client.getDefender()))
                    {
                        List<CritterMove> dmoves = new ArrayList<CritterMove>();
                        for (BattleCritter critter : client.
                                getInactiveBattleUnits())
                        {
                            CritterMove cm = new CritterMove(critter, critter.
                                    getStartingHex(), critter.getCurrentHex());
                            dmoves.add(cm);
                        }
                        List<BattleCritter> attackers =
                                new ArrayList<BattleCritter>();
                        attackers.addAll(client.getActiveBattleUnits());
                        dfmr.insertMove(dmoves, attackers, client.getBattleSite().
                                getTerrain(),
                                client.getDefender().getEntrySide());
                    }
                }
            }
        }


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
            LOGGER.warning(
                    "evaluateCritterMove_Titan called on non-Titan critter");
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
                value.add(bec.TITAN_FORWARD_EARLY_PENALTY *
                        6 - rangeToClosestOpponent(hex),
                        "Defending TitanForwardEarlyPenalty");
                for (int i = 0; i < 6; i++)
                {
                    BattleHex neighbor = hex.getNeighbor(i);
                    if (neighbor == null /* Edge of the map */ || neighbor.
                            getTerrain().blocksGround() || (neighbor.getTerrain().
                            isGroundNativeOnly() && !hasOpponentNativeCreature(
                            neighbor.getTerrain())))
                    {
                        value.add(
                                bec.TITAN_BY_EDGE_OR_BLOCKINGHAZARD_BONUS,
                                "Defending TitanByEdgeOrBlockingHazard (" + i +
                                ")");
                    }
                }
            }
            else
            {
                // attacking titan should progressively involve itself
                value.add(Math.round((((float) 4. - turn) /
                        (float) 3.) *
                        bec.TITAN_FORWARD_EARLY_PENALTY *
                        ((float) 6. - rangeToClosestOpponent(hex))),
                        "Progressive TitanForwardEarlyPenalty");
                for (int i = 0; i < 6; i++)
                {
                    BattleHex neighbor = hex.getNeighbor(i);
                    if (neighbor == null /* Edge of the map */ || neighbor.
                            getTerrain().blocksGround() || (neighbor.getTerrain().
                            isGroundNativeOnly() && !hasOpponentNativeCreature(
                            neighbor.getTerrain())))
                    {
                        // being close to the edge is not a disavantage, even late
                        // in the battle, so min is 0 point.
                        value.add(
                                Math.round(
                                (Math.max(((float) 4. - turn) /
                                (float) 3., (float) 0.) *
                                bec.TITAN_BY_EDGE_OR_BLOCKINGHAZARD_BONUS)),
                                "Progressive TitanByEdgeOrBlockingHazard (" + i +
                                ")");
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
        BattleHex entrance = HexMap.getEntrance(terrain, legion
            .getEntrySide());
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
                value.add(bec.DEFENDER_FORWARD_EARLY_PENALTY * Math.abs(range -
                        preferredRange),
                        "DefenderForwardEarlyPenalty");
            }
            for (int i = 0; i < 6; i++)
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor == null /* Edge of the map */ || neighbor.
                        getTerrain().blocksGround() || (neighbor.getTerrain().
                        isGroundNativeOnly() && !hasOpponentNativeCreature(
                        neighbor.getTerrain())))
                {
                    value.add(bec.DEFENDER_BY_EDGE_OR_BLOCKINGHAZARD_BONUS,
                            "DefenderByEdgeOrBlockingHazard (" + i + ")");
                }
                else if (neighbor.getTerrain().isDamagingToNonNative() &&
                        !hasOpponentNativeCreature(neighbor.getTerrain()))
                {
                    value.add(bec.DEFENDER_BY_DAMAGINGHAZARD_BONUS,
                            "DefenderByDamagingHazard (" + i + ")");
                }
            }
        }
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
                value.add(bec.DEF__AT_MOST_ONE_IS_REACHABLE, "Def_AtMostOneIsReachable");
            }
            if (maxThatCanReach == 1) // TODO: Rangestriker
            {
                value.add(bec.DEF__NOONE_IS_GANGBANGED, "Def_NoOneIsGangbanged");
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
                int temp = to.situationContributeToTheObjective();
                value.add(temp, "Objective: " + to.getDescription());
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
            if (client.getMyEngagedLegion().equals(client.getDefender()))
            {
                defenderObjective();
            }
            else
            {
                attackerObjective();
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
                LOGGER.info("Objective:" + to.getDescription() + " -> " + to.
                        objectiveAttained());
            }
            listObjectives = null;
        }
    }

    /** really stupid heuristic */
    private Creature findCreatureToDestroyInAttacker()
    {
        Creature creature = null;
        int mcount = 0;

        for (Creature lcritter : client.getAttacker().getCreatures())
        {
            int count = countCreatureAccrossAllLegionFromPlayer(lcritter);
            LOGGER.finest("Found " + count + " " + lcritter.getName() + " for the attacker");
            if (!lcritter.isLord() && !lcritter.isDemiLord() &&
                    ((creature == null) || (count < mcount)))
            {
                creature = lcritter;
                mcount = count;
            }
        }

        return creature;
    }

    /** Currently attackerObjective is very dumb:
     * try and kill the Titan (if there) and the biggest creature
     */
    private void attackerObjective()
    {
        listObjectives = new ArrayList<TacticalObjective>();
        Creature toKill = null;
        for (Creature lcritter : client.getDefender().getCreatures())
        {
            if (lcritter.isTitan())
            {
                listObjectives.add(new DestroyCreatureTacticalObjective(10,
                        client,
                        client.getDefender(),
                        lcritter));
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
        if (toKill != null)
        {
            listObjectives.add(new DestroyCreatureTacticalObjective(1,
                    client,
                    client.getDefender(),
                    toKill));
        }
        for (TacticalObjective to : listObjectives)
        {
            LOGGER.info("Attacker Objective: " + to.getDescription());
        }
    }

    private void defenderObjective()
    {
        listObjectives = new ArrayList<TacticalObjective>();
        for (Creature lcritter : client.getAttacker().getCreatures())
        {
            if (lcritter.isTitan())
            {
                listObjectives.add(new DestroyCreatureTacticalObjective(10,
                        client,
                        client.getAttacker(),
                        lcritter));
            }
        }
        Creature toKill = findCreatureToDestroyInAttacker();
        if (toKill != null)
        {
            listObjectives.add(new DestroyCreatureTacticalObjective(1,
                    client,
                    client.getAttacker(),
                    toKill));
        }
        for (TacticalObjective to : listObjectives)
        {
            LOGGER.info("Defender Objective: " + to.getDescription());
        }
    }
}
