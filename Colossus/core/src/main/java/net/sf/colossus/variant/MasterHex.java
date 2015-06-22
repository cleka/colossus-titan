package net.sf.colossus.variant;


import java.awt.Color;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.EntrySide;


/**
 * Class MasterHex describes one Masterboard hex, without GUI info.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */
public class MasterHex extends Hex
{
    private final MasterHex[] neighbors = new MasterHex[6];

    private int labelSide;
    private final Constants.HexsideGates[] exitType = new Constants.HexsideGates[] {
        Constants.HexsideGates.NONE, Constants.HexsideGates.NONE,
        Constants.HexsideGates.NONE, Constants.HexsideGates.NONE,
        Constants.HexsideGates.NONE, Constants.HexsideGates.NONE };
    private final Constants.HexsideGates[] entranceType = new Constants.HexsideGates[] {
        Constants.HexsideGates.NONE, Constants.HexsideGates.NONE,
        Constants.HexsideGates.NONE, Constants.HexsideGates.NONE,
        Constants.HexsideGates.NONE, Constants.HexsideGates.NONE };

    /**
     * TODO these base exit types and labels are somehow used only during setup of the MasterBoard,
     * no real need to keep them around.
     *
     * It seems to be related to the fact that the master board hexes are really triangles and
     * stored as such in the XML files. Maybe they should not be hexes in the code either.
     */
    private final Constants.HexsideGates[] baseExitType = new Constants.HexsideGates[] {
        Constants.HexsideGates.NONE, Constants.HexsideGates.NONE,
        Constants.HexsideGates.NONE };
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

    /**
     * For a given EntrySide, find out which direction that means.
     * Caller can use that e.g. to figure out what is the neighbor hex
     * from where an attacker is coming
     *
     * TODO should this rather be somewhere else? Since it uses entryside,
     * it makes variant package depending on game package ...
     *
     * @param wantedEntrySide
     * @return The direction towards which that entryside is placed
     */
    public int findDirectionForEntrySide(EntrySide wantedEntrySide)
    {
        int i;
        for (i = 0; i < 6; i++)
        {
            int esNr = (6 + i - getLabelSide()) % 6;
            EntrySide esTmp = EntrySide.values()[esNr];

            if (esTmp != null && esTmp.getLabel() != null
                && esTmp.getLabel().equals(wantedEntrySide.getLabel()))
            {
                return i;
            }
        }

        return -1;
    }

    public void setLabelSide(int labelSide)
    {
        this.labelSide = labelSide;
    }

    public Constants.HexsideGates getExitType(int i)
    {
        return exitType[i];
    }

    public void setExitType(int i, Constants.HexsideGates exitType)
    {
        this.exitType[i] = exitType;
    }

    public Constants.HexsideGates getBaseExitType(int i)
    {
        return baseExitType[i];
    }

    public void setBaseExitType(int i, Constants.HexsideGates exitType)
    {
        assert exitType != null;
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

    public Constants.HexsideGates getEntranceType(int i)
    {
        return entranceType[i];
    }

    public void setEntranceType(int i, Constants.HexsideGates entranceType)
    {
        assert entranceType != null;
        this.entranceType[i] = entranceType;
    }
}
