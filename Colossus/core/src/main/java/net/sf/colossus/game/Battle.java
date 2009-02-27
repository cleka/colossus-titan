package net.sf.colossus.game;


import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.colossus.server.Constants;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * An ongoing battle.
 */
public class Battle
{
    private static final Logger LOGGER = Logger
        .getLogger(Battle.class.getName());

    /**
     * Return the range in hexes from hex1 to hex2.  Titan ranges are
     * inclusive at both ends.
     */
    public static int getRange(BattleHex hex1, BattleHex hex2,
            boolean allowEntrance)
    {
        if (hex1 == null || hex2 == null)
        {
            LOGGER.log(Level.WARNING, "passed null hex to getRange()");
            return Constants.OUT_OF_RANGE;
        }
        if (hex1.isEntrance() || hex2.isEntrance())
        {
            if (allowEntrance)
            {
                if (hex1.isEntrance())
                {
                    return 1 + minRangeToNeighbor(hex1, hex2);
                }
                else
                {
                    return 1 + minRangeToNeighbor(hex2, hex1);
                }
            }
            else
            {
                // It's out of range.  No need to do the math.
                return Constants.OUT_OF_RANGE;
            }
        }
        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();
        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }
        double xDist = Math.abs(x2 - x1);
        double yDist = Math.abs(y2 - y1);
        if (xDist >= 2 * yDist)
        {
            return (int) Math.ceil(xDist + 1);
        }
        else if (xDist >= yDist)
        {
            return (int) Math.floor(xDist + 2);
        }
        else if (yDist >= 2 * xDist)
        {
            return (int) Math.ceil(yDist + 1);
        }
        else
        {
            return (int) Math.floor(yDist + 2);
        }
    }

    /**
     * Return the minimum range from any neighbor of hex1 to hex2.
     */
    private static int minRangeToNeighbor(BattleHex hex1, BattleHex hex2)
    {
        int min = Constants.OUT_OF_RANGE;
        for (int i = 0; i < 6;
                i++)
        {
            BattleHex hex = hex1.getNeighbor(i);
            if (hex != null)
            {
                int range = getRange(hex, hex2, false);
                if (range < min)
                {
                    min = range;
                }
            }
        }
        return min;
    }
    private final Game game;
    private final Legion attacker;
    private final Legion defender;
    private final MasterBoardTerrain land;

    public Battle(Game game, Legion attacker, Legion defender,
        MasterBoardTerrain land)
    {
        this.game = game;
        this.attacker = attacker;
        this.defender = defender;
        this.land = land;
    }

    public Game getGame()
    {
        return game;
    }

    public Legion getAttackingLegion()
    {
        return attacker;
    }

    public Legion getDefendingLegion()
    {
        return defender;
    }

    public MasterBoardTerrain getLand()
    {
        return land;
    }
}
