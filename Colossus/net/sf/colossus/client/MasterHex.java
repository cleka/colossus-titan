package net.sf.colossus.client;


import java.util.*;
import java.awt.*;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.server.Creature;
import net.sf.colossus.parser.TerrainRecruitLoader;


/**
 * Class MasterHex describes one Masterboard hex, without GUI info.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class MasterHex extends Hex
{
    private MasterHex [] neighbors = new MasterHex[6];

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
        return (TerrainRecruitLoader.getTerrainName(getTerrain()));
    }

    public String getTerrainDisplayName()
    {
        return (TerrainRecruitLoader.getTerrainDisplayName(getTerrain()));
    }

    public static String getTerrainName(char t)
    {
        return (TerrainRecruitLoader.getTerrainName(t));
    }

    public Color getTerrainColor()
    {
        return (TerrainRecruitLoader.getTerrainColor(getTerrain()));
    }


    public static boolean isNativeCombatBonus(Creature creature, char terrain)
    {
        /*
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

            // XXX In some variants plains and woods might help natives.
            case 'P':
            case 'W':
            default:
                return false;
        }
        */

        int bonusHazardCount = 0;
        int bonusHazardSideCount = 0;
        
        char[] hazard = BattleHex.getTerrains();

        for (int i = 0; i < hazard.length ; i++)
        {
            int count =
              net.sf.colossus.client.HexMap.getHazardCountInTerrain(
                   hazard[i],
                   terrain);
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
        char[] hazardSide = BattleHex.getHexsides();
        
        for (int i = 0; i < hazardSide.length ; i++)
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
        if (((bonusHazardCount + bonusHazardSideCount) > 0)
            && ((bonusHazardCount >= 3) || (bonusHazardSideCount >= 5)))
        {
            return true;
        }
        return false;
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

    void setNeighbor(int i, MasterHex hex)
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

    void setLabelSide(int labelSide)
    {
        this.labelSide = labelSide;
    }


    public int getExitType(int i)
    {
        return exitType[i];
    }

    void setExitType(int i, int exitType)
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

    void setEntranceType(int i, int entranceType)
    {
        this.entranceType[i] = entranceType;
    }
}
