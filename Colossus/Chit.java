import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a monster or a legion.
 * @version $Id$
 * @author David Ripton
 */

public class Chit extends Canvas // XXX JPanel
{
    // XXX Use an icon instead?
    private Image image;
    private Rectangle rect;
    private Container container;

    /** Flag to mark chit as dead and paint it with an "X" through it. */
    private boolean dead;

    /** We need to track whether we're running as an applet to load
        images correctly.  setApplet() should be called before any
        objects of this class (or its children) are created. */
    protected static boolean isApplet = false;

    private String id;


    public Chit(int scale, String imageFilename, Container container)
    {
        super();
        Point point = getLocation();
        rect = new Rectangle(point.x, point.y, scale, scale);
        setBounds(rect);

        this.container = container;

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
                image = Toolkit.getDefaultToolkit().createImage(
                    thanksToNetscape);
            }
            catch (Exception e)
            {
                System.out.println("Couldn't load image " + imageFilename + 
                    "\n" + e);
            }
        }
        else
        {
            image = Toolkit.getDefaultToolkit().getImage(
                getClass().getResource(imageFilename));
        }
    }


    public static void setApplet(boolean isApplet)
    {
        Chit.isApplet = isApplet;
    }


    public void setId(String id)
    {
        this.id = id;
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


    //public void paintComponent(Graphics g)
    public void paint(Graphics g)
    {
	//super.paintComponent(g);
	super.paint(g);

        g.drawImage(image, rect.x, rect.y, rect.width, rect.width, container);
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


    public Image getImage()
    {
        return image;
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
}
