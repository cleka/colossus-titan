import java.awt.*;
import java.util.*;
import java.awt.geom.*;

/**
 * Class GUIMasterHex holds GUI information for a MasterHex.
 * @version $Id$
 * @author David Ripton
 */

public final class GUIMasterHex extends MasterHex
{
    private boolean inverted;
    private FontMetrics fontMetrics;
    private int halfFontHeight;
    private Point offCenter;
    private MasterBoard board;
    /** Terrain name in upper case. */
    private String name;
    private GeneralPath innerHexagon;


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


    // Use two-stage initialization so that we can clone the GUIMasterHex
    // from an existing MasterHex, then add the GUI info.

    public GUIMasterHex()
    {
        super();
    }

    public void init(int cx, int cy, int scale, boolean inverted,
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
        offCenter = new Point((int)Math.round((xVertex[0] + xVertex[1]) / 2),
            (int)Math.round(((yVertex[0] + yVertex[3]) / 2) +
            (inverted ? -(scale / 6.0) : (scale / 6.0))));

        Point2D.Double center = findCenter2D();

        final double innerScale = 0.8;
        AffineTransform at = AffineTransform.getScaleInstance(innerScale,
            innerScale);
        innerHexagon = (GeneralPath)hexagon.createTransformedShape(at);

        // Translate innerHexagon to make it concentric.
        Rectangle2D innerBounds = innerHexagon.getBounds2D(); 
        Point2D.Double innerCenter = new Point2D.Double(innerBounds.getX() +
            innerBounds.getWidth() / 2.0, innerBounds.getY() + 
            innerBounds.getHeight() / 2.0);
        at = AffineTransform.getTranslateInstance(center.getX() - 
            innerCenter.getX(), center.getY() - innerCenter.getY());
        innerHexagon.transform(at);
    }


    public void paint(Graphics g)
    {
        if (hexagon == null)
        {
            return;
        }

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
            g2.fill(hexagon);

            // Fill inscribed hexagon with the terrain color.
            g2.setColor(getTerrainColor());
            g2.fill(innerHexagon);

            // And give it a border.
            g2.setColor(Color.black);
            g2.draw(innerHexagon);
        }
        else
        {
            g2.setColor(getTerrainColor());
            g2.fill(hexagon);
        }

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


    /** Return a point near the center of the hex, vertically offset
     *  a bit toward the fat side. */
    public Point getOffCenter()
    {
        return offCenter;
    }


    public boolean isInverted()
    {
        return inverted;
    }
}
