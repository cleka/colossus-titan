import java.awt.*;
import java.util.*;

/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a monster or a legion.
 * @version $Id$
 * @author David Ripton
 */

class Chit
{
    // The container's MediaTracker needs to access the image.
    Image image;

    private boolean selected;
    private Rectangle rect;
    private Container container;

    // offset of the mouse cursor within the chit.
    private int dx;
    private int dy;


    Chit(int cx, int cy, int scale, String imageFilename,
        Container container)
    {
        selected = false;
        rect = new Rectangle(cx, cy, scale, scale);
        image = Toolkit.getDefaultToolkit().getImage(imageFilename);
        this.container = container;
        dx = 0;
        dy = 0;
    }


    void rescale(int scale)
    {
        dx = 0;
        dy = 0;
        rect.width = scale;
        rect.height = scale;
    }


    public void paint(Graphics g)
    {
        g.drawImage(image, rect.x, rect.y, rect.width, rect.width, container);
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


    void setLocation(Point point)
    {
        point.x -= dx;
        point.y -= dy;
        rect.setLocation(point);
    }


    void setLocationAbs(Point point)
    {
        rect.setLocation(point);
    }


    public Rectangle getBounds()
    {
        return rect;
    }


    public Point center()
    {
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    
    public Point topLeft()
    {
        return new Point(rect.x, rect.y);
    }
}
