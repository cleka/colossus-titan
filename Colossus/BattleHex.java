import java.awt.*;
import java.awt.event.*;

/**
 * Class Hex describes one Battlemap hex.
 * @version $Id$
 * @author David Ripton
 */

class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    private boolean selected;
    private int[] xVertex = new int[6];
    private int[] yVertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;

    private BattleMap map;

    // Normal hexes hold only one chit, but entrances can hold up to 7.
    int numChits = 0;
    private BattleChit [] chits = new BattleChit[7];

    // Valid elevations are 0, 1, and 2.
    private int elevation = 0;

    // p, r, s, t, o, v, d
    // plain, bramble, sand, tree, bog, volcano, drift
    private char terrain = 'p';

    // d, c, s, w, space
    // dune, cliff, slope, wall, no obstacle
    // The hexside is marked only in the higher hex.
    private char [] hexside = new char[6];

    private Hex [] neighbors = new Hex[6];

    private int xCoord;
    private int yCoord;


    Hex(int cx, int cy, int scale, BattleMap map, int xCoord, int yCoord)
    {
        this.map = map;
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        selected = false;

        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[4] = cx;
        yVertex[4] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[5] = cx - 1 * scale;
        yVertex[5] = cy + (int) Math.round(SQRT3 * scale);

        p = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] -
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);

        for (int i = 0; i < 6; i++)
        {
            hexside[i] = ' ';
        }
    }


    void rescale(int cx, int cy, int scale)
    {
        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[4] = cx;
        yVertex[4] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[5] = cx - scale;
        yVertex[5] = cy + (int) Math.round(SQRT3 * scale);

        // The hit testing breaks if we just reassign the vertices
        // of the old polygon.
        p = new Polygon(xVertex, yVertex, 6);

        // Add 1 to width and height because Java rectangles come up
        // one pixel short.
        rectBound.x =  xVertex[5];
        rectBound.y =  yVertex[0];
        rectBound.width = xVertex[2] - xVertex[5] + 1;
        rectBound.height = yVertex[3] - yVertex[0] + 1;
    }


    public void paint(Graphics g)
    {
        if (selected)
        {
            g.setColor(java.awt.Color.white);
        }
        else
        {
            g.setColor(getTerrainColor());
        }

        g.fillPolygon(p);
        g.setColor(java.awt.Color.black);
        g.drawPolygon(p);

        FontMetrics fontMetrics = g.getFontMetrics();
        String name = getTerrainName();
        int fontHeight = fontMetrics.getMaxAscent() +
            fontMetrics.getLeading();

        g.drawString(name, rectBound.x + (rectBound.width -
            fontMetrics.stringWidth(name)) / 2,
            rectBound.y + (fontMetrics.getHeight() + rectBound.height) / 2);

        // XXX: Draw hexside features.
    }


    void repaint()
    {
        map.repaint(rectBound.x, rectBound.y, rectBound.width,
            rectBound.height);
    }


    boolean select(Point point)
    {
        if (p.contains(point))
        {
            selected = !selected;
            return true;
        }
        return false;
    }

    void select()
    {
        selected = true;
    }

    void unselect()
    {
        selected = false;
    }

    boolean isSelected()
    {
        return selected;
    }

    boolean isSelected(Point point)
    {
        return (p.contains(point) && selected == true);
    }


    Point getCenter()
    {
        return new Point((xVertex[0] + xVertex[3]) / 2,
            (yVertex[0] + yVertex[3]) / 2);
    }


    public Rectangle getBounds()
    {
        return rectBound;
    }


    public boolean contains(Point point)
    {
        return (p.contains(point));
    }


    int getNumChits()
    {
        return numChits;
    }


    boolean isOccupied()
    {
        return (numChits > 0);
    }


    void addChit(BattleChit chit)
    {
        if (numChits < 7)
        {
            chits[numChits] = chit;
            numChits++;
            alignChits();
        }
    }


    void removeChit(int i)
    {
        if (i >= 0 && i < numChits)
        {
            for (int j = i; j < numChits - 1; j++)
            {
                chits[j] = chits[j + 1];
            }
            chits[numChits - 1] = null;
            numChits--;

            // XXX: Do only for entrances?
            map.setEraseFlag();

            // Reposition all chits within the hex.
            alignChits();
        }
    }


    void removeChit(Chit chit)
    {
        for (int i = 0; i < numChits; i++)
        {
            if (chits[i] == chit)
            {
                removeChit(i);
            }
        }
    }


    BattleChit getChit()
    {
        if (numChits > 0)
        {
            return chits[0];
        }
        else
        {
            return null;
        }
    }


    BattleChit getChit(int i)
    {
        if (i >= 0 && i < numChits)
        {
            return chits[i];
        }
        else
        {
            return null;
        }
    }


    void alignChits()
    {
        if (numChits == 0)
        {
            return;
        }

        int chitScale = chits[0].getBounds().width;
        Point point = getCenter();

        // Cascade chits diagonally.
        point.x -= chitScale * (1 + (numChits)) / 4;
        point.y -= chitScale * (1 + (numChits)) / 4;

        for (int i = 0; i < numChits; i++)
        {
            chits[i].setLocationAbs(point);
            point.x += chitScale / 4;
            point.y += chitScale / 4;
        }
    }


    void setTerrain(char terrain)
    {
        this.terrain = terrain;
    }

    char getTerrain()
    {
        return terrain;
    }

    String getTerrainName()
    {
        switch (terrain)
        {
            case 'p':
                switch (elevation)
                {
                    case 0:
                        return "PLAINS";
                    case 1:
                        return "PLAINS (1)";
                    case 2:
                        return "PLAINS (2)";
                }
            case 'r':
                return "BRAMBLE";
            case 's':
                return "SAND";
            case 't':
                return "TREE";
            case 'o':
                return "BOG";
            case 'v':
                return "VOLCANO";
            case 'd':
                return "DRIFT";
            default:
                return "?????";
        }

    }

    Color getTerrainColor()
    {
        switch (terrain)
        {
            case 'p':  // plain
                switch (elevation)
                {
                    case 0:
                        return new Color(150, 150, 0);
                    case 1:
                        return new Color(200, 200, 0);
                    case 2:
                        return java.awt.Color.yellow;
                }
            case 'r':  // bramble
                return java.awt.Color.green;
            case 's':  // sand
                return java.awt.Color.orange;
            case 't':  // tree
                return new Color(180, 90, 0);
            case 'o':  // bog
                return java.awt.Color.gray;
            case 'v':  // volcano
                return java.awt.Color.red;
            case 'd':  // drift
                return java.awt.Color.blue;
            default:
                return java.awt.Color.black;
        }
    }


    void setHexside(int i, char hexside)
    {
        this.hexside[i] = hexside;
    }

    char getHexside(int i)
    {
        if (i < 0 || i > 5)
        {
            return ' ';
        }
        else
        {
            return hexside[i];
        }
    }


    void setElevation (int elevation)
    {
        this.elevation = elevation;
    }

    int getElevation()
    {
        return elevation;
    }


    void setNeighbor(int i, Hex hex)
    {
        if (i >= 0 && i < 6)
        {
            neighbors[i] = hex;
        }
        else
        {
            System.out.println("bad setNeighbor " + i);
        }
    }

    Hex getNeighbor(int i)
    {
        if (i < 0 || i > 6)
        {
            return null;
        }
        else
        {
            return neighbors[i];
        }
    }


    int getXCoord()
    {
        return xCoord;
    }


    int getYCoord()
    {
        return yCoord;
    }


    // Return the number of movement points it costs to enter this hex.
    // For fliers, this is the cost to land in this hex, not fly over it.
    // If entry is illegal, just return a cost greater than the maximum
    // possible number of movement points.
    int getEntryCost(Creature creature, int cameFrom)
    {
        // Check to see if the hex is occupied or totally impassable.
        if (isOccupied() || terrain == 't' || (terrain == 'v' && creature !=
            Creature.dragon) || (terrain == 'o' && !creature.isNativeBog()))
        {
            return 5;
        }

        char hexside = getHexside(cameFrom);

        // Non-fliers may not cross cliffs.
        if (hexside == 'c' && !creature.flies())
        {
            return 5;
        }

        // Check for a slowing hexside.
        if ((hexside == 'w' || (hexside == 's' && !creature.isNativeSlope()))
            && !creature.flies() &&
            elevation > getNeighbor(cameFrom).getElevation())
        {
            // All hexes where this applies happen to have no
            // additional movement costs.
            return 2;
        }

        // Bramble, drift, and sand slow non-natives.
        if ((terrain == 'r' && !creature.isNativeBramble()) ||
            (terrain == 'd' && !creature.isNativeDrift()) ||
            (terrain == 's' && !creature.isNativeSandDune()))
        {
            return 2;
        }

        // Other hexes only cost 1.
        return 1;
    }
}
