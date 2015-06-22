package net.sf.colossus.client;


import java.util.HashSet;
import java.util.Set;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Game;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * Class BattleMovement does client-side battle move calculations.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */

// XXX Massively duplicated code.  Merge later.
// YYY Move this up to become game.BattleMovement, and use it on server side
// as well, instead of the methods in BattleServerSide itself.
final class BattleMovement
{
    private final Game game;

    // TODO instead listener to the option changes
    // WARNING Option changes from server appear here as string-based,
    //         so a listener based on the boolean option would not work.
    //         For now, we just make sure we create BattleMovement after
    //         all server-to-client-option-sync'ing is completed.
    final boolean cumulativeSlow;
    final boolean oneHexAllowed;

    BattleMovement(Game game, IOptions options)
    {
        this.game = game;

        cumulativeSlow = options.getOption(Options.cumulativeSlow);
        oneHexAllowed = options.getOption(Options.oneHexAllowed);
    }

    /** Recursively find moves from this hex.  Return a set of all
     * legal destinations.  Do not double back.  */
    private Set<BattleHex> findMoves(BattleHex hex, CreatureType creature,
        boolean flies, int movesLeft, int cameFrom, boolean first)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (int i = 0; i < 6; i++)
        {
            // Do not double back.
            if (i != cameFrom)
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    int reverseDir = (i + 3) % 6;
                    int entryCost;

                    if (!game.getBattle().isOccupied(neighbor))
                    {
                        entryCost = neighbor.getEntryCost(creature,
                            reverseDir, cumulativeSlow);
                    }
                    else
                    {
                        entryCost = BattleHex.IMPASSIBLE_COST;
                    }

                    if ((entryCost != BattleHex.IMPASSIBLE_COST)
                        && ((entryCost <= movesLeft) || (first && oneHexAllowed)))
                    {
                        // Mark that hex as a legal move.
                        set.add(neighbor);

                        // If there are movement points remaining, continue
                        // checking moves from there.  Fliers skip this
                        // because flying is more efficient.
                        if (!flies && movesLeft > entryCost)
                        {
                            set.addAll(findMoves(neighbor, creature, flies,
                                movesLeft - entryCost, reverseDir, false));
                        }
                    }

                    // Fliers can fly over any hex for 1 movement point,
                    // but some Hex cannot be flown over by some creatures.
                    if (flies && movesLeft > 1
                        && neighbor.canBeFlownOverBy(creature))
                    {
                        set.addAll(findMoves(neighbor, creature, flies,
                            movesLeft - 1, reverseDir, false));
                    }
                }
            }
        }
        return set;
    }

    /** This method is called by the defender on turn 1 in a
     *  Startlisted Terrain,
     *  so we know that there are no enemies on board, and all allies
     *  are mobile.
     */
    private Set<BattleHex> findUnoccupiedStartlistHexes(
        MasterBoardTerrain terrain)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (String hexLabel : terrain.getStartList())
        {
            BattleHex hex = terrain.getHexByLabel(hexLabel);
            if (!game.getBattle().isOccupied(hex))
            {
                set.add(hex);
            }
        }
        return set;
    }

    /** Find all legal moves for this critter.*/
    public Set<BattleHex> showMoves(BattleCritter critter)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        if (!critter.hasMoved()
            && !game.getBattle().isInContact(critter, false))
        {
            MasterBoardTerrain terrain = game.getBattleSite().getTerrain();
            if (terrain.hasStartList()
                && game.getBattleTurnNumber() == 1
                && game.getBattleActiveLegion().equals(
                    game.getBattle().getDefendingLegion()))
            {
                set = findUnoccupiedStartlistHexes(terrain);
            }
            else
            {
                CreatureType type = critter.getType();
                BattleHex hex = critter.getCurrentHex();
                set = findMoves(hex, type, type.isFlier(), type.getSkill()
                    - critter.getSlowed(), -1, true);
            }
        }
        return set;
    }

}
