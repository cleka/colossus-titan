package net.sf.colossus.ai;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.ai.helper.LegionMove;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;


/**
 * DON'T USE THAT ONE YET.
 * This one implements a parallel findBestLegionMove. Unfortunately, SimpleAI
 * implementation of evaluateLegionBattleMove is anything but thread-safe, and
 * it goes very deep:
 * Evaluations functions mostly fall-back to asking the Client object property
 * of the current situation. So they have first to move all BattleCritter
 * (which are only a view of full BattleUnit) to the evaluated position, then
 * evaluate.
 * This version only tests the parallelism, so evaluateLegionBattleMove is
 * random.
 * We would need to
 * (1) implements Strike as BattleClientSide, a proper, per-battle extension of
 * Battle (the way it is done on with BattleServerSide)
 * (2) make sure all evaluate functions only use property of BattleClientSide,
 * even if that means changing the state of BattleClientSide.
 * (3) have a proper deep copy constructor in BattleClientSide, so we can work
 * on several variant at once.
 * @author Romain Dolbeau
 */
public class ParallelEvaluatorAI extends ExperimentalAI // NO_UCD
{
    private static final Logger LOGGER = Logger
        .getLogger(ParallelEvaluatorAI.class.getName());

    public ParallelEvaluatorAI(Client client)
    {
        super(client);
    }

    @Override
    protected int evaluateLegionBattleMove(LegionMove lm)
    {
        return random.nextInt(1000);
    }

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
            if (timeLimit < Constants.MIN_AI_TIME_LIMIT
                || timeLimit > Constants.MAX_AI_TIME_LIMIT)
            {
                timeLimit = Constants.DEFAULT_AI_TIME_LIMIT;
            }
            timer
                .schedule(new ThreadedTriggerTimeIsUp(), MS_PER_S * timeLimit);
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
            LOGGER.finest("Running Thread number XXX");
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
                        done = true;
                    }
                }
                if (lm != null)
                {
                    // this can't possibly work, as evaluateLegionBattleMove is
                    // about as non-thread-safe as it is possible to be.
                    int score = evaluateLegionBattleMove(lm);
                    if (score > bestScore)
                    {
                        bestScore = score;
                        best = lm;
                        LOGGER.finest("INTERMEDIATE Best legion move: "
                            + lm.getStringWithEvaluation() + " (" + score
                            + ")");
                    }
                    else
                    {
                        LOGGER.finest("INTERMEDIATE      legion move: "
                            + lm.getStringWithEvaluation() + " (" + score
                            + ")");
                    }

                    count++;

                    if (timeIsUp)
                    {
                        if (count >= MIN_ITERATIONS)
                        {
                            LOGGER
                                .finest("findBestLegionMove() time up after "
                                    + count + " iterations");
                            break;
                        }
                        else
                        {
                            LOGGER
                                .finest("findBestLegionMove() time up after "
                                    + count
                                    + " iterations, but we keep searching until "
                                    + MIN_ITERATIONS);
                        }
                    }
                }
            }
            findBestLegionMoveTimer.cancel();
            LOGGER.finer("Best legion move of " + count + " checked (turn "
                + client.getBattleTurnNumber() + "): "
                + ((best == null) ? "none " : best.getStringWithEvaluation())
                + " (" + bestScore + ")");
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
        findBestLegionMoveThread[] threads = new findBestLegionMoveThread[NTHREADS];

        Iterator<LegionMove> iterator = legionMoves.iterator();

        for (int i = 0; i < NTHREADS; i++)
        {
            threads[i] = new findBestLegionMoveThread(iterator);
            LOGGER.finest("Starting Thread number " + i);
            threads[i].start();
        }
        for (int i = 0; i < NTHREADS; i++)
        {
            try
            {
                threads[i].join();
                bests[i] = threads[i].best;
            }
            catch (InterruptedException ex)
            {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        best = bests[0];

        for (int i = 1; i < NTHREADS; i++)
        {
            if (best == null)
            {
                best = bests[i];
            }
            else
            {
                if (bests[i] != null)
                {
                    if (best.getValue() < bests[i].getValue())
                    {
                        best = bests[i];
                    }
                }
            }
        }
        LOGGER.finer("// Best legion move (turn "
            + client.getBattleTurnNumber() + "): "
            + (best == null ? "none" : best.getStringWithEvaluation()) + " ("
            + (best == null ? "-" : ("" + best.getValue())) + ")");
        return best;
    }
}
