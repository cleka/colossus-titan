/**
 * Class Probs holds utility methods for working with probabilities.
 * @version $Id$
 * @author David Ripton
 */


public class Probs
{
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
    public static double probHits(int dice, int strikeNumber,
        int hits)
    {
        double p = (7.0 - strikeNumber) / 6.0;
        return Math.pow(p, hits) * Math.pow(1 - p, dice - hits) *
            choose(dice, hits);
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
}
