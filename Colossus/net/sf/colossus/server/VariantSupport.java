package net.sf.colossus.server;


import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.parser.VariantLoader;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.parser.AIHintLoader;
import net.sf.colossus.client.HexMap;

import java.io.*;
import java.util.List;
import java.util.ListIterator;

import javax.swing.text.*;


/**
 * Class VariantSupport hold the members and functions required to support Variants in Colossus
 * @version $Id$
 * @author Romain Dolbeau
 */

public final class VariantSupport
{
    private static String varDirectory = "";
    private static String variantName = "";
    private static String mapName = "";
    private static String recruitName = "";
    private static String hintName = "";
    private static String creaturesName = "";
    private static Document varREADME = null;
    private static List dependUpon = null;
    private static boolean loadedVariant = false;
    private static int maxPlayers = Constants.DEFAULT_MAX_PLAYERS;
    private static HintInterface aihl = null;
    private static java.util.Properties markerNames;

    /**
     * Clean-up the ResourceLoader caches to make room for a variant.
     * @param variantName Name of the soon-to-be-loaded variant.
     */
    public static void freshenVariant(String variantName)
    {
        freshenVariant(variantName + ".var", variantName);
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
     * @param tempVarName The name of the file holding the soon-to-be-loaded Variant definition.
     * @param tempVarDirectory The path to the directory holding the soon-to-be-loaded Variant.
     */
    public static void freshenVariant(String tempVarName,
            String tempVarDirectory)
    {
        if (!(loadedVariant && variantName.equals(tempVarName) &&
                varDirectory.equals(tempVarDirectory)))
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
    public static Document loadVariant(String variantName,
            boolean serverSide)
    {
        return loadVariant(variantName + ".var", variantName,
                serverSide);
    }

    /**
     * Load a Colossus Variant from the specified File
     * @param varFile The File to load as a Variant.
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(java.io.File varFile,
            boolean serverSide)
    {
        String tempVarName = varFile.getName();
        String tempVarDirectory = varFile.getParentFile().getAbsolutePath();
        return loadVariant(tempVarName, tempVarDirectory, serverSide);
    }

    /**
     * Load a Colossus Variant from the specified filename in the specified path.
     * @param tempVarName The name of the file holding the Variant definition.
     * @param tempVarDirectory The path to the directory holding the Variant.
     * @param serverSide We're loading on a server.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(String tempVarName,
            String tempVarDirectory,
            boolean serverSide)
    {
        if (loadedVariant && variantName.equals(tempVarName) &&
                varDirectory.equals(tempVarDirectory))
        {
            return varREADME;
        }

        if (serverSide)
        {
            ResourceLoader.purgeImageCache();
            ResourceLoader.purgeFileCache();
        }

        loadedVariant = false;

        Log.debug("Loading variant " + tempVarName +
                ", data files in " + tempVarDirectory);
        try
        {

            /* Can't use getVarDirectoriesList yet ! */
            List directories = new java.util.ArrayList();
            directories.add(tempVarDirectory);
            directories.add(Constants.defaultDirName);
            InputStream varIS = ResourceLoader.getInputStream(
                    tempVarName,
                    directories);
            if (varIS == null)
            {
                throw new FileNotFoundException(tempVarName);
            }
            else
            {
                VariantLoader vl = new VariantLoader(varIS);
                String[] data = new String[VariantLoader.TOTAL_INDEX];
                data[0] = data[1] = data[2] = data[3] = null;
                while (vl.oneLine(data) >= 0)
                {
                }
                if (vl.maxPlayers > 0)
                {
                    maxPlayers = vl.maxPlayers;
                }
                else
                {
                    maxPlayers = Constants.DEFAULT_MAX_PLAYERS;
                }
                if (maxPlayers > Constants.MAX_MAX_PLAYERS)
                {
                    Log.error("Can't use more than " +
                            Constants.MAX_MAX_PLAYERS +
                            " players, while variant requires " +
                            maxPlayers);
                    maxPlayers = Constants.MAX_MAX_PLAYERS;
                }
                varDirectory = tempVarDirectory;
                variantName = tempVarName;
                if (data[VariantLoader.MAP_INDEX] != null)
                {
                    mapName = data[VariantLoader.MAP_INDEX];
                }
                else
                {
                    mapName = Constants.defaultMAPFile;
                }
                Log.debug("Variant using MAP " + mapName);
                if (data[VariantLoader.CRE_INDEX] != null)
                {
                    creaturesName = data[VariantLoader.CRE_INDEX];
                }
                else
                {
                    creaturesName = Constants.defaultCREFile;
                }
                Log.debug("Variant using CRE " + creaturesName);
                if (data[VariantLoader.TER_INDEX] != null)
                {
                    recruitName = data[VariantLoader.TER_INDEX];
                }
                else
                {
                    recruitName = Constants.defaultTERFile;
                }
                if (data[VariantLoader.HINT_INDEX] != null)
                {
                    hintName = data[VariantLoader.HINT_INDEX];
                }
                else
                {
                    hintName = Constants.defaultHINTFile;
                }
                Log.debug("Variant using TER " + recruitName);
                if (data[VariantLoader.DEPEND_INDEX] != null)
                {
                    dependUpon = Split.split(',',
                            data[VariantLoader.DEPEND_INDEX]);
                    Log.debug("Variant depending upon " + dependUpon);
                }
                else
                {
                    dependUpon = null;
                }
            }
            directories = new java.util.ArrayList();
            directories.add(tempVarDirectory);
            varREADME = ResourceLoader.getDocument("README", directories);
        }
        catch (Exception e)
        {
            Log.error("Variant loading failed : " + e);
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
            Creature.loadCreatures();
            loadTerrainsAndRecruits(serverSide);
            loadHints();
            markerNames = loadMarkerNamesProperties();
        }
        else
        {
            if (tempVarName.equals(Constants.defaultVARFile))
            {
                Log.error("Default Variant Loading Failed, aborting !");
                System.exit(1);
            }
            else
            {
                Log.debug("Trying to load Default instead...");
                varREADME = loadVariant(Constants.defaultVARFile,
                        Constants.defaultDirName,
                        serverSide);
            }
        }

        return varREADME;
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

    public static String getHintName()
    {
        return hintName;
    }

    public static String getCreaturesName()
    {
        return creaturesName;
    }

    public static List getVarDirectoriesList()
    {
        List directories = new java.util.ArrayList();
        if (!(varDirectory.equals(Constants.defaultDirName)))
        {
            directories.add(varDirectory);
        }
        if (dependUpon != null)
        {
            java.util.Iterator it = dependUpon.iterator();
            while (it.hasNext())
            {
                directories.add((String)it.next());
            }
        }
        directories.add(Constants.defaultDirName);
        return directories;
    }

    public static List getVarDirectoriesList(String suffixPath)
    {
        List directories = getVarDirectoriesList();
        List suffixedDirs = new java.util.ArrayList();
        java.util.Iterator it = directories.iterator();
        while (it.hasNext())
        {
            String dir = (String)it.next();
            suffixedDirs.add(dir +
                    ResourceLoader.getPathSeparator() +
                    suffixPath);
        }
        return suffixedDirs;
    }

    public static List getImagesDirectoriesList()
    {
        return getVarDirectoriesList(Constants.imagesDirName);
    }

    public static List getBattlelandsDirectoriesList()
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
            List directories = getVarDirectoriesList();
            InputStream terIS = ResourceLoader.getInputStream(
                    getRecruitName(), directories);
            if (terIS == null)
            {
                throw new FileNotFoundException(getRecruitName());
            }
            TerrainRecruitLoader trl = new TerrainRecruitLoader(terIS);

            /* now initialize the static bits of the Battlelands */
            HexMap.staticBattlelandsInit(serverSide);
        }
        catch (Exception e)
        {
            Log.error("Recruit-per-terrain loading failed : " + e);
            System.exit(1);
        }
        //try
        {
            // initialize the static bits of the MasterBoard
            net.sf.colossus.client.MasterBoard.staticMasterboardInit();
        }
        //catch (Exception e) 
        //{
        //    Log.error("Masterboard loading failed : " + e);
        //    System.exit(1);
        //}
    }

    private static java.util.Properties loadMarkerNamesProperties()
    {
        java.util.Properties allNames = new java.util.Properties();
        List directories = getVarDirectoriesList();

        /* unlike other, don't use file-level granularity ; 
         load all files in order, so that we get the
         default mapping at the end */
        ListIterator it = directories.listIterator(directories.size());
        while (it.hasPrevious())
        {
            List singleDirectory = new java.util.ArrayList();
            singleDirectory.add(it.previous());
            try
            {
                InputStream mmfIS =
                        ResourceLoader.getInputStream(Constants.markersNameFile,
                        singleDirectory);
                if (mmfIS != null)
                {
                    allNames.load(mmfIS);
                }
            }
            catch (Exception e)
            {
                Log.warn("Markers name loading partially failed.");
            }
        }
        return allNames;
    }

    public static java.util.Properties getMarkerNamesProperties()
    {
        return markerNames;
    }

    public synchronized static void loadHints()
    {
        aihl = null;
        if (getHintName().endsWith(".hin"))
        { // first if it's a .hin, use the AIHintLoader on one or more .hin files.
            try
            {
                List directories = getVarDirectoriesList();
                List allHintsFiles = net.sf.colossus.util.Split.split(",",
                        getHintName());
                java.util.Iterator it = allHintsFiles.iterator();
                while (it.hasNext())
                {
                    String currHintsFileName = (String)it.next();
                    InputStream aihlIS = ResourceLoader.getInputStream(
                            currHintsFileName,
                            directories);
                    if (aihlIS == null)
                    {
                        throw new FileNotFoundException(currHintsFileName);
                    }
                    if (aihl == null)
                    {
                        aihl = new AIHintLoader(aihlIS);
                    }
                    else
                    {
                        ((AIHintLoader)aihl).ReInit(aihlIS);
                    }
                    boolean done = false;
                    int totalHints = 0;
                    while (!done)
                    {
                        int result = ((AIHintLoader)aihl).oneHint();
                        if (result < 0)
                        {
                            done = true;
                        }
                        else
                        {
                            totalHints += result;
                        }
                    }
                    Log.debug("Found " + totalHints + " hints in " +
                            currHintsFileName);
                }
            }
            catch (Exception e)
            {
                Log.error("Hints loading failed : " + e);
                System.exit(1);
            }
        }
        else
        {
            // let's assume this is a class name, implementing HintInterface
            Object o = ResourceLoader.getNewObject(getHintName(),
                    getVarDirectoriesList());
            if ((o != null) && (o instanceof HintInterface))
            {
                aihl = (HintInterface)o;
                Log.debug("Using class " + getHintName() +
                        " to supply hints to the AIs.");
            }
            else
            {
                aihl = null;
            }
        }
        if (aihl == null)
        {
            if (getHintName().equals(Constants.defaultHINTFile))
            {
                Log.error("Couldn't load default hints !");
                System.exit(1);
            }
            Log.warn("Couldn't load hints. Trying with Default.");
            hintName = Constants.defaultHINTFile;
            loadHints();
        }
    }

    public synchronized static String getRecruitHint(
            String terrain,
            net.sf.colossus.client.LegionInfo legion,
            List recruits,
            net.sf.colossus.server.HintOracleInterface oracle)
    {
        String[] section = new String[1];
        section[0] = AIHintLoader.sectionAllAI;
        return getRecruitHint(terrain, legion, recruits, oracle, section);
    }

    public synchronized static String getRecruitHint(
            String terrain,
            net.sf.colossus.client.LegionInfo legion,
            List recruits,
            net.sf.colossus.server.HintOracleInterface oracle,
            String[] section)
    {
        if (aihl != null)
        {
            return aihl.getRecruitHint(terrain, legion, recruits, oracle,
                    section);
        }
        else
        {
            Log.error("Huh, no AIHintLoader available ! This should never happen.");
        }
        return null;
    }

    public synchronized static List getInitialSplitHint(String label)
    {
        String[] section = new String[1];
        section[0] = AIHintLoader.sectionAllAI;
        return getInitialSplitHint(label, section);
    }

    public synchronized static List getInitialSplitHint(String label,
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
        section[0] = AIHintLoader.sectionAllAI;
        return getHintedRecruitmentValueOffset(name, section);
    }

    public synchronized static int getHintedRecruitmentValueOffset(String name,
            String[] section)
    {
        return aihl.getHintedRecruitmentValueOffset(name, section);
    }

    /** get maximum number of players in that variant */
    public static int getMaxPlayers()
    {
        return maxPlayers;
    }
}
