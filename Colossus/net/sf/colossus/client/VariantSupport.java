package net.sf.colossus.client;

import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;
import net.sf.colossus.parser.VariantLoader;
import java.io.*;

/**
 * Class VariantSupport hold the members and functions required to support Variant in Colossus
 * @version $Id$
 * @author Romain Dolbeau
 */

public final class VariantSupport
{
    private static String varDirectory = Constants.defaultDirName;
    private static String variantName = Constants.defaultVARFile;
    private static String mapName = Constants.defaultMAPFile;
    private static String recruitName = Constants.defaultTERFile;
    private static String creaturesName = Constants.defaultCREFile;
    private static java.util.List dependUpon = null;

    /**
     * Load a Colossus Variant from the specified File
     * @param varFile The File to load as a Variant.
     */
    public static void loadVariant(java.io.File varFile)
    {
        String tempVarName = varFile.getName();
        String tempVarDirectory = varFile.getParentFile().getAbsolutePath();
        loadVariant(tempVarName, tempVarDirectory);
    }
    /**
     * Load a Colossus Variant from the specified filename in the specified path.
     * @param tempVarName The name of the file holding the Variant definition.
     * @param tempVarDirectory The path to the directory holding the Variant.
     */
    public static void loadVariant(String tempVarName, String tempVarDirectory)
    {
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
                String[] data = new String[4];
                data[0] = data[1] = data[2] = data[3] = null;
                while (vl.oneLine(data) >= 0) {}
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
                Log.debug("Variant using TER " + recruitName);
                if (data[VariantLoader.DEPEND_INDEX] != null)
                {
                    dependUpon = Split.split('.', data[VariantLoader.DEPEND_INDEX]);
                    Log.debug("Variant dependng upon " + dependUpon);
                }
                else
                {
                    dependUpon = null;
                }
            }
        }
        catch (Exception e) 
        {
            System.out.println("Variant loading failed : " + e);
            varDirectory = Constants.defaultDirName;
            variantName = Constants.defaultVARFile;
            mapName = Constants.defaultMAPFile;
            recruitName = Constants.defaultTERFile;
            creaturesName = Constants.defaultCREFile;
        }
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
}
