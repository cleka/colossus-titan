import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a monster or a legion.
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

    /** We need to track whether we're running as an applet to load
        images correctly.  setApplet() should be called before any
        objects of this class (or its children) are created. */
    protected static boolean isApplet = false;

    private String id;

    // Constants describing where to find image files.

    // File.separator does not work right in jar files.  A hardcoded
    // forward-slash does, and works in *x and Windows.  So I'm ignoring
    // JavaPureCheck's opinion and using a forward slash.
    // XXX Is there a way to detect whether a program is running from a
    // jar file?
    public static final String pathSeparator = "/";
    public static final String imageDirname = "images";
    public static final String imageExtension = ".gif";
    public static final String invertedPrefix = "i_";


    public Chit(int scale, String id, Container container)
    {
        super();
        Point point = getLocation();
        rect = new Rectangle(point.x, point.y, scale, scale);
        setBounds(rect);

        this.container = container;
        this.id = id;

        setBackground(Color.lightGray);

        String imageFilename = getImagePath(id);

        // The image-loading syntax that works correctly for applications
        // packaged in executable jar files does not work correctly for
        // applets, and vice-versa.

        if (isApplet)
        {
            InputStream in;
            byte[] thanksToNetscape = null;

            try
            {
                in = getClass().getResourceAsStream(imageFilename);
                int length = in.available();
                thanksToNetscape = new byte[length];
                in.read(thanksToNetscape);
                // XXX Test this with browsers.
                icon = new ImageIcon(thanksToNetscape);
            }
            catch (Exception e)
            {
                System.out.println("Couldn't load image " + imageFilename +
                    "\n" + e);
            }
        }
        else
        {
            // This syntax works with either images in a jar file or images
            // in the local filesystem.
            Image image = Toolkit.getDefaultToolkit().getImage(
                getClass().getResource(imageFilename));
            icon = new ImageIcon(image);
        }
    }


    public static void setApplet(boolean isApplet)
    {
        Chit.isApplet = isApplet;
    }


    public String getId()
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
	super.paintComponent(g);

        g.drawImage(icon.getImage(), rect.x, rect.y, rect.width,
            rect.width, container);
        if (isDead())
        {
            // Draw a triple-wide red X.
            g.setColor(Color.red);

            g.drawLine(rect.x, rect.y, rect.x + rect.width,
                rect.y + rect.height);
            g.drawLine(rect.x, rect.y - 1, rect.x + rect.width - 1,
                rect.y + rect.height);
            g.drawLine(rect.x + 1, rect.y, rect.x + rect.width,
                rect.y + rect.height - 1);

            g.drawLine(rect.x + rect.width, rect.y, rect.x,
                rect.y + rect.height);
            g.drawLine(rect.x + rect.width - 1, rect.y, rect.x,
                rect.y + rect.height - 1);
            g.drawLine(rect.x + rect.width, rect.y + 1, rect.x + 1,
                rect.y + rect.height);
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


    /** Return the full path to an image file, given its basename */
    public static String getImagePath(String basename)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(imageDirname);
        buf.append(pathSeparator);
        buf.append(basename);
        buf.append(imageExtension);
        return buf.toString();
    }
}

