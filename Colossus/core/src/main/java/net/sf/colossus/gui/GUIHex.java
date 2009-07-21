package net.sf.colossus.gui;


import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

import net.sf.colossus.variant.Hex;


/**
 * Abstract parent class for various GUI hexes
 */
public abstract class GUIHex<H extends Hex> extends JComponent
{
    public static final double SQRT3 = Math.sqrt(3.0);

    private H model;

    // GUI variables
    double[] xVertex = new double[6];
    double[] yVertex = new double[6];
    double len;
    GeneralPath hexagon;
    Rectangle rectBound;

    /** Globally turns antialiasing on or off for all hexes. */
    static boolean antialias;

    /** Globally turns overlay on or off for all hexes */
    static boolean useOverlay;

    // Selection is in-between GUI and game state.
    private boolean selected;

    public GUIHex(H model)
    {
        this.model = model;
    }

    public H getHexModel()
    {
        return this.model;
    }

    public void setHexModel(H model)
    {
        this.model = model;
    }

    @Override
    public Rectangle getBounds()
    {
        return rectBound;
    }

    @Override
    public boolean contains(Point point)
    {
        return hexagon.contains(point);
    }

    public void select()
    {
        selected = true;
    }

    public void unselect()
    {
        selected = false;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    public boolean isSelected()
    {
        return selected;
    }

    static boolean getAntialias()
    {
        return antialias;
    }

    static void setAntialias(boolean enabled)
    {
        antialias = enabled;
    }

    static boolean getOverlay()
    {
        return useOverlay;
    }

    public static void setOverlay(boolean enabled)
    {
        useOverlay = enabled;
    }

    /** Return a GeneralPath polygon, with the passed number of sides,
     *  and the passed x and y coordinates.  Close the polygon if the
     *  argument closed is true. */
    static GeneralPath makePolygon(int sides, double[] x, double[] y,
        boolean closed)
    {
        GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, sides);
        polygon.moveTo((float)x[0], (float)y[0]);
        for (int i = 1; i < sides; i++)
        {
            polygon.lineTo((float)x[i], (float)y[i]);
        }
        if (closed)
        {
            polygon.closePath();
        }
        return polygon;
    }

    /** Return the Point closest to the center of the polygon. */
    public Point findCenter()
    {
        return new Point((int)Math.round((xVertex[2] + xVertex[5]) / 2),
            (int)Math.round((yVertex[0] + yVertex[3]) / 2));
    }

    /** Return the Point2D.Double at the center of the polygon. */
    Point2D.Double findCenter2D()
    {
        return new Point2D.Double((xVertex[2] + xVertex[5]) / 2.0,
            (yVertex[0] + yVertex[3]) / 2.0);
    }

    protected double[] getCliffOrArrowsPositionXArray(int j, double vx1,
        double vx2, double theta)
    {
        double[] x = new double[3];
        double x0; // first focus point
        double x1; // second focus point
        x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
        x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
        x[0] = x0 - len * Math.sin(theta);
        x[1] = (x0 + x1) / 2 + len * Math.sin(theta);
        x[2] = x1 - len * Math.sin(theta);
        return x;
    }

    protected double[] getCliffOrArrowsPositionYArray(int j, double vy1,
        double vy2, double theta)
    {
        double[] y = new double[3];
        double y0; // first focus point
        double y1; // second focus point
        y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
        y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;
        y[0] = y0 + len * Math.cos(theta);
        y[1] = (y0 + y1) / 2 - len * Math.cos(theta);
        y[2] = y1 + len * Math.cos(theta);
        return y;
    }

    protected double[] getWallOrSlopePositionXArray(int j, double vx1,
        double vx2, double theta, int size)
    {
        double[] x = new double[4];
        double x0; // first focus point
        double x1; // second focus point
        x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
        x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;

        x[0] = x0 - len / size * Math.sin(theta);
        x[1] = x0 + len / size * Math.sin(theta);
        x[2] = x1 + len / size * Math.sin(theta);
        x[3] = x1 - len / size * Math.sin(theta);
        return x;
    }

    protected double[] getWallOrSlopePositionYArray(int j, double vy1,
        double vy2, double theta, int size)
    {
        double[] y = new double[4];
        double y0; // first focus point
        double y1; // second focus point
        y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
        y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

        y[0] = y0 + len / size * Math.cos(theta);
        y[1] = y0 - len / size * Math.cos(theta);
        y[2] = y1 - len / size * Math.cos(theta);
        y[3] = y1 + len / size * Math.cos(theta);
        return y;
    }
}
