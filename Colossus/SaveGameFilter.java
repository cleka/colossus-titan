import java.io.*;

/**
 * Class SaveGameFilter implements a FilenameFilter for savegames.
 * @author David Ripton
 * @version $Id$
 */

public final class SaveGameFilter extends javax.swing.filechooser.FileFilter
    implements FilenameFilter
{
    public static final String description = "savegames";


    public boolean accept(File dir, String name)
    {
        if (name.endsWith(Game.saveExtension))
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
        if (name.endsWith(Game.saveExtension))
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

