package net.sf.colossus.server;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import net.sf.colossus.client.HexMap;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.variant.BattleLand;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoard;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.xmlparser.CreatureLoader;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.xmlparser.VariantLoader;


/**
 * Class VariantSupport hold the members and functions 
 * required to support Variants in Colossus
 * @version $Id$
 * @author Romain Dolbeau
 */

public final class VariantSupport
{
    private static final Logger LOGGER = Logger.getLogger(VariantSupport.class
        .getName());

    private static String varDirectory = "";
    private static String variantName = "";
    private static String mapName = "";
    private static String recruitName = "";
    private static String hintName = "";
    private static String creaturesName = "";
    private static Document varREADME = null;
    private static List<String> dependUpon = null;
    private static boolean loadedVariant = false;
    private static int maxPlayers;
    private static HintInterface aihl = null;
    private static Properties markerNames;

    private static Variant CURRENT_VARIANT;

    /**
     * Clean-up the ResourceLoader caches to make room for a variant.
     * @param variantName Name of the soon-to-be-loaded variant.
     */
    public static void freshenVariant(String variantName)
    {
        freshenVariant(variantName + Constants.varEnd, variantName);
    }

    /**
     * Clean-up the ResourceLoader caches to make room for a variant.
     * @param varFile Soon-to-be-loaded variant File.
     */
    public static void freshenVariant(java.io.File varFile)
    {
        String tempVarName = varFile.getName();
        String tempVarDirectory = varFile.getParentFile().getAbsolutePath();
        freshenVariant(tempVarName, tempVarDirectory);
    }

    /**
     * Clean-up the ResourceLoader caches to make room for a variant.
     * @param tempVarName The name of the file holding the
     *     soon-to-be-loaded Variant definition.
     * @param tempVarDirectory The path to the directory holding the
     *     soon-to-be-loaded Variant.
     */
    public static void freshenVariant(String tempVarName,
        String tempVarDirectory)
    {
        if (!(loadedVariant && variantName.equals(tempVarName) && varDirectory
            .equals(tempVarDirectory)))
        {
            ResourceLoader.purgeImageCache();
            ResourceLoader.purgeFileCache();
        }
    }

    /**
     * Load a Colossus Variant by name.
     * @param variantName The name of the variant.
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(String variantName, boolean serverSide)
    {
        return loadVariant(variantName + Constants.varEnd, Constants.varPath
            + variantName, serverSide);
    }

    /**
     * Load a Colossus Variant from the specified File
     * @param varFile The File to load as a Variant.
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(java.io.File varFile, boolean serverSide)
    {
        String tempVarName = varFile.getName();
        String tempVarDirectory = varFile.getParentFile().getAbsolutePath();
        return loadVariant(tempVarName, tempVarDirectory, serverSide);
    }

    /**
     * Load a Colossus Variant from the specified filename
     *   in the specified path.
     * @param tempVarName The name of the file holding the Variant definition.
     * @param tempVarDirectory The path to the directory holding the Variant.
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(String tempVarName,
        String tempVarDirectory, boolean serverSide)
    {
        if (loadedVariant && variantName.equals(tempVarName)
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

        LOGGER.log(Level.FINEST, "Loading variant " + tempVarName
            + ", data files in " + tempVarDirectory);
        try
        {

            /* Can't use getVarDirectoriesList yet ! */
            List<String> directories = new ArrayList<String>();
            directories.add(tempVarDirectory);
            directories.add(Constants.defaultDirName);
            InputStream varIS = ResourceLoader.getInputStream(tempVarName,
                directories);
            if (varIS == null)
            {
                throw new FileNotFoundException(tempVarName);
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
                variantName = tempVarName;

                mapName = vl.getMap();
                if (mapName == null)
                {
                    mapName = Constants.defaultMAPFile;
                }
                LOGGER.log(Level.FINEST, "Variant using MAP " + mapName);

                creaturesName = vl.getCre();
                if (creaturesName == null)
                {
                    creaturesName = Constants.defaultCREFile;
                }
                LOGGER.log(Level.FINEST, "Variant using CRE " + creaturesName);

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
            varREADME = ResourceLoader.getDocument("README", directories);

            // varREADME seems to be used as flag for a successfully loaded
            // variant, but breaking the whole variant loading just because
            // there is no readme file seems a bit overkill, thus we set
            // a default in this case
            if (varREADME == null)
            {
                varREADME = getMissingReadmeNotification();
            }
        }
        catch (Exception e)
        {
            // TODO this seems like a classic case of fail-slow, rethrowing the exception
            // might be better
            LOGGER.log(Level.SEVERE, "Variant loading failed : " + e, e);
            varDirectory = Constants.defaultDirName;
            variantName = Constants.defaultVARFile;
            mapName = Constants.defaultMAPFile;
            recruitName = Constants.defaultTERFile;
            hintName = Constants.defaultHINTFile;
            creaturesName = Constants.defaultCREFile;
            maxPlayers = Constants.DEFAULT_MAX_PLAYERS;
            varREADME = null;
        }

        if (varREADME != null)
        {
            loadedVariant = true;
            loadTerrainsAndRecruits(serverSide);
            loadHints();
            markerNames = loadMarkerNamesProperties();

            // TODO add things as the variant package gets fleshed out
            List<CreatureType> creatureTypes = loadCreatures();
            List<BattleLand> battleLands = new ArrayList<BattleLand>();
            MasterBoard masterBoard = null;
            CURRENT_VARIANT = new Variant(creatureTypes, battleLands,
                masterBoard);
        }
        else
        {
            if (tempVarName.equals(Constants.defaultVARFile))
            {
                LOGGER.log(Level.SEVERE,
                    "Default Variant Loading Failed, aborting !");
                System.exit(1);
            }
            else
            {
                LOGGER.log(Level.FINEST, "Trying to load Default instead...");
                varREADME = loadVariant(Constants.defaultVARFile,
                    Constants.defaultDirName, serverSide);
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
            InputStream creIS = ResourceLoader.getInputStream(VariantSupport
                .getCreaturesName(), directories);
            if (creIS == null)
            {
                throw new FileNotFoundException(VariantSupport
                    .getCreaturesName());
            }
            CreatureLoader creatureLoader = new CreatureLoader(creIS);
            creatures.addAll(creatureLoader.getCreatures());
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

    public static String getVarName()
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

    public static String getCreaturesName()
    {
        return creaturesName;
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
            LOGGER.log(Level.SEVERE, "Recruit-per-terrain loading failed.", e);
            System.exit(1);
        }
        // initialize the static bits of the MasterBoard
        net.sf.colossus.client.MasterBoard.staticMasterboardInit();
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

    public synchronized static String getRecruitHint(String terrain,
        net.sf.colossus.client.LegionInfo legion, List<CreatureType> recruits,
        net.sf.colossus.server.HintOracleInterface oracle)
    {
        String[] section = new String[1];
        section[0] = Constants.sectionAllAI;
        return getRecruitHint(terrain, legion, recruits, oracle, section);
    }

    public synchronized static String getRecruitHint(String terrain,
        net.sf.colossus.client.LegionInfo legion, List<CreatureType> recruits,
        net.sf.colossus.server.HintOracleInterface oracle, String[] section)
    {
        if (aihl != null)
        {
            return aihl.getRecruitHint(terrain, legion, recruits, oracle,
                section);
        }
        else
        {
            LOGGER.log(Level.SEVERE,
                "No AIHintLoader available ! Should never happen.");
        }
        return null;
    }

    public synchronized static List<String> getInitialSplitHint(String label)
    {
        String[] section = new String[1];
        section[0] = Constants.sectionAllAI;
        return getInitialSplitHint(label, section);
    }

    public synchronized static List<String> getInitialSplitHint(String label,
        String[] section)
    {
        if (aihl != null)
        {
            return aihl.getInitialSplitHint(label, section);
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
