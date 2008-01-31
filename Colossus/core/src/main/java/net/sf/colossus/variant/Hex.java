package net.sf.colossus.variant;


/**
 * Class Hex describes one general hex.
 * @version $Id$
 * @author David Ripton
 */

public abstract class Hex
{
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

    // Game state variables
    // TODO make label final -- currently BattleHex still calculates it
    private String label = "";
    private final int xCoord;
    private final int yCoord;

    public Hex(String label, int xCoord, int yCoord)
    {
        this.label = label;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    public Hex(int xCoord, int yCoord)
    {
        this.xCoord = xCoord;
        this.yCoord = yCoord;
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

    @Override
    public String toString()
    {
        return getDescription();
    }

    public int getXCoord()
    {
        return xCoord;
    }

    public int getYCoord()
    {
        return yCoord;
    }
}
