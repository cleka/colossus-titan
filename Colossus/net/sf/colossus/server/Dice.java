package net.sf.colossus.server;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;



/**
 * Class DevRandom generates random numbers from the "/dev/random" device, or any file or device supplying random binary data. If the source is unreachable it falls back on the regular java Random implementation.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */
class DevRandom extends Random
{
    private final static String urandomFilename = "/dev/urandom";
    private final static String randomFilename = "/dev/random";
    private String source = urandomFilename;
    private File randomSource = null;
    private FileInputStream randStream = null;

    public DevRandom()
    {
        init();
    }
    
    public DevRandom(String sourcename)
    {
        source = sourcename;
        init();
    }
    
    private void init()
    {
        randomSource = new File(source);
        if ((randomSource == null) || (!randomSource.exists()))
        {
            source = urandomFilename;
            randomSource = new File(source);
        }
        if ((randomSource == null) || (!randomSource.exists()))
        {
            source = randomFilename;
            randomSource = new File(source);
        }
        if ((randomSource != null) && (randomSource.exists()))
        {
            try
            {
                randStream = new FileInputStream(randomSource);
            }
            catch (FileNotFoundException ex)
            {
                Log.error("Random source disappeared! " + ex);
                System.exit(1);
            }
            Log.debug("Using " + source + " as the random source.");
        }
        else
        {
            Log.error("Random source unavailable ! Falling back on a PRNG");
        }
    }
    
    public int next(int bits)
    {
        if (randStream == null)
        {
//Log.debug("Error in the random source(s), falling back on the PRNG");
            return super.next(bits);
        }
        if (bits > 32)
            bits = 32;
        if (bits < 1)
            bits = 1;
        int size = (bits + 7) / 8;
        int mask = (1 << bits) - 1;
        byte [] bytes = new byte[size];
        try
        {
            randStream.read(bytes);
        }
        catch (IOException ex)
        {
            Log.error("Problem reading from random source");
            return super.next(bits);
        }
        int result = 0;
        for (int i = 0; i < size ; i++)
        {
            result |= ((long)(bytes[i])) << (i * 8);
        }
        return (result & mask);
    }
}

/**
 * Class Dice handles die-rolling
 * @version $Id$
 * @author David Ripton
 */
public final class Dice
{
    private static Random random = new DevRandom();
    
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
}
