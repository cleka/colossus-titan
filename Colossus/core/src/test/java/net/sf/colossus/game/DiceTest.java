package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Dice;

import junit.framework.TestCase;


/**
 * JUnit test for dice.
 *
 * @author David Ripton
 */
public class DiceTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(DiceTest.class
        .getName());

    private final int trials = 5000;
    double epsilon = 0.000001;

    public DiceTest(String name)
    {
        super(name);
    }

    /* Commented out because it's slow.
     public void testDevRandom()
     {
     System.out.println("---testDevRandom()---");
     int [] rolls = new int[trials];
     Dice.init("/dev/random");
     for (int i = 0; i < trials; i++)
     {
     rolls[i] = Dice.rollDie();
     }
     runAllTests(rolls, true);
     }
     */

    // Comented out because not everyone has a /tmp/random :-)
    /*
     public void testTmpRandom()
     {
     System.out.println("---testTmpRandom()---");
     int [] rolls = new int[trials];
     Dice.init("/tmp/random");
     for (int i = 0; i < trials; i++)
     {
     rolls[i] = Dice.rollDie();
     }
     run4Tests(rolls, true);
     }
     */

    public void testDevUrandom()
    {
        LOGGER.log(Level.FINEST, "---testDevUrandom()---");
        int[] rolls = new int[trials];
        Dice.init("/dev/urandom");
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDie();
        }
        runAllTests(rolls, true);
    }

    public void testPRNG()
    {
        LOGGER.log(Level.FINEST, "---testPRNG()---");
        int[] rolls = new int[trials];
        Dice.init("PRNG");
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDie();
        }
        runAllTests(rolls, true);
    }

    public void testNonRandomDice()
    {
        LOGGER.log(Level.FINEST,
            "---testNonRandomDice()--- [some should fail]");
        int[] rolls = new int[trials];
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDieNonRandom();
        }
        runAllTests(rolls, false);
    }

    void runAllTests(int[] rolls, boolean random)
    {
        chiSquareTest(rolls, random);
        mTest(rolls, random);
        signTest(rolls, random);
        mannKendallTest(rolls, random);
        runsTest(rolls, random);
    }

    // Most tests at http://www.stat.unc.edu/faculty/rs/s102/lec17a.pdf
    // Runs test http://www2.sunysuffolk.edu/wrightj/MA24/Misc/RunsTest.pdf

    // XXX Need to use chi-square distribution not normal distribution.
    void chiSquareTest(int[] rolls, boolean random)
    {
        // We know our non-random dice will ace this test, so use them
        // to find expected mean and variance.
        int divisibleBy6 = 6 * trials / 6;
        int[] fixedRolls = new int[divisibleBy6];
        for (int i = 0; i < fixedRolls.length; i++)
        {
            fixedRolls[i] = Dice.rollDieNonRandom();
        }
        final double meanDieRoll = 3.5;
        double expVariance = findExpectedVariance(fixedRolls, meanDieRoll);
        double expMean = findChiSquare(fixedRolls, meanDieRoll)
            / fixedRolls.length;

        double chisquare = findChiSquare(rolls, meanDieRoll) / rolls.length;
        LOGGER.log(Level.FINEST, "chi-square test: chi-square=" + chisquare
            + " mean=" + expMean + " var=" + expVariance);
        failIfAbnormal(chisquare, expMean, expVariance, random);
    }

    /** Recode each sample as 0 if <= sample median, 1 if > sample median
     M is number of runs of consecutive 0s and 1s.
     r is number of 0s.
     null hypothesis, mean and variance of M in n observations are about
     meanM = 2*r*(n-r)/n + 1
     varianceM = 2*r*(n-r)*(2*r*(n-r)-n)/(n*n*(n-1))
     for large samples Zm = (M - meanM) / standardDevM is standard normal
     prob (M <= val) = Pr((M-meanM)/sdM = Pr(Z)
     */
    void mTest(int[] rolls, boolean random)
    {
        double median = findMedian(rolls);
        int[] ms = convertToBinary(rolls, median);
        int r = countZeros(ms);
        int M = countRuns(ms);
        int n = trials;
        double meanM = (2.0 * r * (n - r) / n) + 1.;
        double varianceM = ((2.0 * r) * (n - r) / n / n * ((2. * r) * (n - r) - n))
            / (n - 1.);
        LOGGER.log(Level.FINEST, "M test: r=" + r + " M=" + M + " mean="
            + meanM + " var=" + varianceM);
        failIfAbnormal(M, meanM, varianceM, random);
    }

    /** P is number of positive signs among x2-x1, x3-x2, etc. (not zeros)
     If m non-zero values of xi - x(i-1), meanP is m/2, varianceP is m/12
     */
    void signTest(int[] rolls, boolean random)
    {
        double P = countPositiveDiffs(rolls);
        double m = countNonZeroDiffs(rolls);
        double meanP = m / 2.;
        double varianceP = m / 12.;
        LOGGER.log(Level.FINEST, "Sign test: P=" + P + " m=" + m + " mean="
            + meanP + " var=" + varianceP);
        failIfAbnormal(P, meanP, varianceP, random);
    }

    void runsTest(int[] rolls, boolean random)
    {
        int[] trimmed = trimZeroRuns(rolls);
        int m = trimmed.length;
        int pos = countPositiveDiffs(trimmed);
        int neg = m - pos;
        double R = 0. + pos;
        double meanR = 1. + (2 * pos * neg) / (pos + neg);
        double varianceR = ((2.0 * pos * neg) * (2 * pos * neg - pos - neg))
            / ((pos + neg) * (pos + neg) * (pos + neg - 1));
        LOGGER.log(Level.FINEST, "Runs test: R=" + R + " m=" + m + " mean="
            + meanR + " var=" + varianceR);
        failIfAbnormal(R, meanR, varianceR, random);
    }

    void mannKendallTest(int[] rolls, boolean random)
    {
        int S = 0;
        int n = rolls.length;
        for (int i = 1; i < n; i++)
        {
            for (int j = 0; j < i; j++)
            {
                int val = sign(rolls[i] - rolls[j]);
                S += val;
            }
        }
        double meanS = 0.;
        double varianceS = (n / 18.) * (n - 1.) * (2. * n + 5.);
        LOGGER.log(Level.FINEST, "Mann-Kendall test: S=" + S + " mean="
            + meanS + " var=" + varianceS);
        failIfAbnormal(S, meanS, varianceS, random);
    }

    double findMean(int[] rolls)
    {
        double sum = 0.0;
        for (int roll : rolls)
        {
            sum += roll;
        }
        return sum / rolls.length;
    }

    double findMedian(int[] rolls)
    {
        int[] copy = rolls.clone();
        Arrays.sort(copy);
        double midpoint = (rolls.length - 1) / 2.0;
        if (Math.abs(midpoint - Math.round(midpoint)) < epsilon)
        {
            return copy[(int)Math.round(midpoint)];
        }
        else
        {
            return (copy[(int)Math.round(midpoint - 0.5)] + copy[(int)Math
                .round(midpoint + 0.5)]) / 2.0;
        }
    }

    int[] convertToBinary(int[] rolls, double median)
    {
        int[] ms = new int[rolls.length];
        for (int i = 0; i < rolls.length; i++)
        {
            if (rolls[i] <= median)
            {
                ms[i] = 0;
            }
            else
            {
                ms[i] = 1;
            }
        }
        return ms;
    }

    int countZeros(int[] rolls)
    {
        int count = 0;
        for (int roll : rolls)
        {
            if (roll == 0)
            {
                count++;
            }
        }
        return count;
    }

    int countRuns(int[] rolls)
    {
        int count = 0;
        for (int i = 0; i < rolls.length; i++)
        {
            if (i == 0 || rolls[i] != rolls[i - 1])
            {
                count++;
            }
        }
        return count;
    }

    int countPositiveDiffs(int[] rolls)
    {
        int count = 0;
        for (int i = 0; i < rolls.length - 1; i++)
        {
            if (rolls[i + 1] > rolls[i])
            {
                count++;
            }
        }
        return count;
    }

    int countNonZeroDiffs(int[] rolls)
    {
        int count = 0;
        for (int i = 1; i < rolls.length; i++)
        {
            if (rolls[i] != rolls[i - 1])
            {
                count++;
            }
        }
        return count;
    }

    /** Return the list with runs of identical rolls reduced to just one */
    int[] trimZeroRuns(int[] rolls)
    {
        List<Integer> li = new ArrayList<Integer>();
        int lastroll = -1;
        for (int roll : rolls)
        {
            if (roll != lastroll)
            {
                lastroll = roll;
                li.add(Integer.valueOf(lastroll));
            }
        }
        int[] results = new int[li.size()];
        for (int i = 0; i < results.length; i++)
        {
            results[i] = li.get(i).intValue();
        }
        return results;
    }

    /** Return the standard normal probability p(z) */
    double normalDistribution(double z)
    {
        return 1 / Math.sqrt(2 * Math.PI) * Math.exp(-z * z / 2);
    }

    /** Return 1 if positive, 0 if zero, -1 if negative. */
    int sign(int num)
    {
        if (num > 0)
        {
            return 1;
        }
        else if (num < 0)
        {
            return -1;
        }
        return 0;
    }

    double findChiSquare(int[] rolls, double mean)
    {
        double sum = 0.;
        for (int roll : rolls)
        {
            double diff = roll - mean;
            sum += diff * diff;
        }
        return sum / mean;
    }

    double findExpectedVariance(int[] rolls, double mean)
    {
        double sum = 0.;
        int n = rolls.length;
        for (int i = 0; i < n; i++)
        {
            int x = rolls[i];
            sum += Math.pow(x - mean, 2);
        }
        return sum / n;
    }

    /** Fail if an expected random result is outside the normal range. */
    void failIfAbnormal(double val, double mean, double variance,
        boolean random)
    {
        double sd;
        double z;
        // Avoid division by zero when we hit spot-on.
        if (Math.abs(variance) < epsilon)
        {
            sd = 0;
            z = 0;
        }
        else
        {
            sd = Math.sqrt(Math.abs(variance));
            z = (val - mean) / sd;
        }
        LOGGER.log(Level.FINEST, "sd=" + sd + " z=" + z);
        if (Math.abs(z) > 3)
        {
            if (random)
            {
                LOGGER.log(Level.FINEST, " (FAILURE)");
                fail("Random result is outside expected normal range.");
            }
            else
            {
                LOGGER.log(Level.FINEST, " (expected failure)");
            }
        }
        else
        {
            LOGGER.log(Level.FINEST, " (success)");
        }
    }
}
