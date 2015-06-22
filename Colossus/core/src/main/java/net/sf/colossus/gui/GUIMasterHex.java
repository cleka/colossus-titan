package net.sf.colossus.gui;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.MasterHex;


/**
 * Class GUIMasterHex holds GUI information for a MasterHex.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */
public final class GUIMasterHex extends GUIHex<MasterHex>
{
    private static final Logger LOGGER = Logger.getLogger(GUIMasterHex.class
        .getName());

    private boolean inverted;
    private FontMetrics fontMetrics;
    private int halfFontHeight;
    private Point offCenter;
    private WeakReference<MasterBoard> weakBoardRef;

    private GeneralPath highlightBorder;
    private Color selectColor = Color.white;

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

    GUIMasterHex(MasterHex model)
    {
        super(model);
    }

    void init(int cx, int cy, int scale, boolean inverted, MasterBoard board)
    {
        this.inverted = inverted;
        this.weakBoardRef = new WeakReference<MasterBoard>(board);
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
            (int)Math.round(((yVertex[0] + yVertex[3]) / 2)
                + (inverted ? -(scale / 6.0) : (scale / 6.0))));

        Point2D.Double center = findCenter2D();

        final double innerScale = 0.8;
        AffineTransform at = AffineTransform.getScaleInstance(innerScale,
            innerScale);
        highlightBorder = (GeneralPath)hexagon.createTransformedShape(at);

        // Translate innerHexagon to make it concentric.
        Rectangle2D innerBounds = highlightBorder.getBounds2D();
        Point2D.Double innerCenter = new Point2D.Double(innerBounds.getX()
            + innerBounds.getWidth() / 2.0, innerBounds.getY()
            + innerBounds.getHeight() / 2.0);
        at = AffineTransform.getTranslateInstance(
            center.getX() - innerCenter.getX(),
            center.getY() - innerCenter.getY());
        highlightBorder.transform(at);

        highlightBorder.append(hexagon, false);
    }

    @Override
    public void paint(Graphics g)
    {
        if (hexagon == null)
        {
            return;
        }

        Graphics2D g2 = (Graphics2D)g;
        Font oldFont = g2.getFont();

        g2.setFont(oldFont.deriveFont(oldFont.getSize2D() * 0.9f));
        fontMetrics = g2.getFontMetrics();
        halfFontHeight = (fontMetrics.getMaxAscent() + fontMetrics
            .getLeading()) / 2;

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

        MasterHex model = this.getHexModel();
        g2.setColor(model.getTerrainColor());
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

            if (model.getExitType(i) != Constants.HexsideGates.NONE)
            {
                drawGate(g2, xVertex[i], yVertex[i], xVertex[n], yVertex[n],
                    model.getExitType(i));
            }

            // Draw entrances
            // Unfortunately, since exits extend out into adjacent hexes,
            // they sometimes get overdrawn.  So we need to draw them
            // again from the other hex, as entrances.

            if (model.getEntranceType(i) != Constants.HexsideGates.NONE)
            {
                drawGate(g2, xVertex[n], yVertex[n], xVertex[i], yVertex[i],
                    model.getEntranceType(i));
            }
        }

        paintLabel(g2);
        if (!(useOverlay && paintOverlay(g2)))
        {
            paintTerrainName(g2);
        }
        g2.setFont(oldFont);
    }

    public void paintHighlightIfNeeded(Graphics2D g2)
    {
        if (isSelected())
        {
            g2.setColor(this.selectColor);
            g2.fill(highlightBorder);
        }
    }

    private int stringWidth(String s, Graphics2D g2)
    {
        return (int)Math.round(fontMetrics.getStringBounds(s, g2).getWidth());
    }

    private void paintLabel(Graphics2D g2)
    {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);

        MasterHex model = this.getHexModel();
        String label = model.getLabel();
        switch (model.getLabelSide())
        {
            case 0:
                g2.drawString(label, rectBound.x
                    + ((rectBound.width - stringWidth(label, g2)) / 2),
                    rectBound.y + halfFontHeight + rectBound.height / 10);
                break;

            case 1:
                g2.drawString(label, rectBound.x
                    + ((rectBound.width - stringWidth(label, g2)) * 5 / 6),
                    rectBound.y + halfFontHeight + rectBound.height / 8);
                break;

            case 2:
                g2.drawString(label, rectBound.x
                    + (rectBound.width - stringWidth(label, g2)) * 5 / 6,
                    rectBound.y + halfFontHeight + rectBound.height * 7 / 8);
                break;

            case 3:
                g2.drawString(label, rectBound.x
                    + ((rectBound.width - stringWidth(label, g2)) / 2),
                    rectBound.y + halfFontHeight + rectBound.height * 9 / 10);
                break;

            case 4:
                g2.drawString(label, rectBound.x
                    + (rectBound.width - stringWidth(label, g2)) / 6,
                    rectBound.y + halfFontHeight + rectBound.height * 5 / 6);
                break;

            case 5:
                g2.drawString(label, rectBound.x
                    + (rectBound.width - stringWidth(label, g2)) / 6,
                    rectBound.y + halfFontHeight + rectBound.height / 8);
                break;
        }
    }

    private void paintTerrainName(Graphics2D g2)
    {
        // Do not anti-alias text.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        if (fontMetrics == null)
        {
            fontMetrics = g2.getFontMetrics();
            halfFontHeight = (fontMetrics.getMaxAscent() + fontMetrics
                .getLeading()) / 2;
        }
        fontMetrics = g2.getFontMetrics();
        halfFontHeight = (fontMetrics.getMaxAscent() + fontMetrics
            .getLeading()) / 2;
        String name = this.getHexModel().getTerrainDisplayName().toUpperCase();
        g2.drawString(name,
            rectBound.x + ((rectBound.width - stringWidth(name, g2)) / 2),
            rectBound.y + halfFontHeight + rectBound.height
                * (isInverted() ? 1 : 2) / 3);
    }

    @Override
    public void repaint()
    {
        MasterBoard board = weakBoardRef.get();
        if (board == null)
        {
            return;
        }
        board.repaint(rectBound.x, rectBound.y, rectBound.width,
            rectBound.height);
    }

    private void drawGate(Graphics2D g2, double vx1, double vy1, double vx2,
        double vy2, Constants.HexsideGates gateType)
    {
        double x0; // first focus point
        double y0;
        double x1; // second focus point
        double y1;
        double x2; // center point
        double y2;
        double theta; // gate angle
        double[] x = new double[4]; // gate points
        double[] y = new double[4];

        x0 = vx1 + (vx2 - vx1) / 6;
        y0 = vy1 + (vy2 - vy1) / 6;
        x1 = vx1 + (vx2 - vx1) / 3;
        y1 = vy1 + (vy2 - vy1) / 3;

        theta = Math.atan2(vy2 - vy1, vx2 - vx1);

        switch (gateType)
        {
            case BLOCK:
                x = getWallOrSlopePositionXArray(0, vx1, vx2, theta, 1);
                y = getWallOrSlopePositionYArray(0, vy1, vy2, theta, 1);

                GeneralPath polygon = makePolygon(4, x, y, false);

                g2.setColor(Color.white);
                g2.fill(polygon);
                g2.setColor(Color.black);
                g2.draw(polygon);
                break;

            case ARCH:
                x = getWallOrSlopePositionXArray(0, vx1, vx2, theta, 1);
                y = getWallOrSlopePositionYArray(0, vy1, vy2, theta, 1);

                x2 = (x0 + x1) / 2;
                y2 = (y0 + y1) / 2;

                Rectangle2D.Double rect = new Rectangle2D.Double();
                rect.x = x2 - len;
                rect.y = y2 - len;
                rect.width = 2 * len;
                rect.height = 2 * len;

                Arc2D.Double arc = new Arc2D.Double(rect.x, rect.y,
                    rect.width, rect.height, Math.toDegrees(-theta), 180,
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
                g2.draw(new Line2D.Double(x0, y0, x1, y1));

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
                    x = getCliffOrArrowsPositionXArray(j, vx1, vx2, theta);
                    y = getCliffOrArrowsPositionYArray(j, vy1, vy2, theta);

                    polygon = makePolygon(3, x, y, false);

                    g2.setColor(Color.white);
                    g2.fill(polygon);
                    g2.setColor(Color.black);
                    g2.draw(polygon);
                }
                break;
            case NONE:
                LOGGER.log(Level.WARNING,
                    "Drawing code called for gate type NONE");
        }
    }

    /** Return a point near the center of the hex, vertically offset
     *  a bit toward the fat side. */
    Point getOffCenter()
    {
        return offCenter;
    }

    boolean isInverted()
    {
        return inverted;
    }

    void setSelectColor(Color color)
    {
        this.selectColor = color;
    }

    @Override
    public void select()
    {
        super.select();
        selectColor = Color.white;
    }

    @Override
    public void unselect()
    {
        super.unselect();
        selectColor = Color.white;
    }

    // overlay picture support
    private static final String invertedPostfix = "_i";

    private Image getOverlayImage()
    {
        Image overlay = null;
        overlay = StaticResourceLoader.getImage(this.getHexModel()
            .getTerrainDisplayName() + (!inverted ? invertedPostfix : ""),
            VariantSupport.getImagesDirectoriesList(), rectBound.width,
            rectBound.height);
        return overlay;
    }

    private boolean paintOverlay(Graphics2D g)
    {
        Image overlay = getOverlayImage();

        if (overlay == null)
        {
            return false;
        }

        MasterBoard board = weakBoardRef.get();
        if (board == null)
        {
            return false;
        }
        Composite oldComp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
            0.3f));
        g.drawImage(overlay, rectBound.x, rectBound.y, rectBound.width,
            rectBound.height, board);
        g.setComposite(oldComp);
        return true;
    }

    public void cleanup()
    {
        this.weakBoardRef = null;
    }

}
