package net.sf.colossus.client;


import java.awt.Color;

import net.sf.colossus.server.Creature;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Class MasterHex describes one Masterboard hex, without GUI info.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class MasterHex extends Hex
{
    private MasterHex[] neighbors = new MasterHex[6];

    /** Terrain types are:
     *  Brush, Desert, Hills, Jungle, mountains, Marsh, Plains,
     *  Swamp, Tower, tundra, Woods */

    // Hex labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // n, ne, se, s, sw, nw
    private int labelSide;
    private int[] exitType = new int[6];
    private int[] baseExitType = new int[3];
    private int[] baseExitLabel = new int[3];
    private int[] entranceType = new int[6];

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
        return getTerrain();
    }

    public String getTerrainDisplayName()
    {
        return (TerrainRecruitLoader.getTerrainDisplayName(getTerrain()));
    }

    public Color getTerrainColor()
    {
        return (TerrainRecruitLoader.getTerrainColor(getTerrain()));
    }

    public static boolean isNativeCombatBonus(Creature creature, String terrain)
    {
        int bonusHazardCount = 0;
        int bonusHazardSideCount = 0;

        final String[] hazard = BattleHex.getTerrains();

        for (int i = 0; i < hazard.length; i++)
        {
            int count = HexMap.getHazardCountInTerrain(hazard[i], terrain);
            if (BattleHex.isNativeBonusHazard(hazard[i]) &&
                creature.isNativeTerrain(hazard[i]))
            {
                bonusHazardCount += count;
            }
            else
            {
                if (BattleHex.isNonNativePenaltyHazard(hazard[i]) &&
                    !creature.isNativeTerrain(hazard[i]))
                {
                    bonusHazardCount -= count;
                }
            }
        }
        final char[] hazardSide = BattleHex.getHexsides();

        for (int i = 0; i < hazardSide.length; i++)
        {
            int count =
                net.sf.colossus.client.HexMap.getHazardSideCountInTerrain(
                hazardSide[i],
                terrain);
            if (BattleHex.isNativeBonusHexside(hazardSide[i]) &&
                creature.isNativeHexside(hazardSide[i]))
            {
                bonusHazardSideCount += count;
            }
            else
            {
                if (BattleHex.isNonNativePenaltyHexside(hazardSide[i]) &&
                    !creature.isNativeHexside(hazardSide[i]))
                {
                    bonusHazardSideCount -= count;
                }
            }
        }
        if (((bonusHazardCount + bonusHazardSideCount) > 0) &&
            ((bonusHazardCount >= 3) || (bonusHazardSideCount >= 5)))
        {
            return true;
        }
        return false;
    }

    public MasterHex getNeighbor(int i)
    {
        assert (i>=0) && (i<=5) : "Neighbor index out of range";
        return neighbors[i];
    }

    void setNeighbor(int i, MasterHex hex)
    {
        assert (i>=0) && (i<=5) : "Neighbor index out of range";
        neighbors[i] = hex;
    }

    public void setLabel(int label)
    {
        setLabel(Integer.toString(label));
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
