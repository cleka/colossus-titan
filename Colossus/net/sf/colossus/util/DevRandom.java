package net.sf.colossus.util;

import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;

/**
 * Class DevRandom generates random numbers from the "/dev/random" device,
 * or any file or device supplying random binary data.
 * If the source is unreachable it falls back on the
 * regular java Random implementation.
 * @version $Id$
 * @author Romain Dolbeau
 * @author David Ripton
 */
public class DevRandom extends Random
{
    final static String urandomFilename = "/dev/urandom";
    final static String randomFilename = "/dev/random";
    final static String PRNG = "PRNG";
    private String source = urandomFilename;
    private File randomSource = null;
    private FileInputStream randStream = null;
    
    public DevRandom()
    {
        super();
        init();
    }
    
    public DevRandom(String sourcename)
    {
        super();
        source = sourcename;
        init();
    }
    
    private void init()
    {
        if (source.equals(PRNG))
        {
            // Don't try other sources.
            return;
        }
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
            Log.debug("Random source unavailable ! Falling back on a PRNG");
        }
    }
    
    protected int next(int bits)
    {
        int nbits = bits;

        if (randStream == null)
        {
            return super.next(bits);
        }
        if (nbits > 32)
        {
            nbits = 32;
        }
        if (nbits < 1)
        {
            nbits = 1;
        }
        int size = (nbits + 7) >> 3;
        // works even in nbits == 32
        int mask = (1 << nbits) - 1;
        byte [] bytes = new byte[size];
        try
        {
            randStream.read(bytes);
        }
        catch (IOException ex)
        {
            Log.error("Problem reading from random source " + source);
            return super.next(bits);
        }
        int result = 0;
        for (int i = 0; i < size ; i++)
        {
            result |= (((int)(bytes[i])) & 0x000000FF) << (i << 3);
        }

        result = (result & mask);
        /*
          String toto = "";
          for (int i = 0; i < size; i++) toto = toto + "|" + bytes[i]; 
          Log.debug("For " + source + ", array is " + toto + "|, result is " +
                    result + ", bits is " + bits + ", mask is " + mask);
        */
        return (result);
    }
}
