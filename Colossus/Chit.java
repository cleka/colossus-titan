import java.awt.*;
import java.io.*;

/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a monster or a legion.
 * @version $Id$
 * @author David Ripton
 */

public class Chit extends Canvas
{
    private Image image;
    private Rectangle rect;
    private static Container container;

    // Flag to mark chit as dead and paint it with an "X" through it.
    private boolean dead = false;

    // We need to track whether we're running as an applet to load
    // images correctly.  setApplet() should be called before any
    // objects of this class (or its children) are created.
    protected static boolean isApplet = false;


    public Chit(int scale, String imageFilename, Container container)
    {
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
    

    public void rescale(int scale)
    {
        rect.width = scale;
        rect.height = scale;
        setBounds(rect);
    }


    public void paint(Graphics g)
    {
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
    
    
    public void repaint()
    {
        container.repaint(rect.x, rect.y, rect.width, rect.height);
        super.repaint();
    }


    public void setLocationAbs(Point point)
    {
        rect.setLocation(point);
        setBounds(rect);
    }


    public boolean select(Point point)
    {
        if (rect.contains(point))
        {
            return true;
        }
        else
        {
            return false;
        }
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
