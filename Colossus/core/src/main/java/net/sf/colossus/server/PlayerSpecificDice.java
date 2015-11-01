package net.sf.colossus.server;


import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.RandomDotOrgFileReader;


/**
 * A dice specifically owned by an individual player. One
 * reason for that is that we want to use "high quality" rolls
 * from e.g. files from random.org for user-visible rolls,
 * but for "less important" things like random-generating of
 * moves or selecting arbitrarily from some choices the usual ones
 * from Java's PRNG are good enough.
 *
 * TODO: Eventually get rid of the "everything static" class "Dice" totally?
 */

public class PlayerSpecificDice
{
    private static final Logger LOGGER = Logger
        .getLogger(PlayerSpecificDice.class.getName());

    private final RandomDotOrgFileReader randomSource;

    private final int[] MovementStats = new int[6];
    private int mmCount = 0;

    private final int[] BattleStats = new int[6];
    private int bsCount = 0;

    public PlayerSpecificDice()
    {
        this.randomSource = new RandomDotOrgFileReader();
        for (int i = 0; i < 6; i++)
        {
            MovementStats[i] = 0;
            BattleStats[i] = 0;
        }
    }

    public int rollMovementDie()
    {
        // compared to "Dice", we omit the synchronized here: on server
        // side, there is always only one thread doing something.
        int roll = randomSource.nextRoll();
        MovementStats[roll - 1]++;
        mmCount++;
        return roll;
    }

    public int rollBattleDie()
    {
        int roll = randomSource.nextRoll();
        BattleStats[roll - 1]++;
        bsCount++;
        return roll;
    }

    public void printMovementRollStats()
    {
        printRollStats("Movement rolls", mmCount, MovementStats);
    }

    public void printBattleRollStats()
    {
        printRollStats("Battle rolls", bsCount, BattleStats);
    }

    private void printRollStats(String what, int rcount, int[] stats)
    {
        LOGGER.log(Level.FINEST, "[rstats] Current D6 distribution for "
            + what + " (" + rcount + " rolls, " + (rcount / 6) + " each):\n"
            + "[rstats] \t1: " + stats[0] + "\n" + "[rstats] \t2: " + stats[1]
            + "\n" + "[rstats] \t3: " + stats[2] + "\n" + "[rstats] \t4: "
            + stats[3] + "\n" + "[rstats] \t5: " + stats[4] + "\n"
            + "[rstats] \t6: " + stats[5]);
    }

}
