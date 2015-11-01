package net.sf.colossus.game;


import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.DevRandom;


/**
 * Class Dice handles nowadays only "general" die-rolling, usually using the
 * default "low-quality" Java PRNG or whatever is defined to use instead.
 *
 * Player specific rolls where we want "as good as possible" rolls moved to
 * PlayerSpecificDice.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */
public final class Dice
{
    private static final Logger LOGGER = Logger
        .getLogger(Dice.class.getName());

    private static Random random = new DevRandom();
    private static final int[] stats = new int[6];
    private static int rcount = 0;

    static void init(String source)
    {
        random = new DevRandom(source);
        for (int i = 0; i < 6; i++)
        {
            stats[i] = 0;
        }
    }

    /** Put all die rolling in one place, in case we decide to change random
     *  number algorithms, use an external dice server, etc. */
    public static int rollDie()
    {
        int roll = rollDie(6);
        synchronized (stats)
        {
            stats[roll - 1]++;
            rcount++;
            if ((rcount % 60) == 0)
            {
                LOGGER.log(Level.FINEST, "[rstats] Current D6 distribution ("
                    + rcount + " rolls, " + (rcount / 6) + " each):\n"
                    + "[rstats] \t1: " + stats[0] + "\n" + "[rstats] \t2: "
                    + stats[1] + "\n" + "[rstats] \t3: " + stats[2] + "\n"
                    + "[rstats] \t4: " + stats[3] + "\n" + "[rstats] \t5: "
                    + stats[4] + "\n" + "[rstats] \t6: " + stats[5]);
            }
        }
        return roll;
    }

    public static int rollDie(int size)
    {
        return random.nextInt(size) + 1;
    }

    private static int[] basicSequence = { 4, 3, 1, 6, 5, 2 };
    //private static int[] basicSequence = {1,2,3,4,5,6};
    private static int seqNum = -1;

    /* this one return from a fixed sequence, instead of a random value */
    public static int rollDieNonRandom()
    {
        seqNum = (seqNum + 1) % basicSequence.length;
        return (basicSequence[seqNum]);
    }
}
