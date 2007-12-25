package net.sf.colossus.server;


import java.io.File;
import java.io.FilenameFilter;


/**
 * Class ConfigFileFilter implements a FilenameFilter for cfg files.
 * @author David Ripton
 * @version $Id$
 */

public final class ConfigFileFilter extends javax.swing.filechooser.FileFilter implements FilenameFilter
{
    public static final String description = "Colossus options file";

    public boolean accept(File dir, String name)
    {
        if (name.endsWith(Constants.optionsExtension))
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
        if (f.isDirectory())
        {
            return true;
        }
        String name = f.getName();
        return accept(null, name);
    }

    public String getDescription()
    {
        return description;
    }
}
