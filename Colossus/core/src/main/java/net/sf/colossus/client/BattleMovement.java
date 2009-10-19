package net.sf.colossus.client;


import java.util.HashSet;
import java.util.Set;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Game;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * Class BattleMovement does battle move calculations;
 * originated from client side;
 *
 * TODO Move up to become the common one, and use the methods here
 * instead of the pendants in BattleServerSide.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */

// XXX Massively duplicated code.  Merge later.
final class BattleMovement
{
    private final Game game;

    private final IOptions options;

    BattleMovement(Game game, IOptions options)
    {
        this.game = game;
        this.options = options;
    }

    /** Recursively find moves from this hex.  Return a set of all
     * legal destinations.  Do not double back.  */
    private Set<BattleHex> findMoves(BattleHex hex, CreatureType creature,
        boolean flies, int movesLeft, int cameFrom, boolean first)
    {
        final boolean cumulativeSlow = options.getOption(Options.cumulativeSlow);
        final boolean oneHexAllowed = options.getOption(Options.oneHexAllowed);

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

                    BattleCritter bogey = game.getBattle().getBattleUnit(
                        neighbor);
                    if (bogey == null)
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
    private Set<BattleHex> findUnoccupiedStartlistHexes()
    {
        MasterBoardTerrain terrain = game.getBattleSite().getTerrain();
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
        final Battle battle = game.getBattle();
        Set<BattleHex> set = new HashSet<BattleHex>();
        if (!critter.hasMoved() && !battle.isInContact(critter, false))
        {
            if (game.getBattleSite().getTerrain().hasStartList()
                && (battle.getBattleTurnNumber() == 1)
                && battle.getBattleActiveLegion().equals(
                        battle.getDefendingLegion()))
            {
                set = findUnoccupiedStartlistHexes();
            }
            else
            {
                CreatureType creature = critter.getCreatureType();
                BattleHex hex = critter.getCurrentHex();
                set = findMoves(hex, creature, creature.isFlier(), creature
                    .getSkill(), -1, true);
            }
        }
        return set;
    }
}
