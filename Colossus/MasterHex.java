import java.util.*;
import java.awt.*;

/**
 * Class MasterHex describes one Masterboard hex, without GUI info.
 * @version $Id$
 * @author David Ripton
 */

public class MasterHex extends Hex
{
    private MasterHex [] neighbors = new MasterHex[6];

    /** Terrain types are:
     *  Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
     *  Swamp, Tower, tundra, Woods */
    public static final char [] terrains =
        {'B', 'D', 'H', 'J', 'm', 'M', 'P', 'S', 'T', 't', 'W'};

    // Hex labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // n, ne, se, s, sw, nw
    private int labelSide;
    protected int[] exitType = new int[6];
    protected int[] baseExitType = new int[3];
    protected int[] baseExitLabel = new int[3];
    protected int[] entranceType = new int[6];

    // Constants for hexside gates.
    public static final int NONE = 0;
    public static final int BLOCK = 1;
    public static final int ARCH = 2;
    public static final int ARROW = 3;
    public static final int ARROWS = 4;


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


    public MasterHex()
    {
        super();
    }


    public String getTerrainName()
    {
        switch (getTerrain())
        {
            case 'B':
                return "Brush";
            case 'D':
                return "Desert";
            case 'H':
                return "Hills";
            case 'J':
                return "Jungle";
            case 'm':
                return "Mountains";
            case 'M':
                return "Marsh";
            case 'P':
                return "Plains";
            case 'S':
                return "Swamp";
            case 'T':
                return "Tower";
            case 't':
                return "Tundra";
            case 'W':
                return "Woods";
            default:
                return "?????";
        }
    }

    public Color getTerrainColor()
    {
        switch (getTerrain())
        {
            case 'B':
                return Color.green;
            case 'D':
                return HTMLColor.orange;
            case 'H':
                return HTMLColor.saddleBrown;
            case 'J':
                return HTMLColor.darkGreen;
            case 'm':
                return Color.red;
            case 'M':
                return HTMLColor.sienna;
            case 'P':
                return Color.yellow;
            case 'S':
                return Color.blue;
            case 'T':
                return Color.gray;
            case 't':
                return HTMLColor.skyBlue;
            case 'W':
                return HTMLColor.olive;
            default:
                return Color.black;
        }
    }


    public static boolean isNativeCombatBonus(Creature creature, char terrain)
    {
        switch (terrain)
        {
            case 'B':
            case 'J':
                return creature.isNativeBramble();

            case 'D':
                return creature.isNativeSandDune();

            case 'H':
            case 'm':
                return creature.isNativeSlope();

            case 't':
                return creature.isNativeDrift();

            case 'M':
            case 'S':
                return creature.isNativeBog();

            case 'T':
                // Everyone benefits from walls.
                return true;

            case 'P':
            case 'W':
            default:
                return false;
        }
    }


    public MasterHex getNeighbor(int i)
    {
        if (i < 0 || i > 6)
        {
            return null;
        }
        else
        {
            return neighbors[i];
        }
    }

    public void setNeighbor(int i, MasterHex hex)
    {
        neighbors[i] = hex;
    }


    public String getLabel()
    {
        return label;
    }

    public void setLabel(int label)
    {
        this.label = Integer.toString(label);
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

    public int getBaseExitLabel(int i)
    {
        return baseExitLabel[i];
    }

    public void setBaseExitLabel(int i, int label)
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
