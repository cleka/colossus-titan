package net.sf.colossus.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class DevRandom generates random bits (same interface as class Random).
 *
 * Which random data source to use can be specified by providing a
 * property called "net.sf.colossus.randomFile" (see randomPropertyName)
 * or as argument to the constructor.
 *
 * If no special source is specified or the specified source is unreachable
 * then it falls back on the regular java Random class.
 *
 * @author Romain Dolbeau
 * @author David Ripton
 */
public class DevRandom extends Random
{
    private static final Logger LOGGER = Logger.getLogger(DevRandom.class
        .getName());

    private final static String PRNG = "PRNG";
    private String source = null;
    private File randomSource = null;
    private FileInputStream randStream = null;

    private static final String randomPropertyName = "net.sf.colossus.randomFile";
    private static String randomPropertySource = null;

    public DevRandom()
    {
        super();
        source = getRandomSourceFromProperties();
        init();
    }

    public DevRandom(String sourcename)
    {
        super();
        source = sourcename;
        init();
    }

    /* Returns the source to use.
     * If set in a property (see randomPropertyName above),
     * then try it and remember whether set, or or not,
     * and subsequent calls return same source then without checking again.
     * If not set at all, return PRNG which makes the class using the default
     * random device.
     */
    private String getRandomSourceFromProperties()
    {
        // did we check earlier? Use that remembered info
        // null means, not checked
        // PRNG means, checked but not set => fall back to default
        if (randomPropertySource != null)
        {
            return randomPropertySource;
        }
        // nope. Check if property set, and remember we checked and
        // the actual value what to use.
        else
        {
            randomPropertySource = PRNG;
            String randomFile = null;
            /*
            It's amazing. When game starts with an external variant
            (name and path from options file), during instantiation
            of the HintsFile it needs DevRandom and during that
            the access to the getProperty(randomPropertyName) call
            throws an SecurityException (from JWS log file):
              <message>Jan 6, 2009 12:22:51 AM net.sf.colossus.util.ResourceLoader getNewObject
              SEVERE: Instantiating "PET3variant.PET3variantHint" failed
              java.security.AccessControlException: access denied (java.util.PropertyPermission net.sf.colossus.randomFile read)
            But when run normally from cmdline this problem does not occur :-(
            Anyway, to be safe we need to catch the exception...
            */
            try
            {
                randomFile = System.getProperty(randomPropertyName);
            }
            catch (SecurityException e)
            {
                LOGGER.info("Unable to access System property '"
                    + randomPropertyName + "'. Ignored.");
            }

            if (randomFile != null)
            {
                LOGGER.log(Level.FINEST, randomPropertyName
                    + " is set to using random source: " + randomFile);
                if (tryOneSource(randomFile))
                {
                    randomPropertySource = randomFile;
                    LOGGER.log(Level.FINEST,
                        "RandomSource looks OK - using it: " + randomFile);
                }
                // stays PRNG, i.e. falls back to default
            }
            return randomPropertySource;
        }
    }

    private boolean tryOneSource(String src)
    {
        if (src == null)
        {
            return false;
        }

        source = src;
        randomSource = new File(source);

        if ((randomSource == null) || (!randomSource.exists()))
        {
            LOGGER.log(Level.WARNING, "Random source unavailable: " + src);
            return false;
        }

        return true;
    }

    private void init()
    {
        if ((source != null) && (source.equals(PRNG)))
        {
            // Don't try other sources.
            return;
        }

        tryOneSource(source);

        if ((randomSource != null) && (randomSource.exists()))
        {
            try
            {
                randStream = new FileInputStream(randomSource);
            }
            catch (FileNotFoundException ex)
            {
                LOGGER.log(Level.SEVERE, "Random source disappeared!", ex);
                System.exit(1);
            }
            LOGGER.log(Level.FINEST, "Using " + source
                + " as the random source.");
        }
        else
        {
            LOGGER.log(Level.FINEST,
                "Random source unavailable ! Falling back on a PRNG");
        }
    }

    @Override
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
        byte[] bytes = new byte[size];
        try
        {
            int got = randStream.read(bytes);
            if (got != size)
            {
                LOGGER.log(Level.WARNING, "Reading from random source "
                    + source + ", got only " + got + " bytes (but expected "
                    + size + ") - falling back to standard Java PRNG.");
                randStream = null;
                return super.next(bits);
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Problem reading from random source "
                + source + " - falling back to standard Java PRNG.");
            randStream = null;
            return super.next(bits);
        }
        int result = 0;
        for (int i = 0; i < size; i++)
        {
            result |= (bytes[i] & 0x000000FF) << (i << 3);
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
