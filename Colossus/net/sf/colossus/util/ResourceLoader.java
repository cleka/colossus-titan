package net.sf.colossus.util;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * Class ResourceLoader is an utility class to load a resource from a filename and a list of directory.
 * @version $Id$
 * @author Romain Dolbeau
 * @author David Ripton
 */

public final class ResourceLoader
{
    // File.separator does not work in jar files, except in Unix.
    // A hardcoded '/' works in Unix, Windows, MacOS X, and jar files.
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
     * @return The Image, or null if it was not found.
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
                image = tryLoadImageFromFile(filename +
                                        imageExtension, path);
                if (image == null)
                {
                    ImageIcon temp = tryLoadImageIconFromResource(filename +
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
     * @return The ImageIcon, or null if it was not found.
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
                Image temp  = tryLoadImageFromFile(filename +
                                              imageExtension, path);
                if (temp == null)
                {
                    icon = tryLoadImageIconFromResource(filename +
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

    private static Image tryLoadImageFromFile(String filename, String path)
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

    private static ImageIcon tryLoadImageIconFromResource(String filename, String path)
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

    // XXX Not used yet, because I haven't found a method that takes a URL
    // and returns an InputStream that works correctly over JWS.
    // "new ImageIcon(URL)" above works.  Unfortunately, ImageIcon passes the
    // URL to Toolkit.getImage(), which is native, so I can't see exactly how.
    // I really don't want to use javax.jnlp code if we can help it.
    // Maybe make a JarURLConnection and then call getInputStream()?
    // Or we can put the variant text files on the web site and stream them
    // to JWS clients.  Sounds wasteful, but we'll need to do that anyway
    // to keep variants in sync for net games.
    // Or we can bury them in an image file using Steganography.  :->
    private static InputStream tryLoadInputStreamFromResource(String filename, String path)
    {
        InputStream stream = null;
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
                // The method below does not exist.
                // stream = cl.getResourceAsStream(url);
            }
        }
        catch (Exception e)
        {
            Log.error("ResourceLoader caught " + e);
        }
        return stream;
    }

    /**
     * Return the first InputStream from file of name filename in the list of directories.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStream(String filename, java.util.List directories)
    {
        InputStream stream = null;
        java.lang.ClassLoader cl =
            java.lang.ClassLoader.getSystemClassLoader();
        java.util.Iterator it = directories.iterator();
        while (it.hasNext() && (stream == null))
        {
            Object o = it.next();
            if (o instanceof String)
            {
                String path = (String)o;
Log.event("trying path: " + o);
                String fullPath = path + pathSeparator + filename;
                try
                {
Log.event("trying FileInputStream constructor for: " + fullPath);
                    stream = new FileInputStream(fullPath);
                } 
                catch (Exception e) { stream = null; }
                if (stream == null)
                {
Log.event("trying cl.getResourceAsStream for: " + fullPath);
                    stream = cl.getResourceAsStream(fullPath);
                }
            }
        }
        return(stream);
    }
}
