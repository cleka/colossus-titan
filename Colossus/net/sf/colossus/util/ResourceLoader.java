package net.sf.colossus.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.*;
import java.util.*;

/**
 * Class ResourceLoader is an utility class to load a resource from a filename and a list of directory.
 * @version $Id$
 * @author Romain Dolbeau
 * @author David Ripton
 */

public final class ResourceLoader
{

    public static final String defaultFontName = "Lucida Sans Bold";
    public static final int defaultFontStyle = Font.PLAIN;
    public static final int defaultFontSize = 12;
    public static final Font defaultFont = new Font(defaultFontName, defaultFontStyle, defaultFontSize);
    // File.separator does not work in jar files, except in Unix.
    // A hardcoded '/' works in Unix, Windows, MacOS X, and jar files.
    private static final String pathSeparator = "/";
    private static final String[] imageExtension = { ".png", ".gif" };
    private static final int trackedId = 1;
    private static final ClassLoader cl =
        net.sf.colossus.util.ResourceLoader.class.getClassLoader();
    private static final Map imageCache = Collections.synchronizedMap(new HashMap());

    /**
     * Give the String to mark directories.
     * @return The String to mark directories.
     */
    public static String getPathSeparator()
    {
        return pathSeparator;
    }

    /** empty the cache so that all Chits have to be redrawn */
    public static void purgeCache()
    {
        synchronized (imageCache)
        {
            imageCache.clear();
        }
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
        String mapKey = getMapKey(filename, directories);
        synchronized (imageCache)
        {
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
                    for (int i = 0 ;
                         ((i < imageExtension.length) &&
                          (image == null)) ;
                         i++)
                    {
                        image = tryLoadImageFromFile(filename +
                                                     imageExtension[i], path);
                        if (image == null)
                        {
                            ImageIcon
                                temp = tryLoadImageIconFromResource(
                                           filename + imageExtension[i], path);
                            if (temp != null)
                            {
                                image = temp.getImage();
                            }
                        }
                        if (image != null)
                        {
                            imageCache.put(mapKey, image);
                            System.err.println("Loaded " + path +
                                               pathSeparator + filename +
                                               imageExtension[i]);
                        }
                    }
                }
            }
            if (image != null)
            {
                waitOnImage(image);
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
        String mapKey = getMapKey(filename, directories);
        synchronized (imageCache)
        {        
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
                    for (int i = 0 ;
                         ((i < imageExtension.length) &&
                          (icon == null)) ;
                         i++)
                    {
                        Image temp =
                            tryLoadImageFromFile(
                                filename + imageExtension[i], path);
                        if (temp == null)
                        {
                            icon =
                                tryLoadImageIconFromResource(filename +
                                                             imageExtension[i],
                                                             path);
                        }
                        else
                        {
                            icon = new ImageIcon(temp);
                        }
                        if (icon != null)
                        {
                            imageCache.put(mapKey, icon);
                            System.err.println("Loaded " + path +
                                               pathSeparator + filename +
                                               imageExtension[i]);
                        }
                    }
                }
            }
            while (icon.getImageLoadStatus() == MediaTracker.LOADING)
            { // no need for CPU time
                Thread.yield();
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
                          fixFilename(filename));
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
            url = cl.getResource(path +
                                 pathSeparator +
                                 fixFilename(filename));
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


    /**
     * Return the first InputStream from file of name filename in the list of directories.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The InputStream, or null if it was not found.
     */
    public static InputStream getInputStream(String filename, java.util.List directories)
    {
        InputStream stream = null;
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
                    stream = new FileInputStream(fullPath);
                } 
                catch (Exception e) { stream = null; }
                if (stream == null)
                {
                    stream = cl.getResourceAsStream(fullPath);
                }
            }
        }
        return(stream);
    }

    /**
     * Return the first OutputStream from file of name filename in the list of directories.
     * @param filename Name of the file to load.
     * @param directories List of directories to search (in order).
     * @return The OutputStream, or null if it was not found.
     */
    public static OutputStream getOutputStream(String filename, java.util.List directories)
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
                catch (Exception e) { stream = null; }
            }
        }
        return(stream);
    }

    private static String getMapKey(String filename, java.util.List directories)
    {
        StringBuffer buf = new StringBuffer(filename);
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

    private static String getMapKey(String filenames[], java.util.List directories)
    {
        StringBuffer buf = new StringBuffer(filenames[0]);
        for (int i = 1; i < filenames.length ; i++)
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
     * Return the composite image made from blending the given filenames from the given directories.
     * @param filenames Names of the Images files to load (without extension).
     * @param directories List of directories to search (in order).
     * @return The compisite Image, or null if any part was not found.
     */
    public static Image getCompositeImage(String[] filenames, java.util.List directories)
    {
        BufferedImage bi;
        synchronized (imageCache)
        {
            String mapKey = getMapKey(filenames, directories);
            Object cached = imageCache.get(mapKey);
            
            if ((cached != null) && (cached instanceof Image))
            {
                return (Image)cached;
            }
            if ((cached != null) && (cached instanceof ImageIcon))
            {
                return((ImageIcon)cached).getImage();
            }
            Image tempImage[] = new Image[filenames.length];
            int basew = 60, baseh = 60;
            for (int i = 0; i < filenames.length ; i++)
            {
                tempImage[i] =
                    getImage(filenames[i], directories);
                if ((i == 0) && (tempImage[i] != null))
                {
                    ImageIcon tempicon = new ImageIcon(tempImage[i]);
                    basew = tempicon.getIconWidth();
                    baseh = tempicon.getIconHeight();
                }
                if (tempImage[i] == null)
                {
                    if (filenames[i].startsWith("Plain-"))
                    {
                        tempImage[i] =
                            createPlainImage(basew,baseh,
                                              colorFromFilename(filenames[i],
                                                                "Plain-"));
                    }
                    if (filenames[i].startsWith("Power-"))
                    {
                        int val = numberFromFilename(filenames[i], "Power-");
                        String mapKey2 = getMapKey(filenames[i], directories);
                        tempImage[i] =
                            createNumberImage(basew,baseh,val,false,mapKey2,
                                              colorFromFilename(filenames[i],
                                                                "Power-"));
                    }
                    if (filenames[i].startsWith("Skill-"))
                    {
                        int val = numberFromFilename(filenames[i], "Skill-");
                        String mapKey2 = getMapKey(filenames[i], directories);
                        tempImage[i] =
                            createNumberImage(basew,baseh,val,true,mapKey2,
                                              colorFromFilename(filenames[i],
                                                                "Skill-"));
                    }
                    if (filenames[i].startsWith("Flying") ||
                        filenames[i].startsWith("Rangestrike"))
                    {
                        int fly_ix = filenames[i].indexOf("Flying");
                        int rgs_ix = filenames[i].indexOf("Rangestrike");
                        String prefix = 
                            (fly_ix != -1 ? "Flying" : "") +
                            (rgs_ix != -1 ? "Rangestrike" : "");
                        String mapKey2 = getMapKey(filenames[i], directories);
                        tempImage[i] =
                            createColorizedImage(prefix + "Base",
                                                 colorFromFilename(filenames[i], prefix), mapKey2, directories);
                    }
                    if (filenames[i].indexOf("-Name") != -1)
                    {
                        String name =
                            filenames[i].substring(0,
                                                   filenames[i].indexOf("-Name"));
                        String mapKey2 = getMapKey(filenames[i], directories);
                        tempImage[i] =
                            createNameImage(basew,baseh,name,mapKey2,
                                            colorFromFilename(filenames[i],
                                                              name + "-Name"));
                    }
                    if (tempImage[i] == null)
                    {
                        System.err.println("Error: during creation of \"" +
                                           mapKey + "\", loading failed for " +
                                           filenames[i]);
                        return null;
                    }
                    waitOnImage(tempImage[i]);
                }
            }
            bi = new BufferedImage(basew, baseh,
                                   BufferedImage.TYPE_INT_ARGB);
            Graphics2D biContext = bi.createGraphics();
            for (int i = 0; (biContext!=null) && (i < filenames.length) ; i++)
            {
                biContext.drawImage(tempImage[i],
                                    0, 0,
                                    basew, baseh,
                                    null);
                waitOnImage(bi);
            }
            if (bi != null)
            {
                waitOnImage(bi);
                imageCache.put(mapKey, bi);
            }
        }
        return bi;
    }

    private static Image createNumberImage(int width, int height, int value, boolean right, String mapKey, Color color)
    {
        BufferedImage bi = new BufferedImage(width, height,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.setColor(new Color((float)1.,(float)1.,(float)1.,(float)0.));
        biContext.fillRect(0,0,width,height);
        biContext.setColor(color);
        int fontsize = (width+height)/10;
        biContext.setFont(defaultFont.deriveFont(fontsize));
        FontMetrics fm = biContext.getFontMetrics();
        Rectangle2D sb = fm.getStringBounds("" + value, biContext);
        int sw = (int)sb.getWidth();
        int sh = (int)sb.getHeight();
        String valueTxt = (value > 0 ? ""+value : "X");

        if (right)
        {
            biContext.drawString(valueTxt,
                                 (width - (sw + 2)),
                                 height - 2);
        }
        else
        {
            biContext.drawString(valueTxt,
                                 2,
                                 height - 2);
        }
        synchronized (imageCache)
        {
            imageCache.put(mapKey, bi);
        }
        return bi;
    }

    private static Image createNameImage(int width, int height, String name, String mapKey, Color color)
    {
        BufferedImage bi = new BufferedImage(width, height,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.setColor(new Color((float)1.,(float)1.,(float)1.,(float)0.));
        biContext.fillRect(0,0,width,height);
        biContext.setColor(color);
        int fontsize = (width+height)/10;
        biContext.setFont(defaultFont.deriveFont(fontsize));
        Font font = biContext.getFont();
        int size = font.getSize();
        FontMetrics fm = biContext.getFontMetrics();
        Rectangle2D sb = fm.getStringBounds(name, biContext);
        int sw = (int)sb.getWidth();
        int sh = (int)sb.getHeight();
        while ((sw >= width) && (size > 1))
        {
            size--;
            biContext.setFont(font.deriveFont((float)size));
            fm = biContext.getFontMetrics();
            sb = fm.getStringBounds(name, biContext);
            sw = (int)sb.getWidth();
            sh = (int)sb.getHeight();
        }
        int offset = (width - sw) / 2;
        biContext.drawString(name, offset, 1 + fm.getMaxAscent());

        synchronized (imageCache)
        {
            imageCache.put(mapKey, bi);
        }
        return bi;
    }

    private static Image createPlainImage(int width, int height, Color color)
    {
        return createPlainImage(width, height, color, 0, 0, width, height);
    }

    private static Image createPlainImage(int width, int height, Color color,
                                          int t_x, int t_y, int t_w, int t_h)
    {
        BufferedImage bi = new BufferedImage(width, height,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.setColor(new Color((float)1.,(float)1.,(float)1.,(float)0.));
        biContext.fillRect(0,0,width,height);
        biContext.setColor(color);
        biContext.fillRect(t_x,t_y,t_w,t_h);
        return bi;
    }

    private static Image createColorizedImage(String filename, Color color, String mapKey, java.util.List directories)
    {
        Image temp = getImage(filename, directories);
        ImageIcon tempIcon = new ImageIcon(temp);
        while (tempIcon.getImageLoadStatus() == MediaTracker.LOADING)
        {
            Thread.yield();
        }
        if (tempIcon.getImageLoadStatus() != MediaTracker.COMPLETE)
        {
            System.err.println("Image loading of " + filename +
                               " failed (" +
                               tempIcon.getImageLoadStatus() + ")");
            return null;
        }
        int width = tempIcon.getIconWidth();
        int height = tempIcon.getIconHeight();
        
        BufferedImage bi = new BufferedImage(width, height,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D biContext = bi.createGraphics();
        biContext.drawImage(temp,
                            0, 0,
                            width, height,
                            null);
        if (bi != null)
        {
            waitOnImage(bi);
        }

        int[] pi;
        WritableRaster ra = bi.getRaster();
        // rebuild the image from the Alpha Channel
        // fully-opaque pixel are set to the color,
        // everything else is white.
        // this should have been a LookupOp, but
        // I couldn't make it reliable across platform :-(
        for (int x = 0; x < width ; x++)
        {
            for (int y = 0 ; y < height ; y++)
            {
                pi = ra.getPixel(x,y, (int[])null);
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
                ra.setPixel(x,y,pi);
            }
        }
        
        synchronized (imageCache)
        {
            if (bi != null)
            {
                imageCache.put(mapKey, bi);
            }
        }
        return bi;
    }

    private static void waitOnImage(Image image)
    {
        ImageIcon icon = new ImageIcon(image);
        while (icon.getImageLoadStatus() == MediaTracker.LOADING)
        {
            Thread.yield();
        }
        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE)
        {
            System.err.println("Image loading failed (" +
                               icon.getImageLoadStatus() + ")");
        }
    }

    private static int numberFromFilename(String filename, String prefix)
    {
        if (!(filename.startsWith(prefix)))
        {
            System.err.println("Warning: " + prefix +
                               " is not prefix of " + filename);
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
                c = filename.charAt(index2);
            else
                c = '*';
        }
        while ((c >= '0') && (c <= '9'))
        {
            index2++;
            if (index2 < filename.length())
                c = filename.charAt(index2);
            else
                c = '*';
        }
        String sub = filename.substring(index,index2);
        int val = 0;
        try
        {
            val = Integer.parseInt(sub);
        }
        catch (Exception e)
        {
            System.err.println("Error during number extraction: " + e);
        }
        return val;
    }

    private static String colorNameFromFilename(String filename, String prefix)
    {
        if (!(filename.startsWith(prefix)))
        {
            System.err.println("Warning: " + prefix +
                               " is not prefix of " + filename);
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
                c = filename.charAt(index2);
            else
                c = '*';
        }
        while ((c >= '0') && (c <= '9'))
        {
            index2++;
            if (index2 < filename.length())
                c = filename.charAt(index2);
            else
                c = '*';
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

    private static Color colorFromFilename(String filename, String prefix)
    {

        return HTMLColor.stringToColor(colorNameFromFilename(filename, prefix));
    }

    private static String fixFilename(String filename)
    {
        return filename.replace(' ', '_');
    }
}
