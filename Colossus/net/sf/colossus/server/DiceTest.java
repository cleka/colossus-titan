package net.sf.colossus.server;

import java.util.*;
import junit.framework.*;


/** 
 *  JUnit test for dice. 
 *  @version $Id$
 *  @author David Ripton
 */
public class DiceTest extends TestCase
{
    private int trials = 10000;
    double epsilon = 0.000001;

    public DiceTest(String name)
    {
        super(name);
    }

    // Commented out because it's so slow.
    /*
    public void testDevRandom()
    {
        System.out.println("testDevRandom()");
        int [] rolls = new int[trials];
        Dice.init("/dev/random");
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDie();
        }
        run4Tests(rolls, true);
    }
    */

    // Comented out because not everyone has a /tmp/random :-)
    /*
    public void testTmpRandom()
    {
        System.out.println("testTmpRandom()");
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
        System.out.println("testDevUrandom()");
        int [] rolls = new int[trials];
        Dice.init("/dev/urandom");
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDie();
        }
        run4Tests(rolls, true);
    }
    
    public void testPRNG()
    {
        System.out.println("testPRNG()");
        int [] rolls = new int[trials];
        Dice.init("PRNG");
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDie();
        }
        run4Tests(rolls, true);
    }

    public void testNonRandomDice()
    {
        System.out.println("testNonRandomDice() [some should fail]");
        int [] rolls = new int[trials];
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDieNonRandom();
        }
        run4Tests(rolls, false);
    }

    void run4Tests(int [] rolls, boolean random)
    {
        mTest(rolls, random);
        signTest(rolls, random);
        runsTest(rolls, random);
        mannKendallTest(rolls, random);
    }

    // Source of tests: http://www.stat.unc.edu/faculty/rs/s102/lec17a.pdf
    // http://www2.sunysuffolk.edu/wrightj/MA24/Misc/RunsTest.pdf

    /** Recode each sample as 0 if <= sample median, 1 if > sample median
        M is number of runs of consecutive 0s and 1s.
        r is number of 0s.
        null hypothesis, mean and variance of M in n observations are about
        meanM = 2*r*(n-r)/n + 1
        varianceM = 2*r*(n-r)*(2*r*(n-r)-n)/(n*n*(n-1))
        for large samples Zm = (M - meanM) / standardDevM is standard normal
        prob (M <= val) = Pr((M-meanM)/sdM = Pr(Z)
     */
    void mTest(int [] rolls, boolean random)
    {
        double median = findMedian(rolls);
        int [] ms = convertToBinary(rolls, median);
        int r = countZeros(ms);
        int M = countRuns(ms);
        int n = trials;
        double meanM = (2.0 * r * (n - r) / n) + 1.;
        double varianceM = ((2.0 * r) * (n - r) / n / n * 
                ((2. * r) * (n - r) - n)) / (n - 1.);
        System.out.println("r=" + r + " M=" + M + " mean=" + meanM + " var=" +
            varianceM);
        failIfAbnormal(M, meanM, varianceM, random); 
    }

    /** P is number of positive signs among x2-x1, x3-x2, etc. (not zeros)
        If m non-zero values of xi - x(i-1), meanP is m/2, varianceP is m/12
     */
    void signTest(int [] rolls, boolean random)
    {
        double P = (double)countPositiveDiffs(rolls);
        double m = (double)countNonZeroDiffs(rolls);
        double meanP = m / 2.;
        double varianceP = m / 12.;
        System.out.println("P=" + P + " m=" + m + " mean=" + meanP + " var=" +
            varianceP);
        failIfAbnormal(P, meanP, varianceP, random); 
    }

    void runsTest(int [] rolls, boolean random)
    {
        double R = (double)countRunsExcludingZeros(rolls);
        double m = (double)countNonZeroDiffs(rolls);
        double meanR = ((2. * m) + 1.) / 3.;
        double varianceR = ((16. * m) - 13.) / 90.;
        System.out.println("R=" + R + " m=" + m + " mean=" + meanR + " var=" +
            varianceR);
        failIfAbnormal(R, meanR, varianceR, random); 
    }

    void mannKendallTest(int [] rolls, boolean random)
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
        System.out.println("S=" + S + " mean=" + meanS + " var=" + varianceS);
        failIfAbnormal(S, meanS, varianceS, random); 
    }
    
    double findMedian(int [] rolls)
    {
        int [] copy = (int [])rolls.clone();
        Arrays.sort(copy);
        double midpoint = (rolls.length - 1) / 2.0;
        if (midpoint - Math.round(midpoint) < epsilon)
        {
            return copy[(int)Math.round(midpoint)];
        }
        else
        {
            return (copy[(int)Math.round(midpoint - 0.5)] + 
                    copy[(int)Math.round(midpoint + 0.5)]) / 2.0;
        }
    }

    int [] convertToBinary(int [] rolls, double median)
    {
        int [] ms = new int[rolls.length];
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

    int countZeros(int [] rolls)
    {
        int count = 0;
        for (int i = 0; i < rolls.length; i++)
        {
            if (rolls[i] == 0)
            {
                count++;
            }
        }
        return count;
    }

    int countRuns(int [] rolls)
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

    int countPositiveDiffs(int [] rolls)
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

    int countNonZeroDiffs(int [] rolls)
    {
        int count = 0;
        for (int i = 1; i < rolls.length ; i++)
        {
            if (rolls[i] != rolls[i - 1])
            {
                 count++;
            }
        }
        return count;
    }

    int countRunsExcludingZeros(int [] rolls)
    {
        int count = 0;
        // states: 1 = positive, -1 = negative
        int state = 0;
        for (int i = 1; i < rolls.length; i++)
        {
            if (rolls[i] > rolls[i - 1])
            {
                 if (state != 1)
                 {
                     count++;
                 }
                 state = 1;
            }
            else if (rolls[i] < rolls[i - 1])
            {
                 if (state != -1)
                 {
                     count++;
                 }
                 state = -1;
            }
        }
        return count;
    }

    /** Return the standard normal probability p(z) */
    double normalDistribution(double z)
    {
        return 1/Math.sqrt(2*Math.PI) * Math.exp(-z*z/2);
    }


    /** Fail if an expected random result is outside the normal range. */
    void failIfAbnormal(double val, double mean, double variance, 
        boolean random)
    {
        double sd = Math.sqrt(variance);
        double z = (val - mean) / sd;
        System.out.print("sd=" + sd + " z=" + z);
        // fail if variance negative, as sd & z are NaN
        if (random && ((Math.abs(z) > 3) || (variance < 0.)))
        {
            System.out.println(" (FAIL)");
            fail();
        }
        else if (!random && ((Math.abs(z) > 3) || (variance < 0.)))
        {
            System.out.println(" (EXPECTED FAIL)");
        }
        else
        {
            System.out.println(" (SUCCESS)");
        }
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
}
