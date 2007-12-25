package net.sf.colossus.util;


import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import net.sf.colossus.server.Constants;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;


/**
 * Class ResourceLoader is an utility class to load a resource from 
 * a filename and a list of directory.
 * @version $Id$
 * @author Romain Dolbeau
 * @author David Ripton
 */

public final class ResourceLoader
{
    private static final Logger LOGGER = Logger.getLogger(ResourceLoader.class
        .getName());

    /**
     * Class ColossusClassLoader allows for class loading outside the
     * CLASSPATH, i.e. from the various variant directories.
     */
    private static class ColossusClassLoader extends ClassLoader
    {
        List directories = null;

        ColossusClassLoader(ClassLoader parent)
        {
            super(parent);
        }

        ColossusClassLoader()
        {
            super();
        }

        public Class findClass(String className) throws ClassNotFoundException
        {
            try
            {
                int index = className.lastIndexOf(".");
                String shortClassName = className.substring(index + 1);
                if (index == -1)
                {
                    LOGGER.log(Level.SEVERE, "Loading of class \"" + className
                        + "\" failed (no dot in class name)");
                    return null;
                }
                InputStream classDataIS = getInputStream(shortClassName
                    + ".class", directories);
                if (classDataIS == null)
                {
                    LOGGER.log(Level.SEVERE,
                        "Couldn't find the class file anywhere ! ("
                            + shortClassName + ".class)");
                    throw new FileNotFoundException("missing "
                        + shortClassName + ".class");
                }
                byte[] classDataBytes = new byte[classDataIS.available()];
                classDataIS.read(classDataBytes);
                return defineClass(className, classDataBytes, 0,
                    classDataBytes.length);
            }
            catch (Exception e)
            {
                return super.findClass(className);
            }
        }

        void setDirectories(List d)
        {
            directories = d;
        }
    }

    public static final String keyContentType = "ResourceLoaderContentType";
    public static final String defaultFontName = "Lucida Sans Bold";
    public static final int defaultFontStyle = Font.PLAIN;
    public static final int defaultFontSize = 12;
    public static final Font defaultFont = new Font(defaultFontName,
        defaultFontStyle, defaultFontSize);
    // File.separator does not work in jar files, except in Unix.
    // A hardcoded '/' works in Unix, Windows, MacOS X, and jar files.
    private static final String pathSeparator = "/";
    private static final String[] imageExtension = { ".png", ".gif" };
    private static final String[] svgExtension = { ".svg" };
    private static final ClassLoader baseCL = ResourceLoader.class
        .getClassLoader();
    private static final ColossusClassLoader cl = new ColossusClassLoader(
        baseCL);
    private static final Map imageCache = Collections
        .synchronizedMap(new HashMap());
    private static final Map fileCache = Collections
        .synchronizedMap(new HashMap());

    private final static String sep = Constants.protocolTermSeparator;

    // check if we can use Batik (SVG files) or not
    private static boolean hasBatik = false;
    static
    {
        try
        {
            baseCL
                .loadClass("org.apache.batik.transcoder.image.ImageTranscoder");
            hasBatik = true;
            // Use Crimson, not Xerces
            XMLResourceDescriptor
                .setXMLParserClassName("org.apache.crimson.parser.XMLReaderImpl");
        }
        catch (Exception e)
        {
            LOGGER.log(Level.FINEST, "Couldn't load class \""
                + "org.apache.batik.transcoder.image.ImageTranscoder\", "
                + "assuming batik (and SVG files) not available.");
        }
    }

    private static String server = null;
    private static int serverPort = 0;

    public static void setDataServer(String server, int port)
    {
        ResourceLoader.server = server;
        ResourceLoader.serverPort = port;
    }

    public static void resetDataServer()
    {
        ResourceLoader.server = null;
        ResourceLoader.serverPort = 0;
    }

    /**
     * Give the String to mark directories.
     * @return The String to mark directories.
     */
    public static String getPathSeparator()
    {
        return pathSeparator;
    }

    /** empty the cache so that all Chits have to be redrawn */
    public synchronized static void purgeImageCache()
    {
        LOGGER.log(Level.FINEST, "Purging Image Cache.");
        imageCache.clear();
    }

    /** empty the cache so that all files have to be reloaded */
    public synchronized static void purgeFileCache()
    {
        LOGGER.log(Level.FINEST, "Purging File Cache.");
        fileCache.clear();
    }

    /**
     * Return the first Image of name filename in the list of directories.
     * @param filename Name of the Image file to load (without extension).
     * @param directories List of directories to search (in order).
     * @return The Image, or null if it was not found.
     */
    public synchronized static Image getImage(String filename,
        List directories, int width, int height)
    {
        Image image = null;
        String mapKey = getMapKey(filename, directories);
        mapKey = mapKey + "(" + width + "," + height + ")";
        Object cached = imageCache.get(mapKey);
        if ((cached != null) && (cached instanceof Image))
        {
            image = (Image)cached;
        }
        if ((cached != null) && (cached instanceof ImageIcon))
        {
            image = ((ImageIcon)cached).getImage();
        }
        java.util.Iterator it = directories.iterator();
        while (it.hasNext() && (image == null))
        {
            Object o = it.next();
            if (o instanceof String)
            {
                String path = (String)o;
                for (int i = 0; ((i < svgExtension.length) && (image == null)); i++)
                {
                    image = tryLoadSVGImageFromFile(
                        filename + svgExtension[i], path, width, height);
                }
                for (int i = 0; ((i < imageExtension.length) && (image == null)); i++)
                {
                    image = tryLoadImageFromFile(filename + imageExtension[i],
                        path, width, height);
                    if (image == null)
                    {
                        ImageIcon temp = tryLoadImageIconFromResource(filename
                            + imageExtension[i], path, width, height);
                        if (temp != null)
                        {
                            image = temp.getImage();
                        }
                    }
                }
                if (image != null)
                {
                    imageCache.put(mapKey, image);
                }
            }
        }
        if (image != null)
        {
            waitOnImage(image);
        }
        return (image);
    }

    /**
     * Return the first ImageIcon of name filename in the list of directories.
     * @param filename Name of the ImageIcon file to load (without extension).
     * @param directories List of directories to search (in order).
     * @return The ImageIcon, or null if it was not found.
     */
    public synchronized static ImageIcon getImageIcon(String filename,
        List directories, int width, int height)
    {
        ImageIcon icon = null;
        String mapKey = getMapKey(filename, directories);
        mapKey = mapKey + "(" + width + "," + height + ")";
        Object cached = imageCache.get(mapKey);
        if ((cached != null) && (cached instanceof Image))
        {
            icon = new ImageIcon((Image)cached);
        }
        if ((cached != null) && (cached instanceof ImageIcon))
        {
            icon = (ImageIcon)cached;
        }
        java.util.Iterator it = directories.iterator();
        while (it.hasNext() && (icon == null))
        {
            Object o = it.next();
            if (o instanceof String)
            {
                String path = (String)o;

                for (int i = 0; ((i < svgExtension.length) && (icon == null)); i++)
                {
                    Image temp = tryLoadSVGImageFromFile(filename
                        + svgExtension[i], path, width, height);
                    if (temp != null)
                    {
                        icon = new ImageIcon(temp);
                    }
                }
                for (int i = 0; ((i < imageExtension.length) && (icon == null)); i++)
                {
                    Image temp = tryLoadImageFromFile(filename
                        + imageExtension[i], path, width, height);
                    if (temp == null)
                    {
                        icon = tryLoadImageIconFromResource(filename
                            + imageExtension[i], path, width, height);
                    }
                    else
                    {
                        icon = new ImageIcon(temp);
                    }
                }
                if (icon != null)
                {
                    imageCache.put(mapKey, icon);
                }
            }
        }
        while (icon != null
            && icon.getImageLoadStatus() == MediaTracker.LOADING)
        { // no need for CPU time
            Thread.yield();
        }
        return (icon);
    }

    /**
     * Try loading the file with the given filename in the given path
     * as an Image.
     * @param filename Name of the file to load.
     * @param path Path to search for the file
     * @return Resulting Image, or null if it fails.
     */
    private static Image tryLoadImageFromFile(String filename, String path,
        int width, int height)
    {
        Image image = null;
        try
        {
            java.net.URL url;
            url = new java.net.URL("file:" + path + pathSeparator
                + fixFilename(filename));
            // URL will not be null even if the file doesn't exist,
            // so we need to check if connection can be opened
            if (url.openStream() != null)
            {
                image = Toolkit.getDefaultToolkit().getImage(url);
            }
        }
        catch (Exception e)
        {
            // nothing to do
        }
        if (image != null)
        {
            return image.getScaledInstance(width, height,
                java.awt.Image.SCALE_SMOOTH);
        }
        else
        {
            return null;
        }
    }

    /**
     * Try loading the file file with the given filename in the given path
     * as an ImageIcon, through a Class loader.
     * @param filename Name of the file to load.
     * @param path Path to search for the file
     * @return Resulting ImageIcon, or null if it fails.
     */
    private static ImageIcon tryLoadImageIconFromResource(String filename,
        String path, int width, int height)
    {
        ImageIcon icon = null;
        try
        {
            java.net.URL url;
            url = cl.getResource(path + pathSeparator + fixFilename(filename));
            // URL will not be null even if the file doesn't exist,
            // so we need to check if connection can be opened
            if (url.openStream() != null)
            {
                icon = new ImageIcon(url);
            }

        }
        catch (Exception e)
        {
            // nothing to do
        }
        if (icon == null)
        {
            return null;
        }
        if ((icon.getIconWidth() == width) && (icon.getIconHeight() == height))
        {
            return icon;
        }
        else
        {
            return new ImageIcon(icon.getImage().getScaledInstance(width,
                height, java.awt.Image.SCALE_SMOOTH));
        }
    }

    /**
     * Return the first InputStream from file of name filename in the 
     * list of directories, tell the getInputStream not to complain
     * if not found. 
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStreamIgnoreFail(String filename,
        List directories)
    {
        return getInputStream(filename, directories, server != null, false,
            true);
    }

    /**
     * Return the first InputStream from file of name filename in the 
     * list of directories.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStream(String filename, List directories)
    {
        return getInputStream(filename, directories, server != null, false,
            false);
    }

    /**
     * Return the first InputStream from file of name filename in
     * the list of directories.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @param remote Ask the server for the stream.
     * @param cachedOnly Only look in the cache file, 
     *     do not try to load the file from permanent storage.
     * @param ignoreFail (=don't complain) if file not found    
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStream(String filename,
        List directories, boolean remote, boolean cachedOnly,
        boolean ignoreFail)
    {
        String mapKey = getMapKey(filename, directories);
        Object cached = fileCache.get(mapKey);
        byte[] data = null;

        if ((cached == null) && cachedOnly)
        {
            if (!ignoreFail)
            {
                LOGGER.log(Level.WARNING, "Requested file " + filename
                    + " is requested cached-only but is not is cache.");
            }
            return null;
        }

        if ((cached == null) && ((!remote) || (server == null)))
        {
            synchronized (fileCache)
            {
                InputStream stream = null;
                java.util.Iterator it = directories.iterator();
                while (it.hasNext() && (stream == null))
                {
                    Object o = it.next();
                    if (o instanceof String)
                    {
                        String path = (String)o;
                        String fullPath = path + pathSeparator
                            + fixFilename(filename);
                        try
                        {
                            File tempFile = new File(fullPath);
                            stream = new FileInputStream(tempFile);
                        }
                        catch (Exception e)
                        {
                            stream = cl.getResourceAsStream(fullPath);
                        }
                    }
                }
                if (stream == null)
                {
                    if (!remote && ignoreFail)
                    {
                        // If someone locally requests it as ignoreFail,
                        // let's assume a remote requester later sees it the
                        // same way.
                        // Right now, the remote-requesting is not able to
                        // submit the "ignore-fail" property... 
                        // @TODO: submit that properly?
                        // fileCacheIgnoreFail.put(mapKey, new Boolean(true));
                    }
                    if (!ignoreFail)
                    {
                        LOGGER.log(Level.WARNING, "getInputStream:: "
                            + " Couldn't get InputStream for file " + filename
                            + " in " + directories
                            + (cachedOnly ? " (cached only)" : ""));
                        // @TODO this sounds more serious than just a warning in the logs
                        // Anyway now at least MarkersLoader does not complain any more...
                    }
                }
                else
                {
                    data = getBytesFromInputStream(stream);
                    fileCache.put(mapKey, data);
                }
            }
        }
        else
        {
            synchronized (fileCache)
            {
                if (cached != null)
                {
                    data = (byte[])cached;
                }
                else
                {
                    try
                    {
                        Socket fileSocket = new Socket(server, serverPort);
                        InputStream is = fileSocket.getInputStream();

                        if (is == null)
                        {
                            LOGGER.log(Level.WARNING, "getInputStream:: "
                                + " Couldn't get InputStream from socket"
                                + " for file " + filename + " in "
                                + directories
                                + (cachedOnly ? " (cached only)" : ""));
                            // @TODO this sounds more serious than just a warning in the logs
                        }
                        else
                        {
                            PrintWriter out = new PrintWriter(fileSocket
                                .getOutputStream(), true);

                            if (ignoreFail)
                            {
                                // Not in this version yet (05/2007). 
                                // New clients could not talk with old server.
                                // Take this into full use somewhat later.
                                //out.print(
                                //   Constants.fileServerIgnoreFailSignal + sep);
                            }
                            out.print(filename);
                            java.util.Iterator it = directories.iterator();
                            while (it.hasNext())
                            {
                                out.print(sep + (String)it.next());
                            }
                            out.println();
                            data = getBytesFromInputStream(is);
                            if (data != null && data.length == 0
                                && !ignoreFail)
                            {
                                LOGGER.log(Level.WARNING,
                                    "Got empty contents for file " + filename
                                        + " directories "
                                        + directories.toString());
                            }
                            fileSocket.close();
                            fileCache.put(mapKey, data);
                        }
                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.SEVERE, "Failed to read from stream",
                            e);
                    }
                }

            }
        }
        return (data == null ? null : getInputStreamFromBytes(data));
    }

    /**
     * Return the content of the specified file as an array of byte.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @param cachedOnly Only look in the cache file, 
     *     do not try to load the file from permanent storage.
     * @return An array of byte representing the content of the file, 
     *     or null if it fails.
     */
    public static byte[] getBytesFromFile(String filename, List directories,
        boolean cachedOnly, boolean ignoreFail)
    {
        InputStream is = getInputStream(filename, directories, server != null,
            cachedOnly, ignoreFail);
        if (is == null)
        {
            // right now only FileServerThread is using this method at all.
            if (!ignoreFail)
            {
                LOGGER.log(Level.WARNING, "getBytesFromFile:: "
                    + " Couldn't get InputStream for file " + filename
                    + " in " + directories
                    + (cachedOnly ? " (cached only)" : ""));
            }
            return null;
        }
        return getBytesFromInputStream(is);
    }

    /**
     * Return the content of the specified InputStream as an array of byte.
     * @param InputStream The InputStream to use.
     * @return An array of byte representing the content 
     *     of the InputStream, or null if it fails.
     */
    private static byte[] getBytesFromInputStream(InputStream is)
    {
        byte[] all = new byte[0];

        try
        {
            byte[] data = new byte[1024 * 64];
            int r = is.read(data);
            while (r > 0)
            {
                byte[] temp = new byte[all.length + r];
                for (int i = 0; i < all.length; i++)
                {
                    temp[i] = all[i];
                }
                for (int i = 0; i < r; i++)
                {
                    temp[i + all.length] = data[i];
                }
                all = temp;
                r = is.read(data);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Can't Stringify stream " + is + ".", e);
        }
        return all;
    }

    /**
     * Return the content of the specified byte array as an InputStream.
     * @param data The byte array to convert.
     * @return An InputStream whose content is the data byte array.
     */
    private static InputStream getInputStreamFromBytes(byte[] data)
    {
        if (data == null)
        {
            LOGGER.log(Level.WARNING, "getInputStreamFromBytes:: "
                + " Can't create InputStream from null byte array");
            return null;
        }
        return new ByteArrayInputStream(data);
    }

    /**
     * Return the first OutputStream from file of name filename in 
     *     the list of directories.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The OutputStream, or null if it was not found.
     */
    public static OutputStream getOutputStream(String filename,
        List directories)
    {
        OutputStream stream = null;
        java.util.Iterator it = directories.iterator();
        while (it.hasNext() && (stream == null))
        {
            Object o = it.next();
            if (o instanceof String)
            {
                String path = (String)o;
                String fullPath = path + pathSeparator + fixFilename(filename);
                try
                {
                    stream = new FileOutputStream(fullPath);
                }
                catch (Exception e)
                {
                    LOGGER.log(Level.FINEST, "getOutputStream:: "
                        + " Couldn't get OutputStream for file " + filename
                        + " in " + directories + "(" + e.getMessage() + ")");
                }
            }
        }
        return (stream);
    }

    /**
     * Return the first Document from file of name filename in 
     *   the list of directories.
     * It also add a property of key keyContentType and of type String 
     *   describing the content type of the Document.
     * This can currently load HTML and pure text.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The Document, or null if it was not found.
     */
    public static Document getDocument(String filename, List directories)
    {
        InputStream htmlIS = getInputStreamIgnoreFail(filename + ".html",
            directories);
        if (htmlIS != null)
        {
            try
            {
                HTMLEditorKit htedk = new HTMLEditorKit();
                HTMLDocument htdoc = new HTMLDocument(htedk.getStyleSheet());
                htdoc.putProperty(keyContentType, "text/html");
                htedk.read(htmlIS, htdoc, 0);
                return htdoc;
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE,
                    "html document exists, but cannot be loaded (" + filename
                        + "): ", e);
            }
            return null;
        }
        InputStream textIS = getInputStreamIgnoreFail(filename + ".txt",
            directories);
        if (textIS == null)
        {
            textIS = getInputStreamIgnoreFail(filename, directories);
        }
        if (textIS != null)
        {
            try
            {
                // Must be a StyledDocument not a PlainDocument for
                // JEditorPane.setDocument()
                StyledDocument txtdoc = new DefaultStyledDocument();
                char[] buffer = new char[128];
                InputStreamReader textISR = new InputStreamReader(textIS);
                int read = 0;
                int offset = 0;
                while (read != -1)
                {
                    read = textISR.read(buffer, 0, 128);
                    if (read != -1)
                    {
                        txtdoc.insertString(offset,
                            new String(buffer, 0, read), null);
                        offset += read;
                    }
                }
                txtdoc.putProperty(keyContentType, "text/plain");
                return txtdoc;
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE,
                    "text document exists, but cannot be loaded (" + filename
                        + "): " + e, e);
            }
            return null;
        }
        LOGGER.log(Level.WARNING, "No document for basename " + filename
            + " found " + "(neither .html, .txt nor without extention)!");
        return null;
    }

    /**
     * Return the key to use in the image and file caches.
     * @param filename Name of the file.
     * @param directories List of directories.
     * @return A String to use as a key when storing/loading in a cache
     *   the specified file from the specified list of directories.
     */
    private static String getMapKey(String filename, List directories)
    {
        String[] filenames = new String[1];
        filenames[0] = filename;
        return getMapKey(filenames, directories);
    }

    /**
     * Return the key to use in the image cache.
     * @param filenames Array of name of files.
     * @param directories List of directories.
     * @return A String to use as a key when storing/loading in a cache
     *   the specified array of name of files from the specified 
     *   list of directories.
     */
    private static String getMapKey(String[] filenames, List directories)
    {
        StringBuffer buf = new StringBuffer(filenames[0]);
        for (int i = 1; i < filenames.length; i++)
        {
            buf.append(",");
            buf.append(filenames[i]);
        }
        Iterator it = directories.iterator();
        while (it.hasNext())
        {
            Object o = it.next();
            if (o instanceof String)
            {
                buf.append(",");
                buf.append(o);
            }
        }
        return buf.toString();
    }

    /**
     * Return the composite image made from blending the given filenames
     *   from the given directories.
     * @param filenames Names of the Images files to load (without extension).
     * @param directories List of directories to search (in order).
     * @return The composite Image, or null if any part was not found.
     */
    public synchronized static Image getCompositeImage(String[] filenames,
        List directories, int width, int height)
    {
        BufferedImage bi;
        String mapKey = getMapKey(filenames, directories);
        mapKey = mapKey + "(" + width + "," + height + ")";
        Object cached = imageCache.get(mapKey);

        if ((cached != null) && (cached instanceof Image))
        {
            return (Image)cached;
        }
        if ((cached != null) && (cached instanceof ImageIcon))
        {
            return ((ImageIcon)cached).getImage();
        }
        Image[] tempImage = new Image[filenames.length];
        for (int i = 0; i < filenames.length; i++)
        {
            tempImage[i] = getImage(filenames[i], directories, width, height);
            if (tempImage[i] == null)
            {
                tempImage[i] = tryBuildingNonexistentImage(filenames[i],
                    width, height, directories);
            }
            if (tempImage[i] == null)
            {
                LOGGER.log(Level.SEVERE, "during creation of [" + mapKey
                    + "], loading failed for " + filenames[i]);
                return null;
            }
        }
        bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        for (int i = 0; (biContext != null) && (i < filenames.length); i++)
        {
            biContext.drawImage(tempImage[i], 0, 0, width, height, null);
            waitOnImage(bi);
        }
        imageCache.put(mapKey, bi);

        return bi;
    }

    /**
     * Try to build an image when there is no source file to create it.
     *   Includes generation of some dynamic layers of images for
     *   composite image building.
     * @see #getCompositeImage(String[], List)
     * @param filename The name of the missing file.
     * @param width Width of the image to create.
     * @param height Height of the image to create.
     * @param directories List of searched directories.
     * @return The generated Image.
     */
    private synchronized static Image tryBuildingNonexistentImage(
        String filename, int width, int height, List directories)
    {
        Image tempImage = null;

        if (filename.startsWith("Plain-"))
        {
            tempImage = createPlainImage(width, height, colorFromFilename(
                filename, "Plain-"));
        }
        if (filename.startsWith("Power-"))
        {
            int val = numberFromFilename(filename, "Power-");
            tempImage = createNumberImage(width, height, val, false,
                colorFromFilename(filename, "Power-"));
        }
        if (filename.startsWith("Skill-"))
        {
            int val = numberFromFilename(filename, "Skill-");
            tempImage = createNumberImage(width, height, val, true,
                colorFromFilename(filename, "Skill-"));
        }
        if (filename.startsWith("Flying")
            || filename.startsWith("Rangestrike"))
        {
            int fly_ix = filename.indexOf("Flying");
            int rgs_ix = filename.indexOf("Rangestrike");
            String prefix = (fly_ix != -1 ? "Flying" : "")
                + (rgs_ix != -1 ? "Rangestrike" : "");
            tempImage = createColorizedImage(prefix + "Base",
                colorFromFilename(filename, prefix), directories, width,
                height);
        }
        if (filename.indexOf("-Name") != -1)
        {
            String name = filename.substring(0, filename.indexOf("-Name"));
            tempImage = createNameImage(width, height, name, false,
                colorFromFilename(filename, name + "-Name"));
        }
        if (filename.indexOf("-Subscript") != -1)
        {
            String name = filename
                .substring(0, filename.indexOf("-Subscript"));
            tempImage = createNameImage(width, height, name, true,
                colorFromFilename(filename, name + "-Subscript"));
        }

        if (tempImage == null)
        {
            LOGGER.log(Level.WARNING, "WARNING: creation failed for "
                + filename);
            return createPlainImage(width, height, Color.white, true);
        }
        waitOnImage(tempImage);
        String mapKey = getMapKey(filename, directories);
        mapKey = mapKey + "(" + width + "," + height + ")";
        imageCache.put(mapKey, tempImage);
        return (tempImage);
    }

    /**
     * Create an Image with only the given number on it.
     * @param width Width of the image to create.
     * @param height Height of the image to create.
     * @param value The number to draw on the image.
     * @param right The number is on the right side (default is left side).
     * @param color The color to use to draw the number.
     * @return The generated Image.
     */
    private static Image createNumberImage(int width, int height, int value,
        boolean right, Color color)
    {
        BufferedImage bi = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.setColor(new Color((float)1., (float)1., (float)1.,
            (float)0.));
        biContext.fillRect(0, 0, width, height);
        biContext.setColor(color);
        int fontsize = (width + height) / 10;
        biContext.setFont(defaultFont.deriveFont((float)fontsize));
        FontMetrics fm = biContext.getFontMetrics();
        Rectangle2D sb = fm.getStringBounds("" + value, biContext);
        int sw = (int)sb.getWidth();
        String valueTxt = (value > 0 ? "" + value : "X");

        if (right)
        {
            biContext.drawString(valueTxt, (width - (sw + 2)), height - 2);
        }
        else
        {
            biContext.drawString(valueTxt, 2, height - 2);
        }
        waitOnImage(bi);

        return bi;
    }

    /**
     * Create an Image with only the given String on it.
     * @param width Width of the image to create.
     * @param height Height of the image to create.
     * @param name The String to draw on the image.
     * @param down The name is on the bottom (default is top).
     * @param color The color to use to draw the String.
     * @return The generated Image.
     */
    private static Image createNameImage(int width, int height, String name,
        boolean down, Color color)
    {
        BufferedImage bi = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.setColor(new Color((float)1., (float)1., (float)1.,
            (float)0.));
        biContext.fillRect(0, 0, width, height);
        biContext.setColor(color);
        int fontsize = (width + height) / 10;
        biContext.setFont(defaultFont.deriveFont((float)fontsize));
        Font font = biContext.getFont();
        int size = font.getSize();
        FontMetrics fm = biContext.getFontMetrics();
        Rectangle2D sb = fm.getStringBounds(name, biContext);
        int sw = (int)sb.getWidth();
        while ((sw >= width) && (size > 1))
        {
            size--;
            biContext.setFont(font.deriveFont((float)size));
            fm = biContext.getFontMetrics();
            sb = fm.getStringBounds(name, biContext);
            sw = (int)sb.getWidth();
        }
        int offset = (width - sw) / 2;
        biContext.drawString(name, offset, (down ? (height - 2) : (1 + fm
            .getMaxAscent())));
        waitOnImage(bi);

        return bi;
    }

    /**
     * Create an Image that is only a plain rectangle.
     * @param width Width of the image to create.
     * @param height Height of the image to create.
     * @param color The color to use to fill the rectangle.
     * @return The generated Image.
     */
    private static Image createPlainImage(int width, int height, Color color)
    {
        return createPlainImage(width, height, color, 0, 0, width, height,
            false);
    }

    /**
     * Create an Image that is only a plain rectangle, with an optional border.
     * @param width Width of the image to create.
     * @param height Height of the image to create.
     * @param color The color to use to fill the rectangle.
     * @param border Whether to add a black border.
     * @return The generated Image.
     */
    private static Image createPlainImage(int width, int height, Color color,
        boolean border)
    {
        return createPlainImage(width, height, color, 0, 0, width, height,
            border);
    }

    /**
     * Create an Image that only contains a colored rectangle, 
     *   with an optional border.
     * @param width Width of the image to create.
     * @param height Height of the image to create
     * @param color The color to use to fill the rectangle.
     * @param t_x Left border of the rectangle.
     * @param t_y Top border of the rectangle.
     * @param t_w Width of the rectangle.
     * @param t_h Height of the rectangle.
     * @param border Whether to add a black border.
     * @return The generated Image.
     */
    private static Image createPlainImage(int width, int height, Color color,
        int t_x, int t_y, int t_w, int t_h, boolean border)
    {
        BufferedImage bi = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.setColor(new Color((float)1., (float)1., (float)1.,
            (float)0.));
        biContext.fillRect(0, 0, width, height);
        biContext.setColor(color);
        biContext.fillRect(t_x, t_y, t_w, t_h);
        if (border)
        {
            biContext.setColor(Color.black);
            biContext.drawRect(0, 0, width - 1, height - 1);
        }
        waitOnImage(bi);
        return bi;
    }

    /**
     * Create a colorized version of the image contained in the given file.
     * @param filename Name of the Image file to load.
     * @param directories List of directories to search (in order).
     * @param color Color to use.
     * @return An Image composed of the content of the file, 
     *     with the transparent part filled the the given color.
     */
    private static Image createColorizedImage(String filename, Color color,
        List directories, int width, int height)
    {
        Image temp = getImage(filename, directories, width, height);
        ImageIcon tempIcon = new ImageIcon(temp);
        while (tempIcon.getImageLoadStatus() == MediaTracker.LOADING)
        {
            Thread.yield();
        }
        if (tempIcon.getImageLoadStatus() != MediaTracker.COMPLETE)
        {
            LOGGER.log(Level.SEVERE, "Image loading of " + filename
                + " failed (" + tempIcon.getImageLoadStatus() + ")");
            return null;
        }

        BufferedImage bi = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.drawImage(temp, 0, 0, width, height, null);
        waitOnImage(bi);

        int[] pi;
        WritableRaster ra = bi.getRaster();
        // rebuild the image from the Alpha Channel
        // fully-opaque pixel are set to the color,
        // everything else is white.
        // this should have been a LookupOp, but
        // I couldn't make it reliable across platform :-(
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                pi = ra.getPixel(x, y, (int[])null);
                if (pi[3] == 0xFF) // fully opaque
                {
                    pi[0] = color.getRed();
                    pi[1] = color.getGreen();
                    pi[2] = color.getBlue();
                }
                else
                {
                    pi[0] = pi[1] = pi[2] = 0xFF;
                }
                ra.setPixel(x, y, pi);
            }
        }

        return bi;
    }

    /**
     * Wait until the Image in parameter is fully drawn.
     * @param image Image to wait upon.
     */
    private static void waitOnImage(Image image)
    {
        ImageIcon icon = new ImageIcon(image);
        while (icon.getImageLoadStatus() == MediaTracker.LOADING)
        {
            Thread.yield();
        }
        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE)
        {
            LOGGER.log(Level.SEVERE, "Image loading failed ("
                + icon.getImageLoadStatus() + ")");
        }
    }

    /**
     * Extract a number from a filename, ignoring a prefix.
     * @param filename File name to extract from.
     * @param prefix Prefix to ignore. Has to match the prefix of the file name, otherwise
     *        0 will be returned.
     * @return The extracted number.
     */
    private static int numberFromFilename(String filename, String prefix)
    {
        if (!(filename.startsWith(prefix)))
        {
            LOGGER.log(Level.WARNING, "Warning: " + prefix
                + " is not prefix of " + filename);
            // boil out if we are on a developer box, use default otherwise
            assert false : "illegal combination for filename and prefix";
            return 0;
        }
        int index = prefix.length();
        int index2 = index;
        if (index2 >= filename.length())
        {
            return 0;
        }
        char c = filename.charAt(index2);
        if (c == '-')
        {
            index2++;
            if (index2 < filename.length())
            {
                c = filename.charAt(index2);
            }
            else
            {
                c = '*';
            }
        }
        while ((c >= '0') && (c <= '9'))
        {
            index2++;
            if (index2 < filename.length())
            {
                c = filename.charAt(index2);
            }
            else
            {
                c = '*';
            }
        }
        String sub = filename.substring(index, index2);
        int val = 0;
        try
        {
            val = Integer.parseInt(sub);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "during number extraction: "
                + e.getMessage(), e);
        }
        return val;
    }

    /**
     * Extract a color name from a filename, ignoring a prefix
     * @param filename File name to extract from.
     * @param prefix Prefix to ignore. Has to match the beginning of the file
     * name, otherwise "black" will be used as default value. 
     * @return The extracted color name.
     */
    private static String colorNameFromFilename(String filename, String prefix)
    {
        if (!(filename.startsWith(prefix)))
        {
            LOGGER
                .log(Level.WARNING, prefix + " is not prefix of " + filename);
            // boil out if we are on a developer box, use default otherwise
            assert false : "illegal combination for filename and prefix";
            return "black";
        }
        int index = prefix.length();
        int index2 = index;
        if (index2 >= filename.length())
        {
            return "black";
        }
        char c = filename.charAt(index2);
        if (c == '-')
        {
            index2++;
            if (index2 < filename.length())
            {
                c = filename.charAt(index2);
            }
            else
            {
                c = '*';
            }
        }
        while ((c >= '0') && (c <= '9'))
        {
            index2++;
            if (index2 < filename.length())
            {
                c = filename.charAt(index2);
            }
            else
            {
                c = '*';
            }
        }
        if (c == '-')
        {
            index2++;
        }
        if (index2 >= filename.length())
        {
            return "black";
        }
        String sub = filename.substring(index2);
        return sub;
    }

    /**
     * Extract a color from a filename, ignoring a prefix.
     * @param filename File name to extract from.
     * @param prefix Prefix to ignore.
     * @return The extracted Color.
     */
    private static Color colorFromFilename(String filename, String prefix)
    {
        return HTMLColor
            .stringToColor(colorNameFromFilename(filename, prefix));
    }

    /**
     * Fix a filename by replacing space with underscore.
     * @param filename Filename to fix.
     * @return The fixed filename.
     */
    private static String fixFilename(String filename)
    {
        return filename.replace(' ', '_');
    }

    /**
     * Create an instance of the class whose name is in parameter.
     * @param className The name of the class to use.
     * @param directories List of directories to search (in order).
     * @return A new object, instance from the given class.
     */
    public static Object getNewObject(String className, List directories)
    {
        return getNewObject(className, directories, null);
    }

    /**
     * Create an instance of the class whose name is in parameter,
     *     using parameters.
     *     
     * If no parameters are given, the default constructor is used.
     * 
     * @TODO this is full of catch(Exception) blocks, which all return null.
     *       Esp. returning null seems a rather bad idea, since it will most
     *       likely turn out to be NPEs somewhere later.
     *     
     * @param className The name of the class to use, must not be null.
     * @param directories List of directories to search (in order), must not be null.
     * @param parameter Array of parameters to pass to the constructor, can be null.
     * @return A new object, instance from the given class or null if 
     *         instantiation failed.
     */
    public static Object getNewObject(String className, List directories,
        Object[] parameter)
    {
        Class theClass = null;
        cl.setDirectories(directories);
        try
        {
            theClass = cl.loadClass(className);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Loading of class \"" + className
                + "\" failed.", e);
            return null;
        }
        if (parameter != null)
        {
            Class[] paramClasses = new Class[parameter.length];
            for (int i = 0; i < parameter.length; i++)
            {
                paramClasses[i] = parameter[i].getClass();
            }
            try
            {
                Constructor c = theClass.getConstructor(paramClasses);
                return c.newInstance(parameter);
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE,
                    "Loading or instantiating class' constructor for \""
                        + className + "\" failed", e);
                return null;
            }
        }
        else
        {
            try
            {
                return theClass.newInstance();
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE, "Instantiating \"" + className
                    + "\" failed", e);
                return null;
            }
        }
    }

    /**
     * Force adding the given data as belonging to the given filename
     * in the file cache.
     * @param filename Name of the Image file to add.
     * @param directories List of directories to search (in order).
     * @param data File content to add.
     */
    public static void putIntoFileCache(String filename, List directories,
        byte[] data)
    {
        String mapKey = getMapKey(filename, directories);
        fileCache.put(mapKey, data);
    }

    /**
     * Force adding the given data as belonging to the given key
     * in the file cache.
     * @see #getMapKey(String, List)
     * @see #getMapKey(String[], List)
     * @param mapKey Key to use in the cache.
     * @param data File content to add.
     */
    public static void putIntoFileCache(String mapKey, byte[] data)
    {
        fileCache.put(mapKey, data);
    }

    /**
     * Dump the file cache as a List of XML "DataFile" Element,
     *     with the file key as attribute "DataFileKey", and the 
     *     file data as a CDATA content.
     * @return A list of XML Element.
     */
    public static List getFileCacheDump()
    {
        List allElement = new ArrayList();
        Set allKeys = fileCache.keySet();
        Iterator it = allKeys.iterator();
        while (it.hasNext())
        {
            String mapKey = (String)it.next();
            byte[] data = (byte[])fileCache.get(mapKey);
            org.jdom.Element el = new org.jdom.Element("DataFile");
            el.setAttribute("DataFileKey", mapKey);
            el.addContent(new org.jdom.CDATA(new String(data)));
            allElement.add(el);
        }
        return allElement;
    }

    /* cheat, using Batik transcoder API. we only want the Image */
    private static class BufferedImageTranscoder extends ImageTranscoder
    {
        private BufferedImage image;

        public BufferedImage createImage(int width, int height)
        {
            return new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        }

        public void writeImage(BufferedImage image, TranscoderOutput output)
            throws TranscoderException
        {
            this.image = image;
        }

        public BufferedImage getImage()
        {
            return image;
        }
    }

    static boolean useSVG = false;

    public static void setUseSVG(boolean useSVG)
    {
        ResourceLoader.useSVG = useSVG;
    }

    /**
     * Try loading the SVG file with the given filename in the given path
     * as an Image.
     * @param filename Name of the file to load.
     * @param path Path to search for the file
     * @return Resulting Image, or null if it fails.
     */
    private static Image tryLoadSVGImageFromFile(String filename, String path,
        int width, int height)
    {
        if (!hasBatik || !useSVG)
        {
            return null;
        }

        Image image = null;
        try
        {
            java.net.URL url;
            url = new java.net.URL("file:" + path + pathSeparator
                + fixFilename(filename));
            // URL will not be null even if the file doesn't exist,
            // so we need to check if connection can be opened
            InputStream stream = url.openStream();
            if (stream != null)
            {
                BufferedImageTranscoder t = new BufferedImageTranscoder();
                t.addTranscodingHint(ImageTranscoder.KEY_WIDTH, new Float(
                    width));
                t.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, new Float(
                    height));
                TranscoderInput input = new TranscoderInput(stream);
                t.transcode(input, null);
                image = t.getImage();
            }
        }
        catch (FileNotFoundException e)
        {
            // nothing to do
        }
        catch (Exception e)
        {
            LOGGER.log(Level.FINE, "SVG transcoding for " + filename + " in "
                + path + " failed.", e);
            // nothing to do
        }
        return image;
    }
}
