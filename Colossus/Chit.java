import java.awt.*;

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
        Container myContainer)
    {
        selected = false;
        rect = new Rectangle(cx, cy, scale, scale);
        image = Toolkit.getDefaultToolkit().getImage(imageFilename);
        container = myContainer;
        dx = 0;
        dy = 0;
    }

    public void paint(Graphics g)
    {
        g.drawImage(image, rect.x, rect.y, container);
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

    public Rectangle getBounds()
    {
        return rect;
    }

}


class Character
{
    String name;
    int power;
    int skill;
    boolean rangeStrikes; 
    boolean flies;
    int pointValue;
    boolean nativeBramble;
    boolean nativeDrift;
    boolean nativeVolcano;
    boolean nativeBog;
    boolean nativeSand;
    boolean nativeDune;
    boolean nativeSlope;
}



class Legion
{
    Chit chit;
    int size;
    String markerId;    // Bk03, Rd12, etc.
    Character [] chars = new Character[7];
    int pointValue;
    
}

