package net.sf.colossus.server;

import java.util.Random;
import net.sf.colossus.util.DevRandom;

/**
 * Class Dice handles die-rolling
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */
public final class Dice
{
    private static Random random = new DevRandom();

    static void init(String source)
    {
        random = new DevRandom(source);
    }
    
    /** Put all die rolling in one place, in case we decide to change random
     *  number algorithms, use an external dice server, etc. */
    public static int rollDie()
    {
        return rollDie(6);
    }
    
    public static int rollDie(int size)
    {
        return random.nextInt(size) + 1;
    }


    private static int[] basicSequence = {4,3,1,6,5,2};
    //private static int[] basicSequence = {1,2,3,4,5,6};
    private static int seqNum = -1;
    
    /* this one return from a fixed sequence, instead of a random value */
    public static int rollDieNonRandom()
    {
        seqNum = (seqNum + 1) % basicSequence.length;
        return (basicSequence[seqNum]);
    }
}
