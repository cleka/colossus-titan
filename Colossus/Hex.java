import java.awt.*;

/**
 * Class Hex describes one general hex.
 * @version $Id$
 * @author David Ripton
 */

public abstract class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    public static final double RAD_TO_DEG = 180 / Math.PI;

    protected int[] xVertex = new int[6];
    protected int[] yVertex = new int[6];
    protected Polygon hexagon;
    protected Rectangle rectBound;
    private boolean selected;
    private char terrain;
    protected int scale;
    protected double len;
    protected String label;



    public void select()
    {
        selected = true;
    }


    public void unselect()
    {
        selected = false;
    }


    public boolean isSelected()
    {
        return selected;
    }


    public Rectangle getBounds()
    {
        return rectBound;
    }


    public boolean contains(Point point)
    {
        return (hexagon.contains(point));
    }


    public Point getCenter()
    {
        return new Point((xVertex[0] + xVertex[3]) / 2,
            (yVertex[0] + yVertex[3]) / 2);
    }  


    public char getTerrain()
    {
        return terrain;
    }


    public void setTerrain(char terrain)
    {
        this.terrain = terrain;
    }        


    public String getLabel()
    {
        return label;
    }


    public String getName()
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
}
