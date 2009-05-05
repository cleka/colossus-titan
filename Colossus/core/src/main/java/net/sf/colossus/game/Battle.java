package net.sf.colossus.game;


import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.util.CompareDoubles;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * An ongoing battle.
 */
abstract public class Battle
{
    private static final Logger LOGGER = Logger.getLogger(Battle.class
        .getName());

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

    /** Whether the hex is occupied by a critter/creature/chit/...
     * This is abstract because the specific information about critter/...
     * is currently kept in the subclass, but this information is required
     * by several helper function located in the Battle class.
     * @param hex The hex whose occupancy is being checked
     * @return Whether the hex is occupied by a critter/creature/chit/...
     */
    abstract protected boolean isOccupied(BattleHex hex);

    /**
     * Caller must ensure that yDist != 0
     */
    protected static boolean toLeft(double xDist, double yDist)
    {
        double ratio = xDist / yDist;
        if (ratio >= 1.5 || (ratio >= 0 && ratio <= 0.75)
            || (ratio >= -1.5 && ratio <= -0.75))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Return the hexside direction of the path from hex1 to hex2.
     * Sometimes two directions are possible.  If the left parameter
     * is set, the direction further left will be given.  Otherwise,
     * the direction further right will be given.
     */
    public static int getDirection(BattleHex hex1, BattleHex hex2, boolean left)
    {
        if (hex1 == hex2)
        {
            return -1;
        }
        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();
        // Offboard creatures are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return -1;
        }
        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }
        int xDist = x2 - x1;
        double yDist = y2 - y1;
        double xDistAndAHalf = 1.5 * xDist;
        if (xDist >= 0)
        {
            if (yDist > xDistAndAHalf)
            {
                return 3;
            }
            else if (CompareDoubles.almostEqual(yDist, xDistAndAHalf))
            {
                if (left)
                {
                    return 2;
                }
                else
                {
                    return 3;
                }
            }
            else if (yDist < -xDistAndAHalf)
            {
                return 0;
            }
            else if (CompareDoubles.almostEqual(yDist, -xDistAndAHalf))
            {
                if (left)
                {
                    return 0;
                }
                else
                {
                    return 1;
                }
            }
            else if (yDist > 0)
            {
                return 2;
            }
            else if (yDist < 0)
            {
                return 1;
            }
            else
            {
                if (left)
                {
                    return 1;
                }
                else
                {
                    return 2;
                }
            }
        }
        else
        {
            if (yDist < xDistAndAHalf)
            {
                return 0;
            }
            else if (CompareDoubles.almostEqual(yDist, xDistAndAHalf))
            {
                if (left)
                {
                    return 5;
                }
                else
                {
                    return 0;
                }
            }
            else if (yDist > -xDistAndAHalf)
            {
                return 3;
            }
            else if (CompareDoubles.almostEqual(yDist, -xDistAndAHalf))
            {
                if (left)
                {
                    return 3;
                }
                else
                {
                    return 4;
                }
            }
            else if (yDist > 0)
            {
                return 4;
            }
            else if (yDist < 0)
            {
                return 5;
            }
            else
            {
                if (left)
                {
                    return 4;
                }
                else
                {
                    return 5;
                }
            }
        }
    }

    /**
     * @deprecated This is the realm of HazardEdge, not direct use of hexside
     */
    @Deprecated
    private static boolean isObstacle(char hexside)
    {
        return (hexside != ' ') && (hexside != 'r');
    }

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
            return (int)Math.ceil(xDist + 1);
        }
        else if (xDist >= yDist)
        {
            return (int)Math.floor(xDist + 2);
        }
        else if (yDist >= 2 * xDist)
        {
            return (int)Math.ceil(yDist + 1);
        }
        else
        {
            return (int)Math.floor(yDist + 2);
        }
    }

    /**
     * Return the minimum range from any neighbor of hex1 to hex2.
     */
    private static int minRangeToNeighbor(BattleHex hex1, BattleHex hex2)
    {
        int min = Constants.OUT_OF_RANGE;
        for (int i = 0; i < 6; i++)
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

    /**
     * Check to see if the LOS from hex1 to hex2 is blocked.  If the LOS
     * lies along a hexspine, check both and return true only if both are
     * blocked.
     */
    public boolean isLOSBlocked(BattleHex hex1, BattleHex hex2)
    {
        if (hex1 == hex2)
        {
            return false;
        }
        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();
        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return true;
        }
        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }
        double xDist = x2 - x1;
        double yDist = y2 - y1;
        // Creatures below the level of the strike do not block LOS.
        int strikeElevation = Math.min(hex1.getElevation(), hex2
            .getElevation());
        if (CompareDoubles.almostEqual(yDist, 0.0)
            || CompareDoubles.almostEqual(Math.abs(yDist), 1.5 * Math
                .abs(xDist)))
        {
            return isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0)
                && isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                    false, false, false, false, false, 0);
        }
        else
        {
            return isLOSBlockedDir(hex1, hex1, hex2, toLeft(xDist, yDist),
                strikeElevation, false, false, false, false, false, 0);
        }
    }

    /**
     * Check LOS, going to the left of hexspines if argument left is true, or
     * to the right if it is false.
     */
    protected boolean isLOSBlockedDir(BattleHex initialHex,
        BattleHex currentHex, BattleHex finalHex, boolean left,
        int strikeElevation, boolean strikerAtop, boolean strikerAtopCliff,
        boolean midObstacle, boolean midCliff, boolean midChit,
        int totalObstacles)
    {
        boolean targetAtop = false;
        boolean targetAtopCliff = false;
        if (currentHex == finalHex)
        {
            return false;
        }
        // Offboard hexes are not allowed.
        if (currentHex.getXCoord() == -1 || finalHex.getXCoord() == -1)
        {
            return true;
        }
        int direction = getDirection(currentHex, finalHex, left);
        BattleHex nextHex = currentHex.getNeighbor(direction);
        if (nextHex == null)
        {
            return true;
        }
        char hexside = currentHex.getHexsideHazard(direction).getCode();
        char hexside2 = currentHex.getOppositeHazard(direction).getCode();
        if (currentHex == initialHex)
        {
            if (isObstacle(hexside))
            {
                strikerAtop = true;
                totalObstacles++;
                if (hexside == 'c')
                {
                    strikerAtopCliff = true;
                }
            }
            if (isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside2 == 'c')
                {
                    midCliff = true;
                }
                if (hexside2 == 'w')
                {
                    return true;
                }
            }
        }
        else if (nextHex == finalHex)
        {
            if (isObstacle(hexside))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c')
                {
                    midCliff = true;
                }
                if (hexside == 'w')
                {
                    return true;
                }
            }
            if (isObstacle(hexside2))
            {
                targetAtop = true;
                totalObstacles++;
                if (hexside2 == 'c')
                {
                    targetAtopCliff = true;
                }
            }
            if (midChit && !targetAtopCliff)
            {
                return true;
            }
            if (midCliff && !strikerAtopCliff && !targetAtopCliff)
            {
                return true;
            }
            if (midObstacle && !strikerAtop && !targetAtop)
            {
                return true;
            }
            if (totalObstacles >= 3 && (!strikerAtop || !targetAtop)
                && (!strikerAtopCliff && !targetAtopCliff))
            {
                return true;
            }
            return false;
        }
        else
        {
            if (midChit)
            {
                // We're not in the initial or final hex, and we have already
                // marked an mid chit, so it's not adjacent to the base of a
                // cliff that the target is atop.
                return true;
            }
            if (isObstacle(hexside) || isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c' || hexside2 == 'c')
                {
                    midCliff = true;
                }
            }
        }
        if (nextHex.blocksLineOfSight())
        {
            return true;
        }
        // Creatures block LOS, unless both striker and target are at higher
        //     elevation than the creature, or unless the creature is at
        //     the base of a cliff and the striker or target is atop it.
        if (isOccupied(nextHex) && nextHex.getElevation() >= strikeElevation
            && (!strikerAtopCliff || currentHex != initialHex))
        {
            midChit = true;
        }
        return isLOSBlockedDir(initialHex, nextHex, finalHex, left,
            strikeElevation, strikerAtop, strikerAtopCliff, midObstacle,
            midCliff, midChit, totalObstacles);
    }

    /**
     * Return true if the rangestrike is possible.
     */
    protected boolean isRangestrikePossible(Creature critter, Creature target,
        BattleHex currentHex, BattleHex targetHex)
    {
        int range = getRange(currentHex, targetHex, false);
        int skill = critter.getType().getSkill();
        if (range > skill)
        {
            return false;
        }
        else if (!critter.getType().useMagicMissile()
            && (range < 3 || target.getType().isLord() || isLOSBlocked(
                currentHex, targetHex)))
        {
            return false;
        }
        return true;
    }

    private int computeSkillPenaltyRangestrikeThroughDir(BattleHex hex1,
        BattleHex hex2, Creature c, boolean left, int previousCount)
    {
        int count = previousCount;

        // Offboard hexes are not allowed.
        if (hex1.getXCoord() == -1 || hex2.getXCoord() == -1)
        {
            return Constants.BIGNUM;
        }

        int direction = getDirection(hex1, hex2, left);

        BattleHex nextHex = hex1.getNeighbor(direction);
        if (nextHex == null)
        {
            return Constants.BIGNUM;
        }

        if (nextHex == hex2)
        {
            // Success!
            return count;
        }

        HazardTerrain terrain = nextHex.getTerrain();

        count += terrain.getSkillPenaltyRangestrikeThrough(c.getType()
            .isNativeIn(terrain));

        return computeSkillPenaltyRangestrikeThroughDir(nextHex, hex2, c,
            left, count);
    }

    /** Compute the minimum Skill penalty that the creature will endure to
     * rangestrike from hex1 to a creature in hex2 from the intervening hex.
     * @param hex1 The hex in which the rangestriker sit
     * @param hex2 The hex in which the rangestruck sit
     * @param c The rangestriker
     * @return The penalty to the Skill Factor of the rangestriker from intervening hex.
     */
    public int computeSkillPenaltyRangestrikeThrough(BattleHex hex1,
        BattleHex hex2, Creature c)
    {
        if (hex1 == hex2)
        {
            return 0;
        }

        int x1 = hex1.getXCoord();
        double y1 = hex1.getYCoord();
        int x2 = hex2.getXCoord();
        double y2 = hex2.getYCoord();

        // Offboard hexes are not allowed.
        if (x1 == -1 || x2 == -1)
        {
            return Constants.BIGNUM;
        }

        // Hexes with odd X coordinates are pushed down half a hex.
        if ((x1 & 1) == 1)
        {
            y1 += 0.5;
        }
        if ((x2 & 1) == 1)
        {
            y2 += 0.5;
        }

        double xDist = x2 - x1;
        double yDist = y2 - y1;

        if (CompareDoubles.almostEqual(yDist, 0.0)
            || CompareDoubles.almostEqual(Math.abs(yDist), 1.5 * Math
                .abs(xDist)))
        {
            int strikeElevation = Math.min(hex1.getElevation(), hex2
                .getElevation());
            // Hexspine; try unblocked side(s)
            if (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0))
            {
                return computeSkillPenaltyRangestrikeThroughDir(hex1, hex2, c,
                    false, 0);
            }
            else if (isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, 0))
            {
                return computeSkillPenaltyRangestrikeThroughDir(hex1, hex2, c,
                    true, 0);
            }
            else
            {
                return Math.min(computeSkillPenaltyRangestrikeThroughDir(hex1,
                    hex2, c, true, 0),
                    computeSkillPenaltyRangestrikeThroughDir(hex1, hex2, c,
                        false, 0));
            }
        }
        else
        {
            return computeSkillPenaltyRangestrikeThroughDir(hex1, hex2, c,
                toLeft(xDist, yDist), 0);
        }
    }

    protected Legion getLegionByPlayer(Player player)
    {
        Legion lattacker = getAttackingLegion();
        if (lattacker != null && lattacker.getPlayer().equals(player))
        {
            return lattacker;
        }
        Legion ldefender = getDefendingLegion();
        if (ldefender != null && ldefender.getPlayer().equals(player))
        {
            return ldefender;
        }
        return null;
    }
}
