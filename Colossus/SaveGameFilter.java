import java.io.*;


/**
 * Class SaveGameFilter is a FilenameFilter for *.sav
 * @version $Id$ 
 * @author David Ripton
 */


public class SaveGameFilter implements FilenameFilter
{
    public boolean accept (File dir, String name)
    {
        if (name.endsWith(".sav"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
