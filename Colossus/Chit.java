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

    public Rectangle getBounds()
    {
        return rect;
    }

    public Point center()
    {
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }
}


class Character
{
    String name;
    int power;
    int skill;
    boolean rangeStrikes; 
    boolean flies;
    boolean nativeBramble;
    boolean nativeDrift;
    boolean nativeBog;
    boolean nativeSandDune;
    boolean nativeSlope;


    // Add various character archetypes as class members
    private static final Character[] characterData =
    {
        new Character("Angel", 6, 4, false, true, false, false, false,
            false, false, true),
        new Character("Archangel", 9, 4, false, true, false, false, false,
            false, false, true),
        new Character("Behemoth", 8, 3, false, false, true, false, false, 
            false, false, false),
        new Character("Centaur", 3, 4, false, false, false, false, false,
            false, false, false),
        new Character("Colossus", 10, 4, false, false, false, true, false,
            false, true, false),
        new Character("Cyclops", 9, 2, false, false, true, false, false,
            false, false, false),
        new Character("Dragon", 9, 3, true, true, false, false, false,
            false, true, false),
        new Character("Gargoyle", 4, 3, false, true, true, false, false,
            false, false, false),
        new Character("Giant", 7, 4, true, false, false, true, false,
            false, false, false),
        new Character("Gorgon", 6, 3, true, true, true, false, false, 
            false, false, false),
        new Character("Griffon", 5, 4, false, true, false, false, false,
            true, false, false),
        new Character("Guardian", 12, 2, false, true, false, false, false,
            false, false, true),
        new Character("Hydra", 10, 3, true, false, false, false, true,
            true, false, false),
        new Character("Lion", 5, 3, false, false, false, false, false,
            true, true, false),
        new Character("Minotaur", 4, 4, true, false, false, false, false,
            false, true, false),
        new Character("Ogre", 6, 2, false, false, false, false, true,
            false, true, false),
        new Character("Ranger", 4, 4, true, true, false, false, true,
            false, false, false),
        new Character("Serpent", 18, 2, false, false, true, false, false,
            false, false, false),
        new Character("Titan", 6, 4, false, false, false, false, false,
            false, false, true)
        new Character("Troll", 8, 2, false, false, false, true, true,
            false, false, false),
        new Character("Unicorn", 6, 4, false, false, false, false, false,
            false, true, false),
        new Character("Warbear", 6, 3, false, false, false, true, false, 
            false, false, false),
        new Character("Warlock", 5, 4, true, false, false, false, false,
            false, false, true),
        new Character("Wyvern", 7, 3, false, true, false, false, true,
            false, false, false),
    };
    
    private static Hashtable lookup = new Hashtable(characterData.length);
    static
    {
        for (int i = 0; i < characterData.length; i++)
        {
            lookup.put(characterData[i].name, characterData[i]);
        }
    }

    Character(String name, int power, int skill, boolean rangeStrikes, 
        boolean flies, boolean nativeBramble, boolean nativeDrift, 
        boolean nativeBog, boolean nativeSandDune, boolean nativeSlope, 
        boolean lord)
    {
        this.name = name;
        this.power = power;
        this.skill = skill;
        this.rangeStrikes = rangeStrikes;
        this.flies = flies;
        this.nativeBramble = nativeBramble;
        this.nativeDrift = nativeDrift;
        this.nativeBog = nativeBog;
        this.nativeSandDune = nativeSandDune;
        this.nativeSlope = nativeSlope;
        this.lord = lord;
    }

    int getPointValue()
    {
        return power * skill;
    }

}



class Legion
{
    Chit chit;
    int size;
    String markerId;    // Bk03, Rd12, etc.
    Character [] chars = new Character[7];
    int pointValue;
}
