package net.sf.colossus.variant;


import java.awt.Color;



/**
 * Class MasterHex describes one Masterboard hex, without GUI info.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class MasterHex extends Hex
{
    private final MasterHex[] neighbors = new MasterHex[6];

    private int labelSide;
    private final int[] exitType = new int[6];
    private final int[] entranceType = new int[6];

    /**
     * TODO these base exit types and labels are somehow used only during setup of the MasterBoard,
     * no real need to keep them around.
     * 
     * It seems to be related to the fact that the master board hexes are really triangles and
     * stored as such in the XML files. Maybe they should not be hexes in the code either.
     */
    private final int[] baseExitType = new int[3];
    private final String[] baseExitLabel = new String[3];

    private final MasterBoardTerrain terrain;

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

    public MasterHex(String label, MasterBoardTerrain terrain, int xCoord,
        int yCoord)
    {
        super(label, xCoord, yCoord);
        this.terrain = terrain;
    }

    public MasterBoardTerrain getTerrain()
    {
        return this.terrain;
    }

    @Override
    public String getTerrainName()
    {
        return this.terrain.getId();
    }

    public String getTerrainDisplayName()
    {
        return terrain.getDisplayName();
    }

    public Color getTerrainColor()
    {
        return terrain.getColor();
    }

    public MasterHex getNeighbor(int i)
    {
        assert (i >= 0) && (i <= 5) : "Neighbor index out of range";
        return neighbors[i];
    }

    void setNeighbor(int i, MasterHex hex)
    {
        assert (i >= 0) && (i <= 5) : "Neighbor index out of range";
        neighbors[i] = hex;
    }

    public int getLabelSide()
    {
        return labelSide;
    }

    public void setLabelSide(int labelSide)
    {
        this.labelSide = labelSide;
    }

    public int getExitType(int i)
    {
        return exitType[i];
    }

    public void setExitType(int i, int exitType)
    {
        this.exitType[i] = exitType;
    }

    public int getBaseExitType(int i)
    {
        return baseExitType[i];
    }

    public void setBaseExitType(int i, int exitType)
    {
        this.baseExitType[i] = exitType;
    }

    public String getBaseExitLabel(int i)
    {
        return baseExitLabel[i];
    }

    public void setBaseExitLabel(int i, String label)
    {
        this.baseExitLabel[i] = label;
    }

    public int getEntranceType(int i)
    {
        return entranceType[i];
    }

    public void setEntranceType(int i, int entranceType)
    {
        this.entranceType[i] = entranceType;
    }
}
