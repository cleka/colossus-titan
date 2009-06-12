package net.sf.colossus.server;


import java.io.File;
import java.io.FilenameFilter;

import net.sf.colossus.common.Constants;


/**
 * Class XMLSnapshotFilter implements a FilenameFilter for savegames.
 *
 * @author David Ripton
 */
public final class XMLSnapshotFilter extends
    javax.swing.filechooser.FileFilter implements FilenameFilter
{
    public static final String description = "snapshots";

    public boolean accept(File dir, String name)
    {
        if (name.endsWith(Constants.XML_EXTENSION))
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
