package net.sf.colossus.client;


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.Log;


/**
 * Class Strike holds client-side strike logic.
 *
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

// XXX Massive code duplication.

final class Strike
{
    private Client client;

    Strike(Client client)
    {
        this.client = client;
    }

    /** Return the set of hex labels for hexes with critters that have
     *  valid strike targets. */
    Set findCrittersWithTargets()
    {
        Set set = new HashSet();
        Iterator it = client.getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (countStrikes(chit, true) > 0)
            {
                set.add(chit.getCurrentHexLabel());
            }
        }

        return set;
    }

    /** Perform strikes for any creature that is forced to strike
     *  and has only one legal target. Forced strikes will never
     *  generate carries, since there's only one target. If
     *  rangestrike is true, also perform rangestrikes for
     *  creatures with only one target, even though they're not
     *  technically forced.  Return after one strike, so the
     *  client can wait for status from the server.
     *  XXX This method does stuff, rather than just returning 
     *  information, unlike the rest of the Strike class.
     *  Returns true if a strike was made. */
    boolean makeForcedStrikes(boolean rangestrike)
    {
        if (!client.getBattlePhase().isFightPhase() &&
                !client.isMyBattlePhase())
        {
            Log.error("Called Strike.makeForcedStrikes() in wrong phase");
            return false;
        }

        Iterator it = client.getActiveBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (!chit.hasStruck())
            {
                Set set = findStrikes(chit, rangestrike);
                if (set.size() == 1)
                {
                    String hexLabel = (String)(set.iterator().next());
                    client.strike(chit.getTag(), hexLabel);
                    return true;
                }
            }
        }
        return false;
    }

    boolean canStrike(BattleChit striker, BattleChit target)
    {
        String targetHexLabel = target.getCurrentHexLabel();
        return findStrikes(striker, true).contains(targetHexLabel);
    }

    Set findStrikes(int tag)
    {
        BattleChit chit = client.getBattleChit(tag);
        return findStrikes(chit, true);
    }

    /** Return a set of hex labels for hexes containing targets that the
     *  critter may strike.  Only include rangestrikes if rangestrike
     *  is true. */
    private Set findStrikes(BattleChit chit, boolean rangestrike)
    {
        Set set = new HashSet();

        // Each creature may strike only once per turn.
        if (chit.hasStruck())
        {
            return set;
        }
        // Offboard creatures can't strike.
        if (chit.getCurrentHexLabel().startsWith("X"))
        {
            return set;
        }

        boolean inverted = chit.isInverted();
        BattleHex currentHex = client.getBattleHex(chit);

        boolean adjacentEnemy = false;

        // First mark and count normal strikes.
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not engaged.
            if (!currentHex.isCliff(i))
            {
                BattleHex targetHex = currentHex.getNeighbor(i);
                if (targetHex != null && client.isOccupied(targetHex) &&
                        !targetHex.isEntrance())
                {
                    BattleChit target = client.getBattleChit(
                            targetHex.getLabel());
                    if (target.isInverted() != inverted)
                    {
                        adjacentEnemy = true;
                        if (!target.isDead())
                        {
                            set.add(targetHex.getLabel());
                        }
                    }
                }
            }
        }

        Creature creature = Creature.getCreatureByName(chit.getCreatureName());

        // Then do rangestrikes if applicable.  Rangestrikes are not allowed
        // if the creature can strike normally, so only look for them if
        // no targets have yet been found.
        if (rangestrike && !adjacentEnemy && creature.isRangestriker() &&
                client.getBattlePhase() != Constants.BattlePhase.STRIKEBACK)
        {
            Iterator it = client.getInactiveBattleChits().iterator();
            while (it.hasNext())
            {
                BattleChit target = (BattleChit)it.next();
                if (!target.isDead())
                {
                    BattleHex targetHex = client.getBattleHex(target);
                    if (isRangestrikePossible(chit, target))
                    {
                        set.add(targetHex.getLabel());
                    }
                }
            }
        }
        return set;
    }

    private int countStrikes(BattleChit chit, boolean rangestrike)
    {
        return findStrikes(chit, rangestrike).size();
    }

    /** Return the range in hexes from hex1 to hex2.  Titan ranges are
     *  inclusive at both ends. */
    static int getRange(BattleHex hex1, BattleHex hex2, boolean allowEntrance)
    {
        if (hex1 == null || hex2 == null)
        {
            Log.warn("passed null hex to getRange()");
            return Constants.OUT_OF_RANGE;
        }
        if (hex1.isEntrance() || hex2.isEntrance())
        {
            if (allowEntrance)
            {
                // The range to an entrance is the range to the
                // closest of its neighbors, plus one.
                if (hex1.isEntrance())
                {
                    return 1 + minRangeToNeighbor(hex1, hex2);
                }
                else  // hex2.isEntrance()
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

    /** Return the minimum range from any neighbor of hex1 to hex2. */
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

    /** Return the titan range (inclusive at both ends) from the critter to the
     *  closest enemy critter.  Return OUT_OF_RANGE if there are none. */
    int minRangeToEnemy(BattleChit chit)
    {
        BattleHex hex = client.getBattleHex(chit);
        int min = Constants.OUT_OF_RANGE;

        List battleChits = client.getBattleChits();
        Iterator it = battleChits.iterator();
        while (it.hasNext())
        {
            BattleChit target = (BattleChit)it.next();
            if (chit.isInverted() != target.isInverted())
            {
                BattleHex targetHex = client.getBattleHex(target);
                int range = getRange(hex, targetHex, false);
                // Exit early if adjacent.
                if (range == 2)
                {
                    return range;
                }
                else if (range < min)
                {
                    min = range;
                }
            }
        }
        return min;
    }

    /** Caller must ensure that yDist != 0 */
    private static boolean toLeft(double xDist, double yDist)
    {
        double ratio = xDist / yDist;
        if (ratio >= 1.5 || (ratio >= 0 && ratio <= .75) ||
                (ratio >= -1.5 && ratio <= -.75))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    static boolean isObstacle(char hexside)
    {
        return (hexside != ' ') && (hexside != 'r');
    }

    /** Check LOS, going to the left of hexspines if argument left is true, or
     *  to the right if it is false. */
    private boolean isLOSBlockedDir(BattleHex initialHex, BattleHex currentHex,
            BattleHex finalHex, boolean left, int strikeElevation,
            boolean strikerAtop, boolean strikerAtopCliff, boolean midObstacle,
            boolean midCliff, boolean midChit, int totalObstacles)
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

        char hexside = currentHex.getHexside(direction);
        char hexside2 = currentHex.getOppositeHexside(direction);

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
                if (hexside == 'w')
                {
                    // Down a wall -- blocked
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
                    // Down a wall -- blocked
                    return true;
                }
            }

            if (isObstacle(hexside2))
            {
                targetAtop = true;
                totalObstacles++;
                if (hexside == 'c')
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

            // If there are three slopes, striker and target must each
            //     be atop one.
            if (totalObstacles >= 3 && (!strikerAtop || !targetAtop) &&
                    (!strikerAtopCliff && !targetAtopCliff))
            {
                return true;
            }

            // Success!
            return false;
        }
        else   // not leaving first or entering last hex
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
        if (client.isOccupied(nextHex) && nextHex.getElevation() >=
                strikeElevation &&
                (!strikerAtopCliff || currentHex != initialHex))
        {
            midChit = true;
        }

        return isLOSBlockedDir(initialHex, nextHex, finalHex, left,
                strikeElevation, strikerAtop, strikerAtopCliff,
                midObstacle, midCliff, midChit, totalObstacles);
    }

    /** Check to see if the LOS from hex1 to hex2 is blocked.  If the LOS
     *  lies along a hexspine, check both and return true only if both are
     *  blocked. */
    private boolean isLOSBlocked(BattleHex hex1, BattleHex hex2)
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
        int strikeElevation = Math.min(hex1.getElevation(),
                hex2.getElevation());

        if (yDist == 0 || Math.abs(yDist) == 1.5 * Math.abs(xDist))
        {
            // Hexspine; try both sides.
            return (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                    false, false, false, false, false, 0) &&
                    isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                    false, false, false, false, false, 0));
        }
        else
        {
            return isLOSBlockedDir(hex1, hex1, hex2, toLeft(xDist, yDist),
                    strikeElevation, false, false, false, false, false, 0);
        }
    }

    /** Return true if the rangestrike is possible. */
    private boolean isRangestrikePossible(BattleChit chit, BattleChit target)
    {
        Creature creature = Creature.getCreatureByName(chit.getCreatureName());
        Creature targetCreature = Creature.getCreatureByName(
                target.getCreatureName());

        BattleHex currentHex = client.getBattleHex(chit);
        BattleHex targetHex = client.getBattleHex(target);

        if (currentHex.isEntrance() || targetHex.isEntrance())
        {
            return false;
        }

        int range = getRange(currentHex, targetHex, false);
        int skill = creature.getSkill();

        if (range > skill)
        {
            return false;
        }

        // Only magicMissile can rangestrike at range 2, rangestrike Lords,
        // or rangestrike without LOS.
        else if (!creature.useMagicMissile() && (range < 3 ||
                targetCreature.isLord() || isLOSBlocked(currentHex, targetHex)))
        {
            return false;
        }

        return true;
    }

    /** Return the hexside direction of the path from hex1 to hex2.
     *  Sometimes two directions are possible.  If the left parameter
     *  is set, the direction further left will be given.  Otherwise,
     *  the direction further right will be given. */
    private static int getDirection(BattleHex hex1, BattleHex hex2,
            boolean left)
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
            else if (yDist == xDistAndAHalf)
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
            else if (yDist == -xDistAndAHalf)
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
            else  // yDist == 0
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
        else  // xDist < 0
        {
            if (yDist < xDistAndAHalf)
            {
                return 0;
            }
            else if (yDist == xDistAndAHalf)
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
            else if (yDist == -xDistAndAHalf)
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
            else  // yDist == 0
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

    /** Return the number of intervening bramble hexes.  If LOS is along a
     *  hexspine, go left if argument left is true, right otherwise.  If
     *  LOS is blocked, return a large number. */
    private int countBrambleHexesDir(BattleHex hex1, BattleHex hex2,
            boolean left, int previousCount)
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

        // Add one if it's bramble.
        if (nextHex.getTerrain().equals("Brambles"))
        {
            count++;
        }

        return countBrambleHexesDir(nextHex, hex2, left, count);
    }

    // Return the number of intervening bramble hexes.  If LOS is along a
    // hexspine and there are two choices, pick the lower one.
    private int countBrambleHexes(BattleHex hex1, BattleHex hex2)
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

        if (yDist == 0 || Math.abs(yDist) == 1.5 * Math.abs(xDist))
        {
            int strikeElevation = Math.min(hex1.getElevation(),
                    hex2.getElevation());
            // Hexspine; try unblocked side(s).
            if (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                    false, false, false, false, false, 0))
            {
                return countBrambleHexesDir(hex1, hex2, false, 0);
            }
            else if (isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                    false, false, false, false, false, 0))
            {
                return countBrambleHexesDir(hex1, hex2, true, 0);
            }
            else
            {
                return Math.min(countBrambleHexesDir(hex1, hex2, true, 0),
                        countBrambleHexesDir(hex1, hex2, false, 0));
            }
        }
        else
        {
            return countBrambleHexesDir(hex1, hex2, toLeft(xDist, yDist), 0);
        }
    }

    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain. */
    int getDice(BattleChit chit, BattleChit target)
    {
        BattleHex hex = client.getBattleHex(chit);
        BattleHex targetHex = client.getBattleHex(target);
        Creature striker = Creature.getCreatureByName(chit.getCreatureName());

        int dice;
        if (striker.isTitan())
        {
            dice = chit.getTitanPower();
        }
        else
        {
            dice = striker.getPower();
        }

        boolean rangestrike = !client.isInContact(chit, true);
        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice /= 2;

            // volcanoNative rangestriking from volcano: +2
            if (striker.isNativeVolcano() && hex.getTerrain().equals("Volcano"))
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // volcanoNative striking from volcano: +2
            if (striker.isNativeVolcano() && hex.getTerrain().equals("Volcano"))
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = getDirection(hex, targetHex, false);
            char hexside = hex.getHexside(direction);

            // Native striking down a dune hexside: +2
            if (hexside == 'd' && striker.isNativeSandDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hexside == 's' && striker.isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (!striker.isNativeSandDune() &&
                    hex.getOppositeHexside(direction) == 'd')
            {
                dice--;
            }
        }

        return dice;
    }

    private int getAttackerSkill(BattleChit striker, BattleChit target)
    {
        BattleHex hex = client.getBattleHex(striker);
        BattleHex targetHex = client.getBattleHex(target);

        int attackerSkill = striker.getSkill();

        boolean rangestrike = !client.isInContact(striker, true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // Non-native striking out of bramble: -1
            if (hex.getTerrain().equals("Brambles") &&
                    !striker.getCreature().isNativeBramble())
            {
                attackerSkill--;
            }

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = getDirection(hex, targetHex, false);
                char hexside = hex.getHexside(direction);
                // Striking down across wall: +1
                if (hexside == 'w')
                {
                    attackerSkill++;
                }
            }
            else if (hex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = getDirection(targetHex, hex, false);
                char hexside = targetHex.getHexside(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hexside == 's' && !striker.getCreature().isNativeSlope()
                    ) || hexside == 'w')
                {
                    attackerSkill--;
                }
            }
        }
        else if (!striker.getCreature().useMagicMissile())
        {
            // Range penalty
            if (getRange(hex, targetHex, false) == 4)
            {
                attackerSkill--;
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!striker.getCreature().isNativeBramble())
            {
                attackerSkill -= countBrambleHexes(hex, targetHex);
            }

            // Rangestrike up across wall: -1 per wall
            if (targetHex.hasWall())
            {
                int heightDeficit = targetHex.getElevation() -
                        hex.getElevation();
                if (heightDeficit > 0)
                {
                    // Because of the design of the tower map, a strike to
                    // a higher tower hex always crosses one wall per
                    // elevation difference.
                    attackerSkill -= heightDeficit;
                }
            }

            // Rangestrike into volcano: -1
            if (targetHex.getTerrain().equals("Volcano"))
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }

    int getStrikeNumber(BattleChit striker, BattleChit target)
    {
        boolean rangestrike = !client.isInContact(striker, true);

        int attackerSkill = getAttackerSkill(striker, target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-magicMissile: +1
        if (client.getBattleHex(target).getTerrain().equals("Brambles") &&
                target.getCreature().isNativeBramble() &&
                !striker.getCreature().isNativeBramble() &&
                !(rangestrike && striker.getCreature().useMagicMissile()))
        {
            strikeNumber++;
        }

        // Native defending in stone, from strike by a non-native: +1
        // Native defending in stone, from rangestrike by a non-native
        //     non-magicMissile: +1
        if (client.getBattleHex(target).getTerrain().equals("Stone") &&
                target.getCreature().isNativeStone() &&
                !striker.getCreature().isNativeStone() &&
                !(rangestrike && striker.getCreature().useMagicMissile()))
        {
            strikeNumber++;
        }

        // Native defending in tree, from strike by a non-native: +1
        // Native defending in tree, from rangestrike by a non-native
        //     non-magicMissile: no effect
        if (client.getBattleHex(target).getTerrain().equals("Tree") &&
                target.getCreature().isNativeTree() &&
                !striker.getCreature().isNativeTree() &&
                !(rangestrike))
        {
            strikeNumber++;
        }

        // Sixes always hit.
        if (strikeNumber > 6)
        {
            strikeNumber = 6;
        }

        return strikeNumber;
    }
}
