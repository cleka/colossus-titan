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
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import net.sf.colossus.common.Constants;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.util.ObjectCreationException;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.AllCreatureType;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.IVariantHint;
import net.sf.colossus.variant.IVariantInitializer;
import net.sf.colossus.variant.MasterBoard;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.xmlparser.CreatureLoader;
import net.sf.colossus.xmlparser.MainVarFileLoader;
import net.sf.colossus.xmlparser.StrategicMapLoader;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.xmlparser.TerrainRecruitLoader.NullTerrainRecruitLoader;


/**
 * Class VariantSupport hold the members and functions
 * required to support Variants in Colossus
 *
 * TODO this should probably move into the variant package sooner or later, possibly
 *      into the {@link Variant} class itself
 *
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
    private static String recruitsFileName = "";
    private static String hintName = "";
    private static List<String> lCreaturesName;
    private static Document varREADME = null;
    private static List<String> dependUpon = null;

    /** whether or not there is currently a valid variant loaded.
     *  TODO: perhaps superfluous - check CURRENT_VARIANT for null
     *  instead?
     */
    private static boolean loadedVariant = false;
    private static Variant CURRENT_VARIANT;

    private static int maxPlayers;
    private static IVariantHint aihl = null;
    private static Properties markerNames;

    /**
     * Remove all variant data, so that next variant loading attempt
     * is guaranteed to load it freshly (e.g. to get XML data from
     * remote server even if currently loaded was same name, but, well,
     * from local files).
     */
    public static void unloadVariant()
    {
        StaticResourceLoader.purgeImageCache();
        StaticResourceLoader.purgeFileCache();
        CURRENT_VARIANT = null;
        loadedVariant = false;
    }

    private static Map<String, String> rememberCustomDirs = new HashMap<String, String>();

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
     * @return The loaded variant.
     */
    public static Variant loadVariantByName(String variantName,
        boolean serverSide)
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

        Variant loadedVariant = loadVariant(variantName, variantFileName,
            variantDir, serverSide);
        return loadedVariant;
    }

    /**
     * Load a Colossus Variant from the specified File
     * @param varFile The File to load as a Variant, probably selected
     *        by user in a FileSelectionDialog, with full absolute path.
     * @param serverSide We're loading on a server.
     * @return The loaded variant.
     */

    public static Variant loadVariantByFile(File varFile, boolean serverSide)
    {
        String tempVarFilename = varFile.getName();
        String tempVarDirectory = varFile.getParentFile().getAbsolutePath();
        String tempVarName = null;
        try
        {
            tempVarName = getVariantNameFromFilename(tempVarFilename);
        }
        catch (Exception e)
        {
            LOGGER.severe("IllegalVariantFileName - unable to conclude "
                + "variant name from filename '" + tempVarFilename + "'!");
            return null;
        }

        // caller need to store that to options so that later a external
        // variant (where re-selected in combo box) can be loaded again:
        String tempFullPathFilename = varFile.getAbsolutePath();
        rememberFullPathFileForVariantName(tempVarName, tempFullPathFilename);

        Variant loadedVariant = loadVariant(tempVarName, tempVarFilename,
            tempVarDirectory, serverSide);
        return loadedVariant;
    }

    private static String getVariantNameFromFilename(String varFilename)
        throws Exception
    {
        String result = null;
        if (varFilename.endsWith(Constants.varEnd))
        {
            // We need the Variantname for loading a game with
            // remote players.
            result = varFilename.substring(0, varFilename.length()
                - Constants.varEnd.length());
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
            throw (new Exception("IllegalVariantFilenameException"));
        }
        return result;

    }

    /**
     * Try to load a Colossus Variant from the specified filename
     * in the specified path. If loading fails, inform user with a message
     * dialog and try to load Default variant instead. If that fails as well,
     * do a system.exit after another message dialog.
     *
     * Synchronized to avoid concurrent threads running into it at same
     * time (probably not possible so far, but if one day Public Server game
     * can run with one local human and several AIs (on user's computer)
     * this would become an issue.
     *
     * @param tempVarFilename The name of the file holding the Variant definition.
     * @param tempVarDirectory The path to the directory holding the Variant.
     * @param tempVariantName The actual plain name of the variant
     * @param serverSide We're loading on a server.
     * @return A variant object, perhaps newly created, perhaps re-used if same
     *         variant was used before.
     *
     * TODO right now variant name might sometimes be null, then we try a hack
     * to retrieve the variant name from the variant file name.
     */
    public static synchronized Variant loadVariant(String tempVariantName,
        String tempVarFilename, String tempVarDirectory, boolean serverSide)
    {
        if (tempVariantName == null)
        {
            LOGGER.severe("variantName must not be null!");
            Thread.dumpStack();
            return null;
        }

        Variant loadedVariant = null;
        try
        {
            loadedVariant = tryLoadVariant(tempVariantName, tempVarFilename,
                tempVarDirectory, serverSide);
        }
        catch (VariantLoadException vle)
        {
            String task = vle.getMessage();
            String message = "Trying to load variant '" + tempVariantName
                + "' failed " + "(task='" + task + "')."
                + "\nI will try to load variant 'Default' instead...";
            String title = "Variant loading failed!";
            LOGGER.warning(message);
            ErrorUtils.showExceptionDialog(null, message, title, false);

            try
            {
                loadedVariant = tryLoadVariant(Constants.defaultVarName,
                    Constants.defaultVARFile, Constants.defaultDirName,
                    serverSide);
            }
            catch (VariantLoadException vle2)
            {
                String task2 = vle2.getMessage();

                String message2 = "Trying to load Variant 'Default' failed "
                    + "as well (task='" + task2 + "').\nCaught exception: "
                    + vle.getCause().toString()
                    + "\n\nGiving up and exiting the application! ";
                String title2 = "Even loading of default variant failed!";

                LOGGER.severe(message2);
                ErrorUtils.showExceptionDialog(null, message2, title2, true);

                System.exit(1);
            }
        }
        return loadedVariant;
    }

    /**
     * This does the actual work for
     *   {@link #loadVariant(String, String, String, boolean)}
     * This here is private and should be called only from the synchronized
     * before-mentioned method.
     *
     * @param tempVariantName
     * @param tempVarFilename
     * @param tempVarDirectory
     * @param serverSide
     * @return A variant object, perhaps newly created,
     *         perhaps re-used if same variant was used before.
     */
    private static Variant tryLoadVariant(String tempVariantName,
        String tempVarFilename, String tempVarDirectory, boolean serverSide)
        throws VariantLoadException
    {
        if (loadedVariant && varFilename.equals(tempVarFilename)
            && varDirectory.equals(tempVarDirectory))
        {
            LOGGER.info("Same variant " + tempVariantName
                + ", returning just same again.");
            return CURRENT_VARIANT;
        }

        LOGGER.info("Loading variant " + tempVariantName + " freshly...");

        // As long as this is static, only server may do this, not the
        // local clients.
        // TODO What about the remote clients? Shouldn't they do it too?
        if (serverSide)
        {
            StaticResourceLoader.purgeImageCache();
            StaticResourceLoader.purgeFileCache();
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
            InputStream varIS = StaticResourceLoader.getInputStream(
                tempVarFilename, directories);
            if (varIS == null)
            {
                throw new FileNotFoundException(tempVarFilename);
            }
            else
            {
                MainVarFileLoader mvfLoader = new MainVarFileLoader(varIS);
                if (mvfLoader.getMaxPlayers() > 0)
                {
                    maxPlayers = mvfLoader.getMaxPlayers();
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

                mapName = mvfLoader.getMap();
                if (mapName == null)
                {
                    mapName = Constants.defaultMAPFile;
                }
                LOGGER.log(Level.FINEST, "Variant using MAP " + mapName);

                lCreaturesName = mvfLoader.getCre();
                for (String creaturesName : lCreaturesName)
                {
                    LOGGER.log(Level.FINEST, "Variant using CRE "
                        + creaturesName);
                }

                recruitsFileName = mvfLoader.getTer();
                if (recruitsFileName == null)
                {
                    recruitsFileName = Constants.defaultTERFile;
                }
                LOGGER.log(Level.FINEST, "Variant using TER "
                    + recruitsFileName);

                hintName = mvfLoader.getHintName();
                LOGGER.log(Level.FINEST, "Variant using hint " + hintName);
                dependUpon = mvfLoader.getDepends();
                LOGGER.log(Level.FINEST, "Variant depending upon "
                    + dependUpon);
            }
            directories = new ArrayList<String>();
            directories.add(tempVarDirectory);
            task = "getDocument README*";
            varREADME = StaticResourceLoader
                .getDocument("README", directories);

            /* OK, what is the proper order here ?
             * We should start with HazardTerrain & HazardHexside, but those
             * aren't in variant yet. They don't require anything else.
             * Then must comes the CreatureType. They are only natives to
             * HazardTerrain & HazardHexside, and don't need anything else.
             * Then we can load the terrains & recruits ; they need the
             * CreatureType.
             * Finally we can load the Battlelands, they need the terrain.
             */

            AllCreatureType creatureTypes = loadCreatures();

            IVariantInitializer trl = loadTerrainsAndRecruits(creatureTypes);
            // TODO add things as the variant package gets fleshed out

            List<String> directoriesForMap = getVarDirectoriesList();
            InputStream mapIS = StaticResourceLoader.getInputStream(
                VariantSupport.getMapName(), directoriesForMap);
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

            CURRENT_VARIANT = new Variant(trl, creatureTypes, masterBoard,
                varREADME, variantName);
            loadedVariant = true;
            loadHints(CURRENT_VARIANT);
            task = "loadMarkerNamesProperties";
            markerNames = loadMarkerNamesProperties();
        }
        catch (Exception e)
        {
            VariantLoadException vle = new VariantLoadException(task, e);
            CURRENT_VARIANT = null;
            throw vle;
        }

        return CURRENT_VARIANT;
    }

    /**
     * A helper class to store the exception that happened during
     * VariantLoading together with the task during which that happened.
     */
    private static class VariantLoadException extends Exception
    {
        public VariantLoadException(String message, Throwable e)
        {
            super(message, e);
        }
    }

    /** Call immediately after loading variant, before using creatures. */
    public static AllCreatureType loadCreatures()
    {
        CreatureLoader creatureLoader = new CreatureLoader();
        try
        {
            List<String> directories = VariantSupport.getVarDirectoriesList();
            for (String creaturesName : VariantSupport.getCreaturesNames())
            {
                InputStream creIS = StaticResourceLoader.getInputStream(
                    creaturesName, directories);
                if (creIS == null)
                {
                    throw new FileNotFoundException(creaturesName);
                }
                creatureLoader.fillCreatureLoader(creIS, directories);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load Creatures definition",
                e);
        }
        return creatureLoader;
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
        txtdoc
            .putProperty(StaticResourceLoader.KEY_CONTENT_TYPE, "text/plain");
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
            suffixedDirs.add(dir + StaticResourceLoader.getPathSeparator()
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

    public synchronized static IVariantInitializer loadTerrainsAndRecruits(
        AllCreatureType creatureTypes)
    {
        // remove all old stuff in the custom recruitments system
        CustomRecruitBase.reset();

        IVariantInitializer terrainRecruitLoader = new NullTerrainRecruitLoader(
            true);

        try
        {
            List<String> directories = getVarDirectoriesList();
            InputStream terIS = StaticResourceLoader.getInputStream(
                recruitsFileName, directories);
            if (terIS == null)
            {
                throw new FileNotFoundException(recruitsFileName);
            }
            // TODO parsing into static fields is a side effect of this
            // constructor - that's somehow not the right way...

            // Clemens: started working on that.
            //  =>  partly now done via the IVariantInitializer
            terrainRecruitLoader = new TerrainRecruitLoader(terIS,
                creatureTypes);
        }
        catch (Exception e)
        {
            // TODO another exception anti-pattern: calling System.exit which means
            // no one can escape the disappearing VM, even if they would know how
            LOGGER.log(Level.SEVERE, "Recruit-per-terrain loading failed.", e);
            System.exit(1);
        }
        return terrainRecruitLoader;
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
                InputStream mmfIS = StaticResourceLoader
                    .getInputStreamIgnoreFail(Constants.markersNameFile,
                        singleDirectory);
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

    private synchronized static void loadHints(Variant variant)
    {
        aihl = null;
        Object o = null;
        if (hintName != null)
        {
            try
            {
                o = StaticResourceLoader.getNewObject(hintName,
                    getVarDirectoriesList(), new Object[] { variant });
            }
            catch (ObjectCreationException e)
            {
                // ignore here, the o == null case is covered below
            }
        }
        if ((o != null) && (o instanceof IVariantHint))
        {
            aihl = (IVariantHint)o;
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
                loadHints(variant);
            }
        }
    }

    public synchronized static CreatureType getRecruitHint(
        MasterBoardTerrain terrain, IOracleLegion legion,
        List<CreatureType> recruits, IHintOracle oracle)
    {
        return getRecruitHint(terrain, legion, recruits, oracle,
            Collections.singletonList(IVariantHint.AIStyle.Any));
    }

    public synchronized static CreatureType getRecruitHint(
        MasterBoardTerrain terrain, IOracleLegion legion,
        List<CreatureType> recruits, IHintOracle oracle,
        List<IVariantHint.AIStyle> aiStyles)
    {
        assert aihl != null : "No AIHintLoader available";
        return aihl
            .getRecruitHint(terrain, legion, recruits, oracle, aiStyles);
    }

    public synchronized static List<CreatureType> getInitialSplitHint(
        MasterHex hex)
    {
        return getInitialSplitHint(hex,
            Collections.singletonList(IVariantHint.AIStyle.Any));
    }

    public synchronized static List<CreatureType> getInitialSplitHint(
        MasterHex hex, List<IVariantHint.AIStyle> aiStyles)
    {
        if (aihl != null)
        {
            return aihl.getInitialSplitHint(hex, aiStyles);
        }
        return null;
    }

    public synchronized static int getHintedRecruitmentValueOffset(
        CreatureType creature)
    {
        return getHintedRecruitmentValueOffset(creature,
            Collections.singletonList(IVariantHint.AIStyle.Any));
    }

    public synchronized static int getHintedRecruitmentValueOffset(
        CreatureType creature, List<IVariantHint.AIStyle> aiStyles)
    {
        return aihl.getHintedRecruitmentValueOffset(creature, aiStyles);
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
