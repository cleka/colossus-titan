package net.sf.colossus.server;


import java.util.HashSet;
import java.util.Set;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Game;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 *  This holds currently the BattleMovement related methods that has
 *  been so far part of BattleServerSide.
 *
 *  TODO Further clean up, and unify with client.BattleMovement, and
 *  eventually move up to game.
 *
 *  The client side version is in better shape, but some of the methods
 *  it uses from "game" package classes are abstract in game package and in
 *  server package classes they are only dummies!
 *
 * @author David Ripton (BattleServerSide)
 * @author Romain Dolbeau (BattleServerSide)
 * @author Clemens Katzer
 */
public class BattleMovementServerSide
{
    private final Game game;

    // TODO instead listener to the option changes
    final boolean cumulativeSlow;
    final boolean oneHexAllowed;

    BattleMovementServerSide(IOptions options, Game game)
    {
        this.game = game;

        cumulativeSlow = options.getOption(Options.cumulativeSlow);
        oneHexAllowed = options.getOption(Options.oneHexAllowed);
    }

    /** Recursively find moves from this hex.  Return a set of string hex IDs
     *  for all legal destinations.  Do not double back.  If ignoreMobileAllies
     *  is true, pretend that allied creatures that can move out of the
     *  way are not there. */
    private Set<BattleHex> findMoves(BattleHex hex,
        CreatureServerSide critter, boolean flies, int movesLeft,
        int cameFrom, boolean ignoreMobileAllies, boolean first)
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

                    CreatureServerSide bogey = getBattleSS().getCreatureSS(
                        neighbor);
                    if (bogey == null
                        || (ignoreMobileAllies
                            && bogey.getMarkerId().equals(
                                critter.getMarkerId()) && !getBattleSS()
                            .isInContact(bogey, false)))
                    {
                        entryCost = neighbor.getEntryCost(critter.getType(),
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
                            set.addAll(findMoves(neighbor, critter, flies,
                                movesLeft - entryCost, reverseDir,
                                ignoreMobileAllies, false));
                        }
                    }

                    // Fliers can fly over any hex for 1 movement point,
                    // but some Hex cannot be flown over by some creatures.
                    if (flies && movesLeft > 1
                        && neighbor.canBeFlownOverBy(critter.getType()))
                    {
                        set.addAll(findMoves(neighbor, critter, flies,
                            movesLeft - 1, reverseDir, ignoreMobileAllies,
                            false));
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
        boolean ignoreMobileAllies, MasterBoardTerrain terrain)
    {
        assert terrain != null;
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (String hexLabel : terrain.getStartList())
        {
            BattleHex hex = terrain.getHexByLabel(hexLabel);
            if (ignoreMobileAllies || !getBattleSS().isOccupied(hex))
            {
                set.add(hex);
            }
        }
        return set;
    }

    /**
     * Find all legal moves for this critter.
     */
    public Set<BattleHex> showMoves(CreatureServerSide critter,
        boolean ignoreMobileAllies)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        if (!critter.hasMoved() && !getBattleSS().isInContact(critter, false))
        {
            if (getBattleSS().getLocation().getTerrain().hasStartList()
                && (getBattleSS().getBattleTurnNumber() == 1)
                && getBattleSS().isDefenderActive())
            {
                set = findUnoccupiedStartlistHexes(ignoreMobileAllies,
                    getBattleSS().getLocation().getTerrain());
            }
            else
            {
                set = findMoves(critter.getCurrentHex(), critter,
                    critter.isFlier(),
                    critter.getSkill() - critter.getSlowed(), -1,
                    ignoreMobileAllies, true);
            }
        }
        return set;
    }

    BattleServerSide getBattleSS()
    {
        return ((GameServerSide)game).getBattleSS();
    }
}
