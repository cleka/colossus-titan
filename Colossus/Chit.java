import java.awt.*;
import java.io.*;

/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a monster or a legion.
 * @version $Id$
 * @author David Ripton
 */

class Chit extends Canvas
{
    private Image image;
    private boolean selected = false;
    private Rectangle rect;
    private static Container container;

    // Offset of the mouse cursor within the chit.
    private int dx = 0;
    private int dy = 0;

    // Flag to mark chit as dead and paint it with an "X" through it.
    private boolean dead = false;

    // We need to track whether we're running as an applet to load
    // images correctly.  setApplet() should be called before any
    // objects of this class (or its children) are created.
    protected static boolean isApplet = false;


    Chit(int cx, int cy, int scale, String imageFilename,
        Container container)
    {
        // If cx or cy is set to -1, that means that the Chit
        // should allow the layout manager to place it.
        if (cx == -1 || cy == -1)
        {
            Point point = getLocation();
            cx = point.x;
            cy = point.y;
        }

        rect = new Rectangle(cx, cy, scale, scale);
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
    

    void rescale(int scale)
    {
        dx = 0;
        dy = 0;
        rect.width = scale;
        rect.height = scale;
        setBounds(rect);
    }


    public void paint(Graphics g)
    {
        g.drawImage(image, rect.x, rect.y, rect.width, rect.width, container);
        if (dead)
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


    boolean select(Point point)
    {
        if (rect.contains(point))
        {
            selected = true;
            dx = point.x - rect.x;
            dy = point.y - rect.y;
        }
        else
        {
            selected = false;
        }
        return selected;
    }


    void setLocationAbs(Point point)
    {
        rect.setLocation(point);
        setBounds(rect);
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
