package net.sf.colossus.client;


import java.util.HashSet;
import java.util.Set;

import net.sf.colossus.server.BattleServerSide;
import net.sf.colossus.util.Options;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * Class BattleMovement does client-side battle move calculations.
 *
 * @version $Id$
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

    /** Recursively find moves from this hex.  Return an array of hex IDs for
     *  all legal destinations.  Do not double back.  */
    private Set<String> findMoves(BattleHex hex, CreatureType creature,
        boolean flies, int movesLeft, int cameFrom, boolean first)
    {
        Set<String> set = new HashSet<String>();
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

                    BattleChit bogey = client.getBattleChit(neighbor
                        .getLabel());
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
                        set.add(neighbor.getLabel());

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
     *  
     * TODO same as {@link BattleServerSide#findUnoccupiedStartlistHexes()}
     */
    private Set<String> findUnoccupiedStartlistHexes()
    {
        MasterBoardTerrain terrain = client.getBattleSite().getTerrain();
        Set<String> set = new HashSet<String>();
        for (String hexLabel : terrain.getStartList())
        {
            BattleHex hex = HexMap.getHexByLabel(terrain, hexLabel);
            if (!isOccupied(hexLabel))
            {
                set.add(hex.getLabel());
            }
        }
        return set;
    }

    private boolean isOccupied(String hexLabel)
    {
        return !client.getBattleChits(hexLabel).isEmpty();
    }

    Set<String> showMoves(int tag)
    {
        BattleChit chit = client.getBattleChit(tag);
        return showMoves(chit);
    }

    /** Find all legal moves for this critter. The returned list
     *  contains hex IDs, not hexes. */
    Set<String> showMoves(BattleChit chit)
    {
        Set<String> set = new HashSet<String>();
        if (!chit.hasMoved() && !client.isInContact(chit, false))
        {
            if (client.getBattleSite().getTerrain().hasStartList()
                && (client.getBattleTurnNumber() == 1)
                && client.getBattleActiveLegion().equals(client.getDefender()))
            {
                set = findUnoccupiedStartlistHexes();
            }
            else
            {
                CreatureType creature = client.getGame().getVariant()
                    .getCreatureByName(chit.getCreatureName());
                BattleHex hex = client.getBattleHex(chit);
                set = findMoves(hex, creature, creature.isFlier(), creature
                    .getSkill(), -1, true);
            }
        }
        return set;
    }
}
