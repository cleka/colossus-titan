package net.sf.colossus.server;

import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.parser.VariantLoader;
import net.sf.colossus.parser.TerrainRecruitLoader;
import net.sf.colossus.parser.AIHintLoader;
import net.sf.colossus.client.HexMap;

import java.io.*;
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
    private static java.util.List dependUpon = null;
    private static boolean loadedVariant = false;
    private static int maxPlayers = Constants.DEFAULT_MAX_PLAYERS;
    private static AIHintLoader aihl = null;

    /**
     * Load a Colossus Variant by name.
     * @param variantName The name of the variant.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(String variantName)
    {
        return loadVariant(variantName + ".var", variantName);
    }

    /**
     * Load a Colossus Variant from the specified File
     * @param varFile The File to load as a Variant.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(java.io.File varFile)
    {
        String tempVarName = varFile.getName();
        String tempVarDirectory = varFile.getParentFile().getAbsolutePath();
        return loadVariant(tempVarName, tempVarDirectory);
    }

    /**
     * Load a Colossus Variant from the specified filename in the specified path.
     * @param tempVarName The name of the file holding the Variant definition.
     * @param tempVarDirectory The path to the directory holding the Variant.
     * @return A Document describing the variant.
     */
    public static Document loadVariant(String tempVarName, 
        String tempVarDirectory)
    {
        if (loadedVariant && variantName.equals(tempVarName) &&
            varDirectory.equals(tempVarDirectory))
        {
            return varREADME;
        }

        loadedVariant = false;

        Log.debug("Loading variant " + tempVarName +
                  ", data files in " + tempVarDirectory);
        try
        {
            /* Can't use getVarDirectoriesList yet ! */
            java.util.List directories = new java.util.ArrayList();
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
                while (vl.oneLine(data) >= 0) {}
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
                    Log.debug("Variant dependng upon " + dependUpon);
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
            loadTerrainsAndRecruits();
            loadHints();
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
                                        Constants.defaultDirName);
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

    public static java.util.List getVarDirectoriesList()
    {
        java.util.List directories = new java.util.ArrayList();
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

    public static java.util.List getVarDirectoriesList(String suffixPath)
    {
        java.util.List directories = getVarDirectoriesList();
        java.util.List suffixedDirs = new java.util.ArrayList();
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

    public static java.util.List getImagesDirectoriesList()
    {
        return getVarDirectoriesList(Constants.imagesDirName);
    }

    public static java.util.List getBattlelandsDirectoriesList()
    {
        return getVarDirectoriesList(Constants.battlelandsDirName);
    }

    /** TerrainRecruitLoader is needed by many classes, so load it
     *  immediately after loading the variant. */ 
    public synchronized static void loadTerrainsAndRecruits()
    {
        try
        {
            java.util.List directories = 
                VariantSupport.getVarDirectoriesList();
            InputStream terIS = ResourceLoader.getInputStream(
                VariantSupport.getRecruitName(), directories);
            if (terIS == null) 
            {
                throw new FileNotFoundException(
                    VariantSupport.getRecruitName());
            }
            TerrainRecruitLoader trl = new TerrainRecruitLoader(terIS);
            while (trl.oneTerrain() >= 0) {}

            /* now initialize the static bits of the Battlelands */
            HexMap.staticBattlelandsInit();
        }
        catch (Exception e) 
        {
            Log.error("Recruit-per-terrain loading failed : " + e);
            System.exit(1);
        }
        try
        {
            // initialize the static bits of the MasterBoard
            net.sf.colossus.client.MasterBoard.staticMasterboardInit();
        }
        catch (Exception e) 
        {
            Log.error("Masterboard loading failed : " + e);
            System.exit(1);
        }
    }

    public synchronized static void loadHints()
    {
        aihl = null;
        try
        {
            java.util.List directories = 
                VariantSupport.getVarDirectoriesList();
            InputStream aihlIS = ResourceLoader.getInputStream(
                VariantSupport.getHintName(), directories);
            if (aihlIS == null) 
            {
                throw new FileNotFoundException(
                    VariantSupport.getHintName());
            }
            aihl = new AIHintLoader(aihlIS);
            boolean done = false;
            int totalHints = 0;
            while (!done)
            {
                int result = aihl.oneHint();
                if (result < 0)
                {
                    done = true;
                }
                else
                {
                    totalHints += result;
                }
            }
            Log.debug("Found " + totalHints + " hints in " + VariantSupport.getHintName());
        }
        catch (Exception e) 
        {
            Log.error("Hints loading failed : " + e);
            System.exit(1);
        }
    }

    public synchronized static String getRecruitHint(
        char terrain,
        net.sf.colossus.client.LegionInfo legion,
        net.sf.colossus.server.HintOracleInterface oracle)
    {
        if (aihl != null)
        {
            return aihl.getRecruitHint(terrain,legion,oracle);
        }
        return null;
    }
    
    /** get maximum number of players in that variant */
    public static int getMaxPlayers()
    {
        return maxPlayers;
    }
}
