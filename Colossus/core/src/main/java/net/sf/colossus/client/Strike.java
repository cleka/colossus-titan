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
        boolean strikerAtopWall, boolean midObstacle, boolean midCliff,
        boolean midChit, int totalObstacles, int totalWalls)
    {
        boolean targetAtop = false;
        boolean targetAtopCliff = false;
        boolean targetAtopWall = false;
        if (currentHex == finalHex)
        {
            return false;
        }
        // Offboard hexes are not allowed.
        if (currentHex.getXCoord() == -1 || finalHex.getXCoord() == -1)
        {
            return true;
        }
        int direction = BattleClientSide.getDirection(currentHex, finalHex,
            left);
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
                else if (hexside == 'w')
                {
                    strikerAtopWall = true;
                    totalWalls++;
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
                else if (hexside == 'w')
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
                if (hexside == 'c' || hexside == 'd')
                {
                    midCliff = true;
                }
                else if (hexside == 'w')
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
                else if (hexside == 'w')
                {
                    totalWalls++;
                    targetAtopWall = true;
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
            if (totalWalls >= 2)
            {
                if (!(strikerAtopWall || targetAtopWall))
                {
                    return true;
                }
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
                // marked a mid chit, so it's not adjacent to the base of a
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
            strikeElevation, strikerAtop, strikerAtopCliff, strikerAtopWall,
            midObstacle, midCliff, midChit, totalObstacles, totalWalls);
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
                false, false, false, false, false, false, 0, 0) &&
                isLOSBlockedDir(hex1, hex1, hex2, false, strikeElevation,
                false, false, false, false, false, false, 0, 0));
        }
        else
        {
            return isLOSBlockedDir(hex1, hex1, hex2, Battle.toLeft(
                xDist, yDist), strikeElevation, false, false, false, false,
                false, false, 0, 0);
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

}
