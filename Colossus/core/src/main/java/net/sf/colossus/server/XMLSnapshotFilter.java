package net.sf.colossus.server;


import java.io.File;
import java.io.FilenameFilter;


/**
 * Class XMLSnapshotFilter implements a FilenameFilter for savegames.
 * @author David Ripton
 * @version $Id$
 */

public final class XMLSnapshotFilter extends javax.swing.filechooser.FileFilter implements FilenameFilter
{
    public static final String description = "snapshots";

    public boolean accept(File dir, String name)
    {
        if (name.endsWith(Constants.xmlExtension))
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

