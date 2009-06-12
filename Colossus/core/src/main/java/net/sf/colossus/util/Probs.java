package net.sf.colossus.util;


import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class Probs holds utility methods for working with probabilities.
 *
 * @author David Ripton
 */
public final class Probs
{
    private static final Logger LOGGER = Logger.getLogger(Probs.class
        .getName());

    static int lastFakeDie = 5;

    /** Compute n! */
    public static int factorial(int n)
    {
        int answer = 1;
        for (int i = n; i >= 2; i--)
        {
            answer *= i;
        }
        return answer;
    }

    /** Compute a choose b. */
    public static int choose(int a, int b)
    {
        return factorial(a) / (factorial(b) * factorial(a - b));
    }

    /** Return the probability of getting exactly this number of hits. */
    public static double probHits(int dice, int strikeNumber, int hits)
    {
        double p = (7.0 - strikeNumber) / 6.0;
        return Math.pow(p, hits) * Math.pow(1 - p, dice - hits)
            * choose(dice, hits);
    }

    /** Return the probability of getting this number of hits or more. */
    public static double probHitsOrMore(int dice, int strikeNumber, int hits)
    {
        double total = 0.0;
        for (int i = hits; i <= dice; i++)
        {
            total += probHits(dice, strikeNumber, i);
        }
        return total;
    }

    /** Return the probability of getting this number of hits or less. */
    public static double probHitsOrLess(int dice, int strikeNumber, int hits)
    {
        double total = 0.0;
        for (int i = 0; i <= hits; i++)
        {
            total += probHits(dice, strikeNumber, i);
        }
        return total;
    }

    /** Return the unrounded mean number of hits. */
    public static double meanHits(int dice, int strikeNumber)
    {
        return dice * (7 - strikeNumber) / 6.0;
    }

    /** Return the most likely number of hits.  If there are two
     * modes, return the higher one. */
    public static int modeHits(int dice, int strikeNumber)
    {
        return (int)Math.round(dice * (7 - strikeNumber) / 6.0);
    }

    /** Return the next die roll in a predictable regular sequence,
     *  useful for estimating combat results.  The current sequence
     *  is 436125. */
    public static int rollFakeDie()
    {
        switch (lastFakeDie)
        {
            case 1:
                lastFakeDie = 2;
                break;

            case 2:
                lastFakeDie = 5;
                break;

            case 3:
                lastFakeDie = 6;
                break;

            case 4:
                lastFakeDie = 3;
                break;

            case 5:
                lastFakeDie = 4;
                break;

            case 6:
                lastFakeDie = 1;
                break;

            default:
                LOGGER.log(Level.SEVERE, "Bogus fake die roll");
        }
        return lastFakeDie;
    }
}
