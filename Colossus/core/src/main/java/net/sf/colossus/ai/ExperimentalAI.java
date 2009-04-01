package net.sf.colossus.ai;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.Legion;
import net.sf.colossus.gui.BattleChit;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * Yet Another AI, to test some stuff.
 * @version $Id$
 * @author Romain Dolbeau
 */
public class ExperimentalAI extends SimpleAI // NO_UCD
{

    private static final Logger LOGGER = Logger.getLogger(SimpleAI.class
        .getName());
    private final static long MAX_EXHAUSTIVE_SEARCH_MOVES = 15000;

    public ExperimentalAI(Client client)
    {
        super(client);
    }
    /*
    private class findBestLegionMoveThread extends Thread
    {

        private boolean timeIsUp = false;
        LegionMove best = null;

        Timer threadedSetupTimer()
        {
            // java.util.Timer, not Swing Timer
            Timer timer = new Timer();
            this.timeIsUp = false;
            final int MS_PER_S = 1000;
            if (timeLimit < Constants.MIN_AI_TIME_LIMIT || timeLimit >
                    Constants.MAX_AI_TIME_LIMIT)
            {
                timeLimit = Constants.DEFAULT_AI_TIME_LIMIT;
            }
            timer.schedule(new ThreadedTriggerTimeIsUp(), MS_PER_S * timeLimit);
            return timer;
        }

        protected class ThreadedTriggerTimeIsUp extends TimerTask
        {
            @Override
            public void run()
            {
                timeIsUp = true;
            }
        }


        private final Iterator<LegionMove> iterator;

        findBestLegionMoveThread(Iterator<LegionMove> it)
        {
            iterator = it;
        }

        @Override
        public void run()
        {
            int bestScore = Integer.MIN_VALUE;
            int count = 0;

            Timer findBestLegionMoveTimer = threadedSetupTimer();
            boolean done = false;
            while (!done)
            {
                LegionMove lm = null;
                synchronized (iterator)
                {
                    if (iterator.hasNext())
                    {
                        lm = iterator.next();
                    }
                    else
                    {
                        lm = null;
                        done = true;
                    }
                }
                if (!done)
                {
                    // this can't possibly work, as evaluateLegionBattleMove is
                    // about as non-thread-safe as it is possible to be.
                    int score = evaluateLegionBattleMove(lm);
                    if (score > bestScore)
                    {
                        bestScore = score;
                        best = lm;
                        LOGGER.finest("INTERMEDIATE Best legion move: " + lm.
                                getStringWithEvaluation() + " (" + score + ")");
                    }
                    else
                    {
                        LOGGER.finest("INTERMEDIATE      legion move: " + lm.
                                getStringWithEvaluation() + " (" + score + ")");
                    }

                    count++;

                    if (timeIsUp)
                    {
                        if (count >= MIN_ITERATIONS)
                        {
                            LOGGER.finest("findBestLegionMove() time up after " +
                                    count + " iterations");
                            break;
                        }
                        else
                        {
                            LOGGER.finest("findBestLegionMove() time up after " +
                                    count +
                                    " iterations, but we keep searching until " +
                                    MIN_ITERATIONS);
                        }
                    }
                }
            }
            findBestLegionMoveTimer.cancel();
            LOGGER.finer("Best legion move of " + count + " checked (turn " +
                    client.getBattleTurnNumber() + "): " + ((best == null) ? "none "
                    : best.getStringWithEvaluation()) + " (" + bestScore + ")");
        }
    }

    private final static int NTHREADS = 2;

    @Override
    protected LegionMove findBestLegionMove(Collection<LegionMove> legionMoves)
    {
        LegionMove best = null;

        if (legionMoves instanceof List)
            Collections.shuffle((List<LegionMove>)legionMoves, random);

        LegionMove[] bests = new LegionMove[NTHREADS];
        Thread[] threads = new Thread[NTHREADS];

        Iterator<LegionMove> iterator = legionMoves.iterator();

        for (int i = 0 ; i < NTHREADS ; i++) {
            threads[i] = new findBestLegionMoveThread(iterator);
            threads[i].start();
        }
        for (int i = 0 ; i < NTHREADS ; i++) {
            try
            {
                threads[i].join();
            } catch (InterruptedException ex)
            {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        best = bests[0];
        for (int i = 1 ; i < NTHREADS ; i++) {
            if (best.getValue() < bests[i].getValue()) {
                best = bests[i];
            }
        }
        LOGGER.finer("// Best legion move (turn "
            + client.getBattleTurnNumber() + "): "
            + ((best == null) ? "none " : best.getStringWithEvaluation())
            + " (" + best.getValue() + ")");
        return best;
    }
    */

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
    public List<CritterMove> battleMove() {
        List<CritterMove> r = super.battleMove();
        /* force the GC, so we have a chance to call the finalize() from
         * the OnTheFlyLegionMove::Iterator used in findBattleMoves.
         */
        Runtime.getRuntime().gc();
        return r;
    }


    /** this computes the special case of the Titan critter */
    @Override
    protected void evaluateCritterMove_Titan(final BattleChit critter,
            ValueRecorder value, final MasterBoardTerrain terrain,
            final BattleHex hex, final Legion legion, final int turn)
    {
        // Reward titans sticking to the edges of the back row
        // surrounded by allies.  We need to relax this in the
        // last few turns of the battle, so that attacking titans
        // don't just sit back and wait for a time loss.
        // entrance is never read right now, commented out to get rid
        // of Eclipse warnings. -Clemens
        // BattleHex entrance = BattleMap.getEntrance(terrain,
        //         legion.getEntrySide());
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

    @Override
    protected int evaluateLegionBattleMoveAsAWhole(LegionMove lm,
        Map<BattleHex, Integer> strikeMap, StringBuffer why)
    {
        int value = 0;
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
            for (BattleChit critter : client.getActiveBattleChits())
            {
                int canReachMe = 0;
                BattleHex myHex = critter.getCurrentHex();
                List<BattleChit> foes = client.getInactiveBattleChits();
                for (BattleChit foe : foes)
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
                value += bec.DEF__AT_MOST_ONE_IS_REACHABLE;
                why.append("+");
                why.append(bec.DEF__AT_MOST_ONE_IS_REACHABLE);
                why.append(" [Def_AtMostOneIsReachable]");
            }
            if (maxThatCanReach == 1) // TODO: Rangestriker
            {
                value += bec.DEF__NOONE_IS_GANGBANGED;
                why.append("+");
                why.append(bec.DEF__NOONE_IS_GANGBANGED);
                why.append(" [Def_NoOneIsGangbanged]");
            }
            if (nobodyGetsHurt) // TODO: Rangestriker
            {
                value += bec.DEF__NOBODY_GETS_HURT;
                why.append("+");
                why.append(bec.DEF__NOBODY_GETS_HURT);
                why.append(" [Def_NobodyGetsHurt]");
            }
        }
        return value;
    }
}
