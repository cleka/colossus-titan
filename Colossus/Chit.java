import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.awt.geom.*;

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
    public static final String imageDirName = "images";
    public static final String imageExtension = ".gif";
    public static final String invertedPrefix = "i_";

    private static BasicStroke threeWide = new BasicStroke(3);


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
            try
            {
                InputStream in = getClass().getResourceAsStream(imageFilename);
                int length = in.available();
                byte [] thanksToNetscape = new byte[length];
                in.read(thanksToNetscape);
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
        buf.append(imageDirName);
        buf.append(pathSeparator);
        buf.append(basename);
        buf.append(imageExtension);
        return buf.toString();
    }


    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing Chit");
        Container contentPane = frame.getContentPane();
        int scale = 60;

        File dir = new File(imageDirName);
        if (!dir.exists() || !dir.isDirectory())
        {
            System.out.println("No images directory");
            return;
        }
        String [] filenames = dir.list();
        int sqrt = (int)Math.floor(Math.sqrt(filenames.length));
        contentPane.setLayout(new GridLayout(sqrt, 0));
        Arrays.sort(filenames);
        int extLen = imageExtension.length();
        for (int i = 0; i < filenames.length; i++)
        {
            if (filenames[i].endsWith(imageExtension))
            {
                String basename = filenames[i].substring(0,
                    filenames[i].length() - extLen);
                Chit chit = new Chit(scale, basename, frame);
                contentPane.add(chit);
            }
        }

        frame.pack();
        frame.setVisible(true);
    }
}
