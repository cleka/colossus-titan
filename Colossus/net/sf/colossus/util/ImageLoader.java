package net.sf.colossus.util;

import javax.swing.*;
import java.awt.*;

/**
 * Class ImageLoader is an utlity class to load an Image from a filename and a list of directory.
 * @version $Id$
 * @author Romain Dolbeau
 * @author David Ripton
 */

public final class ImageLoader
{
    // File.separator does not work right in jar files.  A hardcoded
    // forward-slash does, and works in *x and Windows.  So I'm ignoring
    // JavaPureCheck's opinion and using a forward slash.
    // XXX Is there a way to detect whether a program is running from a
    // jar file?
    private static final String pathSeparator = "/";
    private static final String imageExtension = ".gif";

    /*
     * Give the String to mark directories.
     * @return The String to mark directories.
     */
    public static String getPathSeparator()
    {
        return pathSeparator;
    }

    /**
     * Return the first Image of name filename in the list of directories.
     * @param filename Name of the Image file to load (without extension).
     * @param directories List of directories to search (in order).
     * @return The Image, or null if it was not found
     */
    public static Image getImage(String filename, java.util.List directories)
    {
        Image image = null;
        java.util.Iterator it = directories.iterator();
        while (it.hasNext() && (image == null))
        {
            Object o = it.next();
            if (o instanceof String)
            {
                String path = (String)o;
                image = tryLoadFromFile(filename +
                                        imageExtension, path);
                if (image == null)
                {
                    ImageIcon temp = tryLoadFromResource(filename +
                                                         imageExtension, path);
                    if (temp != null)
                    {
                        image = temp.getImage();
                    }
                }
            }
        }
        return(image);
    }

    /**
     * Return the first ImageIcon of name filename in the list of directories.
     * @param filename Name of the ImageIcon file to load (without extension).
     * @param directories List of directories to search (in order).
     * @return The ImageIcon, or null if it was not found
     */
    public static ImageIcon getImageIcon(String filename, java.util.List directories)
    {
        ImageIcon icon = null;
        java.util.Iterator it = directories.iterator();
        while (it.hasNext() && (icon == null))
        {
            Object o = it.next();
            if (o instanceof String)
            {
                String path = (String)o;
                Image temp  = tryLoadFromFile(filename +
                                              imageExtension, path);
                if (temp == null)
                {
                    icon = tryLoadFromResource(filename +
                                               imageExtension, path);
                }
                else
                {
                    icon = new ImageIcon(temp);
                }
            }
        }
        return(icon);
    }

    private static Image tryLoadFromFile(String filename, String path)
    {
        Image image = null;
        try
        {
            java.net.URL url;
            url = new java.net.URL("file:" +
                          path +
                          pathSeparator +
                          filename);
            // url will not be null even is the file doesn't exist,
            // so we need to check if connection can be opened
            if ((url != null) && (url.openStream() != null))
            {
                image = Toolkit.getDefaultToolkit().getImage(url);
            }
        }
        catch (Exception e)
        {
            // nothing to do
        }
        return image;
    }

    private static ImageIcon tryLoadFromResource(String filename, String path)
    {
        ImageIcon icon = null;
        try
        {
            java.net.URL url;
            java.lang.ClassLoader cl =
                java.lang.ClassLoader.getSystemClassLoader();
            url = cl.getResource(path +
                                 pathSeparator +
                                 filename);
            // url will not be null even is the file doesn't exist,
            // so we need to check if connection can be opened
            if ((url != null) && (url.openStream() != null))
            {
                icon = new ImageIcon(url);
            }
            
        }
        catch (Exception e)
        {
            // nothing to do
        }
        return icon;
    }
}
