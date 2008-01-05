package net.sf.colossus.client;


import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.server.Creature;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.variant.HazardTerrain;


/**
 * Class BattleHex holds game state for battle hex.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class BattleHex extends Hex
{
    private static final Logger LOGGER = Logger.getLogger(BattleHex.class
        .getName());

    /** Valid elevations are 0, 1, and 2.  Also 3 for JDG Badlands. */
    private int elevation;

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
    //{ "Nothing", "Dune", "Cliff", "Slope", "Wall", "River"};

    /**
     * Hold the type of the six side of the BattleHex.
     * The hexside is marked only in the higher hex.
     */
    private final char[] hexsides = new char[6];

    /**
     * Links to the neighbors of the BattleHex.
     * Neighbors have one hex side in common.
     * Non-existent neighbor are marked with <b>null</b>.
     */
    private final BattleHex[] neighbors = new BattleHex[6];

    private final int xCoord;
    private final int yCoord;

    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

    /*
     * TODO this should be final, but can't be at the moment
     */
    private HazardTerrain terrain;

    /** Movement costs */
    public static final int IMPASSIBLE_COST = 99;
    private static final int SLOW_COST = 2;
    private static final int NORMAL_COST = 1;
    private static final int SLOW_INCREMENT_COST = SLOW_COST - NORMAL_COST;

    public BattleHex(int xCoord, int yCoord)
    {
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        for (int i = 0; i < 6; i++)
        {
            hexsides[i] = ' ';
        }

        terrain = HazardTerrain.PLAINS;
        assignLabel();
    }

    public HazardTerrain getTerrain()
    {
        return this.terrain;
    }

    public void setTerrain(HazardTerrain terrain)
    {
        this.terrain = terrain;
    }

    public String getTerrainName()
    {
        String terrainName = terrain.getName();
        if (elevation == 0)
        {
            return terrainName;
        }
        else
        {
            return terrainName + " (" + elevation + ")";
        }
    }

    public Color getTerrainColor()
    {
        if (terrain.equals(HazardTerrain.PLAINS))
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
        else if (terrain.equals(HazardTerrain.TOWER))
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
        else if (terrain.equals(HazardTerrain.BRAMBLES))
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
        else if (terrain.equals(HazardTerrain.SAND))
        {
            return Color.orange;
        }
        else if (terrain.equals(HazardTerrain.TREE))
        {
            return HTMLColor.brown;
        }
        else if (terrain.equals(HazardTerrain.BOG))
        {
            return Color.gray;
        }
        else if (terrain.equals(HazardTerrain.VOLCANO))
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
        else if (terrain.equals(HazardTerrain.DRIFT))
        {
            return Color.blue;
        }
        else if (terrain.equals(HazardTerrain.LAKE))
        {
            return HTMLColor.skyBlue;
        }
        else if (terrain.equals(HazardTerrain.STONE))
        {
            return HTMLColor.dimGray;
        }
        else
        {
            return Color.black;
        }
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
        result = terrain.isNativeBonusTerrain();

        for (int i = 0; i < 6; i++)
        {
            char h = getHexside(i);
            result = result || isNativeBonusHexside(h);
        }
        return result;
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
        result = terrain.isNonNativePenaltyTerrain();
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
        if (xCoord < 0)
        {
            label = "X" + yCoord;
        }
        else
        {
            final int yLabel = 6 - yCoord - Math.abs(((xCoord - 3) / 2));
            label = "" + _intXCoordToXLabel(xCoord) + yLabel;
        }
        setLabel(label);
    }

    /** a char for an int: 0:'A'=0, 1:'B', ... int(w):'W', else:'?', <0:undef.
     * */
    private final static char _intXCoordToXLabel(final int x)
    {
        return (x < 'X') // 'X' is used for -1 
        ? (char)('A' + x)
            : '?';
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
            LOGGER.log(Level.WARNING, "Called BattleHex.getHexside() with "
                + i);
            return '?';
        }
    }

    public String getHexsideName(int i)
    {
        switch (hexsides[i])
        {
            default:
            case ' ':
                return ("Nothing");

            case 'd':
                return ("Dune");

            case 'c':
                return ("Cliff");

            case 's':
                return ("Slope");

            case 'w':
                return ("Wall");

            case 'r':
                return ("River");
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
        assert (i >= 0) && (i <= 5) : "Neighbor index out of range";
        return neighbors[i];
    }

    public void setNeighbor(int i, BattleHex hex)
    {
        assert (i >= 0) && (i <= 5) : "Neighbor index out of range";
        neighbors[i] = hex;
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
        return (terrain.equals(HazardTerrain.TREE) || terrain
            .equals(HazardTerrain.STONE));
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
        if ((terrain.equals(HazardTerrain.LAKE) && (!creature
            .isWaterDwelling()))
            || (terrain.equals(HazardTerrain.TREE) && (!creature
                .isNativeTree()))
            || (terrain.equals(HazardTerrain.STONE) && (!creature
                .isNativeStone()))
            || (terrain.equals(HazardTerrain.VOLCANO) && (!creature
                .isNativeVolcano()))
            || (terrain.equals(HazardTerrain.BOG) && (!creature.isNativeBog())))
        {
            cost += IMPASSIBLE_COST;
        }

        char hexside = getHexside(cameFrom);

        // Non-fliers may not cross cliffs.
        if ((hexside == 'c' || getOppositeHexside(cameFrom) == 'c')
            && !creature.isFlier())
        {
            cost += IMPASSIBLE_COST;
        }

        // river slows both way, except native & water dwellers
        if ((hexside == 'r' || getOppositeHexside(cameFrom) == 'r')
            && !creature.isFlier() && !creature.isWaterDwelling()
            && !creature.isNativeRiver())
        {
            cost += SLOW_INCREMENT_COST;
        }

        // Check for a slowing hexside.
        if ((hexside == 'w' || (hexside == 's' && !creature.isNativeSlope()))
            && !creature.isFlier()
            && elevation > getNeighbor(cameFrom).getElevation())
        {
            cost += SLOW_INCREMENT_COST;
        }

        // Bramble, drift, and sand slow non-natives, except that sand
        //     doesn't slow fliers.
        if ((terrain.equals(HazardTerrain.BRAMBLES) && !creature
            .isNativeBramble())
            || (terrain.equals(HazardTerrain.DRIFT) && !creature
                .isNativeDrift())
            || (terrain.equals(HazardTerrain.SAND)
                && !creature.isNativeSandDune() && !creature.isFlier()))
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
        if (terrain.equals(HazardTerrain.STONE))
        { // no one can fly through stone
            return false;
        }
        if (terrain.equals(HazardTerrain.VOLCANO))
        { // only volcano-native can fly over volcano
            return creature.isNativeVolcano();
        }
        return (true);
    }

    /**
     * Return how much damage the Creature should take from this Hex.
     * @param creature The Creature that may suffer damage.
     * @return How much damage the Creature should take from being there.
     */
    public int damageToCreature(Creature creature)
    {
        if (terrain.equals(HazardTerrain.DRIFT) && (!creature.isNativeDrift()))
        { // Non-native take damage in Drift
            return 1;
        }
        if (terrain.equals(HazardTerrain.SAND) && (creature.isWaterDwelling()))
        { // Water Dweller (amphibious) take damage in Sand
            return 1;
        }
        // default : no damage !
        return 0;
    }

    public boolean isCliff(int hexside)
    {
        return getHexside(hexside) == 'c'
            || getOppositeHexside(hexside) == 'c';
    }

    public static char[] getHexsides()
    {
        return allHexsides;
    }
}
