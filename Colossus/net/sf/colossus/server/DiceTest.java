package net.sf.colossus.server;

import net.sf.colossus.util.SpecialFunction;

import java.util.*;
import junit.framework.*;


/** 
 *  JUnit test for dice. 
 *  @version $Id$
 *  @author David Ripton
 */
public class DiceTest extends TestCase
{
    private int trials = 5000;
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
        System.out.println("---testDevUrandom()---");
        int [] rolls = new int[trials];
        Dice.init("/dev/urandom");
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDie();
        }
        runAllTests(rolls, true);
    }
    
    public void testPRNG()
    {
        System.out.println("---testPRNG()---");
        int [] rolls = new int[trials];
        Dice.init("PRNG");
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDie();
        }
        runAllTests(rolls, true);
    }

    public void testNonRandomDice()
    {
        System.out.println("---testNonRandomDice()--- [some should fail]");
        int [] rolls = new int[trials];
        for (int i = 0; i < trials; i++)
        {
            rolls[i] = Dice.rollDieNonRandom();
        }
        runAllTests(rolls, false);
    }

    void runAllTests(int [] rolls, boolean random)
    {
        mTest(rolls, random);
        signTest(rolls, random);
        mannKendallTest(rolls, random);
        runsTest(rolls, random);
    }


    // Most tests at http://www.stat.unc.edu/faculty/rs/s102/lec17a.pdf
    // Runs test http://www2.sunysuffolk.edu/wrightj/MA24/Misc/RunsTest.pdf

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
        System.out.println("M test: r=" + r + " M=" + M + " mean=" + meanM + 
            " var=" + varianceM);
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
        System.out.println("Sign test: P=" + P + " m=" + m + " mean=" + 
            meanP + " var=" + varianceP);
        failIfAbnormal(P, meanP, varianceP, random); 
    }

    void runsTest(int [] rolls, boolean random)
    {
        int [] trimmed = trimZeroRuns(rolls);
        int m = trimmed.length;
        int pos = countPositiveDiffs(trimmed);
        int neg = m - pos;
        double R = pos;
        double meanR = 1.0 + (2 * pos * neg) / (pos + neg);
        double varianceR = ((2.0 * pos * neg) * (2 * pos * neg - pos - neg)) /
                ((pos + neg) * (pos + neg) * (pos + neg - 1));
        System.out.println("Runs test: R=" + R + " m=" + m + " mean=" + 
            meanR + " var=" + varianceR);
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
        System.out.println("M-K test: S=" + S + " mean=" + meanS + 
            " var=" + varianceS);
        failIfAbnormal(S, meanS, varianceS, random); 
    }


    double findMean(int [] rolls)
    {
        double sum = 0.0;
        for (int i = 0; i < rolls.length; i++)
        {
            sum += rolls[i];
        }
        return sum / rolls.length;
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

    /** Return the list with runs of identical rolls reduced to just one */
    int [] trimZeroRuns(int [] rolls)
    {
        List li = new ArrayList();
        int lastroll = -1;
        for (int i = 0; i < rolls.length; i++)
        {
            if (rolls[i] != lastroll)
            {
                lastroll = rolls[i];
                li.add(new Integer(lastroll));
            }
        }
        int [] results = new int[li.size()];
        for (int i = 0; i < results.length; i++)
        {
            results[i] = ((Integer)li.get(i)).intValue();
        }
        return results;
    }


    /** Return the standard normal probability p(z) */
    double normalDistribution(double z)
    {
        return 1/Math.sqrt(2*Math.PI) * Math.exp(-z*z/2);
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
            sd = Math.sqrt(variance);
            z = (val - mean) / sd;
        }
        System.out.print("sd=" + sd + " z=" + z);
        if (random && (Math.abs(z) > 3))
        {
            System.out.println(" (FAILURE)");
            fail();
        }
        else if (!random && ((Math.abs(z) > 3)))
        {
            System.out.println(" (expected failure)");
        }
        else
        {
            System.out.println(" (success)");
        }
    }
}
