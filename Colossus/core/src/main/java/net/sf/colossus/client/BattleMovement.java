package net.sf.colossus.client;


import java.util.HashSet;
import java.util.Set;

import net.sf.colossus.common.Options;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.gui.BattleUnit;
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
final class BattleMovement
{
    private final Client client;

    BattleMovement(Client client)
    {
        this.client = client;
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

                    BattleCritter bogey = client.getBattleUnit(neighbor);
                    if (bogey == null)
                    {
                        entryCost = neighbor.getEntryCost(creature,
                            reverseDir, client.getOptions().getOption(
                                Options.cumulativeSlow));
                    }
                    else
                    {
                        entryCost = BattleHex.IMPASSIBLE_COST;
                    }

                    if ((entryCost != BattleHex.IMPASSIBLE_COST)
                        && ((entryCost <= movesLeft) || (first && client
                            .getOptions().getOption(Options.oneHexAllowed))))
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
        MasterBoardTerrain terrain = client.getBattleSite().getTerrain();
        Set<BattleHex> set = new HashSet<BattleHex>();
        for (String hexLabel : terrain.getStartList())
        {
            BattleHex hex = HexMap.getHexByLabel(terrain, hexLabel);
            if (!isOccupied(hex))
            {
                set.add(hex);
            }
        }
        return set;
    }

    private boolean isOccupied(BattleHex hex)
    {
        return !client.getBattleUnits(hex).isEmpty();
    }

    Set<BattleHex> showMoves(int tag)
    {
        BattleCritter battleUnit = client.getBattleUnit(tag);
        return showMoves(battleUnit);
    }

    /** Find all legal moves for this critter.*/
    private Set<BattleHex> showMoves(BattleCritter battleUnit)
    {
        Set<BattleHex> set = new HashSet<BattleHex>();
        if (!battleUnit.hasMoved() && !client.isInContact(battleUnit, false))
        {
            if (client.getBattleSite().getTerrain().hasStartList()
                && (client.getBattleTurnNumber() == 1)
                && client.getBattleActiveLegion().equals(client.getDefender()))
            {
                set = findUnoccupiedStartlistHexes();
            }
            else
            {
                CreatureType creature = battleUnit.getCreatureType();
                BattleHex hex = battleUnit.getCurrentHex();
                set = findMoves(hex, creature, creature.isFlier(), creature
                    .getSkill(), -1, true);
            }
        }
        return set;
    }
}
