package net.sf.colossus.server;


import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.variant.Variant;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


public class GameLoading
{
    private static final Logger LOGGER = Logger.getLogger(GameLoading.class
        .getName());

    private Variant variant;
    private Element root;

    public GameLoading()
    {
        super();
        LOGGER.info("Instantiated GameLoading");
    }

    public Variant getVariant()
    {
        return variant;
    }

    public Element getRoot()
    {
        return root;
    }

    /**
     * Try to load a game from saveDirName/filename.
     *
     * If the filename is "--latest" then load the latest savegame that
     * can be found in saveDirName.
     *
     * @return String telling reason for failure, or null if all ok
     */
    public String loadGame(String filename)
    {
        File file = resolveFileNameToFile(filename);
        if (file == null)
        {
            return "Can't resolve filename to file: " + filename;
        }
        return loadGameFromFile(file);
    }

    /**
     * For a given filename, open the file with that name from
     * save game directory.
     * If no such file, try also whether with adding ".xml" it would
     * become a valid/existing file.
     *
     * If the filename is "--latest" then load the latest savegame that
     * can be found in saveDirName.
     *
     *
     * @param filename The name of the file to load, or "--latest" for
     *        looking for latest save game (snapshot) file.
     * @return The File object for that filename, or null if no such file
     *         can't be found
     */
    private File resolveFileNameToFile(String filename)
    {
        File file = null;

        if (filename.equals("--latest"))
        {
            File dir = new File(Constants.SAVE_DIR_NAME);

            if (!dir.exists() || !dir.isDirectory())
            {
                LOGGER.log(Level.SEVERE, "No saves directory");
                return null;
            }
            String[] filenames = dir.list(new XMLSnapshotFilter());
            if (filenames.length < 1)
            {
                LOGGER.log(Level.SEVERE,
                    "No XML savegames found in saves directory");
                return null;
            }
            String name = latestSaveFilename(filenames);
            System.out.println("name " + name);
            file = new File(Constants.SAVE_DIR_NAME + name);
        }
        else if (filename.indexOf("/") >= 0 || filename.indexOf("\\") >= 0)
        {
            // Already a full path
            file = new File(filename);
        }
        else
        {
            file = new File(Constants.SAVE_DIR_NAME + filename);
        }

        if (!file.exists())
        {
            String tryXMLFile = file.getPath() + ".xml";
            File xmlFile = new File(tryXMLFile);
            if (xmlFile.exists())
            {
                LOGGER.warning("Given filename does not exist - loading "
                    + "instead the one with .xml appended to the name!");
                file = xmlFile;
            }
            else
            {
                LOGGER.severe("Cannot load saved game: file " + file.getPath()
                    + " does not exist!");
                return null;
            }
        }

        return file;
    }

    /**
     * Load contents of the file, get variant name, load the right variant
     * and get the root element.
     * Currently also gets all variant data files and puts them to file
     * cache but that is going to be removed one day. Soon ;-)
     *
     * @param file The file from which to load the game
     * @return True if load was successful, otherwise false
     */
    public String loadGameFromFile(File file)
    {
        try
        {
            LOGGER.info("Loading game from " + file);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            this.root = doc.getRootElement();
            Attribute ver = root.getAttribute("version");

            if (!ver.getValue().equals(Constants.XML_SNAPSHOT_VERSION))
            {
                LOGGER.severe("Can't load this savegame version.");
                // TODO not only would this fail to load quietly, it also fails
                // to fail quietly rather noisily by causing an NPE in dispose().

                return "Can't load this savegame version (" + ver.getValue()
                    + ", expected: " + Constants.XML_SNAPSHOT_VERSION + ")";
            }

            Element el = root.getChild("Variant");
            Attribute namAttr = el.getAttribute("name");
            String varName = null;
            if (namAttr != null)
            {
                varName = namAttr.getValue();
            }
            else
            {
                LOGGER.severe("Variant name not set in saveGame file!");
                return "Variant name not set in saveGame file!";
            }

            this.variant = VariantSupport.loadVariantByName(varName, true);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Exception during loading Game: ", e);
            return "Exception during loading game: " + e.getMessage();
        }
        return null;
    }

    /**
     * Find from the list of savegame filenames the one with the highest
     * numerical value (1000000000_xxx.xml comes after 999999999_xxx.xml)
     * @param filenames An array of strings which represent filenames
     * @return Latest savegame from the list
     */
    private String latestSaveFilename(String[] filenames)
    {
        return Collections.max(Arrays.asList(filenames),
            new Comparator<String>()
            {
                public int compare(String s1, String s2)
                {
                    long diff = (numberValue(s1) - numberValue(s2));

                    if (diff > Integer.MAX_VALUE)
                    {
                        return Integer.MAX_VALUE;
                    }
                    if (diff < Integer.MIN_VALUE)
                    {
                        return Integer.MIN_VALUE;
                    }
                    return (int)diff;
                }
            });
    }

    /** Extract and return the numeric part of a filename. */
    private long numberValue(String filename)
    {
        StringBuilder numberPart = new StringBuilder();
        boolean foundFirstDigit = false;
        boolean done = false;

        for (int i = 0; i < filename.length() && !done; i++)
        {
            char ch = filename.charAt(i);

            if (Character.isDigit(ch))
            {
                numberPart.append(ch);
                foundFirstDigit = true;
            }
            else if (foundFirstDigit)
            {
                // Found first non-digit after digits block - done.
                done = true;
            }
        }
        try
        {
            return Long.parseLong(numberPart.toString());
        }
        catch (NumberFormatException e)
        {
            return -1L;
        }
    }

}
