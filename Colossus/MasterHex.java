import java.awt.*;
import java.util.*;

/**
 * Class MasterHex describes one Masterboard hex
 * @version $Id$
 * @author David Ripton
 */

public class MasterHex extends Hex
{
    private boolean inverted;
    private ArrayList legions = new ArrayList(3);
    private MasterBoard board;
    private FontMetrics fontMetrics;
    private int halfFontHeight;
    private String name;

    private MasterHex [] neighbors = new MasterHex[6];

    // Terrain types are:
    // B,D,H,J,m,M,P,S,T,t,W
    // Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
    // Swamp, Tower, tundra, Woods

    // Hex labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // n, ne, se, s, sw, nw
    private int labelSide;
    private int[] exitType = new int[6];
    private int[] entranceType = new int[6];

    // hexsides 0 - 5, though only 1, 3, and 5 are actually used.
    // 1 is right, 3 is bottom, 5 is left
    private TreeSet entrySides = new TreeSet();

    private boolean teleported;

    public static final int NONE = 0;
    public static final int BLOCK = 1;
    public static final int ARCH = 2;
    public static final int ARROW = 3;
    public static final int ARROWS = 4;


    public MasterHex(int cx, int cy, int scale, boolean inverted,
        MasterBoard board)
    {
        this.inverted = inverted;
        this.board = board;
        this.scale = scale;
        len = scale / 3.0;
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

        hexagon = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short of the area actually painted.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] -
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);
    }


    public void paint(Graphics g)
    {
        if (isSelected())
        {
            g.setColor(Color.white);
        }
        else
        {
            g.setColor(getTerrainColor());
        }

        g.fillPolygon(hexagon);
        g.setColor(Color.black);
        g.drawPolygon(hexagon);

        // Draw label and terrain name
        if (fontMetrics == null)
        {
            fontMetrics = g.getFontMetrics();
            halfFontHeight = (fontMetrics.getMaxAscent() +
                fontMetrics.getLeading()) >> 1;
            name = getTerrainName().toUpperCase();
        }

        switch (getLabelSide())
        {
            case 0:
                g.drawString(label, rectBound.x +
                    ((rectBound.width - fontMetrics.stringWidth(label)) >> 1),
                    rectBound.y + halfFontHeight + rectBound.height / 10);
                break;

            case 1:
                g.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) * 4 / 5,
                    rectBound.y + halfFontHeight + rectBound.height / 5);
                break;

            case 2:
                g.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) * 4 / 5,
                    rectBound.y + halfFontHeight +
                    rectBound.height * 4 / 5);
                break;

            case 3:
                g.drawString(label, rectBound.x + ((rectBound.width -
                    fontMetrics.stringWidth(label)) >> 1),
                    rectBound.y + halfFontHeight +
                    rectBound.height * 9 / 10);
                break;

            case 4:
                g.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) / 5,
                    rectBound.y + halfFontHeight +
                    rectBound.height * 4 / 5);
                break;

            case 5:
                g.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) / 5,
                    rectBound.y + halfFontHeight + rectBound.height / 5);
                break;
        }

        // The word "MOUNTAINS" needs to be printed in the wide part of the hex,
        // with a smaller font.
        if (name.equals("MOUNTAINS"))
        {
            Font oldFont = g.getFont();
            String fontName = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();

            Font font = new Font(fontName, style,  9 * size / 10);
            g.setFont(font);
            FontMetrics fontMetrics = g.getFontMetrics();
            halfFontHeight = (fontMetrics.getMaxAscent() +
                fontMetrics.getLeading()) >> 1;

            g.drawString(name, rectBound.x + ((rectBound.width -
                fontMetrics.stringWidth(name)) >> 1),
                rectBound.y + halfFontHeight + rectBound.height * 2 / 3);

            g.setFont(oldFont);
        }
        else
        {
            g.drawString(name, rectBound.x + ((rectBound.width -
                fontMetrics.stringWidth(name)) >> 1),
                rectBound.y + halfFontHeight + (rectBound.height >> 1));
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


    public void repaint()
    {
        try
        {
            board.repaint(rectBound.x, rectBound.y, rectBound.width,
                rectBound.height);
        }
        catch (NullPointerException e)
        {
            // Don't crash if we're testing a battle and board is null.
            e.printStackTrace();
        }
    }


    private void drawGate(Graphics g, int vx1, int vy1, int vx2, int vy2,
        int gateType)
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

        switch (gateType)
        {
            case BLOCK:
                x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                x[1] = (int) Math.round(x0 + len * Math.sin(theta));
                y[1] = (int) Math.round(y0 - len * Math.cos(theta));
                x[2] = (int) Math.round(x1 + len * Math.sin(theta));
                y[2] = (int) Math.round(y1 - len * Math.cos(theta));
                x[3] = (int) Math.round(x1 - len * Math.sin(theta));
                y[3] = (int) Math.round(y1 + len * Math.cos(theta));

                g.setColor(Color.white);
                g.fillPolygon(x, y, 4);
                g.setColor(Color.black);
                g.drawPolyline(x, y, 4);
                break;

            case ARCH:
                x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                x[1] = (int) Math.round(x0 + len * Math.sin(theta));
                y[1] = (int) Math.round(y0 - len * Math.cos(theta));
                x[2] = (int) Math.round(x1 + len * Math.sin(theta));
                y[2] = (int) Math.round(y1 - len * Math.cos(theta));
                x[3] = (int) Math.round(x1 - len * Math.sin(theta));
                y[3] = (int) Math.round(y1 + len * Math.cos(theta));

                x2 = (int) Math.round((x0 + x1) >> 1);
                y2 = (int) Math.round((y0 + y1) >> 1);
                Rectangle rect = new Rectangle();
                rect.x = x2 - (int) Math.round(len);
                rect.y = y2 - (int) Math.round(len);
                rect.width = (int) (2 * Math.round(len));
                rect.height = (int) (2 * Math.round(len));

                g.setColor(Color.white);
                // Draw a bit more than a semicircle, to clean edge.
                g.fillArc(rect.x, rect.y, rect.width, rect.height,
                    (int) Math.round((2 * Math.PI - theta) *
                    RAD_TO_DEG - 10), 200);
                g.setColor(Color.black);
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
                g.setColor(Color.white);
                g.fillPolygon(x, y, 4);
                g.setColor(Color.black);
                g.drawLine(x1, y1, x[1], y[1]);
                g.drawLine(x[2], y[2], x0, y0);
                break;

            case ARROW:
                x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                x[1] = (int) Math.round(((x0 + x1) >> 1) + len *
                    Math.sin(theta));
                y[1] = (int) Math.round(((y0 + y1) >> 1) - len *
                    Math.cos(theta));
                x[2] = (int) Math.round(x1 - len * Math.sin(theta));
                y[2] = (int) Math.round(y1 + len * Math.cos(theta));

                g.setColor(Color.white);
                g.fillPolygon(x, y, 3);
                g.setColor(Color.black);
                g.drawPolyline(x, y, 3);
                break;

            case ARROWS:
                for (int j = 0; j < 3; j++)
                {
                    x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                    y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;

                    x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                    y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                    x[0] = (int) Math.round(x0 - len * Math.sin(theta));
                    y[0] = (int) Math.round(y0 + len * Math.cos(theta));
                    x[1] = (int) Math.round(((x0 + x1) >> 1) + len *
                           Math.sin(theta));
                    y[1] = (int) Math.round(((y0 + y1) >> 1) - len *
                           Math.cos(theta));
                    x[2] = (int) Math.round(x1 - len * Math.sin(theta));
                    y[2] = (int) Math.round(y1 + len * Math.cos(theta));

                    g.setColor(Color.white);
                    g.fillPolygon(x, y, 3);
                    g.setColor(Color.black);
                    g.drawPolyline(x, y, 3);
                }
                break;
        }
    }


    public String getTerrainName()
    {
        switch (getTerrain())
        {
            case 'B':
                return "Brush";
            case 'D':
                return "Desert";
            case 'H':
                return "Hills";
            case 'J':
                return "Jungle";
            case 'm':
                return "Mountains";
            case 'M':
                return "Marsh";
            case 'P':
                return "Plains";
            case 'S':
                return "Swamp";
            case 'T':
                return "Tower";
            case 't':
                return "Tundra";
            case 'W':
                return "Woods";
            default:
                return "?????";
        }
    }


    public Color getTerrainColor()
    {
        switch (getTerrain())
        {
            case 'B':
                return Color.green;
            case 'D':
                return Color.orange;
            case 'H':
                return new Color(128, 64, 0);
            case 'J':
                return new Color(0, 128, 0);
            case 'm':
                return Color.red;
            case 'M':
                return new Color(180, 90, 0);
            case 'P':
                return Color.yellow;
            case 'S':
                return Color.blue;
            case 'T':
                return Color.gray;
            case 't':
                return new Color(128, 170, 255);
            case 'W':
                return new Color(128, 128, 0);
            default:
                return Color.black;
        }
    }


    // Return a point near the center of the hex, vertically offset
    // a bit toward the fat side.
    private Point getOffCenter()
    {
        return new Point((xVertex[0] + xVertex[1]) >> 1, ((yVertex[0] +
            yVertex[3]) >> 1) + (inverted ? -(scale / 6) : (scale / 6)));
    }


    public int getNumLegions()
    {
        return legions.size();
    }


    public boolean isOccupied()
    {
        return (legions.size() > 0);
    }


    public Legion getLegion(int i)
    {
        return (Legion)legions.get(i);
    }


    public int getNumFriendlyLegions(Player player)
    {
        int count = 0;
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getPlayer() == player)
            {
                count++;
            }
        }
        return count;
    }


    public int getNumEnemyLegions(Player player)
    {
        return legions.size() - getNumFriendlyLegions(player);
    }


    public boolean isEngagement()
    {
        if (getNumLegions() > 1)
        {
            Iterator it = legions.iterator();
            Legion legion = (Legion)it.next();
            Player player = legion.getPlayer();
            while (it.hasNext())
            {
                legion = (Legion)it.next();
                if (legion.getPlayer() != player)
                {
                    return true;
                }
            }
        }

        return false;
    }


    /** Return the first legion belonging to player. */
    public Legion getFriendlyLegion(Player player)
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getPlayer() == player)
            {
                return legion;
            }
        }
        return null;
    }


    /** Return the first legion not belonging to player. */
    public Legion getEnemyLegion(Player player)
    {
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getPlayer() != player)
            {
                return legion;
            }
        }
        return null;
    }


    public void alignLegions()
    {
        int numLegions = getNumLegions();
        if (numLegions == 0)
        {
            return;
        }

        Legion legion0 = (Legion)legions.get(0);
        Marker marker = legion0.getMarker();
        if (marker == null)
        {
            return;
        }
        int chitScale = marker.getBounds().width;
        Point startingPoint = getOffCenter();
        Point point = new Point(startingPoint);

        if (numLegions == 1)
        {
            // Place legion in the center of the hex.
            point.x -= chitScale >> 1;
            point.y -= chitScale >> 1;
            marker.setLocation(point);
        }
        else if (numLegions == 2)
        {
            // Place legions in NW and SE corners.
            point.x -= 3 * chitScale >> 2;
            point.y -= 3 * chitScale >> 2;
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale >> 2;
            point.y -= chitScale >> 2;
            Legion legion1 = (Legion)legions.get(1);
            marker = legion1.getMarker();
            marker.setLocation(point);
        }
        else if (numLegions == 3)
        {
            // Place legions in NW, SE, NE corners.
            point.x -= 3 * chitScale >> 2;
            point.y -= 3 * chitScale >> 2;
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale >> 2;
            point.y -= chitScale >> 2;
            Legion legion1 = (Legion)legions.get(1);
            marker = legion1.getMarker();
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale >> 2;
            point.y -= chitScale;
            Legion legion2 = (Legion)legions.get(2);
            marker = legion2.getMarker();
            marker.setLocation(point);
        }

        repaint();
    }


    public void addLegion(Legion legion)
    {
        legions.add(legion);

        // Reposition all legions within the hex.
        alignLegions();
    }


    public void removeLegion(Legion legion)
    {
        legions.remove(legion);

        // Write over the bounding area of the hex with the background
        // color, to prevent artifacts from chits that used to hang outside
        // the hex boundary.
        if (getNumLegions() >= 1)
        {
            // Reposition all legions within the hex.
            alignLegions();
        }
    }


    public void setNeighbor(int i, MasterHex hex)
    {
        neighbors[i] = hex;
    }


    public MasterHex getNeighbor(int i)
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


    public String getLabel()
    {
        return label;
    }


    public int getLabelSide()
    {
        return labelSide;
    }


    public void setLabel(int label)
    {
        this.label = Integer.toString(label);
    }


    public void setLabelSide(int labelSide)
    {
        this.labelSide = labelSide;
    }


    public void setExitType(int i, int exitType)
    {
        this.exitType[i] = exitType;
    }


    public int getExitType(int i)
    {
        return exitType[i];
    }


    public void setEntranceType(int i, int entranceType)
    {
        this.entranceType[i] = entranceType;
    }


    public int getEntranceType(int i)
    {
        return entranceType[i];
    }


    public void setEntrySide(int side)
    {
        entrySides.add(new Integer(side));
    }


    // Return the number of possible entry sides.
    public int getNumEntrySides()
    {
        return entrySides.size();
    }


    public boolean canEnterViaSide(int side)
    {
        return entrySides.contains(new Integer(side));
    }


    public boolean canEnterViaLand()
    {
        return !entrySides.isEmpty();
    }


    /** Return a possible entry side.  If there is more than one, only one
     *  will be returned.  If there is none, -1 will be returned. */
    public int getEntrySide()
    {
        if (entrySides.isEmpty())
        {
            return -1;
        }
        else
        {
            return ((Integer)entrySides.first()).intValue();
        }
    }


    public boolean getTeleported()
    {
        return teleported;
    }


    public void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }


    public void clearAllEntrySides()
    {
        entrySides.clear();
    }
}
