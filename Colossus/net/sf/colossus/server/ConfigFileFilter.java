package net.sf.colossus.server;


import java.io.*;


/**
 * Class ConfigFileFilter implements a FilenameFilter for cfg files.
 * @author David Ripton
 * @version $Id$
 */

public final class ConfigFileFilter extends javax.swing.filechooser.FileFilter
    implements FilenameFilter
{
    public static final String description = "config file";


    public boolean accept(File dir, String name)
    {
        if (name.endsWith(Constants.configExtension))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    public boolean accept(File f)
    {
        String name = f.getName();
        if (name.endsWith(Constants.configExtension))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    public String getDescription()
    {
        return description;
    }
}
