package net.sf.colossus.client;


import java.awt.*;
import java.awt.geom.*;


/**
 * Class Hex describes one general hex.
 * @version $Id$
 * @author David Ripton
 */

public abstract class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    public static final double RAD_TO_DEG = 180 / Math.PI;

    // The hex vertexes are numbered like this:
    //
    //              0---------1
    //             /           \
    //            /             \
    //           /               \
    //          /                 \
    //         5                   2
    //          \                 /
    //           \               /
    //            \             /
    //             \           /
    //              4---------3

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

    // Game state variables
    private String baseName = "";
    private String label = "";  // Avoid null pointer in stringWidth()
    private int xCoord = -1;
    private int yCoord = -1;

    public String getTerrain()
    {
        return baseName;
    }

    public void setTerrain(String bn)
    {
        baseName = bn;
    }

    public Rectangle getBounds()
    {
        return rectBound;
    }

    public boolean contains(Point point)
    {
        return (hexagon.contains(point));
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public abstract String getTerrainName();

    public String getDescription()
    {
        return getTerrainName() + " hex " + getLabel();
    }

    public String toString()
    {
        return getDescription();
    }

    public int getXCoord()
    {
        return xCoord;
    }

    public void setXCoord(int xCoord)
    {
        this.xCoord = xCoord;
    }

    public int getYCoord()
    {
        return yCoord;
    }

    public void setYCoord(int yCoord)
    {
        this.yCoord = yCoord;
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
    static GeneralPath makePolygon(int sides, double [] x, double [] y,
        boolean closed)
    {
        GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
            sides);
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
}
