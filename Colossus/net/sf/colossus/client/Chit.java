package net.sf.colossus.client;


import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.awt.geom.*;
import java.net.*;


/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a character or a legion.
 * @version $Id$
 * @author David Ripton
 */

class Chit extends JPanel
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

    private static BasicStroke threeWide = new BasicStroke(3);


    Chit(int scale, String id, Container container)
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

    static ImageIcon getImageIcon(String imageFilename)
    {
        ImageIcon icon = null;

        try 
        {
            URL url;
            // try first with the var-specific directory
            try {
                url = new URL("file:" + GetPlayers.getVarDirectory() +
                              imageFilename);
                // url will not be null even is the file doesn't exist,
                // so we need to check if connection can be opened
                if ((url != null) && (url.openStream() != null))
                {
                    Image image = Toolkit.getDefaultToolkit().getImage(url);
                    if (image != null)
                    {
                        icon = new ImageIcon(image);
                    }
                }
            } catch (Exception e) {}
            // try second with the default loader
            if (icon == null)
            {
                ClassLoader cl = Client.class.getClassLoader();
                url = cl.getResource(imageFilename);
                if (url != null)
                {
                    icon = new ImageIcon(url);
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


    String getId()
    {
        return id;
    }

    public String toString()
    {
        return id;
    }


    void rescale(int scale)
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


    boolean isDead()
    {
        return dead;
    }

    void setDead(boolean dead)
    {
        this.dead = dead;
    }

    void toggleDead()
    {
        dead = !dead;
    }


    void setBorder(boolean border)
    {
        this.border = border;
    }


    /** Return the full path to an image file, given its basename */
    static String getImagePath(String basename)
    {
        return imageDirName + pathSeparator + basename + imageExtension;
    }
}
