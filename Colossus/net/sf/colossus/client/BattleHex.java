package net.sf.colossus.client;


import java.awt.*;
import java.util.*;
import java.awt.geom.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.Log;


/**
 * Class BattleHex holds game state for battle hex.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class BattleHex extends Hex
{
    /** Valid elevations are 0, 1, and 2. */
    private int elevation;

    // Hex terrain types are:
    // p, r, s, t, o, v, d, w
    // plain, bramble, sand, tree, bog, volcano, drift, tower
    // also
    // l
    // lake

    /**
     * The array of all the valid terrain type for a BattleHex.
     */
    private static final char[] allTerrains =
    { 'p', 'w', 'r', 's', 't', 'o', 'v', 'd', 'l' };

    // Hexside terrain types are:
    // d, c, s, w, space
    // dune, cliff, slope, wall, no obstacle
    /**
     * The array of all the valid terrain type for a BattleHex Side.
     */
    private static final char[] allHexsides =
    { ' ', 'd', 'c', 's', 'w' };

    /**
     * Hold the type of the six side of the BattleHex.
     * The hexside is marked only in the higher hex.
     */
    private char [] hexsides = new char[6];

    /**
     * Links to the neighbors of the BattleHex.
     * Neighbors have one hex side in common.
     * Non-existant neighbor are marked with <b>null</b>.
     */
    private BattleHex [] neighbors = new BattleHex[6];

    private int xCoord;
    private int yCoord;

    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

    /** Movement costs */
    public static final int IMPASSIBLE_COST = 99;
    private static final int SLOW_COST = 2;
    private static final int NORMAL_COST = 1;



    BattleHex(int xCoord, int yCoord)
    {
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        for (int i = 0; i < 6; i++)
        {
            hexsides[i] = ' ';
        }

        setTerrain('p');
        assignLabel();
    }


    public String getTerrainName()
    {
        String terrainName;

        switch (getTerrain())
        {
        case 'p':
            terrainName = "Plains";
            break;
        case 'w':
            terrainName = "Tower";
            break;
        case 'r':
            terrainName = "Brambles";
            break;
        case 's':
            terrainName = "Sand";
            break;
        case 't':
            /* tree height is irrelevant, so get out now */
            return "Tree";
        case 'o':
            terrainName = "Bog";
            break;
        case 'v':
            terrainName = "Volcano";
            break;
        case 'd':
            terrainName = "Drift";
            break;
        case 'l':
            terrainName = "Lake";
            break;
        default:
            terrainName = "?????";
            break;
        }
        if (elevation == 0)
            return(terrainName);
        else
            return(terrainName + " (" + elevation + ")");
    }
    

    Color getTerrainColor()
    {
        switch (getTerrain())
        {
        case 'p':  // plain
            switch (elevation)
            {
            case 0:
                return HTMLColor.lightOlive;
            case 1:
                return HTMLColor.darkYellow;
            default:
            case 2:
                return Color.yellow;
            }
        case 'w':  // tower
            switch (elevation)
            {
            case 0:
                return HTMLColor.lightGray;
            case 1:
                return Color.gray;
            default:
            case 2:
                return HTMLColor.darkGray;
            }
        case 'r':  // bramble
            return Color.green;
        case 's':  // sand
            return Color.orange;
        case 't':  // tree
            return HTMLColor.brown;
        case 'o':  // bog
            return Color.gray;
        case 'v':  // volcano
            return Color.red;
        case 'd':  // drift
            return Color.blue;
        case 'l':  // lake
            return HTMLColor.skyBlue;
        default:
            return Color.black;
        }
    }


    public boolean isNativeBonusTerrain()
    {
        char t = getTerrain();
        if (t == 'r' || t == 'v')
        {
            return true;
        }
        for (int i = 0; i < 6; i++)
        {
            char h = getHexside(i);
            if (h == 'w' || h == 's' || h == 'd')
            {
                return true;
            }
        }
        return false;
    }


    public boolean isNonNativePenaltyTerrain()
    {
        char t = getTerrain();
        if (t == 'r' || t == 'd')
        {
            return true;
        }
        for (int i = 0; i < 6; i++)
        {
            char h = getOppositeHexside(i);
            if (h == 'w' || h == 's' || h == 'd')
            {
                return true;
            }
        }
        return false;
    }


    private void assignLabel()
    {
        if (xCoord == -1)
        {
            label = "X" + yCoord;
            return;
        }

        char xLabel;
        switch (xCoord)
        {
            case 0:
                xLabel = 'A';
                break;
            case 1:
                xLabel = 'B';
                break;
            case 2:
                xLabel = 'C';
                break;
            case 3:
                xLabel = 'D';
                break;
            case 4:
                xLabel = 'E';
                break;
            case 5:
                xLabel = 'F';
                break;
            default:
                xLabel = '?';
        }

        int yLabel = 6 - yCoord - (int)Math.abs(((xCoord - 3) / 2));
        label = xLabel + Integer.toString(yLabel);
    }


    public void setHexside(int i, char hexside)
    {
        this.hexsides[i] = hexside;
    }


    public char getHexside(int i)
    {
        if (i >= 0 && i <= 5)
        {
            return hexsides[i];
        }
        else
        {
            Log.warn("Called BattleHex.getHexside() with " + i);
            return '?';
        }
    }

    public String getHexsideName(int i)
    {
        switch(hexsides[i])
        {
        default:
        case ' ':
            return("Nothing");
        case 'd':
            return("Dune");
        case 'c':
            return("Cliff");
        case 's':
            return("Slope");
        case 'w':
            return("Wall");
        }
    }

    /** Return the flip side of hexside i. */
    public char getOppositeHexside(int i)
    {
        char hexside = ' ';

        BattleHex neighbor = getNeighbor(i);
        if (neighbor != null)
        {
            hexside = neighbor.getHexside((i + 3) % 6);
        }

        return hexside;
    }


    public int getElevation()
    {
        return elevation;
    }

    public void setElevation (int elevation)
    {
        this.elevation = elevation;
    }


    public BattleHex getNeighbor(int i)
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

    public void setNeighbor(int i, BattleHex hex)
    {
        if (i >= 0 && i < 6)
        {
            neighbors[i] = hex;
        }
    }


    public int getXCoord()
    {
        return xCoord;
    }

    public int getYCoord()
    {
        return yCoord;
    }


    public boolean isEntrance()
    {
        return (xCoord == -1);
    }


    public boolean hasWall()
    {
        for (int i = 0; i < 6; i++)
        {
            if (hexsides[i] == 'w')
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Return the number of movement points it costs to enter this hex.
     * For fliers, this is the cost to land in this hex, not fly over it.
     * If entry is illegal, just return a cost greater than the maximum
     * possible number of movement points. This caller is responsible
     * for checking to see if this hex is already occupied.
     * @param creature The Creature that is trying to move into the BattleHex.
     * @param cameFrom The HexSide through which the Creature try to enter.
     * @return Cost to enter the BattleHex.
     */
    public int getEntryCost(Creature creature, int cameFrom)
    {
        char terrain = getTerrain();

        // Check to see if the hex is occupied or totally impassable.
        if (((terrain == 'l') && (!creature.isWaterDwelling())) ||
            (terrain == 't') ||
            ((terrain == 'v') && (!creature.isNativeVolcano())) ||
            ((terrain == 'o') && (!creature.isNativeBog())))
        {
            return IMPASSIBLE_COST;
        }

        char hexside = getHexside(cameFrom);

        // Non-fliers may not cross cliffs.
        if ((hexside == 'c' || getOppositeHexside(cameFrom) == 'c') &&
            !creature.isFlier())
        {
            return IMPASSIBLE_COST;
        }

        // Check for a slowing hexside.
        if ((hexside == 'w' || (hexside == 's' && !creature.isNativeSlope()))
            && !creature.isFlier() &&
            elevation > getNeighbor(cameFrom).getElevation())
        {
            // All hexes where this applies happen to have no
            // additional movement costs.
            return SLOW_COST;
        }

        // Bramble, drift, and sand slow non-natives, except that sand
        //     doesn't slow fliers.
        if ((terrain == 'r' && !creature.isNativeBramble()) ||
            (terrain == 'd' && !creature.isNativeDrift()) ||
            (terrain == 's' && !creature.isNativeSandDune() &&
            !creature.isFlier()))
        {
            return SLOW_COST;
        }

        // Other hexes only cost 1.
        return NORMAL_COST;
    }

    /**
     * Check if the Creature given in parameter can fly over
     * the BattleHex, or not.
     * @param creature The Creature that want to fly over this BattleHex
     * @return If the Creature can fly over here or not.
     */
    public boolean canBeFliedOverBy(Creature creature)
    {
        char terrain = getTerrain();
        if (!creature.isFlier())
        { // non-flyer can't fly, obviously...
            return false;
        }
        if (terrain == 'v')
        { // only volcano-native can fly over volcano
            return creature.isNativeVolcano();
        }
        return(true);
    }

    /**
     * Return how much damage the Creature should take from this Hex.
     * @param creature The Creature that may suffer damage.
     * @return HOw much damage the Creature should take from being there.
     */
    public int damageToCreature(Creature creature)
    {
        char terrain = getTerrain();
        if ((terrain == 'd') && (!creature.isNativeDrift()))
        { // Non-native take damage in Drift
            return 1;
        }
        if ((terrain == 's') && (creature.isWaterDwelling()))
        { // Water Dweller (amphibious) take damage in Sand
            return 1;
        }
        // default : no damage !
        return 0;
    }

    public boolean isCliff(int hexside)
    {
        return getHexside(hexside) == 'c' || 
            getOppositeHexside(hexside) == 'c';
    }

    public static char[] getTerrains()
    {
        return (char[])allTerrains.clone();
    }

    public static char[] getHexsides()
    {
        return (char[])allHexsides.clone();
    }
}
