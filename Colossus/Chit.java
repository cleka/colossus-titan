import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.awt.geom.*;

import net.sf.colossus.*;

/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a character or a legion.
 * @version $Id$
 * @author David Ripton
 */

public class Chit extends JPanel
{
    private ImageIcon icon;
    private Rectangle rect;
    private Container container;

    /** Flag to mark chit as dead and paint it with an "X" through it. */
    private boolean dead;

    /** Flag to paint a border around the chit. */
    private boolean border;

    private String id;

    // Constants describing where to find image files.

    // File.separator does not work right in jar files.  A hardcoded
    // forward-slash does, and works in *x and Windows.  So I'm ignoring
    // JavaPureCheck's opinion and using a forward slash.
    // XXX Is there a way to detect whether a program is running from a
    // jar file?
    private static final String pathSeparator = "/";
    private static String imageDirName = "images";
    private static final String imageExtension = ".gif";
    public static final String invertedPrefix = "i_";

    private static BasicStroke threeWide = new BasicStroke(3);


    public Chit(int scale, String id, Container container)
    {
        super();
        Point point = getLocation();

        // Images are 60x60, so if scale is close to that, avoid
        // rescaling.
        if (scale > 50 && scale < 70)
        {
            scale = 60;
        }

        rect = new Rectangle(point.x, point.y, scale, scale);
        setBounds(rect);

        this.container = container;
        this.id = id;

        setBackground(Color.lightGray);

        String imageFilename = getImagePath(id);

        icon = getImageIcon(imageFilename);
    }

    /**
     * TMJF -- made this a utility function for the CreatureCollectionView.
     * It seems a step backwards from some of the code Dave is using now
     */
    public ImageIcon getImageIcon(String imageFilename)
    {
        // The image-loading syntax that works correctly for applications
        // packaged in executable jar files does not work correctly for
        // applets, and vice-versa.

        ImageIcon icon = null;

        // This syntax works with either images in a jar file or images
        // in the local filesystem.
        
        try 
        {
            java.net.URL url = getClass().getResource(imageFilename);
            if (url != null)
            {
                Image image = Toolkit.getDefaultToolkit().getImage(url);
                if (image != null)
                {
                    icon = new ImageIcon(image);
                }
            }
            if (icon == null)
            {
                // try direct file access
                Image image = Toolkit.getDefaultToolkit().getImage(
                    imageFilename);
                if (image != null)
                {
                    icon = new ImageIcon(image);
                }
            }
            if (icon == null)
            {
                throw new FileNotFoundException(imageFilename);
            }
        } 
        catch (Exception e) 
        {
            System.out.println("Couldn't get image :" + e);
            System.exit(1);
        }

        return icon;
    }

    private static Chit s_oChit = new Chit(1, "Angel", null);
    public static ImageIcon getImageIconUtility(String imageFilename)
    {
        return s_oChit.getImageIcon(imageFilename);
    }


    public String getId()
    {
        return id;
    }

    public String toString()
    {
        return id;
    }


    public void rescale(int scale)
    {
        rect.width = scale;
        rect.height = scale;
        setBounds(rect);
    }


    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        super.paintComponent(g2);

        g2.drawImage(icon.getImage(), rect.x, rect.y, rect.width,
            rect.height, container);

        if (isDead())
        {
            // Draw a triple-wide red X.
            g2.setStroke(threeWide);
            g2.setColor(Color.red);
            g2.drawLine(rect.x, rect.y, rect.x + rect.width,
                rect.y + rect.height);
            g2.drawLine(rect.x + rect.width, rect.y, rect.x,
                rect.y + rect.height);
        }

        if (border)
        {
            g2.setColor(Color.black);
            Rectangle rect = getBounds();
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
    }


    public void setLocation(Point point)
    {
        rect.setLocation(point);
        setBounds(rect);
    }


    public boolean contains(Point point)
    {
        return rect.contains(point);
    }


    public Rectangle getBounds()
    {
        return rect;
    }


    public Dimension getPreferredSize()
    {
        return new Dimension(rect.width, rect.height);
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }


    public boolean isDead()
    {
        return dead;
    }

    public void setDead(boolean dead)
    {
        this.dead = dead;
    }

    public void toggleDead()
    {
        dead = !dead;
    }


    public void setBorder(boolean border)
    {
        this.border = border;
    }


    /** Return the full path to an image file, given its basename */
    public static String getImagePath(String basename)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(imageDirName);
        buf.append(pathSeparator);
        buf.append(basename);
        buf.append(imageExtension);
        return buf.toString();
    }
}


/**
 * Adapts static methods of Chit so they can be used by the 
 * CreatureCollectionView
 */
class ChitImageUtility implements IImageUtility
{
    public ImageIcon getImageIcon(String strPath)
    {
        return Chit.getImageIconUtility(strPath);
    }

    public String getImagePath(String strName)
    {
        return Chit.getImagePath(strName);
    }
}
