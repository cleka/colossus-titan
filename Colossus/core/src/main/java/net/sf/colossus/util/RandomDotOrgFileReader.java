package net.sf.colossus.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.WhatNextManager;


public class RandomDotOrgFileReader
{
    private static final Logger LOGGER = Logger
        .getLogger(RandomDotOrgFileReader.class.getName());

    private static final String propertyName = "net.sf.colossus.randomDotOrgDirectory";

    private final List<File> files = new ArrayList<File>();

    private final ArrayList<File> unusedFiles = new ArrayList<File>();

    private File currentFile = null;

    private FileInputStream randomByteStream = null;

    private final Random fallbackRandom = new Random();



    public RandomDotOrgFileReader()
    {
        init(System.getProperty(propertyName));
    }

    public RandomDotOrgFileReader(String directoryPath)
    {
        init(directoryPath);
    }

    /**
     * Initializes the list of files, and sets up a FileInputStream that
     * reads from the first file.
     * currentFile and randomByteStream will be initialized to be ready
     * to read first byte from it.
     *
     * If something goes wrong, the list will be empty, currentFile
     * and randomByteStream will be null, so any call to nextRoll()
     * will fall back to the normal Java PRNG.
     */
    private void init(String directoryPath)
    {
        try
        {
            File directory = new File(directoryPath);
            File[] filenames = directory.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().endsWith(".bin");
                }
            });

            files.addAll(Arrays.asList(filenames));
        }
        // NullPointerException, SecurityException
        catch (Exception e)
        {
            LOGGER.warning("Error reading files from directory '"
                + directoryPath + "'! Using PRNG as fallback.");
            files.clear();
            return;
        }

        if (files.size() == 0)
        {
            LOGGER.info("Directory " + files.size() + " is empty.");

        }

        LOGGER.info("Directory contains " + files.size()
            + " 'random.org'-files.");

        unusedFiles.addAll(files);
        prepareNextFile();
    }

    /**
     *  After this returns, the randomByteInputStream is prepared for
     *  that one can read bytes from it, OR, randomByteStream is null
     *  so that reads fallback to PRNG.
     *
     */
    private void prepareNextFile()
    {
        try
        {
            // close previous one, if necessary
            if (randomByteStream != null)
            {
                randomByteStream.close();
                randomByteStream = null;
                currentFile = null;
            }

            while (randomByteStream == null)
            {
                if (files.isEmpty())
                {
                    LOGGER.severe("No usable files left in file list! "
                        + "Falling back to PRNG.");
                    randomByteStream = null;
                    currentFile = null;
                    return;
                }

                if (unusedFiles.isEmpty())
                {
                    LOGGER
                        .warning("All random.org files used, starting from beginning.");
                    unusedFiles.addAll(files);
                }

                currentFile = unusedFiles.remove(0);
                System.out.println("\nOpening file " + currentFile
                    + " for reading random data.");
                WhatNextManager.sleepFor(500);

                LOGGER.info("Opening file " + currentFile
                    + " for reading random data.");
                randomByteStream = new FileInputStream(currentFile);
                if (randomByteStream.available() == 0)
                {
                    LOGGER.warning("File " + currentFile.toString()
                        + " seems to be empty? " + "Trying next file.");
                    WhatNextManager.sleepFor(1000);

                    // Totally remove from files to consider
                    files.remove(currentFile);
                    currentFile = null;
                    randomByteStream.close();
                    randomByteStream = null;
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warning("Exception while handling randomByteInputStream "
                + "files. Falling back to PRNG.");
            randomByteStream = null;
            currentFile = null;
        }
    }

    private int oneByteFromFallback()
    {
        return fallbackRandom.nextInt(255);
    }

    /**
     * returns a 7 byte integer, or -1.
     * Returning -1 means, this input file is exhausted, next file
     * is already prepared to read from, but caller must call us again
     * to do so.
     *
     * @return
     */
    private int nextSingleByte()
    {
        if (randomByteStream == null)
        {
            return oneByteFromFallback();
        }

        int size = 1;
        byte[] bytes = new byte[1];
        try
        {
            int got = randomByteStream.read(bytes);
            if (got == -1)
            {
                prepareNextFile();
                return -1;
            }
            else if (got != size)
            {
                LOGGER.log(Level.WARNING, "Reading from random.org file "
                    + currentFile.toString() + " got wrong amount of bytes!"
                    + " Falling back to standard Java PRNG.");
                randomByteStream = null;
                return oneByteFromFallback();
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "Exception during reading from random "
                + "file '" + currentFile.toString() + "'. "
                + " Falling back to standard Java PRNG.");
            randomByteStream = null;
            return oneByteFromFallback();
        }

        int result = 0;
        result |= bytes[0];
        return result;
    }

    /**
     * For dice rolls, which can be satisfied with a single byte, we
     * read the byte and handle the evaluation ourself, instead of having
     * Random blindly requesting 32 bits, of which most is then wasted.
     */
    public int nextRoll()
    {
        int n = 6;
        int signedInt, result;

        // last complete block of six numbers (N*6 + (0 .. 5))
        int upperBoundary = 251;
        do
        {
            signedInt = nextSingleByte();
            // use it and do conversion anyway, otherwise compiler
            // complained about result possibly uninitialized
            result = (signedInt & 0xFF) % n;
        }
        while (signedInt == -1 || result > upperBoundary);
        return result + 1;
    }

}
