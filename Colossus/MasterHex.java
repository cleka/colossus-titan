import java.awt.*;
import java.util.*;
import java.awt.geom.*;

/**
 * Class MasterHex describes one Masterboard hex
 * @version $Id$
 * @author David Ripton
 */

public final class MasterHex extends Hex
{
    private boolean inverted;
    private MasterBoard board;
    private FontMetrics fontMetrics;
    private int halfFontHeight;
    private String name;
    private Point offCenter;

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

    public static final int NONE = 0;
    public static final int BLOCK = 1;
    public static final int ARCH = 2;
    public static final int ARROW = 3;
    public static final int ARROWS = 4;


    // The hex vertexes are numbered like this:
    //
    //               normal                     inverted
    //
    //              0------1                  0------------1
    //             /        \                /              \
    //            /          \              /                \
    //           /            \            /                  \
    //          /              \          5                    2
    //         /                \          \                  /
    //        /                  \          \                /
    //       5                    2          \              /
    //        \                  /            \            /
    //         \                /              \          /
    //          \              /                \        /
    //           4------------3                  4------3


    public MasterHex(int cx, int cy, int scale, boolean inverted,
        MasterBoard board)
    {
        this.inverted = inverted;
        this.board = board;
        len = scale / 3.0;
        if (inverted)
        {
            xVertex[0] = cx - scale;
            yVertex[0] = cy;
            xVertex[1] = cx + 3 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + SQRT3 * scale;
            xVertex[3] = cx + 2 * scale;
            yVertex[3] = cy + 3 * SQRT3 * scale;
            xVertex[4] = cx;
            yVertex[4] = cy + 3 * SQRT3 * scale;
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + SQRT3 * scale;
        }
        else
        {
            xVertex[0] = cx;
            yVertex[0] = cy;
            xVertex[1] = cx + 2 * scale;
            yVertex[1] = cy;
            xVertex[2] = cx + 4 * scale;
            yVertex[2] = cy + 2 * SQRT3 * scale;
            xVertex[3] = cx + 3 * scale;
            yVertex[3] = cy + 3 * SQRT3 * scale;
            xVertex[4] = cx - scale;
            yVertex[4] = cy + 3 * SQRT3 * scale;
            xVertex[5] = cx - 2 * scale;
            yVertex[5] = cy + 2 * SQRT3 * scale;
        }

        hexagon = makePolygon(6, xVertex, yVertex, true);
        rectBound = hexagon.getBounds();
        center = findCenter();
        offCenter = new Point((int)Math.round((xVertex[0] + xVertex[1]) / 2),
            (int)Math.round(((yVertex[0] + yVertex[3]) / 2) +
            (inverted ? -(scale / 6.0) : (scale / 6.0))));
    }


    // No-arg constructor for testing.
    public MasterHex()
    {
    }


    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        if (getAntialias())
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
                drawGate(g2, xVertex[i], yVertex[i], xVertex[n], yVertex[n],
                                exitType[i]);
            }

            // Draw entrances
            // Unfortunately, since exits extend out into adjacent hexes,
            // they sometimes get overdrawn.  So we need to draw them
            // again from the other hex, as entrances.

            if (entranceType[i] != NONE)
            {
                drawGate(g2, xVertex[n], yVertex[n], xVertex[i], yVertex[i],
                                entranceType[i]);
            }
        }

        // Do not anti-alias text.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw label and terrain name
        if (fontMetrics == null)
        {
            fontMetrics = g2.getFontMetrics();
            halfFontHeight = (fontMetrics.getMaxAscent() +
                fontMetrics.getLeading()) / 2;
            name = getTerrainName().toUpperCase();
        }

        switch (getLabelSide())
        {
            case 0:
                g2.drawString(label, rectBound.x +
                    ((rectBound.width - fontMetrics.stringWidth(label)) / 2),
                    rectBound.y + halfFontHeight + rectBound.height / 10);
                break;

            case 1:
                g2.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) * 4 / 5,
                    rectBound.y + halfFontHeight + rectBound.height / 5);
                break;

            case 2:
                g2.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) * 4 / 5,
                    rectBound.y + halfFontHeight +
                    rectBound.height * 4 / 5);
                break;

            case 3:
                g2.drawString(label, rectBound.x + ((rectBound.width -
                    fontMetrics.stringWidth(label)) / 2),
                    rectBound.y + halfFontHeight +
                    rectBound.height * 9 / 10);
                break;

            case 4:
                g2.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) / 5,
                    rectBound.y + halfFontHeight +
                    rectBound.height * 4 / 5);
                break;

            case 5:
                g2.drawString(label, rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(label)) / 5,
                    rectBound.y + halfFontHeight + rectBound.height / 5);
                break;
        }

        // The word "MOUNTAINS" needs to be printed in the wide part of
        // the hex, with a smaller font.
        if (name.equals("MOUNTAINS"))
        {
            Font oldFont = g2.getFont();
            String fontName = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();

            Font font = new Font(fontName, style,  9 * size / 10);
            g2.setFont(font);
            FontMetrics fontMetrics = g2.getFontMetrics();
            halfFontHeight = (fontMetrics.getMaxAscent() +
                fontMetrics.getLeading()) / 2;

            g2.drawString(name, rectBound.x + ((rectBound.width -
                fontMetrics.stringWidth(name)) / 2),
                rectBound.y + halfFontHeight + rectBound.height * 2 / 3);

            g2.setFont(oldFont);
        }
        else
        {
            g2.drawString(name, rectBound.x + ((rectBound.width -
                fontMetrics.stringWidth(name)) / 2),
                rectBound.y + halfFontHeight + (rectBound.height / 2));
        }
    }


    public void repaint()
    {
        board.repaint(rectBound.x, rectBound.y, rectBound.width,
            rectBound.height);
    }


    private void drawGate(Graphics2D g2, double vx1, double vy1, double vx2,
        double vy2, int gateType)
    {
        double x0;                    // first focus point
        double y0;
        double x1;                    // second focus point
        double y1;
        double x2;                    // center point
        double y2;
        double theta;                 // gate angle
        double [] x = new double[4];  // gate points
        double [] y = new double[4];

        x0 = vx1 + (vx2 - vx1) / 6;
        y0 = vy1 + (vy2 - vy1) / 6;
        x1 = vx1 + (vx2 - vx1) / 3;
        y1 = vy1 + (vy2 - vy1) / 3;

        theta = Math.atan2(vy2 - vy1, vx2 - vx1);

        switch (gateType)
        {
            case BLOCK:
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
                break;

            case ARCH:
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

                Arc2D.Double arc = new Arc2D.Double(rect.x, rect.y,
                    rect.width, rect.height,
                    ((2 * Math.PI - theta) * RAD_TO_DEG), 180,
                    Arc2D.OPEN);

                g2.setColor(Color.white);
                g2.fill(arc);
                g2.setColor(Color.black);
                g2.draw(arc);

                x[2] = x[0];
                y[2] = y[0];
                x[0] = x1;
                y[0] = y1;
                x[1] = x[3];
                y[1] = y[3];
                x[3] = x0;
                y[3] = y0;

                polygon = makePolygon(4, x, y, false);

                g2.setColor(Color.white);
                g2.fill(polygon);
                // Erase the existing hexside line.
                g2.draw(new Line2D.Double(x0, y0 , x1, y1));

                g2.setColor(Color.black);
                g2.draw(new Line2D.Double(x1, y1, x[1], y[1]));
                g2.draw(new Line2D.Double(x[2], y[2], x0, y0));
                break;

            case ARROW:
                x[0] = x0 - len * Math.sin(theta);
                y[0] = y0 + len * Math.cos(theta);
                x[1] = (x0 + x1) / 2 + len * Math.sin(theta);
                y[1] = (y0 + y1) / 2 - len * Math.cos(theta);
                x[2] = x1 - len * Math.sin(theta);
                y[2] = y1 + len * Math.cos(theta);

                polygon = makePolygon(3, x, y, false);

                g2.setColor(Color.white);
                g2.fill(polygon);
                g2.setColor(Color.black);
                g2.draw(polygon);
                break;

            case ARROWS:
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

                    polygon = makePolygon(3, x, y, false);

                    g2.setColor(Color.white);
                    g2.fill(polygon);
                    g2.setColor(Color.black);
                    g2.draw(polygon);
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
                return HTMLColor.orange;
            case 'H':
                return HTMLColor.saddleBrown;
            case 'J':
                return HTMLColor.darkGreen;
            case 'm':
                return Color.red;
            case 'M':
                return HTMLColor.sienna;
            case 'P':
                return Color.yellow;
            case 'S':
                return Color.blue;
            case 'T':
                return Color.gray;
            case 't':
                return HTMLColor.skyBlue;
            case 'W':
                return HTMLColor.olive;
            default:
                return Color.black;
        }
    }


    public static boolean isNativeCombatBonus(Creature creature, char terrain)
    {
        switch (terrain)
        {
            case 'B':
            case 'J':
                return creature.isNativeBramble();

            case 'D':
                return creature.isNativeSandDune();

            case 'H':
            case 'm':
                return creature.isNativeSlope();

            case 't':
                return creature.isNativeDrift();

            case 'M':
            case 'S':
                return creature.isNativeBog();

            case 'T':
                // Everyone benefits from walls.
                return true;

            case 'P':
            case 'W':
            default:
                return false;
        }
    }


    /** Return a point near the center of the hex, vertically offset
     *  a bit toward the fat side. */
    public Point getOffCenter()
    {
        return offCenter;
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
}
