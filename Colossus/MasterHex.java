import java.awt.*;
import java.awt.event.*;

/**
 * Class MasterHex describes one Masterboard hex
 * @version $Id$
 * @author David Ripton
 */

class MasterHex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    public static final double RAD_TO_DEG = 180 / Math.PI;

    private int[] xVertex = new int[6];
    private int[] yVertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;
    private boolean inverted;
    private int scale;
    private double l;              // hexside length
    private int numLegions = 0;
    private Legion [] legions = new Legion[3];
    private boolean selected = false;
    private MasterBoard board;

    private MasterHex [] neighbors = new MasterHex[6];
    
    // B,D,H,J,m,M,P,S,T,t,W
    // Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
    // Swamp, Tower, tundra, Woods
    private char terrain;

    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000
    private int label;

    // n, ne, se, s, sw, nw
    private int[] exitType = new int[6];
    private int[] entranceType = new int[6];

    // 0=none, 1=block, 2=arch, 3=arrow 4=arrows
    public static final int NONE = 0;
    public static final int BLOCK = 1;
    public static final int ARCH = 2;
    public static final int ARROW = 3;
    public static final int ARROWS = 4;


    MasterHex(int cx, int cy, int scale, boolean inverted, MasterBoard board)
    {
        this.inverted = inverted;
        this.scale = scale;
        this.board = board;
        l = scale / 3.0;
        if (inverted)
        {
            xVertex[0] = cx - scale;
            yVertex[0] = cy;
            xVertex[1] = cx + 3 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
            xVertex[3] = cx + 2 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(SQRT3 * scale);
        }
        else
        {
            xVertex[0] = cx;
            yVertex[0] = cy;
            xVertex[1] = cx + 2 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(2 * SQRT3 * scale);
            xVertex[3] = cx + 3 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx - scale;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(2 * SQRT3 * scale);
        }

        p = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short of the area actually painted.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] -
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);
    }


    void rescale(int cx, int cy, int scale)
    {
        this.scale = scale;
        l = scale / 3.0;
        if (inverted)
        {
            xVertex[0] = cx - scale;
            yVertex[0] = cy;
            xVertex[1] = cx + 3 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
            xVertex[3] = cx + 2 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(SQRT3 * scale);
        }
        else
        {
            xVertex[0] = cx;
            yVertex[0] = cy;
            xVertex[1] = cx + 2 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + (int) Math.round(2 * SQRT3 * scale);
            xVertex[3] = cx + 3 * scale;
            yVertex[3] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[4] = cx - scale;
            yVertex[4] = cy + (int) Math.round(3 * SQRT3 * scale);
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + (int) Math.round(2 * SQRT3 * scale);
        }

        p.xpoints = xVertex;
        p.ypoints = yVertex;

        // Add 1 to width and height because Java rectangles come up
        // one pixel short of the area actually painted.
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
        String sLabel = Integer.toString(label);
        String sName = getTerrainName();
        int fontHeight = fontMetrics.getMaxAscent() + 
            fontMetrics.getLeading();

        if (inverted)
        {
            g.drawString(sLabel, rectBound.x + (rectBound.width - 
                fontMetrics.stringWidth(sLabel)) / 2,
                rectBound.y + rectBound.height * 19 / 20);
            g.drawString(sName, rectBound.x + (rectBound.width -
                fontMetrics.stringWidth(sName)) / 2,
                rectBound.y + fontHeight + rectBound.height / 4);
        }
        else
        {
            g.drawString(sLabel, rectBound.x + (rectBound.width -
                fontMetrics.stringWidth(sLabel)) / 2,
                rectBound.y + fontHeight + rectBound.height / 20 );
            g.drawString(sName, rectBound.x + (rectBound.width -
                fontMetrics.stringWidth(sName)) / 2,
                rectBound.y + rectBound.height * 3 / 4);
        }


        // Draw exits and entrances
        for (int i = inverted ? 0 : 1; i < 6; i += 2)
        {
            int n = (i + 1) % 6;

            // Draw exits
            // There are up to 3 gates to draw.  Each is 1/6 of a hexside
            // square.  The first is positioned from 1/6 to 1/3 of the way
            // along the hexside, the second from 5/12 to 7/12, and the
            // third from 2/3 to 5/6.  The inner edge of each is 1/12 of a
            // hexside inside the hexside, and the outer edge is 1/12 of a
            // hexside outside the hexside.

            if (exitType[i] != NONE)
            {
                drawGate(g, xVertex[i], yVertex[i], xVertex[n], yVertex[n],
                                exitType[i]);
            }

            // Draw entrances
            // Unfortunately, since exits extend out into adjacent hexes,
            // they sometimes get overdrawn.  So we need to draw them
            // again from the other hex, as entrances.

            if (entranceType[i] != NONE)
            {
                drawGate(g, xVertex[n], yVertex[n], xVertex[i], yVertex[i],
                                entranceType[i]);
            }
        }
    }


    void repaint()
    {
        board.repaint(rectBound.x, rectBound.y, rectBound.width, 
            rectBound.height);
    }


    void drawGate(Graphics g, int vx1, int vy1, int vx2, int vy2, int gateType)
    {
        int x0;                 // first focus point
        int y0;
        int x1;                 // second focus point
        int y1;
        int x2;                 // center point
        int y2;
        double theta;           // gate angle
        int [] x = new int[4];  // gate points
        int [] y = new int[4];

        x0 = vx1 + (vx2 - vx1) / 6;
        y0 = vy1 + (vy2 - vy1) / 6;
        x1 = vx1 + (vx2 - vx1) / 3;
        y1 = vy1 + (vy2 - vy1) / 3;

        theta = Math.atan2(vy2 - vy1, vx2 - vx1);

        switch(gateType)
        {
            case BLOCK:
                x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                x[1] = (int) Math.round(x0 + l * Math.sin(theta));
                y[1] = (int) Math.round(y0 - l * Math.cos(theta));
                x[2] = (int) Math.round(x1 + l * Math.sin(theta));
                y[2] = (int) Math.round(y1 - l * Math.cos(theta));
                x[3] = (int) Math.round(x1 - l * Math.sin(theta));
                y[3] = (int) Math.round(y1 + l * Math.cos(theta));

                g.setColor(java.awt.Color.white);
                g.fillPolygon(x, y, 4);
                g.setColor(java.awt.Color.black);
                g.drawPolyline(x, y, 4);
                break;

            case ARCH:
                x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                x[1] = (int) Math.round(x0 + l * Math.sin(theta));
                y[1] = (int) Math.round(y0 - l * Math.cos(theta));
                x[2] = (int) Math.round(x1 + l * Math.sin(theta));
                y[2] = (int) Math.round(y1 - l * Math.cos(theta));
                x[3] = (int) Math.round(x1 - l * Math.sin(theta));
                y[3] = (int) Math.round(y1 + l * Math.cos(theta));

                x2 = (int) Math.round((x0 + x1) / 2);
                y2 = (int) Math.round((y0 + y1) / 2);
                Rectangle rect = new Rectangle();
                rect.x = x2 - (int) Math.round(l);
                rect.y = y2 - (int) Math.round(l);
                rect.width = (int) (2 * Math.round(l));
                rect.height = (int) (2 * Math.round(l));
                
                g.setColor(java.awt.Color.white);
                // Draw a bit more than a semicircle, to clean edge.
                g.fillArc(rect.x, rect.y, rect.width, rect.height,
                    (int) Math.round((2 * Math.PI - theta) * 
                    RAD_TO_DEG - 10), 200);
                g.setColor(java.awt.Color.black);
                g.drawArc(rect.x, rect.y, rect.width, rect.height,
                    (int) Math.round((2 * Math.PI - theta) * RAD_TO_DEG), 
                    180);
                        
                x[2] = x[0];
                y[2] = y[0];
                x[0] = x1;
                y[0] = y1;
                x[1] = x[3];
                y[1] = y[3];
                x[3] = x0;
                y[3] = y0;
                g.setColor(java.awt.Color.white);
                g.fillPolygon(x, y, 4);
                g.setColor(java.awt.Color.black);
                g.drawLine(x1, y1, x[1], y[1]);
                g.drawLine(x[2], y[2], x0, y0);
                break;

            case ARROW:
                x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                x[1] = (int) Math.round((x0 + x1) / 2 + l * 
                    Math.sin(theta));
                y[1] = (int) Math.round((y0 + y1) / 2 - l * 
                    Math.cos(theta));
                x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                y[2] = (int) Math.round(y1 + l * Math.cos(theta));

                g.setColor(java.awt.Color.white);
                g.fillPolygon(x, y, 3);
                g.setColor(java.awt.Color.black);
                g.drawPolyline(x, y, 3);
                break;

            case ARROWS:
                for (int j = 0; j < 3; j++)
                {
                    x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                    y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;

                    x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                    y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                    x[0] = (int) Math.round(x0 - l * Math.sin(theta));
                    y[0] = (int) Math.round(y0 + l * Math.cos(theta));
                    x[1] = (int) Math.round((x0 + x1) / 2 + l * 
                           Math.sin(theta));
                    y[1] = (int) Math.round((y0 + y1) / 2 - l * 
                           Math.cos(theta));
                    x[2] = (int) Math.round(x1 - l * Math.sin(theta));
                    y[2] = (int) Math.round(y1 + l * Math.cos(theta));
    
                    g.setColor(java.awt.Color.white);
                    g.fillPolygon(x, y, 3);
                    g.setColor(java.awt.Color.black);
                    g.drawPolyline(x, y, 3);
                }
                break;
        }
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


    public Rectangle getBounds()
    {
        return rectBound;
    }


    public boolean contains(Point point)
    {
        return (p.contains(point));
    }


    char getTerrain()
    {
        return terrain;
    }
    
    void setTerrain(char terrain)
    {
        this.terrain = terrain;
    }

    String getTerrainName()
    {
        switch(terrain)
        {
            case 'B':
                return "BRUSH";
            case 'D':
                return "DESERT";
            case 'H':
                return "HILLS";
            case 'J':
                return "JUNGLE";
            case 'm':
                return "MOUNTAINS";
            case 'M':
                return "MARSH";
            case 'P':
                return "PLAINS";
            case 'S':
                return "SWAMP";
            case 'T':
                return "TOWER";
            case 't':
                return "TUNDRA";
            case 'W':
                return "WOODS";
            default:
                return "?????";
        }
    }

    Color getTerrainColor()
    {
        switch(terrain)
        {
            case 'B':
                return java.awt.Color.green;
            case 'D':
                return java.awt.Color.orange;
            case 'H':
                return new Color(128, 64, 0);
            case 'J':
                return new Color(0, 128, 0);
            case 'm':
                return java.awt.Color.red;
            case 'M':
                return new Color(180, 90, 0);
            case 'P':
                return java.awt.Color.yellow;
            case 'S':
                return java.awt.Color.blue;
            case 'T':
                return java.awt.Color.gray;
            case 't':
                return new Color(128, 170, 255);
            case 'W':
                return new Color(128, 128, 0);
            default:
                return java.awt.Color.black;
        }
    }


    // Return a point near the center of the hex, vertically offset
    // a bit toward the fat side.
    Point getOffCenter()
    {
        return new Point((xVertex[0] + xVertex[1]) / 2, (yVertex[0] + 
            yVertex[3]) / 2 + (inverted ? -(scale / 6) : (scale / 6))); 
    }


    int getNumLegions()
    {
        return numLegions;
    }


    Legion getLegion(int i)
    {
        if (i < 0 || i > numLegions - 1)
        {
            return null;
        }
        else
        {
            return legions[i];
        }
    }


    int getNumFriendlyLegions(Player player)
    {
        int count = 0;
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() == player)
            {
                count++;
            }
        }
        return count;
    }

    
    int getNumEnemyLegions(Player player)
    {
        int count = 0;
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() != player)
            {
                count++;
            }
        }
        return count;
    }


    boolean isEngagement()
    {
        if (numLegions > 0)
        {
            Player player = legions[0].getPlayer();
            for (int i = 1; i < numLegions; i++)
            {
                if (legions[i].getPlayer() != player)
                {
                    return true;
                }
            }
        }

        return false;
    }


    Legion getFriendlyLegion(Player player)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() == player)
            {
                return legions[i];
            }
        }
        return null;
    }


    Legion getEnemyLegion(Player player)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i].getPlayer() != player)
            {
                return legions[i];
            }
        }
        return null;
    }


    void alignLegions()
    {
        if (numLegions == 0)
        {
            return;
        }

        int chitScale = legions[0].getMarker().getBounds().width;
        Point point = getOffCenter();

        if (numLegions == 1)
        {
            // Place legion in the center of the hex.
            point.x -= chitScale / 2;
            point.y -= chitScale / 2;
            legions[0].getMarker().setLocationAbs(point);     
        }
        else if (numLegions == 2)
        {
            // Place legions in NW and SE corners.
            point.x -= 3 * chitScale / 4;
            point.y -= 3 * chitScale / 4;
            legions[0].getMarker().setLocationAbs(point);

            point = getOffCenter();
            point.x -= chitScale / 4;
            point.y -= chitScale / 4;
            legions[1].getMarker().setLocationAbs(point);
        }
        else if (numLegions == 3)
        {
            // Place legions in NW, SE, NE corners.
            point.x -= 3 * chitScale / 4;
            point.y -= 3 * chitScale / 4;
            legions[0].getMarker().setLocationAbs(point);

            point = getOffCenter();
            point.x -= chitScale / 4;
            point.y -= chitScale / 4;
            legions[1].getMarker().setLocationAbs(point);

            point = getOffCenter();
            point.x -= chitScale / 4;
            point.y -= chitScale;
            legions[2].getMarker().setLocationAbs(point);
        }
    }
    
    
    void addLegion(Legion legion)
    {
        numLegions++;
        legions[numLegions - 1] = legion;

        // Reposition all legions within the hex.
        alignLegions();
    }


    void removeLegion(Legion legion)
    {
        for (int i = 0; i < numLegions; i++)
        {
            if (legions[i] == legion)
            {
                for (int j = i; j < numLegions - 1; j++)
                {
                    legions[j] = legions[j + 1];
                }
                legions[numLegions - 1] = null;
                numLegions--;

                // Write over the bounding area of the hex with
                // the background color, to prevent artifacts from
                // chits that used to hang outside the hex boundary.
                if (numLegions >= 1)
                {
                    board.setEraseFlag();
                }

                // Reposition all legions within the hex.
                alignLegions();
                return;
            }
        }
    }


    void setNeighbor(int i, MasterHex hex)
    {
        neighbors[i] = hex;
    }


    MasterHex getNeighbor(int i)
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


    int getLabel()
    {
        return label;
    }


    void setLabel(int label)
    {
        this.label = label;
    }


    void setExitType(int i, int exitType)
    {
        this.exitType[i] = exitType;
    }

    int getExitType(int i)
    {
        return exitType[i];
    }
    
    void setEntranceType(int i, int entranceType)
    {
        this.entranceType[i] = entranceType;
    }

    int getEntranceType(int i)
    {
        return entranceType[i];
    }
}
