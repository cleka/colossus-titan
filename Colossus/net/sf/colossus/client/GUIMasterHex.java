package net.sf.colossus.client;


import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.geom.*;
import java.net.*;
import javax.swing.*;
import java.io.*;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Log;
import net.sf.colossus.server.Constants;

/**
 * Class GUIMasterHex holds GUI information for a MasterHex.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public final class GUIMasterHex extends MasterHex
{
    private boolean inverted;
    private Font oldFont;
    private FontMetrics fontMetrics;
    private int halfFontHeight;
    private Point offCenter;
    private MasterBoard board;
    /** Terrain display name in upper case. */
    private String name;
    private GeneralPath innerHexagon;
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

    GUIMasterHex()
    {
        super();
    }

    /** Create a near-clone of the passed MasterHex.  Need to call
     *  init to setup GUI values, and need to setup neighbors. */
    GUIMasterHex(MasterHex mh)
    {
        super();

        setSelected(mh.isSelected());
        setTerrain(mh.getTerrain());
        setXCoord(mh.getXCoord());
        setYCoord(mh.getYCoord());
        setLabel(mh.getLabel());
        setLabelSide(mh.getLabelSide());
        for (int i = 0; i < 6; i++)
        {
            setEntranceType(i, mh.getEntranceType(i));
            setExitType(i, mh.getExitType(i));
        }
        for (int i = 0; i < 3; i++)
        {
            setBaseExitType(i, mh.getBaseExitType(i));
            setBaseExitLabel(i, mh.getBaseExitLabel(i));
        }
    }

    void init(int cx, int cy, int scale, boolean inverted, MasterBoard board)
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
        loadOverlay();
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
            g2.setColor(selectColor);
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

            if (getExitType(i) != Constants.NONE)
            {
                drawGate(g2, xVertex[i], yVertex[i], xVertex[n], yVertex[n],
                    getExitType(i));
            }

            // Draw entrances
            // Unfortunately, since exits extend out into adjacent hexes,
            // they sometimes get overdrawn.  So we need to draw them
            // again from the other hex, as entrances.

            if (getEntranceType(i) != Constants.NONE)
            {
                drawGate(g2, xVertex[n], yVertex[n], xVertex[i], yVertex[i],
                    getEntranceType(i));
            }
        }

        if (useOverlay && (overlay != null))
        {
            paintLabel(g2);
            paintOverlay(g2);
        }
        else
        {
            paintLabel(g2);
            paintTerrainName(g2);
        }
    }

    private void shrinkFont(Graphics2D g2)
    {
        oldFont = g2.getFont();
        String fontName = oldFont.getName();
        int size = oldFont.getSize();
        int style = oldFont.getStyle();
            
        Font font = new Font(fontName, style,  9 * size / 10);
        g2.setFont(font);
        fontMetrics = g2.getFontMetrics();
        halfFontHeight = (fontMetrics.getMaxAscent() +
            fontMetrics.getLeading()) / 2;
        name = getTerrainDisplayName().toUpperCase();
    }

    private void restoreFont(Graphics2D g2)
    {
        g2.setFont(oldFont);
    }

    private int stringWidth(String s, Graphics2D g2)
    {
        return (int)Math.round(fontMetrics.getStringBounds(s, g2).getWidth());
    }

    private void paintLabel(Graphics2D g2) 
    {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        shrinkFont(g2);
        
        switch (getLabelSide())
        {
        case 0:
            g2.drawString(label, rectBound.x +
                ((rectBound.width - stringWidth(label, g2)) / 2), 
                rectBound.y + halfFontHeight + rectBound.height / 10);
            break;

        case 1:
            g2.drawString(label, rectBound.x + ((rectBound.width -
                stringWidth(label, g2)) * 5 / 6), rectBound.y + 
                halfFontHeight + rectBound.height / 8);
            break;

        case 2:
            g2.drawString(label, rectBound.x + (rectBound.width -
                stringWidth(label, g2)) * 5 / 6, rectBound.y + halfFontHeight +
                rectBound.height * 7 / 8);
            break;

        case 3:
            g2.drawString(label, rectBound.x + ((rectBound.width -
                stringWidth(label, g2)) / 2), rectBound.y + halfFontHeight +
                rectBound.height * 9 / 10);
            break;

        case 4:
            g2.drawString(label, rectBound.x + (rectBound.width -
                stringWidth(label, g2)) / 6, rectBound.y + halfFontHeight +
                rectBound.height * 5 / 6);
            break;

        case 5:
            g2.drawString(label, rectBound.x + (rectBound.width -
                stringWidth(label, g2)) / 6, rectBound.y + halfFontHeight + 
                rectBound.height / 8);
            break;
        }

        restoreFont(g2);
    }

    private void paintTerrainName(Graphics2D g2)
    {
        // Do not anti-alias text.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);
        if (fontMetrics == null)
        {
            fontMetrics = g2.getFontMetrics();
            halfFontHeight = (fontMetrics.getMaxAscent() +
                              fontMetrics.getLeading()) / 2;
            name = getTerrainDisplayName().toUpperCase();
        }

        shrinkFont(g2); 
        g2.drawString(name, 
            rectBound.x + ((rectBound.width - stringWidth(name, g2)) / 2), 
            rectBound.y + halfFontHeight + rectBound.height * 
                (isInverted() ? 1 : 2) / 3);
        restoreFont(g2);
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
            case Constants.BLOCK:
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

            case Constants.ARCH:
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

            case Constants.ARROW:
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

            case Constants.ARROWS:
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

            default:
                Log.error("Bogus gate type");
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

    public void select()
    {
        super.select();
        selectColor = Color.white;
    }

    public void unselect()
    {
        super.unselect();
        selectColor = Color.white;
    }

    // overlay picture support
    private static final String invertedPostfix = "_i";
    private Image overlay;

    private void loadOverlay()
    {
        if (overlay == null)
        {
            java.util.List directories = 
                VariantSupport.getImagesDirectoriesList();
            overlay = ResourceLoader.getImage(getTerrainDisplayName() +
                                           (!inverted ? invertedPostfix : ""),
                                           directories);
            
            /* DISABLED
            // code to use if we want rotate the overlay,
            // to look more like the 'regular' Titan Masterboard
            // need to give theta the proper value,
            // depending where on the masterboard we are.
            // Disabled, as not only the theta is computed wrong,
            // But it looks ugly (the destination rectangle is wrong too)

            int width = overlay.getWidth(board);
            int height = overlay.getHeight(board);
            BufferedImage bi = new BufferedImage(width, height,
                                                 BufferedImage.TYPE_INT_ARGB);
            Graphics2D biContext = bi.createGraphics();
            biContext.drawImage(overlay, 0, 0, null);
            double theta =
                theta = ((getLabelSide() + (isInverted() ? 3 : 0)) % 6) *
                Math.PI / 3.;
            
            AffineTransform at = AffineTransform.getRotateInstance(theta,
                                                                   width / 2,
                                                                   height / 2);
            AffineTransformOp ato = new AffineTransformOp(at,
                                        AffineTransformOp.TYPE_BILINEAR);
            BufferedImage bi2 = ato.createCompatibleDestImage(bi, null);
            bi2 = ato.filter(bi, bi2);
            overlay = bi2;
            */
        }
    }
    
    private void paintOverlay(Graphics2D g)
    {
        if (overlay == null)
        {
            return;
        }

        g.drawImage(overlay,
                    rectBound.x,
                    rectBound.y,
                    rectBound.width,
                    rectBound.height,
                    board);
    }
}
