import java.awt.*;
import java.util.*;
import java.awt.geom.*;

/**
 * Class BattleHex describes one Battlemap hex.
 * @version $Id$
 * @author David Ripton
 */

public final class BattleHex extends Hex
{
    private HexMap map;

    private String name;

    /** Normal hexes hold only one creature, but entrances can hold up to 7. */
    private ArrayList critters = new ArrayList(7);

    /** Valid elevations are 0, 1, and 2. */
    private int elevation;

    // Hex terrain types are:
    // p, r, s, t, o, v, d
    // plain, bramble, sand, tree, bog, volcano, drift

    // Hexside terrain types are:
    // d, c, s, w, space
    // dune, cliff, slope, wall, no obstacle
    // The hexside is marked only in the higher hex.
    private char [] hexsides = new char[6];

    private BattleHex [] neighbors = new BattleHex[6];

    private int chitScale;

    private int xCoord;
    private int yCoord;

    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

    /** Movement costs */
    private static final int IMPASSIBLE_COST = 99;
    private static final int SLOW_COST = 2;
    private static final int NORMAL_COST = 1;



    public BattleHex(int cx, int cy, int scale, HexMap map, int xCoord,
        int yCoord)
    {
        this.map = map;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.scale = scale;
        len = scale / 3.0;
        chitScale = 2 * scale;

        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + SQRT3 * scale;
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + 2 * SQRT3 * scale;
        xVertex[4] = cx;
        yVertex[4] = cy + 2 * SQRT3 * scale;
        xVertex[5] = cx - 1 * scale;
        yVertex[5] = cy + SQRT3 * scale;

        hexagon = makePolygon(6, xVertex, yVertex, true);
        rectBound = hexagon.getBounds();
        center = findCenter();

        for (int i = 0; i < 6; i++)
        {
            hexsides[i] = ' ';
        }

        setTerrain('p');
        assignLabel();
    }


    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        if (Game.getAntialias())
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        else
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        if (isSelected())
        {
            g2.setColor(Color.white);
        }
        else
        {
            g2.setColor(getTerrainColor());
        }

        g2.fill(hexagon);
        g2.setColor(Color.black);
        g2.draw(hexagon);


        // Draw hexside features.
        for (int i = 0; i < 6; i++)
        {
            char hexside = hexsides[i];
            int n;
            if (hexside != ' ')
            {
                n = nextHexsideNum(i);
                drawHexside(g2, xVertex[i], yVertex[i], xVertex[n], yVertex[n],
                    hexside);
            }

            // Draw them again from the other side.
            hexside = getOppositeHexside(i);
            if (hexside != ' ')
            {
                n = nextHexsideNum(i);
                drawHexside(g2, xVertex[n], yVertex[n], xVertex[i], yVertex[i],
                    hexside);
            }
        }

        // Do not anti-alias text.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        if (name == null)
        {
            name = getTerrainName().toUpperCase();
        }

        FontMetrics fontMetrics = g2.getFontMetrics();

        g2.drawString(name, rectBound.x + ((rectBound.width -
            fontMetrics.stringWidth(name)) >> 1),
            rectBound.y + ((fontMetrics.getHeight() + rectBound.height) >> 1));

        // Show hex label in upper left corner.
        g2.drawString(label, rectBound.x + (rectBound.width -
            fontMetrics.stringWidth(label)) / 3,
            rectBound.y + ((fontMetrics.getHeight() + rectBound.height) >> 2));
    }


    public void repaint()
    {
        // If an entrance needs repainting, paint the whole map.
        if (isEntrance())
        {
            map.repaint();
        }
        else
        {
            map.repaint(rectBound.x, rectBound.y, rectBound.width,
                rectBound.height);
        }
    }


    public void drawHexside(Graphics2D g2, double vx1, double vy1, double vx2,
        double vy2, char hexsideType)
    {
        double x0;                     // first focus point
        double y0;
        double x1;                     // second focus point
        double y1;
        double x2;                     // center point
        double y2;
        double theta;                  // gate angle
        double [] x = new double[4];   // hexside points
        double [] y = new double[4];   // hexside points


        x0 = vx1 + (vx2 - vx1) / 6;
        y0 = vy1 + (vy2 - vy1) / 6;
        x1 = vx1 + (vx2 - vx1) / 3;
        y1 = vy1 + (vy2 - vy1) / 3;

        theta = Math.atan2(vy2 - vy1, vx2 - vx1);

        switch (hexsideType)
        {
            case 'c':     // cliff -- triangles
                for (int j = 0; j < 3; j++)
                {
                    x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                    y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                    x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                    y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                    x[0] = x0 - len * Math.sin(theta);
                    y[0] = y0 + len * Math.cos(theta);
                    x[1] = (x0 + x1) / 2 + len * Math.sin(theta);
                    y[1] = (y0 + y1) / 2 - len * Math.cos(theta);
                    x[2] = x1 - len * Math.sin(theta);
                    y[2] = y1 + len * Math.cos(theta);

                    GeneralPath polygon = makePolygon(3, x, y, false);

                    g2.setColor(Color.white);
                    g2.fill(polygon);
                    g2.setColor(Color.black);
                    g2.draw(polygon);
                }
                break;

            case 'd':     // dune --  arcs
                for (int j = 0; j < 3; j++)
                {
                    x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                    y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                    x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                    y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                    x[0] = x0 - len * Math.sin(theta);
                    y[0] = y0 + len * Math.cos(theta);
                    x[1] = x0 + len * Math.sin(theta);
                    y[1] = y0 - len * Math.cos(theta);
                    x[2] = x1 + len * Math.sin(theta);
                    y[2] = y1 - len * Math.cos(theta);
                    x[3] = x1 - len * Math.sin(theta);
                    y[3] = y1 + len * Math.cos(theta);

                    x2 = (x0 + x1) / 2;
                    y2 = (y0 + y1) / 2;
                    Rectangle2D.Double rect = new Rectangle2D.Double();
                    rect.x = x2 - len;
                    rect.y = y2 - len;
                    rect.width = 2 * len;
                    rect.height = 2 * len;

                    g2.setColor(Color.white);
                    Arc2D.Double arc = new Arc2D.Double(rect.x, rect.y,
                        rect.width, rect.height,
                        ((2 * Math.PI - theta) * RAD_TO_DEG), 180,
                        Arc2D.OPEN);
                    g2.fill(arc);
                    g2.setColor(Color.black);
                    g2.draw(arc);
                }
                break;

            case 's':     // slope -- lines
                for (int j = 0; j < 3; j++)
                {
                    x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                    y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                    x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                    y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                    x[0] = x0 - len / 3 * Math.sin(theta);
                    y[0] = y0 + len / 3 * Math.cos(theta);
                    x[1] = x0 + len / 3 * Math.sin(theta);
                    y[1] = y0 - len / 3 * Math.cos(theta);
                    x[2] = x1 + len / 3 * Math.sin(theta);
                    y[2] = y1 - len / 3 * Math.cos(theta);
                    x[3] = x1 - len / 3 * Math.sin(theta);
                    y[3] = y1 + len / 3 * Math.cos(theta);

                    g2.setColor(Color.black);
                    g2.draw(new Line2D.Double(x[0], y[0], x[1], y[1]));
                    g2.draw(new Line2D.Double(x[2], y[2], x[3], y[3]));
                }
                break;

            case 'w':     // wall --  blocks
                for (int j = 0; j < 3; j++)
                {
                    x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                    y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                    x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                    y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                    x[0] = x0 - len * Math.sin(theta);
                    y[0] = y0 + len * Math.cos(theta);
                    x[1] = x0 + len * Math.sin(theta);
                    y[1] = y0 - len * Math.cos(theta);
                    x[2] = x1 + len * Math.sin(theta);
                    y[2] = y1 - len * Math.cos(theta);
                    x[3] = x1 - len * Math.sin(theta);
                    y[3] = y1 + len * Math.cos(theta);

                    GeneralPath polygon = makePolygon(4, x, y, false);

                    g2.setColor(Color.white);
                    g2.fill(polygon);
                    g2.setColor(Color.black);
                    g2.draw(polygon);
                }
                break;
        }
    }


    public int getNumCritters()
    {
        return critters.size();
    }


    public boolean isOccupied()
    {
        return (!critters.isEmpty());
    }


    public void addCritter(Critter critter)
    {
        critters.add(critter);
        alignChits();
    }


    public void removeCritter(Critter critter)
    {
        critters.remove(critter);
        alignChits();
    }


    public Critter getCritter()
    {
        if (critters.size() > 0)
        {
            return (Critter)critters.get(0);
        }
        return null;
    }


    private void alignChits()
    {
        if (critters.isEmpty())
        {
            return;
        }

        Point point = new Point(center);

        // Cascade chits diagonally.
        point.x -= chitScale * (1 + (critters.size())) >> 2;
        point.y -= chitScale * (1 + (critters.size())) >> 2;

        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            BattleChit chit = critter.getChit();
            chit.setLocation(point);
            point.x += chitScale >> 2;
            point.y += chitScale >> 2;
        }

        repaint();
    }


    public String getTerrainName()
    {
        switch (getTerrain())
        {
            case 'p':
                switch (elevation)
                {
                    case 0:
                        return "Plains";
                    case 1:
                        return "Plains (1)";
                    case 2:
                        return "Plains (2)";
                }
            case 'r':
                return "Bramble";
            case 's':
                return "Sand";
            case 't':
                return "Tree";
            case 'o':
                return "Bog";
            case 'v':
                return "Volcano (2)";
            case 'd':
                return "Drift";
            default:
                return "?????";
        }
    }


    public Color getTerrainColor()
    {
        switch (getTerrain())
        {
            case 'p':  // plain
                switch (elevation)
                {
                    case 0:
                        return HTMLColor.lightOlive;
                    case 1:
                        return HTMLColor.darkYellow;
                    case 2:
                        return Color.yellow;
                }
            case 'r':  // bramble
                return Color.green;
            case 's':  // sand
                return Color.orange;
            case 't':  // tree
                return HTMLColor.brown;
            case 'o':  // bog
                return Color.gray;
            case 'v':  // volcano
                return Color.red;
            case 'd':  // drift
                return Color.blue;
            default:
                return Color.black;
        }
    }


    private void assignLabel()
    {
        if (xCoord == -1)
        {
            label = "entrance";
            return;
        }

        char xLabel;
        switch (xCoord)
        {
            case 0:
                xLabel = 'A';
                break;
            case 1:
                xLabel = 'B';
                break;
            case 2:
                xLabel = 'C';
                break;
            case 3:
                xLabel = 'D';
                break;
            case 4:
                xLabel = 'E';
                break;
            case 5:
                xLabel = 'F';
                break;
            default:
                xLabel = '?';
        }

        int yLabel = 6 - yCoord - (int)Math.abs(((xCoord - 3) / 2));
        label = xLabel + Integer.toString(yLabel);
    }


    public void setHexside(int i, char hexside)
    {
        this.hexsides[i] = hexside;
    }


    public char getHexside(int i)
    {
        return hexsides[i];
    }


    /** Return the flip side of hexside i. */
    public char getOppositeHexside(int i)
    {
        char hexside = ' ';

        BattleHex neighbor = getNeighbor(i);
        if (neighbor != null)
        {
            hexside = neighbor.getHexside(oppositeHexsideNum(i));
        }

        return hexside;
    }


    public void setElevation (int elevation)
    {
        this.elevation = elevation;
    }


    public int getElevation()
    {
        return elevation;
    }


    public void setNeighbor(int i, BattleHex hex)
    {
        if (i >= 0 && i < 6)
        {
            neighbors[i] = hex;
        }
    }


    public BattleHex getNeighbor(int i)
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


    public int getXCoord()
    {
        return xCoord;
    }


    public int getYCoord()
    {
        return yCoord;
    }


    public boolean isEntrance()
    {
        return (xCoord == -1);
    }


    public boolean hasWall()
    {
        for (int i = 0; i < 6; i++)
        {
            if (hexsides[i] == 'w')
            {
                return true;
            }
        }
        return false;
    }


    /** Return the number of movement points it costs to enter this hex.
     *  For fliers, this is the cost to land in this hex, not fly over it.
     *  If entry is illegal, just return a cost greater than the maximum
     *  possible number of movement points. */
    public int getEntryCost(Creature creature, int cameFrom)
    {
        char terrain = getTerrain();

        // Check to see if the hex is occupied or totally impassable.
        if (isOccupied() || terrain == 't' || (terrain == 'v' &&
            !creature.getName().equals("Dragon")) || (terrain == 'o' &&
            !creature.isNativeBog()))
        {
            return IMPASSIBLE_COST;
        }

        char hexside = getHexside(cameFrom);

        // Non-fliers may not cross cliffs.
        if ((hexside == 'c' || getOppositeHexside(cameFrom) == 'c') &&
            !creature.isFlier())
        {
            return IMPASSIBLE_COST;
        }

        // Check for a slowing hexside.
        if ((hexside == 'w' || (hexside == 's' && !creature.isNativeSlope()))
            && !creature.isFlier() &&
            elevation > getNeighbor(cameFrom).getElevation())
        {
            // All hexes where this applies happen to have no
            // additional movement costs.
            return SLOW_COST;
        }

        // Bramble, drift, and sand slow non-natives, except that sand
        //     doesn't slow fliers.
        if ((terrain == 'r' && !creature.isNativeBramble()) ||
            (terrain == 'd' && !creature.isNativeDrift()) ||
            (terrain == 's' && !creature.isNativeSandDune() &&
            !creature.isFlier()))
        {
            return SLOW_COST;
        }

        // Other hexes only cost 1.
        return NORMAL_COST;
    }
}
