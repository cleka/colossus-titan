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


    Hex(int cx, int cy, int scale, BattleMap map)
    {
        this.map = map;

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
            g.setColor(java.awt.Color.red);
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
        else
        {
            g.setColor(java.awt.Color.white);
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
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
        }
    }
    
    
    void removeChit(int i)
    {
        for (int j = i; j < numChits - 1; j++)
        {
            chits[j] = chits[j + 1];
        }
        chits[numChits - 1] = null;
        numChits--;
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
        if (numChits > i)
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
        point.x -= chitScale * (numChits / 2) / 4;
        point.y -= chitScale * (numChits / 2) / 4;

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
        neighbors[i] = hex;
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
