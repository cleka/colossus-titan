package net.sf.colossus.server;


import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

import net.sf.colossus.common.Constants;


/**
 * A FilenameFilter for cfg files.
 *
 * @author David Ripton
 */
public final class ConfigFileFilter extends FileFilter implements
    FilenameFilter
{
    public static final String description = "Colossus options file";

    public boolean accept(File dir, String name)
    {
        if (name.endsWith(Constants.OPTIONS_EXTENSION))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean accept(File f)
    {
        if (f.isDirectory())
        {
            return true;
        }
        String name = f.getName();
        return accept(null, name);
    }

    @Override
    public String getDescription()
    {
        return description;
    }
}
