package net.sf.colossus.client;


import java.awt.Color;

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

    /** Valid elevations are 0, 1, and 2.  Also 3 for JDG Badlands. */
    private int elevation;

    // Hex terrain types are:
    // p, r, s, t, o, v, d, w
    // plain, bramble, sand, tree, bog, volcano, drift, tower
    // also
    // l, n
    // lake, stone

    /**
     * The array of all the valid terrain type for a BattleHex.
     */
    //private static final char[] allTerrains =
    //{ 'p', 'w', 'r', 's', 't', 'o', 'v', 'd', 'l', 'n' };
    private static final String[] allTerrains = { "Plains", "Tower", "Brambles",
        "Sand", "Tree", "Bog", "Volcano", "Drift",
        "Lake", "Stone"};

    // Hexside terrain types are:
    // d, c, s, w, space
    // dune, cliff, slope, wall, no obstacle
    // also
    // r
    // river
    /**
     * The array of all the valid terrain type for a BattleHex Side.
     */
    private static final char[] allHexsides = { ' ', 'd', 'c', 's', 'w', 'r' };
    //private static final String[] allHexsides =
    //{ "Nothing", "Dune", "Cliff", "Slope", "Wall", "River"};

    /**
     * Hold the type of the six side of the BattleHex.
     * The hexside is marked only in the higher hex.
     */
    private char[] hexsides = new char[6];

    /**
     * Links to the neighbors of the BattleHex.
     * Neighbors have one hex side in common.
     * Non-existant neighbor are marked with <b>null</b>.
     */
    private BattleHex[] neighbors = new BattleHex[6];

    private int xCoord;
    private int yCoord;

    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

    /** Movement costs */
    public static final int IMPASSIBLE_COST = 99;
    private static final int SLOW_COST = 2;
    private static final int NORMAL_COST = 1;
    private static final int SLOW_INCREMENT_COST = SLOW_COST - NORMAL_COST;

    BattleHex(int xCoord, int yCoord)
    {
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        for (int i = 0; i < 6; i++)
        {
            hexsides[i] = ' ';
        }

        setTerrain("Plains");
        assignLabel();
    }

    public String getTerrainName()
    {
        if (elevation == 0)
        {
            return(getTerrain());
        }
        else
        {
            return(getTerrain() + " (" + elevation + ")");
        }
    }

    Color getTerrainColor()
    {
        if (getTerrain().equals("Plains"))
        {
            switch (elevation)
            {
                case 0:
                    return HTMLColor.lightOlive;

                case 1:
                    return HTMLColor.darkYellow;

                case 2:
                    return Color.yellow;

                default:
                case 3:
                    return HTMLColor.lightYellow;
            }
        }
        else if (getTerrain().equals("Tower"))
        {
            switch (elevation)
            {
                case 0:
                    return HTMLColor.dimGray;

                case 1:
                    return HTMLColor.darkGray;

                case 2:
                    return Color.gray;

                default:
                case 3:
                    return HTMLColor.lightGray;
            }
        }
        else if (getTerrain().equals("Brambles"))
        {
            switch (elevation)
            {
                case 0:
                    return Color.green;

                case 1:
                    return HTMLColor.brambleGreen1;

                case 2:
                    return HTMLColor.brambleGreen2;

                default:
                case 3:
                    return HTMLColor.darkGreen;
            }
        }
        else if (getTerrain().equals("Sand"))
        {
            return Color.orange;
        }
        else if (getTerrain().equals("Tree"))
        {
            return HTMLColor.brown;
        }
        else if (getTerrain().equals("Bog"))
        {
            return Color.gray;
        }
        else if (getTerrain().equals("Volcano"))
        {
            switch (elevation)
            {
                case 3:
                    return Color.red;

                default:
                case 2:
                    return HTMLColor.darkRed;
            }
        }
        else if (getTerrain().equals("Drift"))
        {
            return Color.blue;
        }
        else if (getTerrain().equals("Lake"))
        {
            return HTMLColor.skyBlue;
        }
        else if (getTerrain().equals("Stone"))
        {
            return HTMLColor.dimGray;
        }
        else
        {
            return Color.black;
        }
    }

    public static boolean isNativeBonusHazard(String name)
    {
        if (name.equals("Brambles") ||
                name.equals("Volcano"))
        {
            return true;
        }
        return false;
    }

    public static boolean isNativeBonusHexside(char h)
    {
        if (h == 'w' || h == 's' || h == 'd')
        {
            return true;
        }
        return false;
    }

    public boolean isNativeBonusTerrain()
    {
        boolean result;
        result = isNativeBonusHazard(getTerrain());

        for (int i = 0; i < 6; i++)
        {
            char h = getHexside(i);
            result = result || isNativeBonusHexside(h);
        }
        return result;
    }

    public static boolean isNonNativePenaltyHazard(String name)
    {
        if (name.equals("Brambles") ||
                name.equals("Drift"))
        {
            return true;
        }
        return false;
    }

    public static boolean isNonNativePenaltyHexside(char h)
    {
        if (h == 'w' || h == 's' || h == 'd')
        {
            return true;
        }
        return false;
    }

    public boolean isNonNativePenaltyTerrain()
    {
        boolean result;
        result = isNonNativePenaltyHazard(getTerrain());
        for (int i = 0; i < 6; i++)
        {
            char h = getOppositeHexside(i);
            result = result || isNonNativePenaltyHexside(h);
        }
        return result;
    }

    private void assignLabel()
    {
        String label;

        if (xCoord == -1)
        {
            label = "X" + yCoord;

        }
        else
        {
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
        setLabel(label);
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
        switch (hexsides[i])
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

            case 'r':
                return("River");
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

    public void setElevation(int elevation)
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

    public boolean blocksLineOfSight()
    {
        return (getTerrain().equals("Tree") || getTerrain().equals("Stone"));
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
    public int getEntryCost(Creature creature, int cameFrom, boolean cumul)
    {
        int cost = NORMAL_COST;

        // Check to see if the hex is occupied or totally impassable.
        if ((getTerrain().equals("Lake") && (!creature.isWaterDwelling())) ||
                (getTerrain().equals("Tree") && (!creature.isNativeTree())) ||
                (getTerrain().equals("Stone") && (!creature.isNativeStone())) ||
                (getTerrain().equals("Volcano") &&
                (!creature.isNativeVolcano())) ||
                (getTerrain().equals("Bog") && (!creature.isNativeBog())))
        {
            cost += IMPASSIBLE_COST;
        }

        char hexside = getHexside(cameFrom);

        // Non-fliers may not cross cliffs.
        if ((hexside == 'c' || getOppositeHexside(cameFrom) == 'c') &&
                !creature.isFlier())
        {
            cost += IMPASSIBLE_COST;
        }

        // river slows both way, except native & water dwellers
        if ((hexside == 'r' || getOppositeHexside(cameFrom) == 'r') &&
                !creature.isFlier() && !creature.isWaterDwelling() &&
                !creature.isNativeRiver())
        {
            cost += SLOW_INCREMENT_COST;
        }

        // Check for a slowing hexside.
        if ((hexside == 'w' ||
                (hexside == 's' && !creature.isNativeSlope())) &&
                !creature.isFlier() &&
                elevation > getNeighbor(cameFrom).getElevation())
        {
            cost += SLOW_INCREMENT_COST;
        }

        // Bramble, drift, and sand slow non-natives, except that sand
        //     doesn't slow fliers.
        if ((getTerrain().equals("Brambles") && !creature.isNativeBramble()) ||
                (getTerrain().equals("Drift") && !creature.isNativeDrift()) ||
                (getTerrain().equals("Sand") && !creature.isNativeSandDune() &&
                !creature.isFlier()))
        {
            cost += SLOW_INCREMENT_COST;
        }

        if (cost > IMPASSIBLE_COST)
        { // max out impassible at IMPASSIBLE_COST
            cost = IMPASSIBLE_COST;
        }

        if ((cost < IMPASSIBLE_COST) && (cost > SLOW_COST) && (!cumul))
        { // don't cumul Slow
            cost = SLOW_COST;
        }

        return cost;
    }

    /**
     * Check if the Creature given in parameter can fly over
     * the BattleHex, or not.
     * @param creature The Creature that want to fly over this BattleHex
     * @return If the Creature can fly over here or not.
     */
    public boolean canBeFlownOverBy(Creature creature)
    {
        if (!creature.isFlier())
        { // non-flyer can't fly, obviously...
            return false;
        }
        if (getTerrain().equals("Stone"))
        { // no one can fly through stone
            return false;
        }
        if (getTerrain().equals("Volcano"))
        { // only volcano-native can fly over volcano
            return creature.isNativeVolcano();
        }
        return(true);
    }

    /**
     * Return how much damage the Creature should take from this Hex.
     * @param creature The Creature that may suffer damage.
     * @return How much damage the Creature should take from being there.
     */
    public int damageToCreature(Creature creature)
    {
        if (getTerrain().equals("Drift") && (!creature.isNativeDrift()))
        { // Non-native take damage in Drift
            return 1;
        }
        if (getTerrain().equals("Sand") && (creature.isWaterDwelling()))
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

    public static String[] getTerrains()
    {
        return (String[])allTerrains.clone();
    }

    public static char[] getHexsides()
    {
        return (char[])allHexsides.clone();
    }
}
