package net.sf.colossus.server;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;


/**
 * Class Dice handles die-rolling
 * @version $Id$
 * @author David Ripton
 */

public final class Dice
{
    private static Random random = new Random();
    /** If you're *really* paranoid, change to urandom. */
    private static String randomFilename = "/dev/random";
    private static File randomSource = new File(randomFilename);
    private static FileInputStream randStream = null;
    
    
    /** Put all die rolling in one place, in case we decide to change random
     *  number algorithms, use an external dice server, etc. */
    public static int rollDie()
    {
        return rollDie(6);
    }

    public static int rollDie(int size)
    {
        if (size > 10000)
        {
            Log.error("Looks like someone is trying to cheat");
            return 0;
        }
        if (randomSource.exists())
        {
            if (randStream == null)
            {
                try
                {
                    randStream = new FileInputStream(randomSource);
                }
                catch (FileNotFoundException ex)
                {
                    Log.error("/dev/random disappeared!");
                    System.exit(1);
                }
            }
            byte [] bytes = new byte[8];
            try
            {
                randStream.read(bytes);
            }
            catch (IOException ex)
            {
                Log.error("Problem reading from /dev/random");
            }
            random.setSeed(eightBytesToLong(bytes));
        }
        return random.nextInt(size) + 1;
    }

    /** Generate a long from an array of eight bytes. */
    private static long eightBytesToLong(byte [] bytes)
    {
        long val = 0L;
        for (int i = 0; i < 8; i++)
        {
            val += bytes[i] << i;
        }
        return val;
    }
}

