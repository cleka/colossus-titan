import java.awt.*;

/**
 * Class Hex describes one general hex.
 * @version $Id$
 * @author David Ripton
 */

class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    public static final double RAD_TO_DEG = 180 / Math.PI;

    protected int[] xVertex = new int[6];
    protected int[] yVertex = new int[6];
    protected Polygon hexagon;
    protected Rectangle rectBound;
    private boolean selected = false;
    private char terrain;
    protected int scale;
    protected double len;



    public boolean select(Point point)
    {
        if (hexagon.contains(point))
        {
            selected = !selected;
            return true;
        }
        return false;
    }


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


    public boolean isSelected(Point point)
    {
        return (contains(point) && isSelected());
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


    char getTerrain()
    {
        return terrain;
    }


    void setTerrain(char terrain)
    {
        this.terrain = terrain;
    }        
}
