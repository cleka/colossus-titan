package net.sf.colossus.client;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.util.CompareDoubles;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;


/**
 * Class Strike holds client-side strike logic.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */

// XXX Massive code duplication.
public final class Strike
{
    private static final Logger LOGGER = Logger.getLogger(Strike.class
        .getName());

    private final Client client;

    Strike(Client client)
    {
        this.client = client;
    }

    /** Return the set of hexes with critters that have
     *  valid strike targets. */
    Set<BattleHex> findCrittersWithTargets()
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (BattleCritter battleUnit : client.getActiveBattleUnits())
        {
            if (countStrikes(battleUnit, true) > 0)
            {
                set.add(battleUnit.getCurrentHex());
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
        if (client.getBattlePhase() == null)
        {
            LOGGER.log(Level.SEVERE,
                "Called Strike.makeForcedStrikes() when there is no battle");
            return false;
        }
        else if (!client.getBattlePhase().isFightPhase()
            && !client.isMyBattlePhase())
        {
            LOGGER.log(Level.SEVERE,
                "Called Strike.makeForcedStrikes() in wrong phase");
            return false;
        }

        for (BattleCritter battleUnit : client.getActiveBattleUnits())
        {
            if (!battleUnit.hasStruck())
            {
                Set<BattleHex> set = findStrikes(battleUnit, rangestrike);
                if (set.size() == 1)
                {
                    BattleHex hex = set.iterator().next();
                    client.strike(battleUnit.getTag(), hex);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canStrike(BattleCritter striker, BattleCritter target)
    {
        BattleHex targetHex = target.getCurrentHex();
        return findStrikes(striker, true).contains(targetHex);
    }

    // TODO move all this strike stuff to BattleClientSide / Battle
    Set<BattleHex> findStrikes(int tag)
    {
        BattleCritter battleUnit = client.getBattle().getBattleUnit(tag);
        return findStrikes(battleUnit, true);
    }

    /** Return a set of hexes containing targets that the
     *  critter may strike.  Only include rangestrikes if rangestrike
     *  is true. */
    private Set<BattleHex> findStrikes(BattleCritter battleUnit,
        boolean rangestrike)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();

        // Each creature may strike only once per turn.
        if (battleUnit.hasStruck())
        {
            return set;
        }
        // Offboard creatures can't strike.
        if (battleUnit.getCurrentHex().getLabel().startsWith("X"))
        {
            return set;
        }

        boolean inverted = battleUnit.isDefender();
        BattleHex currentHex = battleUnit.getCurrentHex();

        boolean adjacentEnemy = false;

        // First mark and count normal strikes.
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not engaged.
            if (!currentHex.isCliff(i))
            {
                BattleHex targetHex = currentHex.getNeighbor(i);
                if (targetHex != null && client.isOccupied(targetHex)
                    && !targetHex.isEntrance())
                {
                    BattleCritter target = client.getBattle().getBattleUnit(
                        targetHex);
                    if (target.isDefender() != inverted)
                    {
                        adjacentEnemy = true;
                        if (!target.isDead())
                        {
                            set.add(targetHex);
                        }
                    }
                }
            }
        }

        CreatureType creature = battleUnit.getCreatureType();

        // Then do rangestrikes if applicable.  Rangestrikes are not allowed
        // if the creature can strike normally, so only look for them if
        // no targets have yet been found.
        if (rangestrike && !adjacentEnemy && creature.isRangestriker()
            && client.getBattlePhase() != BattlePhase.STRIKEBACK)
        {
            for (BattleCritter target : client.getInactiveBattleUnits())
            {
                if (!target.isDead())
                {
                    BattleHex targetHex = target.getCurrentHex();
                    if (isRangestrikePossible(battleUnit, target))
                    {
                        set.add(targetHex);
                    }
                }
            }
        }
        return set;
    }

    private int countStrikes(BattleCritter battleUnit, boolean rangestrike)
    {
        return findStrikes(battleUnit, rangestrike).size();
    }

    /** Return the titan range (inclusive at both ends) from the critter to the
     *  closest enemy critter.  Return OUT_OF_RANGE if there are none.
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike
     */
    @Deprecated
    public int minRangeToEnemy(BattleCritter battleUnit)
    {
        BattleHex hex = battleUnit.getCurrentHex();
        int min = Constants.OUT_OF_RANGE;

        for (BattleCritter target : client.getBattle().getBattleUnits())
        {
            if (battleUnit.isDefender() != target.isDefender())
            {
                BattleHex targetHex = target.getCurrentHex();
                int range = Battle.getRange(hex, targetHex, false);
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

    /**
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike
     */
    @Deprecated
    private static boolean toLeft(double xDist, double yDist)
    {
        double ratio = xDist / yDist;
        if (ratio >= 1.5 || (ratio >= 0 && ratio <= .75)
            || (ratio >= -1.5 && ratio <= -.75))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike
     * @deprecated This is the realm of HazardEdge, not direct use of hexside
     * */
    @Deprecated
    static boolean isObstacle(char hexside)
    {
        return (hexside != ' ') && (hexside != 'r');
    }

    /** Check LOS, going to the left of hexspines if argument left is true, or
     *  to the right if it is false.
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike
     */
    @Deprecated
    private boolean isLOSBlockedDir(BattleHex initialHex,
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
                if (hexside == 'c' || hexside == 'd')
                {
                    strikerAtopCliff = true;
                }
            }

            if (isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside2 == 'c' || hexside2 == 'd')
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
                if (hexside == 'c' || hexside == 'd')
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
                if (hexside2 == 'c' || hexside2 == 'd')
                {
                    targetAtopCliff = true;
                }
            }

            if (midChit && !targetAtopCliff)
            {
                return true;
            }

            if (midCliff && (!strikerAtopCliff || !targetAtopCliff))
            {
                return true;
            }

            if (midObstacle && !strikerAtop && !targetAtop)
            {
                return true;
            }

            // If there are three slopes, striker and target must each
            //     be atop one.
            if (totalObstacles >= 3 && (!strikerAtop || !targetAtop)
                && (!strikerAtopCliff && !targetAtopCliff))
            {
                return true;
            }

            // Success!
            return false;
        }
        else
        // not leaving first or entering last hex
        {
            if (midChit)
            {
                // We're not in the initial or final hex, and we have already
                // marked an mid battleChit, so it's not adjacent to the base of a
                // cliff that the target is atop.
                return true;
            }

            if (isObstacle(hexside) || isObstacle(hexside2))
            {
                midObstacle = true;
                totalObstacles++;
                if (hexside == 'c' || hexside2 == 'c' || hexside == 'd' ||
                    hexside2 == 'd')
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
        if (client.isOccupied(nextHex)
            && nextHex.getElevation() >= strikeElevation
            && (!strikerAtopCliff || currentHex != initialHex))
        {
            midChit = true;
        }

        return isLOSBlockedDir(initialHex, nextHex, finalHex, left,
            strikeElevation, strikerAtop, strikerAtopCliff, midObstacle,
            midCliff, midChit, totalObstacles);
    }

    /** Check to see if the LOS from hex1 to hex2 is blocked.  If the LOS
     *  lies along a hexspine, check both and return true only if both are
     *  blocked.
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike
     */
    @Deprecated
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
        int strikeElevation = Math.min(hex1.getElevation(), hex2
            .getElevation());

        if (CompareDoubles.almostEqual(yDist, 0.0)
            || CompareDoubles.almostEqual(Math.abs(yDist), 1.5 * Math
                .abs(xDist)))
        {
            // Hexspine; try both sides.
            return (isLOSBlockedDir(hex1, hex1, hex2, true, strikeElevation,
                false, false, false, false, false, 0) && isLOSBlockedDir(hex1,
                hex1, hex2, false, strikeElevation, false, false, false,
                false, false, 0));
        }
        else
        {
            return isLOSBlockedDir(hex1, hex1, hex2, toLeft(xDist, yDist),
                strikeElevation, false, false, false, false, false, 0);
        }
    }

    /** Return true if the rangestrike is possible. */
    /*
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike, with
     *   extension of Creature instead of BattleCritter and extra BattleHex
     */
    @Deprecated
    private boolean isRangestrikePossible(BattleCritter striker,
        BattleCritter target)
    {
        CreatureType creature = striker.getCreatureType();
        CreatureType targetCreature = target.getCreatureType();

        BattleHex currentHex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        if (currentHex.isEntrance() || targetHex.isEntrance())
        {
            return false;
        }

        int range = Battle.getRange(currentHex, targetHex, false);
        int skill = creature.getSkill();

        if (range > skill)
        {
            return false;
        }

        // Only magicMissile can rangestrike at range 2, rangestrike Lords,
        // or rangestrike without LOS.
        else if (!creature.useMagicMissile()
            && (range < 3 || targetCreature.isLord() || isLOSBlocked(
                currentHex, targetHex)))
        {
            return false;
        }

        return true;
    }

    /** Return the hexside direction of the path from hex1 to hex2.
     *  Sometimes two directions are possible.  If the left parameter
     *  is set, the direction further left will be given.  Otherwise,
     *  the direction further right will be given.
     * WARNING: this is a duplication from code in Battle ; caller should use
     * a Battle instance instead.
     * @deprecated Should use an extension of Battle instead of Strike
     */
    @Deprecated
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
            // yDist == 0
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
        // xDist < 0
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
            // yDist == 0
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
     *  LOS is blocked, return a large number.
     * @deprecated another function with explicit reference to Bramble
     * that should be fixed.
     */
    @Deprecated
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
        if (nextHex.getTerrain().equals(HazardTerrain.BRAMBLES))
        {
            count++;
        }

        return countBrambleHexesDir(nextHex, hex2, left, count);
    }

    /** Return the number of intervening bramble hexes.  If LOS is along a
     * hexspine and there are two choices, pick the lower one.
     * @deprecated another function with explicit reference to Bramble
     * that should be fixed.
     */
    @Deprecated
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
        if (CompareDoubles.almostEqual(x1, -1.0)
            || CompareDoubles.almostEqual(x2, -1))
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

    // TODO : duplicate with Server Side??
    // This calculcates the penalties etc for client side, and we had a bug
    // [2028230] claiming strike penalties were not applied.
    // For the hit result it was applied right, but the preview (here,
    // client side) was missing something. ==> should use same code as
    // on server side.

    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain.
     * WARNING: this is duplicated in CreatureServerSide
     */
    public int getDice(BattleCritter battleUnit, BattleCritter target)
    {
        BattleHex hex = battleUnit.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();
        CreatureType striker = battleUnit.getCreatureType();

        int dice;
        if (striker.isTitan())
        {
            dice = battleUnit.getTitanPower();
        }
        else
        {
            dice = striker.getPower();
        }

        boolean rangestrike = !client.isInContact(battleUnit, true);
        HazardTerrain terrain = hex.getTerrain();
        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice /= 2;

            // volcanoNative rangestriking from volcano: +2
            if (terrain.equals(HazardTerrain.VOLCANO)
                && striker.isNativeIn(terrain))
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // volcanoNative striking from volcano: +2
            if (terrain.equals(HazardTerrain.VOLCANO)
                && striker.isNativeIn(terrain))
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = getDirection(hex, targetHex, false);
            HazardHexside hazard = hex.getHexsideHazard(direction);

            // Native striking down a dune hexside: +2
            if (hazard == HazardHexside.DUNE && striker.isNativeDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hazard == HazardHexside.SLOPE && striker.isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (!striker.isNativeDune()
                && hex.getOppositeHazard(direction) == HazardHexside.DUNE)
            {
                dice--;
            }
        }

        return dice;
    }

    /** WARNING: this is duplicated in CreatureServerSide */
    private int getAttackerSkill(BattleCritter striker, BattleCritter target)
    {
        BattleHex hex = striker.getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = striker.getSkill();

        boolean rangestrike = !client.isInContact(striker, true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            HazardTerrain terrain = hex.getTerrain();
            // striking out of possible hazard
            attackerSkill -= hex.getTerrain().getSkillPenaltyStrikeFrom(
                striker.getCreatureType().isNativeIn(terrain),
                target.getCreatureType().isNativeIn(terrain));

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = getDirection(hex, targetHex, false);
                // TODO the hexside should be called WALL...
                // Striking down across wall: +1
                if (hex.getHexsideHazard(direction) == HazardHexside.TOWER)
                {
                    attackerSkill++;
                }
            }
            else if (hex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = getDirection(targetHex, hex, false);
                HazardHexside hazard = targetHex.getHexsideHazard(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                // TODO Tower vs. Wall ...
                if ((hazard == HazardHexside.SLOPE && !striker
                    .getCreatureType().isNativeSlope())
                    || hazard == HazardHexside.TOWER)
                {
                    attackerSkill--;
                }
            }
        }
        else if (!striker.getCreatureType().useMagicMissile())
        {
            // Range penalty
            int range = Battle.getRange(hex, targetHex, false);
            if (range >= 4)
            {
                attackerSkill -= (range - 3);
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!striker.getCreatureType().isNativeIn(HazardTerrain.BRAMBLES))
            {
                attackerSkill -= countBrambleHexes(hex, targetHex);
            }

            // Rangestrike up across wall: -1 per wall
            if (targetHex.hasWall())
            {
                int heightDeficit = targetHex.getElevation()
                    - hex.getElevation();
                if (heightDeficit > 0)
                {
                    // Because of the design of the tower map, a strike to
                    // a higher tower hex always crosses one wall per
                    // elevation difference.
                    attackerSkill -= heightDeficit;
                }
            }

            // Rangestrike into volcano: -1
            if (targetHex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }

    /** WARNING: this is duplicated in CreatureServerSide */
    public int getStrikeNumber(BattleCritter striker, BattleCritter target)
    {
        boolean rangestrike = !client.isInContact(striker, true);

        int attackerSkill = getAttackerSkill(striker, target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        HazardTerrain terrain = target.getCurrentHex().getTerrain();

        if (!rangestrike)
        {
            // Strike number can be modified directly by terrain.
            strikeNumber += terrain.getSkillBonusStruckIn(striker
                .getCreatureType().isNativeIn(terrain), target
                .getCreatureType().isNativeIn(terrain));
        }
        else
        {
            // Native defending in bramble, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.BRAMBLES)
                && target.getCreatureType().isNativeIn(HazardTerrain.BRAMBLES)
                && !striker.getCreatureType().isNativeIn(
                    HazardTerrain.BRAMBLES)
                && !striker.getCreatureType().useMagicMissile())
            {
                strikeNumber++;
            }

            // Native defending in stone, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.STONE)
                && target.getCreatureType().isNativeIn(HazardTerrain.STONE)
                && !striker.getCreatureType().isNativeIn(HazardTerrain.STONE)
                && !striker.getCreatureType().useMagicMissile())
            {
                strikeNumber++;
            }
        }

        // Sixes always hit.
        if (strikeNumber > 6)
        {
            strikeNumber = 6;
        }

        return strikeNumber;
    }
}
