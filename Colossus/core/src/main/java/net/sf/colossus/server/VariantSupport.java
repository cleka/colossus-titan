package net.sf.colossus.server;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import net.sf.colossus.client.HexMap;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoard;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.xmlparser.CreatureLoader;
import net.sf.colossus.xmlparser.StrategicMapLoader;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.xmlparser.VariantLoader;


/**
 * Class VariantSupport hold the members and functions 
 * required to support Variants in Colossus
 * 
 * TODO this should probably move into the variant package sooner or later, possibly
 *      into the {@link Variant} class itself
 * 
 * @version $Id$
 * @author Romain Dolbeau
 */

public final class VariantSupport
{
    private static final Logger LOGGER = Logger.getLogger(VariantSupport.class
        .getName());

    private static String varDirectory = "";
    private static String varFilename = "";
    private static String variantName = "";    
    private static String mapName = "";
    private static String recruitName = "";
    private static String hintName = "";
    private static List<String> lCreaturesName;
    private static Document varREADME = null;
    private static List<String> dependUpon = null;
    private static boolean loadedVariant = false;
    private static int maxPlayers;
    private static HintInterface aihl = null;
    private static Properties markerNames;

    private static Variant CURRENT_VARIANT;

    /**
     * Clean-up the ResourceLoader caches to make room for a variant.
     * @param tempVarFiel The name of the file holding the
     *     soon-to-be-loaded Variant definition.
     * @param tempVarDirectory The path to the directory holding the
     *     soon-to-be-loaded Variant.
     */
    public static void freshenVariant(String tempVarFilename,
        String tempVarDirectory)
    {
        if (!(loadedVariant && varFilename.equals(tempVarFilename) && varDirectory
            .equals(tempVarDirectory)))
        {
            ResourceLoader.purgeImageCache();
            ResourceLoader.purgeFileCache();
        }
    }

    private static HashMap<String, String> rememberCustomDirs 
    = new HashMap<String, String>();

    public static void rememberFullPathFileForVariantName(String varName,
        String varFullPathFilename)
    {
        
        rememberCustomDirs.put(varName, varFullPathFilename);
    }

    public static String getFullPathFileForVariantName(String varName)
    {
        return rememberCustomDirs.get(varName);
    }

    /**
     * Load a Colossus Variant by name.
     * @param variantName The name of the variant.
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     */
    public static Document loadVariantByName(String variantName, boolean serverSide)
    {
        // if it's a variant earlier loaded with Load Extern Variant, find out the 
        // directory path for it:
        String fullPathFileName = getFullPathFileForVariantName(variantName);
        String variantDir;
        if (fullPathFileName == null)
        {
            // not remembered => a built-in variant
            variantDir = Constants.varPath + variantName;
        }
        else
        {
            // remembered => conclude full path of directory:
            File fullPathFile = new File(fullPathFileName);
            variantDir = fullPathFile.getParentFile().getAbsolutePath();
        }
        String variantFileName = variantName + Constants.varEnd;
        return loadVariant(variantName, variantFileName, variantDir,
            serverSide);
    }

    /**
     * Load a Colossus Variant from the specified File
     * @param varFile The File to load as a Variant, probably selected
     *        by user in a FileSelectionDialog, with full absolute path.
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     */

    public static Document loadVariantByFile(java.io.File varFile,
        boolean serverSide)
    {
        
        String tempVarFilename  = varFile.getName();
        String tempVarDirectory = varFile.getParentFile().getAbsolutePath();
        String tempVarName = null;
        try
        {
            tempVarName = getVariantNameFromFilename(tempVarFilename);
        }
        catch(Exception e)
        {
            LOGGER.severe("IllegalVariantFileName - unable to conclude "
                + "variant name from filename '" + tempVarFilename + "'!");
            return null;
        }

        // caller need to store that to options so that later a external
        // variant (where re-selected in combo box) can be loaded again:
        String tempFullPathFilename = varFile.getAbsolutePath();
        rememberFullPathFileForVariantName(tempVarName, tempFullPathFilename);
        
        return loadVariant(tempVarName, tempVarFilename, tempVarDirectory,
            serverSide);
    }

    private static String getVariantNameFromFilename(String varFilename)
        throws Exception
    {
        String result = null;
        if (varFilename.endsWith(Constants.varEnd))
        {
            // We need the Variantname for loading a game with
            // remote players.
            result = varFilename.substring(0,
                varFilename.length() - Constants.varEnd.length());
        }
        else
        {
            /* Seems the filename is not <variantname>VAR.xml
             *  - then we cannot conclude the name.
             * TODO every loading of a variant should get the
             * variant name as argument and also set the 
             * variantName variable from that, and saving a game
             * it should store a 3rd property to hold the variant
             * name (not just file and dir).
             */
            throw(new Exception(
                "IllegalVariantFilenameException"));
        }
        return result;

    }
    
    /**
     * Load a Colossus Variant from the specified filename
     *   in the specified path.
     * @param tempVarFilename The name of the file holding the Variant definition.
     * @param tempVarDirectory The path to the directory holding the Variant.
     * @param tempVariantName The actual plain name of the variant
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     * 
     * TODO right now variant name might sometimes be null, then we try a hack
     * to retrieve the variant name from the variant file name.
     */
    public static Document loadVariant(String tempVariantName,
        String tempVarFilename,
        String tempVarDirectory, 
        boolean serverSide)
    {
        if (tempVariantName == null)
        {
            LOGGER.severe("variantName must not be null!");
            return null;
        }
        
        if (loadedVariant && varFilename.equals(tempVarFilename)
            && varDirectory.equals(tempVarDirectory))
        {
            return varREADME;
        }

        if (serverSide)
        {
            ResourceLoader.purgeImageCache();
            ResourceLoader.purgeFileCache();
        }

        loadedVariant = false;
        String task = "<nothing yet";

        LOGGER.log(Level.FINEST, "Loading variant file " + tempVarFilename
            + ", data files in " + tempVarDirectory);
        try
        {

            /* Can't use getVarDirectoriesList yet ! */
            List<String> directories = new ArrayList<String>();
            directories.add(tempVarDirectory);
            directories.add(Constants.defaultDirName);
            task = "Load variant file \"" + tempVarFilename + "\"";
            InputStream varIS = ResourceLoader.getInputStream(tempVarFilename,
                directories);
            if (varIS == null)
            {
                throw new FileNotFoundException(tempVarFilename);
            }
            else
            {
                VariantLoader vl = new VariantLoader(varIS);
                if (vl.getMaxPlayers() > 0)
                {
                    maxPlayers = vl.getMaxPlayers();
                }
                else
                {
                    maxPlayers = Constants.DEFAULT_MAX_PLAYERS;
                }
                if (maxPlayers > Constants.MAX_MAX_PLAYERS)
                {
                    LOGGER.log(Level.SEVERE, "Can't use more than "
                        + Constants.MAX_MAX_PLAYERS
                        + " players, while variant requires " + maxPlayers);
                    maxPlayers = Constants.MAX_MAX_PLAYERS;
                }
                varDirectory = tempVarDirectory;
                varFilename = tempVarFilename;
                variantName = tempVariantName;

                mapName = vl.getMap();
                if (mapName == null)
                {
                    mapName = Constants.defaultMAPFile;
                }
                LOGGER.log(Level.FINEST, "Variant using MAP " + mapName);

                lCreaturesName = vl.getCre();
                if (lCreaturesName.isEmpty())
                {
                    lCreaturesName.add(Constants.defaultCREFile);
                }
                for (String creaturesName : lCreaturesName)
                {
                    LOGGER.log(Level.FINEST, "Variant using CRE " + creaturesName);
                }

                recruitName = vl.getTer();
                if (recruitName == null)
                {
                    recruitName = Constants.defaultTERFile;
                }
                LOGGER.log(Level.FINEST, "Variant using TER " + recruitName);

                hintName = vl.getHintName();
                LOGGER.log(Level.FINEST, "Variant using hint " + hintName);
                dependUpon = vl.getDepends();
                LOGGER.log(Level.FINEST, "Variant depending upon "
                    + dependUpon);
            }
            directories = new ArrayList<String>();
            directories.add(tempVarDirectory);
            task = "getDocument README*";
            varREADME = ResourceLoader.getDocument("README", directories);

            /* OK, what is the proper order here ?
             * We should start with HazardTerrain & HazardHexside, but those
             * aren't in variant yet. They don't require anything else.
             * Then must comes the CreatureType. They are only natives to
             * HazardTerrain & HazardHexside, and don't need anything else.
             * Then we can load the terrains & recruits ; they need the
             * CreatureType.
             * Finally we can load the Battlelands, they need the terrain.
             */

            List<CreatureType> creatureTypes = loadCreatures();
            loadTerrainsAndRecruits(serverSide);
            // TODO add things as the variant package gets fleshed out
            List<MasterBoardTerrain> battleLands = new ArrayList<MasterBoardTerrain>();

            List<String> directoriesForMap = getVarDirectoriesList();
            InputStream mapIS = ResourceLoader.getInputStream(VariantSupport
                .getMapName(), directoriesForMap);
            if (mapIS == null)
            {
                throw new FileNotFoundException(VariantSupport.getMapName());
            }
            StrategicMapLoader sml = new StrategicMapLoader(mapIS);
            
            MasterBoard masterBoard = new MasterBoard(sml.getHorizSize(),
                sml.getVertSize(), sml.getShow(), sml.getHexes());

            // varREADME seems to be used as flag for a successfully loaded
            // variant, but breaking the whole variant loading just because
            // there is no readme file seems a bit overkill, thus we set
            // a default in this case
            if (varREADME == null)
            {
                varREADME = getMissingReadmeNotification();
            }

            CURRENT_VARIANT = new Variant(creatureTypes, battleLands,
                masterBoard, varREADME, variantName);
        }
        catch (Exception e)
        {
            LOGGER.severe("\n******\nCATCH - reset to DEFAULT\n");
            
            // TODO this seems like a classic case of fail-slow, rethrowing the exception
            // might be better
            LOGGER.log(Level.SEVERE, "Variant loading failed : " + e, e);
            varDirectory = Constants.defaultDirName;
            varFilename = Constants.defaultVARFile;
            variantName = Constants.defaultVarName;
            mapName = Constants.defaultMAPFile;
            recruitName = Constants.defaultTERFile;
            hintName = Constants.defaultHINTFile;
            lCreaturesName.clear();
            lCreaturesName.add(Constants.defaultCREFile);
            maxPlayers = Constants.DEFAULT_MAX_PLAYERS;
            varREADME = null;
        }

        if (varREADME != null)
        {
            loadedVariant = true;
            loadHints();
            task = "loadMarkerNamesProperties";
            markerNames = loadMarkerNamesProperties();
        }
        else
        {
            if (tempVarFilename.equals(Constants.defaultVARFile))
            {
                LOGGER.log(Level.SEVERE,
                    "Default Variant Loading Failed, aborting !");
                System.exit(1);
            }
            else
            {
                JOptionPane.showMessageDialog(null,
                    "Trying to load variant '" + tempVariantName + "' failed "
                    + "(task='"+task+"')."
                    + "\nI will try to load variant 'Default' instead...",
                    "Variant loading failed!",
                    JOptionPane.ERROR_MESSAGE);
                LOGGER.log(Level.WARNING, "Loading variant " + tempVariantName
                    + " failed - trying to load Default instead...");

                varREADME = loadVariant(Constants.defaultVarName,
                    Constants.defaultVARFile,
                    Constants.defaultDirName,  
                    serverSide);
            }
        }

        return varREADME;
    }

    /** Call immediately after loading variant, before using creatures. */
    public static List<CreatureType> loadCreatures()
    {
        List<CreatureType> creatures = new ArrayList<CreatureType>();
        try
        {
            creatures.clear();
            List<String> directories = VariantSupport.getVarDirectoriesList();
            for (String creaturesName : VariantSupport.getCreaturesNames())
            {
                InputStream creIS =
                        ResourceLoader.getInputStream(creaturesName,
                        directories);
                if (creIS == null)
                {
                    throw new FileNotFoundException(creaturesName);
                }
                CreatureLoader creatureLoader = new CreatureLoader(creIS);
                creatures.addAll(creatureLoader.getCreatures());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load Creatures definition",
                e);
        }
        Collections.sort(creatures, CreatureType.NAME_ORDER);
        return creatures;
    }

    private static Document getMissingReadmeNotification()
    {
        StyledDocument txtdoc = new DefaultStyledDocument();
        try
        {
            txtdoc
                .insertString(
                    0,
                    "No README found -- variant is lacking a README.txt or README.html.",
                    null);
        }
        catch (BadLocationException e)
        {
            // really shouldn't happen with the constant offset
            LOGGER
                .log(
                    Level.WARNING,
                    "Failed to insert warning about missing readme into Document object",
                    e);
        }
        txtdoc.putProperty(ResourceLoader.keyContentType, "text/plain");
        return txtdoc;
    }

    public static String getVarDirectory()
    {
        return varDirectory;
    }

    public static String getVarFilename()
    {
        return varFilename;
    }

    public static String getVariantName()
    {
        return variantName;
    }

    public static String getMapName()
    {
        return mapName;
    }

    public static String getRecruitName()
    {
        return recruitName;
    }

    public static List<String> getCreaturesNames()
    {
        return lCreaturesName;
    }

    public static List<String> getVarDirectoriesList()
    {
        List<String> directories = new ArrayList<String>();
        if (!(varDirectory.equals(Constants.defaultDirName)))
        {
            directories.add(varDirectory);
        }
        Iterator<String> it = dependUpon.iterator();
        while (it.hasNext())
        {
            directories.add(it.next());
        }
        directories.add(Constants.defaultDirName);
        return directories;
    }

    public static List<String> getVarDirectoriesList(String suffixPath)
    {
        List<String> directories = getVarDirectoriesList();
        List<String> suffixedDirs = new ArrayList<String>();
        Iterator<String> it = directories.iterator();
        while (it.hasNext())
        {
            String dir = it.next();
            suffixedDirs.add(dir + ResourceLoader.getPathSeparator()
                + suffixPath);
        }
        return suffixedDirs;
    }

    public static List<String> getImagesDirectoriesList()
    {
        return getVarDirectoriesList(Constants.imagesDirName);
    }

    public static List<String> getBattlelandsDirectoriesList()
    {
        return getVarDirectoriesList(Constants.battlelandsDirName);
    }

    /** TerrainRecruitLoader is needed by many classes, so load it
     *  immediately after loading the variant. */
    public synchronized static void loadTerrainsAndRecruits(boolean serverSide)
    {
        // remove all old stuff in the custom recruitments system
        CustomRecruitBase.reset();

        try
        {
            List<String> directories = getVarDirectoriesList();
            InputStream terIS = ResourceLoader.getInputStream(
                getRecruitName(), directories);
            if (terIS == null)
            {
                throw new FileNotFoundException(getRecruitName());
            }
            // @todo parsing into static fields is a side effect of this constructor
            new TerrainRecruitLoader(terIS);

            /* now initialize the static bits of the Battlelands */
            HexMap.staticBattlelandsInit(serverSide);
        }
        catch (Exception e)
        {
            // TODO another exception anti-pattern: calling System.exit which means
            // noone can escape the disappearing VM, even if they would know how
            LOGGER.log(Level.SEVERE, "Recruit-per-terrain loading failed.", e);
            System.exit(1);
        }
    }

    private static Properties loadMarkerNamesProperties()
    {
        Properties allNames = new Properties();
        List<String> directories = getVarDirectoriesList();

        /* unlike other, don't use file-level granularity ;
         load all files in order, so that we get the
         default mapping at the end */
        ListIterator<String> it = directories.listIterator(directories.size());
        boolean foundOne = false;
        while (it.hasPrevious())
        {
            List<String> singleDirectory = new ArrayList<String>();
            singleDirectory.add(it.previous());
            try
            {
                InputStream mmfIS = ResourceLoader.getInputStreamIgnoreFail(
                    Constants.markersNameFile, singleDirectory);
                if (mmfIS != null)
                {
                    allNames.load(mmfIS);
                    foundOne = true;
                }
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING,
                    "Markers name loading partially failed.");
            }
        }
        if (!foundOne)
        {
            LOGGER.log(Level.WARNING, "No file " + Constants.markersNameFile
                + " found anywhere in directories " + directories.toString());
        }
        return allNames;
    }

    public static Properties getMarkerNamesProperties()
    {
        return markerNames;
    }

    private synchronized static void loadHints()
    {
        aihl = null;
        Object o = null;
        if (hintName != null)
        {
            o = ResourceLoader.getNewObject(hintName, getVarDirectoriesList());
        }
        if ((o != null) && (o instanceof HintInterface))
        {
            aihl = (HintInterface)o;
            LOGGER.log(Level.FINEST, "Using class " + hintName
                + " to supply hints to the AIs.");
        }
        else
        {
            if (hintName.equals(Constants.defaultHINTFile))
            {
                LOGGER.log(Level.SEVERE, "Couldn't load default hints !");
                System.exit(1);
            }
            else
            {
                LOGGER.log(Level.WARNING,
                    "Couldn't load hints. Trying with Default.");
                hintName = Constants.defaultHINTFile;
                loadHints();
            }
        }
    }

    public synchronized static String getRecruitHint(
        MasterBoardTerrain terrain, LegionClientSide legion,
        List<CreatureType> recruits, HintOracleInterface oracle)
    {
        String[] section = new String[1];
        section[0] = Constants.sectionAllAI;
        return getRecruitHint(terrain, legion, recruits, oracle, section);
    }

    public synchronized static String getRecruitHint(
        MasterBoardTerrain terrain, LegionClientSide legion,
        List<CreatureType> recruits, HintOracleInterface oracle,
        String[] section)
    {
        assert aihl != null : "No AIHintLoader available";
        return aihl.getRecruitHint(terrain, legion, recruits, oracle, section);
    }

    public synchronized static List<String> getInitialSplitHint(MasterHex hex)
    {
        String[] section = new String[1];
        section[0] = Constants.sectionAllAI;
        return getInitialSplitHint(hex, section);
    }

    public synchronized static List<String> getInitialSplitHint(MasterHex hex,
        String[] section)
    {
        if (aihl != null)
        {
            return aihl.getInitialSplitHint(hex.getLabel(), section);
        }
        return null;
    }

    public synchronized static int getHintedRecruitmentValueOffset(String name)
    {
        String[] section = new String[1];
        section[0] = Constants.sectionAllAI;
        return getHintedRecruitmentValueOffset(name, section);
    }

    public synchronized static int getHintedRecruitmentValueOffset(
        String name, String[] section)
    {
        return aihl.getHintedRecruitmentValueOffset(name, section);
    }

    /** get maximum number of players in that variant */
    public static int getMaxPlayers()
    {
        return maxPlayers;
    }

    /**
     * Retrieves the currently loaded variant.
     * 
     * TODO this is a helper method to introduce the Variant objects into the code,
     * in the long run they should be passed around instead of being in a static
     * member here.
     */
    public static Variant getCurrentVariant()
    {
        return CURRENT_VARIANT;
    }
}
